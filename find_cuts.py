from PIL import Image, ImageSequence, ImageChops, ImageStat
import os

gif_path = r"c:\Users\regre\Downloads\capri-desktop-main\assets\sprites\capri_tenor.gif"
im = Image.open(gif_path)
frames = [f.convert("RGB") for f in ImageSequence.Iterator(im)]

cuts = [0]
for i in range(1, len(frames)):
    diff = ImageChops.difference(frames[i], frames[i-1])
    stat = ImageStat.Stat(diff)
    diff_val = sum(stat.mean)
    if diff_val > 45: # significant scene cut
        cuts.append(i)
cuts.append(len(frames))
print("Scene cuts detected at frames:", cuts)
