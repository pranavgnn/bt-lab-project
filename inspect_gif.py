from PIL import Image, ImageSequence
import os

gif_path = r"c:\Users\regre\Downloads\capri-desktop-main\assets\sprites\capri_tenor.gif"
im = Image.open(gif_path)
frames = [f.copy() for f in ImageSequence.Iterator(im)]
print("Frames count:", len(frames))
print("Size:", frames[0].size)
print("Duration per frame (ms):", frames[0].info.get("duration", 50))
