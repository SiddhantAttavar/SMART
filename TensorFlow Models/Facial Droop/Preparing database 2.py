import shutil
import os

no_stroke_dir = r"D:\Competitions\Stroke Project\Facial Droop Database\No Stroke"
stroke_dir = r"D:\Competitions\Stroke Project\Facial Droop Database\Stroke"

train_dir = r"D:\Competitions\Stroke Project\Facial Droop Database\train"
test_dir = r"D:\Competitions\Stroke Project\Facial Droop Database\test"

train_s = train_dir + r"\Stroke"
train_ns = train_dir + r"\No Stroke"

test_s = test_dir + r"\Stroke"
test_ns = test_dir + r"\No Stroke"

s_files = len(os.listdir(stroke_dir))
ns_files = len(os.listdir(no_stroke_dir))

for filename in os.listdir(stroke_dir)[:int(0.8 * s_files)]:
    if filename.endswith(".jpg"):
        shutil.copy(stroke_dir + "\\" + filename, train_s)
        continue
    else:
        continue

for filename in os.listdir(stroke_dir)[int(0.8 * s_files):]:
    if filename.endswith(".jpg"):
        shutil.copy(stroke_dir + "\\" + filename, test_s)
        continue
    else:
        continue

for filename in os.listdir(no_stroke_dir)[:int(0.8 * ns_files)]:
    if filename.endswith(".jpg"):
        shutil.copy(no_stroke_dir + "\\" + filename, train_ns)
        continue
    else:
        continue

for filename in os.listdir(no_stroke_dir)[int(0.8 * ns_files):]:
    if filename.endswith(".jpg"):
        shutil.copy(no_stroke_dir + "\\" + filename, test_ns)
        continue
    else:
        continue

print(s_files, ns_files)

