using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Globalization;
using System.Runtime.InteropServices;
using System.Speech.Recognition;
using System.Threading;

namespace CapriDesktop
{
    [StructLayout(LayoutKind.Sequential)]
    internal struct WaveFormat
    {
        internal short Tag, Channels;
        internal int SampleRate, BytesPerSecond;
        internal short Align, Bits, Extra;

        internal static WaveFormat Mono(int rate) { return new WaveFormat { Tag = 1, Channels = 1, SampleRate = rate, BytesPerSecond = rate * 2, Align = 2, Bits = 16 }; }
    }

    [StructLayout(LayoutKind.Sequential)]
    internal struct WaveHeader
    {
        internal IntPtr Data;
        internal int Length, Recorded;
        internal IntPtr User;
        internal int Flags, Loops;
        internal IntPtr Next, Reserved;
    }

    internal sealed class Ears : IDisposable
    {
        internal const int Rate = 16000;
        internal const int Chunk = Rate / 10;
        private const int Buffers = 4;
        internal event Action<short[]> Heard;
        internal event Action<double> Level;
        internal event Action<string> Recognized;
        internal event Action<bool> Speaking;
        internal volatile bool Muted;
        internal volatile bool SuppressWhileSpeaking;
        private IntPtr device;
        private IntPtr[] headers;
        private Thread worker;
        private AutoResetEvent signal;
        private volatile bool running;
        private SpeechRecognitionEngine offlineEngine;
        private volatile bool offlineRunning;
        private volatile bool vadFallback;
        internal string SttLanguage = "en";
        internal double MicGateThreshold = 0.010;
        private int vadSpeechChunks;
        private int vadQuietChunks;
        private double ambientNoise = 0.005;
        private bool userSpeaking;
        private readonly List<short> vadBuffer = new List<short>();
        private readonly Queue<short[]> preSpeechRing = new Queue<short[]>();
        private readonly List<short[]> candidateSpeech = new List<short[]>();
        private const int PreSpeechMaxChunks = 6;

        [DllImport("winmm.dll")] private static extern int waveInOpen(out IntPtr device, int id, ref WaveFormat format, IntPtr callback, IntPtr instance, int flags);
        [DllImport("winmm.dll")] private static extern int waveInPrepareHeader(IntPtr device, IntPtr header, int size);
        [DllImport("winmm.dll")] private static extern int waveInUnprepareHeader(IntPtr device, IntPtr header, int size);
        [DllImport("winmm.dll")] private static extern int waveInAddBuffer(IntPtr device, IntPtr header, int size);
        [DllImport("winmm.dll")] private static extern int waveInStart(IntPtr device);
        [DllImport("winmm.dll")] private static extern int waveInReset(IntPtr device);
        [DllImport("winmm.dll")] private static extern int waveInClose(IntPtr device);

        internal bool Listening { get { return running || offlineRunning; } }
        internal bool OfflineListening { get { return offlineRunning; } }

        private static SpeechRecognitionEngine CreateDirectEngine()
        {
            try
            {
                var installed = SpeechRecognitionEngine.InstalledRecognizers();
                if (installed != null && installed.Count > 0)
                {
                    RecognizerInfo chosen = installed[0];
                    foreach (RecognizerInfo info in installed)
                    {
                        if (info != null && info.Culture != null && info.Culture.TwoLetterISOLanguageName == "en")
                        {
                            chosen = info;
                            break;
                        }
                    }
                    if (chosen != null) return new SpeechRecognitionEngine(chosen.Id);
                }
            }
            catch { }

            // Direct token reflection binding for Windows 11 systems where root Recognizers category lacks default attributes
            string[] tokenPaths = new[]
            {
                @"HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Speech\Recognizers\Tokens\MS-1033-80-DESK",
                @"HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Speech\Recognizers\Tokens\MS-2057-80-DESK",
                @"HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Speech_OneCore\Recognizers\Tokens\MS-1033-110-WINMO-DNN"
            };
            var asm = typeof(RecognizerInfo).Assembly;
            Type otType = asm.GetType("System.Speech.Internal.ObjectTokens.ObjectToken");
            if (otType == null) return null;
            var openMethod = otType.GetMethod("Open", System.Reflection.BindingFlags.Public | System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Static, null, new Type[] { typeof(string), typeof(string), typeof(bool) }, null);
            var createMethod = typeof(RecognizerInfo).GetMethod("Create", System.Reflection.BindingFlags.Public | System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Static);
            if (openMethod == null || createMethod == null) return null;

            foreach (string path in tokenPaths)
            {
                try
                {
                    object tok = openMethod.Invoke(null, new object[] { null, path, false });
                    if (tok == null) continue;
                    RecognizerInfo ri = createMethod.Invoke(null, new object[] { tok }) as RecognizerInfo;
                    if (ri != null) return new SpeechRecognitionEngine(ri);
                }
                catch { }
            }
            return null;
        }

        private static void LoadCapriGrammars(SpeechRecognitionEngine engine)
        {
            try
            {
                Choices phrases = new Choices(
                    "Search mode",
                    "Search for Cyberpunk Edgerunners",
                    "Open YouTube",
                    "Open Google",
                    "What is on my screen",
                    "What am I looking at",
                    "Look at my screen",
                    "Call me a good boy",
                    "Am I a good boy",
                    "Good boy",
                    "Thank you Capri",
                    "Thanks Capri",
                    "Hello Capri",
                    "Hey Capri",
                    "Yo Capri",
                    "Hi Capri",
                    "Why did you open images of cats",
                    "Why did you open cats",
                    "Open images of cats",
                    "No I am not relax",
                    "Relax",
                    "Calm down",
                    "Awesome",
                    "Hey I'm sorry that I reset your memory",
                    "Sorry I reset your memory",
                    "Sometimes you get out of hand",
                    "You get out of hand",
                    "Reset memory",
                    "Next song",
                    "Change the song",
                    "Previous song",
                    "Pause music",
                    "Play music",
                    "Volume up",
                    "Volume down",
                    "Mute audio",
                    "Take a screenshot",
                    "Scroll down",
                    "Scroll up",
                    "What are you doing",
                    "Who are you",
                    "Tell me about Project Capricorn",
                    "Dormant mode",
                    "Wake up",
                    "Box mode",
                    "Holo frame",
                    "Human mode",
                    "Full body mode"
                );
                GrammarBuilder gb = new GrammarBuilder(phrases);
                try { if (engine.RecognizerInfo != null) gb.Culture = engine.RecognizerInfo.Culture; } catch { }
                Grammar commandGrammar = new Grammar(gb) { Weight = 0.9f, Priority = 5 };
                engine.LoadGrammar(commandGrammar);
            }
            catch { }
            try
            {
                DictationGrammar dictation = new DictationGrammar { Weight = 1.0f, Priority = 10 };
                engine.LoadGrammar(dictation);
            }
            catch { }
        }

        private string TranscribeViaHelper(byte[] wav)
        {
            if (wav == null || wav.Length < 400) return null;
            try
            {
                using (var client = new System.Net.WebClient())
                {
                    client.Headers[System.Net.HttpRequestHeader.ContentType] = "audio/wav";
                    string lang = SttLanguage ?? "en";
                    byte[] response = client.UploadData("http://127.0.0.1:17777/stt?lang=" + Uri.EscapeDataString(lang), wav);
                    if (response != null && response.Length > 0)
                    {
                        string json = System.Text.Encoding.UTF8.GetString(response);
                        int idx = json.IndexOf("\"text\":");
                        if (idx >= 0)
                        {
                            int start = json.IndexOf('"', idx + 7);
                            if (start >= 0)
                            {
                                int end = json.IndexOf('"', start + 1);
                                if (end > start)
                                {
                                    string res = json.Substring(start + 1, end - start - 1).Trim();
                                    if (res.Length > 0) return res;
                                }
                            }
                        }
                    }
                }
            }
            catch { }
            return null;
        }

        internal void StartOffline()
        {
            if (offlineRunning) return;
            vadFallback = true;
            vadSpeechChunks = 0;
            vadQuietChunks = 0;
            lock (vadBuffer) vadBuffer.Clear();
            try
            {
                Start();
                offlineRunning = true;
            }
            catch (Exception error)
            {
                vadFallback = false;
                throw new VoiceException("Local mic unavailable: " + error.Message);
            }
        }

        internal void StopOffline()
        {
            offlineRunning = false;
            bool wasVad = vadFallback;
            vadFallback = false;
            vadSpeechChunks = 0;
            vadQuietChunks = 0;
            userSpeaking = false;
            lock (preSpeechRing) preSpeechRing.Clear();
            SpeechRecognitionEngine eng = offlineEngine;
            offlineEngine = null;
            if (eng != null)
            {
                try { eng.RecognizeAsyncCancel(); } catch { }
                try { eng.Dispose(); } catch { }
            }
            if (wasVad && running)
            {
                try { Stop(); } catch { }
            }
        }

        internal void Start()
        {
            if (running) return;
            WaveFormat format = WaveFormat.Mono(Rate);
            signal = new AutoResetEvent(false);
            int size = Marshal.SizeOf(typeof(WaveHeader));
            if (waveInOpen(out device, -1, ref format, signal.SafeWaitHandle.DangerousGetHandle(), IntPtr.Zero, 0x00050000) != 0)
            {
                signal.Dispose();
                signal = null;
                throw new VoiceException("Can't open your microphone.");
            }
            headers = new IntPtr[Buffers];
            for (int i = 0; i < Buffers; i++)
            {
                WaveHeader header = new WaveHeader { Data = Marshal.AllocHGlobal(Chunk * 2), Length = Chunk * 2 };
                headers[i] = Marshal.AllocHGlobal(size);
                Marshal.StructureToPtr(header, headers[i], false);
                waveInPrepareHeader(device, headers[i], size);
                waveInAddBuffer(device, headers[i], size);
            }
            running = true;
            waveInStart(device);
            worker = new Thread(Pump);
            worker.IsBackground = true;
            worker.Name = "Capri ears";
            worker.Start();
        }

        internal void Stop()
        {
            vadFallback = false;
            StopOffline();
            if (!running) return;
            running = false;
            signal.Set();
            worker.Join(1000);
            waveInReset(device);
            int size = Marshal.SizeOf(typeof(WaveHeader));
            foreach (IntPtr pointer in headers)
            {
                waveInUnprepareHeader(device, pointer, size);
                WaveHeader header = (WaveHeader)Marshal.PtrToStructure(pointer, typeof(WaveHeader));
                Marshal.FreeHGlobal(header.Data);
                Marshal.FreeHGlobal(pointer);
            }
            waveInClose(device);
            device = IntPtr.Zero;
            headers = null;
            signal.Dispose();
            signal = null;
        }

        public void Dispose() { Stop(); }

        private void Pump()
        {
            int size = Marshal.SizeOf(typeof(WaveHeader));
            int flags = (int)Marshal.OffsetOf(typeof(WaveHeader), "Flags");
            int next = 0;
            try
            {
                while (running)
                {
                    signal.WaitOne(250);
                    while (running)
                    {
                        IntPtr pointer = headers[next];
                        WaveHeader header = (WaveHeader)Marshal.PtrToStructure(pointer, typeof(WaveHeader));
                        if ((header.Flags & 1) == 0) break;
                        short[] chunk = new short[header.Recorded / 2];
                        Marshal.Copy(header.Data, chunk, 0, chunk.Length);
                        Marshal.WriteInt32(pointer, flags, header.Flags & ~1);
                        waveInAddBuffer(device, pointer, size);
                        next = (next + 1) % Buffers;
                        try { Feed(chunk); }
                        catch (Exception) { }
                    }
                }
            }
            catch (Exception) { }
        }

        internal void Feed(short[] chunk)
        {
            double loud = Muted || SuppressWhileSpeaking ? 0 : Speaker.Loudness(chunk, 0, chunk.Length);
            Action<double> level = Level;
            if (level != null) level(loud);
            if (vadFallback && !Muted && !SuppressWhileSpeaking)
            {
                if (loud < ambientNoise * 1.5)
                    ambientNoise = ambientNoise * 0.95 + loud * 0.05;

                double speechThreshold = Math.Max(MicGateThreshold, ambientNoise * 1.8);

                if (loud >= speechThreshold)
                {
                    if (!userSpeaking)
                    {
                        vadSpeechChunks++;
                        vadQuietChunks = 0;
                        lock (candidateSpeech)
                        {
                            candidateSpeech.Add((short[])chunk.Clone());
                        }
                        if (vadSpeechChunks >= 2)
                        {
                            userSpeaking = true;
                            lock (vadBuffer)
                            {
                                vadBuffer.Clear();
                                lock (preSpeechRing)
                                {
                                    foreach (short[] pre in preSpeechRing)
                                        vadBuffer.AddRange(pre);
                                }
                                lock (candidateSpeech)
                                {
                                    foreach (short[] cand in candidateSpeech)
                                        vadBuffer.AddRange(cand);
                                    candidateSpeech.Clear();
                                }
                            }
                            Action<bool> spk = Speaking;
                            if (spk != null) spk(true);
                        }
                    }
                    else
                    {
                        vadQuietChunks = 0;
                        lock (vadBuffer)
                        {
                            if (vadBuffer.Count < Rate * 15) vadBuffer.AddRange(chunk);
                        }
                    }
                }
                else
                {
                    if (userSpeaking)
                    {
                        vadQuietChunks++;
                        lock (vadBuffer)
                        {
                            if (vadBuffer.Count < Rate * 15) vadBuffer.AddRange(chunk);
                        }

                        // 6 quiet chunks = ~600ms silence confirms end of speech promptly and naturally
                        if (vadQuietChunks >= 6)
                        {
                            userSpeaking = false;
                            vadSpeechChunks = 0;
                            vadQuietChunks = 0;
                            lock (candidateSpeech) { candidateSpeech.Clear(); }
                            Action<bool> spk = Speaking;
                            if (spk != null) spk(false);

                            short[] captured;
                            lock (vadBuffer)
                            {
                                captured = vadBuffer.ToArray();
                                vadBuffer.Clear();
                            }

                            if (captured.Length >= Rate * 35 / 100)
                            {
                                ThreadPool.QueueUserWorkItem(delegate
                                {
                                    try
                                    {
                                        byte[] wav = Voice.Wave(captured, Rate);
                                        string text = TranscribeViaHelper(wav);
                                        if (string.IsNullOrEmpty(text))
                                        {
                                            using (SpeechRecognitionEngine eng = CreateDirectEngine())
                                            {
                                                if (eng != null)
                                                {
                                                    LoadCapriGrammars(eng);
                                                    using (System.IO.MemoryStream ms = new System.IO.MemoryStream(wav))
                                                    {
                                                        eng.SetInputToWaveStream(ms);
                                                        RecognitionResult res = eng.Recognize();
                                                        if (res != null && !string.IsNullOrEmpty(res.Text))
                                                            text = res.Text.Trim();
                                                    }
                                                }
                                            }
                                        }
                                        if (!string.IsNullOrEmpty(text))
                                        {
                                            Action<string> rec = Recognized;
                                            if (rec != null) rec(text);
                                        }
                                    }
                                    catch { }
                                });
                            }
                        }
                    }
                    else
                    {
                        if (vadSpeechChunks > 0)
                        {
                            vadQuietChunks++;
                            if (vadQuietChunks >= 2)
                            {
                                vadSpeechChunks = 0;
                                vadQuietChunks = 0;
                                lock (candidateSpeech) { candidateSpeech.Clear(); }
                            }
                        }
                        lock (preSpeechRing)
                        {
                            preSpeechRing.Enqueue((short[])chunk.Clone());
                            while (preSpeechRing.Count > PreSpeechMaxChunks)
                                preSpeechRing.Dequeue();
                        }
                    }
                }
            }
            Action<short[]> heard = Heard;
            if (heard != null) heard(Muted ? new short[chunk.Length] : chunk);
        }
    }

    internal sealed class Speaker : IDisposable
    {
        private readonly bool silent;
        private readonly List<IntPtr> pending = new List<IntPtr>();
        private readonly List<int> shapes = new List<int>();
        private readonly List<short> leftover = new List<short>();
        private IntPtr device;
        private int rate;
        private double loudest = 0.05;
        private long began;
        private long until;

        [DllImport("winmm.dll")] private static extern int waveOutOpen(out IntPtr device, int id, ref WaveFormat format, IntPtr callback, IntPtr instance, int flags);
        [DllImport("winmm.dll")] private static extern int waveOutPrepareHeader(IntPtr device, IntPtr header, int size);
        [DllImport("winmm.dll")] private static extern int waveOutUnprepareHeader(IntPtr device, IntPtr header, int size);
        [DllImport("winmm.dll")] private static extern int waveOutWrite(IntPtr device, IntPtr header, int size);
        [DllImport("winmm.dll")] private static extern int waveOutReset(IntPtr device);
        [DllImport("winmm.dll")] private static extern int waveOutClose(IntPtr device);

        internal Speaker(bool quiet) { silent = quiet; }

        internal static double Loudness(short[] samples, int start, int count)
        {
            if (count <= 0) return 0;
            double sum = 0;
            for (int i = start; i < start + count; i++) sum += (double)samples[i] * samples[i];
            return Math.Sqrt(sum / count) / 32768.0;
        }

        internal void Open(int sampleRate)
        {
            if (sampleRate == rate && (silent || device != IntPtr.Zero)) return;
            Close();
            rate = sampleRate;
            if (silent) return;
            WaveFormat format = WaveFormat.Mono(rate);
            if (waveOutOpen(out device, -1, ref format, IntPtr.Zero, IntPtr.Zero, 0) != 0)
            {
                device = IntPtr.Zero;
                throw new VoiceException("Can't open your speakers.");
            }
        }

        internal bool Busy { get { return Stopwatch.GetTimestamp() < Interlocked.Read(ref until) + Stopwatch.Frequency * 35 / 100; } }

        internal void Play(short[] samples)
        {
            if (rate == 0 || samples.Length == 0) return;
            Sweep();
            long now = Stopwatch.GetTimestamp();
            if (now >= Interlocked.Read(ref until)) { began = now; shapes.Clear(); leftover.Clear(); Interlocked.Exchange(ref until, now); }
            Interlocked.Add(ref until, samples.Length * Stopwatch.Frequency / rate);
            leftover.AddRange(samples);
            int step = Math.Max(1, rate / 12);
            short[] all = leftover.ToArray();
            int at = 0;
            for (; at + step <= all.Length; at += step)
            {
                double level = Loudness(all, at, step);
                loudest = Math.Max(loudest * 0.995, level);
                int index = shapes.Count;
                shapes.Add(level <= Math.Max(0.004, loudest * 0.08) ? -1 : level < loudest * 0.3 ? (index % 3 == 0 ? 0 : 1) : level < loudest * 0.55 ? (index % 2 == 1 ? 2 : 1) : level < loudest * 0.85 ? 3 : 4);
            }
            leftover.RemoveRange(0, at);
            if (silent || device == IntPtr.Zero) return;
            int size = Marshal.SizeOf(typeof(WaveHeader));
            WaveHeader header = new WaveHeader { Data = Marshal.AllocHGlobal(samples.Length * 2), Length = samples.Length * 2 };
            Marshal.Copy(samples, 0, header.Data, samples.Length);
            IntPtr pointer = Marshal.AllocHGlobal(size);
            Marshal.StructureToPtr(header, pointer, false);
            waveOutPrepareHeader(device, pointer, size);
            waveOutWrite(device, pointer, size);
            pending.Add(pointer);
        }

        internal int Shape()
        {
            long now = Stopwatch.GetTimestamp();
            if (now >= Interlocked.Read(ref until) || shapes.Count == 0) return -1;
            int index = (int)((now - began) * 12 / Stopwatch.Frequency);
            return index >= 0 && index < shapes.Count ? shapes[index] : -1;
        }

        internal void Clear()
        {
            if (device != IntPtr.Zero) waveOutReset(device);
            Sweep();
            shapes.Clear();
            leftover.Clear();
            Interlocked.Exchange(ref until, 0);
        }

        private void Sweep()
        {
            int size = Marshal.SizeOf(typeof(WaveHeader));
            for (int i = pending.Count - 1; i >= 0; i--)
            {
                WaveHeader header = (WaveHeader)Marshal.PtrToStructure(pending[i], typeof(WaveHeader));
                if ((header.Flags & 1) == 0) continue;
                waveOutUnprepareHeader(device, pending[i], size);
                Marshal.FreeHGlobal(header.Data);
                Marshal.FreeHGlobal(pending[i]);
                pending.RemoveAt(i);
            }
        }

        internal void Close()
        {
            Clear();
            if (device != IntPtr.Zero) waveOutClose(device);
            device = IntPtr.Zero;
            rate = 0;
        }

        public void Dispose() { Close(); }
    }
}
