from PIL import Image, ImageSequence
import os

gif_path = r"c:\Users\regre\Downloads\capri-desktop-main\assets\sprites\capri_tenor.gif"
im = Image.open(gif_path)
all_frames = [f.convert("RGBA") for f in ImageSequence.Iterator(im)]
total = len(all_frames)
print("Total GIF frames:", total)

target_w = 320
target_h = 180

# Strip 1: box_tenor_talk (24 frames from frames 25 to 75)
f_start = 25
f_end = 75
indices_talk = [int(f_start + i * (f_end - f_start) / 24) for i in range(24)]
strip_talk = Image.new("RGBA", (target_w * 24, target_h))
for i, idx in enumerate(indices_talk):
    resized = all_frames[idx].resize((target_w, target_h), Image.LANCZOS)
    strip_talk.paste(resized, (i * target_w, 0))

out_talk = r"c:\Users\regre\Downloads\capri-desktop-main\assets\sprites\box_tenor_talk.png"
strip_talk.save(out_talk, "PNG")
print("Saved", out_talk, strip_talk.size)

# Strip 2: box_tenor_glitch (24 frames from frames 76 to 155)
f_start_g = 76
f_end_g = 155
indices_glitch = [int(f_start_g + i * (f_end_g - f_start_g) / 24) for i in range(24)]
strip_glitch = Image.new("RGBA", (target_w * 24, target_h))
for i, idx in enumerate(indices_glitch):
    resized = all_frames[idx].resize((target_w, target_h), Image.LANCZOS)
    strip_glitch.paste(resized, (i * target_w, 0))

out_glitch = r"c:\Users\regre\Downloads\capri-desktop-main\assets\sprites\box_tenor_glitch.png"
strip_glitch.save(out_glitch, "PNG")
print("Saved", out_glitch, strip_glitch.size)
