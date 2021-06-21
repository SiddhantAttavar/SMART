import tensorflow as tf
import os
import numpy as np
from PIL import Image

version = input("Enter the version number")

export_dir = r'D:/Competitions/Stroke Project/Code/Facial Droop/Model versions/' + version + '/saved_model/1'
model = tf.keras.models.load_model(export_dir)

val_s = r"D:/Competitions/Stroke Project/Facial Droop Database/val/Stroke"
val_ns = r"D:/Competitions/Stroke Project/Facial Droop Database/val/No Stroke"

n = 5
predictions_ns = []

for i in range(len(os.listdir(val_ns))):
    p = os.listdir(val_ns)[i]
    if i <=5:
        test_image_original = Image.open(os.path.join(val_ns, p))
        test_image = np.asarray(test_image_original.resize((220, 220)))
        test_image = np.expand_dims(test_image, axis=0)
        test_label = np.asarray([["No Stroke"]])
        predictions_ns.append(model.predict(test_image))
    else:
        break
print(predictions_ns)

predictions_s = []

for i in range(len(os.listdir(val_s))):
    p = os.listdir(val_s)[i]
    if i <=5:
        test_image_original = Image.open(os.path.join(val_s, p))
        test_image = np.asarray(test_image_original.resize((220, 220)))
        test_image = np.expand_dims(test_image, axis=0)
        test_label = np.asarray([["Stroke"]])
        predictions_s.append(model.predict(test_image))
    else:
        break
print(predictions_s)
