from PIL import Image, ImageSequence
import os

gif_path = r"c:\Users\regre\Downloads\capri-desktop-main\assets\sprites\capri_tenor.gif"
im = Image.open(gif_path)
frames = [f.convert("RGBA") for f in ImageSequence.Iterator(im)]

out_dir = r"c:\Users\regre\Downloads\capri-desktop-main\assets\sprites\tenor_preview"
# Let's save frames 30, 40, 50, 60, 70
for i in [10, 20, 30, 40, 50, 60, 70, 80, 100, 120, 140, 160]:
    frames[i].save(os.path.join(out_dir, f"sample_{i:03d}.png"))
print("Saved samples!")
