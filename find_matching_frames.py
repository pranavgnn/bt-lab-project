from PIL import Image
import os
import glob

# Check where the user images match
user_img1 = Image.open(r"C:\Users\regre\.gemini\antigravity\brain\dd69aafc-ecde-46be-bdc2-c8c74342d25f\.user_uploaded\media_1793027041370_5cc2838f.png")
user_img2 = Image.open(r"C:\Users\regre\.gemini\antigravity\brain\dd69aafc-ecde-46be-bdc2-c8c74342d25f\.user_uploaded\media_1793027055307_83a868f4.png")

print("User image 1 (pointing finger):", user_img1.size)
print("User image 2 (cat screenshot):", user_img2.size)

for folder in glob.glob("scratch/*"):
    print("Checking", folder)
    for f in glob.glob(folder + "/*.png"):
        im = Image.open(f)
        # Check if contains red finger or cat
        # hi_box: 256x216
        # box_cutins: 360x240
