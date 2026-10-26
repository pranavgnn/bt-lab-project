import urllib.request
import re
import os

url = 'https://tenor.com/en-GB/view/cyberpunk-capri-cyberpunk-capri-edgerunners-2-edgerunners-2-gif-5774155399214368565'
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'})
try:
    with urllib.request.urlopen(req) as resp:
        html = resp.read().decode('utf-8', errors='ignore')
    
    gifs = re.findall(r'https://media\.tenor\.com/[^"\'\s]+\.gif', html)
    mp4s = re.findall(r'https://media\.tenor\.com/[^"\'\s]+\.mp4', html)
    print("Found GIFs:", len(gifs))
    for g in gifs[:3]:
        print("GIF:", g)
    print("Found MP4s:", len(mp4s))
    for m in mp4s[:3]:
        print("MP4:", m)
        
    target = None
    if gifs:
        target = gifs[0]
    elif mp4s:
        target = mp4s[0]
        
    if target:
        ext = ".gif" if ".gif" in target else ".mp4"
        out_path = os.path.join(r"c:\Users\regre\Downloads\capri-desktop-main\assets\sprites", "tenor_capri" + ext)
        print("Downloading", target, "to", out_path)
        urllib.request.urlretrieve(target, out_path)
        print("Saved successfully! Size:", os.path.getsize(out_path))
except Exception as e:
    print("Error:", e)
