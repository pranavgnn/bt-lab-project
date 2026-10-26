from PIL import Image
import os

def inspect_png(name, count):
    p = os.path.join("assets", "sprites", name)
    if not os.path.exists(p):
        print("Missing:", p)
        return
    im = Image.open(p)
    w, h = im.size
    print(f"{name}: size=({w}, {h}), count={count}, frame_w={w//count}")
    # save frames to scratch
    out_dir = os.path.join("scratch", name.replace(".png", ""))
    os.makedirs(out_dir, exist_ok=True)
    fw = w // count
    for i in range(count):
        crop = im.crop((i * fw, 0, (i + 1) * fw, h))
        crop.save(os.path.join(out_dir, f"frame_{i}.png"))

inspect_png("hi_box.png", 24)
inspect_png("box_cutins.png", 5)
inspect_png("holo_banner.png", 32)
inspect_png("holo_call.png", 24)
inspect_png("box_talk.png", 24)
inspect_png("box_listen.png", 24)
