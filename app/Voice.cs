using System;
using System.Collections.Generic;
using System.IO;
using System.Media;
using System.Net;
using System.Runtime.Serialization;
using System.Runtime.Serialization.Json;
using System.Security.Cryptography;
using System.Speech.AudioFormat;
using System.Speech.Synthesis;
using System.Text;
using System.Text.RegularExpressions;

namespace CapriDesktop
{
    [DataContract]
    internal sealed class VoiceEntry
    {
        [DataMember(Name = "voice_id")] public string Id;
        [DataMember(Name = "name")] public string Name;
        [DataMember(Name = "category")] public string Category;
    }

    [DataContract]
    internal sealed class VoicePage
    {
        [DataMember(Name = "voices")] public List<VoiceEntry> Voices;
        [DataMember(Name = "has_more")] public bool More;
        [DataMember(Name = "next_page_token")] public string Next;
    }

    [DataContract]
    internal sealed class VoiceModel
    {
        [DataMember(Name = "model_id")] public string Id;
        [DataMember(Name = "name")] public string Name;
        [DataMember(Name = "can_do_text_to_speech")] public bool Speaks;
    }

    internal sealed class VoiceException : Exception
    {
        internal readonly int Status;

        internal VoiceException(string message, int status = 0) : base(message) { Status = status; }
    }

    internal sealed class VoiceListing
    {
        internal List<VoiceEntry> Voices = new List<VoiceEntry>();
        internal List<VoiceModel> Models = new List<VoiceModel>();
    }

    internal sealed class Voice
    {
        internal static string Host = "https://api.elevenlabs.io";
        internal const string Default = "MkTSSXNgnBULS6ek4pon";
        private static readonly byte[] Entropy = Encoding.UTF8.GetBytes("Capri.ElevenLabs");
        internal volatile string Key;
        internal volatile string VoiceId;
        internal volatile string Model;
        internal volatile bool OfflineEnabled = true;
        private SoundPlayer player;

        internal bool Ready { get { return Key != null && VoiceId != null; } }
        internal bool CanSpeak { get { return Ready || OfflineEnabled; } }

        internal static string Protect(string key) { return Convert.ToBase64String(ProtectedData.Protect(Encoding.UTF8.GetBytes(key), Entropy, DataProtectionScope.CurrentUser)); }

        internal static string Unprotect(string saved)
        {
            if (string.IsNullOrEmpty(saved)) return null;
            try { return Encoding.UTF8.GetString(ProtectedData.Unprotect(Convert.FromBase64String(saved), Entropy, DataProtectionScope.CurrentUser)); }
            catch (CryptographicException) { return null; }
            catch (FormatException) { return null; }
        }

        internal static string Tail(string key) { return key == null || key.Length < 4 ? "" : key.Substring(key.Length - 4); }

        internal static string Spoken(string said) { return Regex.Replace(Regex.Replace(said ?? "", @"\[[^\]]*\]", " ").Replace("*", ""), @"\s+", " ").Trim(); }

        internal static HttpWebRequest Open(string path, string key, string method)
        {
            ServicePointManager.SecurityProtocol |= (SecurityProtocolType)3072;
            HttpWebRequest request = (HttpWebRequest)WebRequest.Create(Host + path);
            request.Method = method;
            request.UserAgent = "Capri-Desktop";
            request.Headers["xi-api-key"] = key;
            request.ServicePoint.Expect100Continue = false;
            request.Timeout = 20000;
            request.ReadWriteTimeout = 20000;
            return request;
        }

        internal static byte[] Send(HttpWebRequest request, byte[] body)
        {
            try
            {
                if (body != null)
                {
                    request.ContentType = "application/json";
                    request.ContentLength = body.Length;
                    using (Stream upload = request.GetRequestStream()) upload.Write(body, 0, body.Length);
                }
                using (WebResponse response = request.GetResponse())
                using (Stream stream = response.GetResponseStream())
                using (MemoryStream copy = new MemoryStream())
                {
                    stream.CopyTo(copy);
                    return copy.ToArray();
                }
            }
            catch (WebException error)
            {
                HttpWebResponse response = error.Response as HttpWebResponse;
                if (response == null) throw new VoiceException("Can't reach ElevenLabs. Check your internet.");
                int status = (int)response.StatusCode;
                string detail;
                using (response)
                using (StreamReader reader = new StreamReader(response.GetResponseStream())) detail = reader.ReadToEnd();
                throw new VoiceException(Explain(status, detail), status);
            }
            catch (IOException) { throw new VoiceException("Can't reach ElevenLabs. Check your internet."); }
        }

        internal static string Explain(int status, string detail)
        {
            detail = detail ?? "";
            if (detail.Contains("quota_exceeded")) return "Out of ElevenLabs credits.";
            if (detail.Contains("missing_permissions")) return "Key needs Voices and Text to Speech access.";
            if (detail.Contains("voice_not_found")) return "Voice not found. Add it to your ElevenLabs voices.";
            if (detail.Contains("invalid_api_key")) return "ElevenLabs rejected the key.";
            Match message = Regex.Match(detail, "\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
            if (message.Success)
            {
                string text = Regex.Unescape(message.Groups[1].Value).Trim();
                if (text.Length > 0) return Short(text);
            }
            if (status == 401) return "ElevenLabs rejected the key.";
            if (status == 429) return "ElevenLabs is busy. Try again in a moment.";
            return "ElevenLabs error " + status + ".";
        }

        internal static string Short(string text)
        {
            text = (text ?? "").Trim();
            return text.Length > 72 ? text.Substring(0, 69) + "..." : text;
        }

        internal static T Read<T>(byte[] json) { using (MemoryStream stream = new MemoryStream(json)) return (T)new DataContractJsonSerializer(typeof(T)).ReadObject(stream); }

        internal static VoiceListing Fetch(string key)
        {
            VoiceListing listing = new VoiceListing();
            string next = null;
            for (int page = 0; page < 10; page++)
            {
                string path = "/v2/voices?page_size=100" + (next == null ? "" : "&next_page_token=" + Uri.EscapeDataString(next));
                VoicePage found = Read<VoicePage>(Send(Open(path, key, "GET"), null));
                if (found.Voices != null) listing.Voices.AddRange(found.Voices);
                if (!found.More || string.IsNullOrEmpty(found.Next)) break;
                next = found.Next;
            }
            try { foreach (VoiceModel model in Read<List<VoiceModel>>(Send(Open("/v1/models", key, "GET"), null))) if (model.Speaks) listing.Models.Add(model); }
            catch (VoiceException error) { if (error.Status == 401) throw; }
            catch (SerializationException) { }
            listing.Voices.Sort(delegate(VoiceEntry a, VoiceEntry b)
            {
                int order = (a.Category == "premade").CompareTo(b.Category == "premade");
                return order != 0 ? order : string.Compare(a.Name, b.Name, StringComparison.OrdinalIgnoreCase);
            });
            return listing;
        }

        internal byte[] Speak(string text)
        {
            string key = Key;
            string id = VoiceId;
            string model = Model;
            if (key == null || id == null) throw new VoiceException("No voice picked.");
            StringBuilder body = new StringBuilder("{\"text\":");
            body.Append(Quote(text));
            if (!string.IsNullOrEmpty(model)) body.Append(",\"model_id\":").Append(Quote(model));
            body.Append('}');
            return Clean(Send(Open("/v1/text-to-speech/" + Uri.EscapeDataString(id) + "?output_format=wav_24000", key, "POST"), Encoding.UTF8.GetBytes(body.ToString())));
        }

        internal byte[] SpeakOffline(string text)
        {
            text = Spoken(text);
            if (string.IsNullOrEmpty(text)) return Wave(new short[0], 22050);

            DesktopActions.EnsureHelperRunning();

            // 1. Primary: Local Neural Anime Voice bridge (high-definition, expressive, emotional anime dub)
            try
            {
                HttpWebRequest req = (HttpWebRequest)WebRequest.Create("http://127.0.0.1:17777/tts");
                req.Method = "POST";
                req.ContentType = "application/json";
                req.Timeout = 45000;
                req.ReadWriteTimeout = 45000;
                string pickedVoice = (!string.IsNullOrEmpty(VoiceId) && VoiceId != Default) ? VoiceId : "construct_echo";
                byte[] payload = Encoding.UTF8.GetBytes("{\"text\":" + Quote(text) + ",\"voice\":" + Quote(pickedVoice) + "}");
                req.ContentLength = payload.Length;
                using (Stream st = req.GetRequestStream()) st.Write(payload, 0, payload.Length);
                using (HttpWebResponse resp = (HttpWebResponse)req.GetResponse())
                using (Stream rs = resp.GetResponseStream())
                using (MemoryStream ms = new MemoryStream())
                {
                    rs.CopyTo(ms);
                    byte[] wav = ms.ToArray();
                    if (wav.Length > 100) return Clean(wav);
                }
            }
            catch { }

            // 2. Secondary fallback: Native Windows 11 WinRT SpeechSynthesizer (Microsoft Zira)
            try
            {
                byte[] winrtWav = SpeakViaWinRT(text);
                if (winrtWav != null && winrtWav.Length >= 64)
                {
                    int r;
                    short[] samples = Samples(winrtWav, out r);
                    if (samples.Length > 0)
                        return Wave(CyberpunkVoicePolish(samples, r), r);
                }
            }
            catch { }

            return Wave(new short[0], 22050);
        }

        private static byte[] SpeakViaWinRT(string text)
        {
            try
            {
                System.Reflection.Assembly winrtAssembly = System.Reflection.Assembly.Load("System.Runtime.WindowsRuntime, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b77a5c561934e089");
                Type extType = winrtAssembly.GetType("System.WindowsRuntimeSystemExtensions");
                Type streamExtType = winrtAssembly.GetType("System.IO.WindowsRuntimeStreamExtensions");

                Type synthType = Type.GetType("Windows.Media.SpeechSynthesis.SpeechSynthesizer, Windows.Media.SpeechSynthesis, ContentType=WindowsRuntime");
                if (synthType == null) return null;
                object synth = Activator.CreateInstance(synthType);

                System.Reflection.PropertyInfo allVoicesProp = synthType.GetProperty("AllVoices", System.Reflection.BindingFlags.Public | System.Reflection.BindingFlags.Static);
                System.Collections.IEnumerable allVoices = (System.Collections.IEnumerable)allVoicesProp.GetValue(null, null);
                object chosenVoice = null;
                foreach (object v in allVoices)
                {
                    string name = (string)v.GetType().GetProperty("DisplayName").GetValue(v, null);
                    if (name.IndexOf("Zira", StringComparison.OrdinalIgnoreCase) >= 0 || name.IndexOf("Jenny", StringComparison.OrdinalIgnoreCase) >= 0 || name.IndexOf("Aria", StringComparison.OrdinalIgnoreCase) >= 0)
                    {
                        chosenVoice = v;
                        break;
                    }
                }
                if (chosenVoice != null) synthType.GetProperty("Voice").SetValue(synth, chosenVoice, null);

                System.Reflection.MethodInfo synthMethod = synthType.GetMethod("SynthesizeTextToStreamAsync", new Type[] { typeof(string) });
                object asyncOp = synthMethod.Invoke(synth, new object[] { text });

                System.Reflection.MethodInfo asTaskGeneric = null;
                foreach (System.Reflection.MethodInfo m in extType.GetMethods(System.Reflection.BindingFlags.Public | System.Reflection.BindingFlags.Static))
                {
                    if (m.Name == "AsTask" && m.IsGenericMethod && m.GetParameters().Length == 1)
                    {
                        asTaskGeneric = m;
                        break;
                    }
                }
                Type streamType = Type.GetType("Windows.Media.SpeechSynthesis.SpeechSynthesisStream, Windows.Media.SpeechSynthesis, ContentType=WindowsRuntime");
                System.Reflection.MethodInfo asTask = asTaskGeneric.MakeGenericMethod(streamType);
                object task = asTask.Invoke(null, new object[] { asyncOp });

                task.GetType().GetMethod("Wait", new Type[] { typeof(int) }).Invoke(task, new object[] { 15000 });
                object stream = task.GetType().GetProperty("Result").GetValue(task, null);

                System.Reflection.MethodInfo asStreamMethod = null;
                foreach (System.Reflection.MethodInfo m in streamExtType.GetMethods(System.Reflection.BindingFlags.Public | System.Reflection.BindingFlags.Static))
                {
                    if (m.Name == "AsStreamForRead") { asStreamMethod = m; break; }
                }

                using (Stream netStream = (Stream)asStreamMethod.Invoke(null, new object[] { stream }))
                using (MemoryStream ms = new MemoryStream())
                {
                    netStream.CopyTo(ms);
                    byte[] wav = ms.ToArray();
                    return wav != null && wav.Length > 100 ? Clean(wav) : null;
                }
            }
            catch
            {
                return null;
            }
        }

        internal static short[] CyberpunkVoicePolish(short[] input, int rate)
        {
            if (input == null || input.Length < 128) return input ?? new short[0];
            short[] output = new short[input.Length];
            double prevIn = 0;
            double prevHp = 0;
            for (int i = 0; i < input.Length; i++)
            {
                double val = input[i];
                double hp = 0.84 * (prevHp + val - prevIn);
                prevIn = val;
                prevHp = hp;
                double crisp = val * 0.88 + hp * 0.32;
                double norm = Math.Tanh((crisp / 32768.0) * 1.25) * 30000.0;
                output[i] = (short)Math.Max(-32000, Math.Min(32000, (int)norm));
            }
            return output;
        }

        internal static string Quote(string value)
        {
            StringBuilder quoted = new StringBuilder("\"");
            foreach (char c in value)
            {
                if (c == '"' || c == '\\') quoted.Append('\\').Append(c);
                else if (c < ' ') quoted.Append("\\u").Append(((int)c).ToString("x4"));
                else quoted.Append(c);
            }
            return quoted.Append('"').ToString();
        }

        internal static short[] Samples(byte[] wav, out int rate)
        {
            rate = 24000;
            int at = 12;
            while (at + 8 <= wav.Length)
            {
                string chunk = Encoding.ASCII.GetString(wav, at, 4);
                int size = BitConverter.ToInt32(wav, at + 4);
                if (chunk == "fmt " && at + 16 <= wav.Length) rate = Math.Max(1000, BitConverter.ToInt32(wav, at + 12));
                if (chunk == "data")
                {
                    int available = wav.Length - at - 8;
                    int count = (size > 0 && size <= available ? size : available) / 2;
                    short[] samples = new short[count];
                    Buffer.BlockCopy(wav, at + 8, samples, 0, count * 2);
                    return samples;
                }
                long next = (long)at + 8 + size + (size & 1);
                if (size < 0 || next > wav.Length) break;
                at = (int)next;
            }
            return new short[0];
        }

        internal static int[] Mouths(byte[] wav, double perSecond)
        {
            int rate;
            short[] samples = Samples(wav, out rate);
            int step = Math.Max(1, (int)(rate / perSecond));
            int count = samples.Length / step;
            double[] levels = new double[count];
            for (int i = 0; i < count; i++)
            {
                double sum = 0;
                for (int j = i * step; j < (i + 1) * step; j++) sum += (double)samples[j] * samples[j];
                levels[i] = Math.Sqrt(sum / step) / 32768.0;
            }
            double loudest = 0;
            foreach (double level in levels) loudest = Math.Max(loudest, level);
            double quiet = Math.Max(0.004, loudest * 0.08);
            List<double> talking = new List<double>();
            foreach (double level in levels) if (level > quiet) talking.Add(level);
            talking.Sort();
            Func<double, double> cut = delegate(double q) { return talking.Count == 0 ? 1 : talking[Math.Min(talking.Count - 1, (int)(q * talking.Count))]; };
            double low = cut(0.35), mid = cut(0.7), high = cut(0.97);
            int[] shapes = new int[count];
            for (int i = 0; i < count; i++)
            {
                double level = levels[i];
                shapes[i] = level <= quiet ? -1 : level < low ? (i % 3 == 0 ? 0 : 1) : level < mid ? (i % 2 == 1 ? 2 : 1) : level < high ? 3 : 4;
            }
            return shapes;
        }

        internal static double Seconds(byte[] wav)
        {
            int rate;
            return Samples(wav, out rate).Length / (double)rate;
        }

        internal static byte[] Clean(byte[] wav)
        {
            int rate;
            short[] samples = Samples(wav, out rate);
            return Wave(samples, rate);
        }

        internal static byte[] Wave(short[] samples, int rate)
        {
            using (MemoryStream stream = new MemoryStream())
            using (BinaryWriter writer = new BinaryWriter(stream))
            {
                writer.Write(Encoding.ASCII.GetBytes("RIFF"));
                writer.Write(36 + samples.Length * 2);
                writer.Write(Encoding.ASCII.GetBytes("WAVEfmt "));
                writer.Write(16);
                writer.Write((short)1);
                writer.Write((short)1);
                writer.Write(rate);
                writer.Write(rate * 2);
                writer.Write((short)2);
                writer.Write((short)16);
                writer.Write(Encoding.ASCII.GetBytes("data"));
                writer.Write(samples.Length * 2);
                byte[] data = new byte[samples.Length * 2];
                Buffer.BlockCopy(samples, 0, data, 0, data.Length);
                writer.Write(data);
                writer.Flush();
                return stream.ToArray();
            }
        }

        internal void Play(byte[] wav)
        {
            Stop();
            player = new SoundPlayer(new MemoryStream(wav));
            player.Play();
        }

        internal void Stop()
        {
            if (player == null) return;
            player.Stop();
            player.Dispose();
            player = null;
        }
    }
}
