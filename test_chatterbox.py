import torch
from chatterbox.tts_turbo import ChatterboxTurboTTS

device = "cuda" if torch.cuda.is_available() else "cpu"
print(f"Loading Chatterbox model on {device}...")
model = ChatterboxTurboTTS.from_pretrained(device=device)
print("Chatterbox model loaded successfully!")

ref_audio = "Capri Explains How She Was Created ~ Cyberpunk Edgerunners Season 2 Episode 4.mp3"
print(f"Generating test speech using voice clone from {ref_audio}...")
wav = model.generate(
    text="Hey choom, look at you! I'm live on your desktop.",
    audio_prompt_path=ref_audio
)
print("Generated audio shape:", wav.shape, "Sample rate:", model.sr)
import torchaudio
torchaudio.save("test_chatterbox_out.wav", wav, model.sr)
print("Saved test_chatterbox_out.wav successfully!")
