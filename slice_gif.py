from PIL import Image, ImageSequence
import os

gif_path = r"c:\Users\regre\Downloads\capri-desktop-main\assets\sprites\capri_tenor.gif"
im = Image.open(gif_path)
frames = [f.convert("RGBA") for f in ImageSequence.Iterator(im)]

# Let's save a sample of frames across the 163 frames to see what actions occur
out_dir = r"c:\Users\regre\Downloads\capri-desktop-main\assets\sprites\tenor_preview"
os.makedirs(out_dir, exist_ok=True)
for i in range(0, len(frames), 15):
    frames[i].save(os.path.join(out_dir, f"frame_{i:03d}.png"))
print("Saved preview frames. Total:", len(frames))
