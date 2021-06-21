import os
import shutil

directory = r"D:\Competitions\Stroke Project\Facial Droop Database\No Stroke"

names = []

with open("D:\Competitions\Stroke Project\Facial Droop Database\SMILE_list.txt", "r") as f:
  for line in f:
    stripped_line = line.strip()
    names.append(stripped_line)

smile_names = []

print(len(names))

cpt = sum([len(files) for r, d, files in os.walk(r"D:\Competitions\Stroke Project\Facial Droop Database\No Stroke")])
print(cpt)
cpt = sum([len(files) for r, d, files in os.walk("D:\Competitions\Stroke Project\Facial Droop Database\Stroke")])
print(cpt)

for filename in os.listdir(directory):
    if filename.endswith(".jpg"):
        for i in names:
            if i == filename:
                smile_names.append(i)
                shutil.copy(r"D:\Competitions\Stroke Project\Facial Droop Database\No Stroke\\" + filename, r"D:\Competitions\Stroke Project\Facial Droop Database\No Stroke 2\\")
        continue
    else:
        continue

print(smile_names)
