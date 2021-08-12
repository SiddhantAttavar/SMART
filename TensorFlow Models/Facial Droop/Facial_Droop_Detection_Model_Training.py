#!pip install tensorflow==2.0

from google.colab import drive

drive.mount('/content/drive')

from __future__ import absolute_import, division, print_function, unicode_literals

import os
import numpy as np
import glob
import shutil
import matplotlib.pyplot as plt

try:
  # Use the %tensorflow_version magic if in colab.
  #%tensorflow_version 2.x
  pass
except Exception:
  pass

import tensorflow as tf
from tensorflow import keras

from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Conv2D, Flatten, Dropout, MaxPooling2D
from tensorflow.keras.preprocessing.image import ImageDataGenerator

base_dir = r"/content/drive/My Drive/Competitions/Grade 10/Stroke Project/Facial Droop Database"
train_dir = r"/content/drive/My Drive/Competitions/Grade 10/Stroke Project/Facial Droop Database/train"
val_dir = r"/content/drive/My Drive/Competitions/Grade 10/Stroke Project/Facial Droop Database/val"

train_s = r"/content/drive/My Drive/Competitions/Grade 10/Stroke Project/Facial Droop Database/train/Stroke"
train_ns = r"/content/drive/My Drive/Competitions/Grade 10/Stroke Project/Facial Droop Database/train/No Stroke"

val_s = r"/content/drive/My Drive/Competitions/Grade 10/Stroke Project/Facial Droop Database/val/Stroke"
val_ns = r"/content/drive/My Drive/Competitions/Grade 10/Stroke Project/Facial Droop Database/val/No Stroke"

num_train_ns = len(os.listdir(train_ns))
num_train_s = len(os.listdir(train_s))

num_val_ns = len(os.listdir(val_ns))
num_val_s = len(os.listdir(val_s))

num_train = num_train_ns + num_train_s
num_val = num_val_ns + num_val_s

print('total training stroke images:', num_train_s)
print('total training no stroke images:', num_train_ns)

print('total val stroke images:', num_val_s)
print('total val no stroke images:', num_val_ns)
print("--")
print("Total training images:", num_train)
print("Total val images:", num_val)

BATCH_SIZE = 100
IMG_SHAPE = 220

classes = ['Stroke', 'No Stroke']

# This function will plot images in the form of a grid with 1 row and 5 columns where images are placed in each column.
def plotImages(images_arr):
    fig, axes = plt.subplots(1, 5, figsize=(20,20))
    axes = axes.flatten()
    for img, ax in zip(images_arr, axes):
        ax.imshow(img)
    plt.tight_layout()
    plt.show()

image_gen_train = ImageDataGenerator(
                    rescale=1./255,
                    zoom_range=0.5
                    )


train_data_gen = image_gen_train.flow_from_directory(
                                                batch_size=BATCH_SIZE,
                                                directory=train_dir,
                                                shuffle=True,
                                                target_size=(IMG_SHAPE,IMG_SHAPE),
                                                class_mode='sparse'
                                                )

augmented_images = [train_data_gen[0][0][0] for i in range(5)]
plotImages(augmented_images)

image_gen_val = ImageDataGenerator(rescale=1./255)

val_data_gen = image_gen_val.flow_from_directory(batch_size=BATCH_SIZE,
                                                 directory=val_dir,
                                                 target_size=(IMG_SHAPE, IMG_SHAPE),
                                                 class_mode='sparse')

model = Sequential([
    Conv2D(16, 3, padding='same', activation='relu', input_shape=(IMG_SHAPE,IMG_SHAPE, 3)),
    MaxPooling2D(pool_size=(2, 2)),

    Conv2D(32, 3, padding='same', activation='relu'),
    MaxPooling2D(pool_size=(2, 2)),

    Conv2D(64, 3, padding='same', activation='relu'),
    MaxPooling2D(pool_size=(2, 2)),

    Conv2D(128, 3, padding='same', activation='relu'),
    MaxPooling2D(pool_size=(2, 2)),

    Flatten(),

    Dropout(0.2),
    Dense(512, activation='relu'),

    Dropout(0.2),
    Dense(256, activation='relu'),

    Dropout(0.2),
    Dense(2),
])

model.compile(optimizer='adam',
              loss=tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True),
              metrics=['accuracy'])

model.summary()

model_checkpoint = keras.callbacks.ModelCheckpoint(
    "my_checkpoint.h5", save_best_only=True)
early_stopping = keras.callbacks.EarlyStopping(patience=10)

epochs = 100

history = model.fit(
    train_data_gen,
    steps_per_epoch=int(np.ceil(train_data_gen.n / float(BATCH_SIZE))),
    epochs=epochs,
    validation_data=val_data_gen,
    validation_steps=int(np.ceil(val_data_gen.n / float(BATCH_SIZE))),
    callbacks=[early_stopping, model_checkpoint]
)

acc = history.history['accuracy']
val_acc = history.history['val_accuracy']

loss = history.history['loss']
val_loss = history.history['val_loss']

epochs_range = range(27)

plt.figure(figsize=(8, 8))
plt.subplot(1, 2, 1)
plt.plot(epochs_range, acc, label='Training Accuracy')
plt.plot(epochs_range, val_acc, label='Validation Accuracy')
plt.legend(loc='lower right')
plt.title('Training and Validation Accuracy')

plt.subplot(1, 2, 2)
plt.plot(epochs_range, loss, label='Training Loss')
plt.plot(epochs_range, val_loss, label='Validation Loss')
plt.legend(loc='upper right')
plt.title('Training and Validation Loss')
plt.show()

from tensorflow.keras.models import Model
from tensorflow.keras.layers import Input

export_dir = 'saved_model/1'
tf.saved_model.save(model, export_dir)

loaded = tf.saved_model.load(export_dir)

converter = tf.lite.TFLiteConverter.from_saved_model(export_dir)

converter.optimizations = [tf.lite.Optimize.DEFAULT]

tflite_model = converter.convert()
tflite_model_file = 'converted_model.tflite'

with open(tflite_model_file, "wb") as f:
  f.write(tflite_model)

labels = ['No Stroke', 'Stroke']

with open('labels.txt', 'w') as f:
  f.write('\n'.join(labels))

try:
  from google.colab import files
  files.download('converted_model.tflite')
  files.download('labels.txt')
except:
  pass

import cv2
n = 5
for i in os.listdir(test_s):
    if i >= n:
        test_images = cv2.imread(i)
        test_labels = np.asarray([["Stroke"]])
        predictions = model.predict(test_images)
    else:
        break

for i in os.listdir(test_ns):
    if i >= n:
        test_images = cv2.imread(i)
        test_labels = np.asarray([["No Stroke"]])
        predictions = model.predict(test_images)
    else:
        break

predictions.shape

predictions[0]

np.argmax(predictions[0])

test_labels[0]

def plot_image(i, predictions_array, true_labels, images):
  predictions_array, true_label, img = predictions_array[i], true_labels[i], images[i]
  plt.grid(False)
  plt.xticks([])
  plt.yticks([])
  
  plt.imshow(img[...,0], cmap=plt.cm.binary)

  predicted_label = np.argmax(predictions_array)
  if predicted_label == true_label:
    color = 'blue'
  else:
    color = 'red'
  
  plt.xlabel("{} {:2.0f}% ({})".format(class_names[predicted_label],
                                100*np.max(predictions_array),
                                class_names[true_label]),
                                color=color)

def plot_value_array(i, predictions_array, true_label):
  predictions_array, true_label = predictions_array[i], true_label[i]
  plt.grid(False)
  plt.xticks([])
  plt.yticks([])
  thisplot = plt.bar(range(10), predictions_array, color="#777777")
  plt.ylim([0, 1])
  predicted_label = np.argmax(predictions_array)
  
  thisplot[predicted_label].set_color('red')
  thisplot[true_label].set_color('blue')

i = 0
plt.figure(figsize=(6,3))
plt.subplot(1,2,1)
plot_image(i, predictions, test_labels, test_images)
plt.subplot(1,2,2)
plot_value_array(i, predictions, test_labels)

i = 12
plt.figure(figsize=(6,3))
plt.subplot(1,2,1)
plot_image(i, predictions, test_labels, test_images)
plt.subplot(1,2,2)
plot_value_array(i, predictions, test_labels)

# Plot the first X test images, their predicted label, and the true label
# Color correct predictions in blue, incorrect predictions in red
num_rows = 5
num_cols = 3
num_images = num_rows*num_cols
plt.figure(figsize=(2*2*num_cols, 2*num_rows))
for i in range(num_images):
  plt.subplot(num_rows, 2*num_cols, 2*i+1)
  plot_image(i, predictions, test_labels, test_images)
  plt.subplot(num_rows, 2*num_cols, 2*i+2)
  plot_value_array(i, predictions, test_labels)


# Grab an image from the test dataset
img = test_images[0]

print(img.shape)

# Add the image to a batch where it's the only member.
img = np.array([img])

print(img.shape)

predictions_single = model.predict(img)

print(predictions_single)

plot_value_array(0, predictions_single, test_labels)
_ = plt.xticks(range(10), class_names, rotation=45)

np.argmax(predictions_single[0])

print(model.predict(r"D:\Downloads\Webp.net-resizeimage (1).jpg"))
print(model.predict(r"D:\Downloads\Webp.net-resizeimage.jpg"))
