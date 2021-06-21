from __future__ import absolute_import, division, print_function, unicode_literals

import os
import numpy as np
import glob
import shutil
import matplotlib.pyplot as plt
import tensorflow as tf
from tensorflow import keras

from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Conv2D, Flatten, Dropout, MaxPooling2D
from tensorflow.keras.preprocessing.image import ImageDataGenerator

base_dir = r"D:/Competitions/Stroke Project/Facial Droop Database"
train_dir = r"D:/Competitions/Stroke Project/Facial Droop Database/train"
val_dir = r"D:/Competitions/Stroke Project/Facial Droop Database/val"

train_s = r"D:/Competitions/Stroke Project/Facial Droop Database/train/Stroke"
train_ns = r"D:/Competitions/Stroke Project/Facial Droop Database/train/No Stroke"

val_s = r"D:/Competitions/Stroke Project/Facial Droop Database/val/Stroke"
val_ns = r"D:/Competitions/Stroke Project/Facial Droop Database/val/No Stroke"

num_train_ns = len(os.listdir(train_ns))
num_train_s = len(os.listdir(train_s))

num_val_ns = len(os.listdir(val_ns))
num_val_s = len(os.listdir(val_s))

num_train = num_train_ns + num_train_s
num_val = num_val_ns + num_val_s

print('Total training stroke images:', num_train_s)
print('Total training no stroke images:', num_train_ns)

print('Total val stroke images:', num_val_s)
print('Total val no stroke images:', num_val_ns)
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
                    )


train_data_gen = image_gen_train.flow_from_directory(
                                                batch_size=BATCH_SIZE,
                                                directory=train_dir,
                                                shuffle=True,
                                                target_size=(IMG_SHAPE,IMG_SHAPE),
                                                class_mode='sparse'
                                                )

augmented_images = [train_data_gen[0][0][i] for i in range(5)]
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

epochs_range = range(len(history.history['loss']))

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

export_dir = r'D:\Competitions\Stroke Project\Code\Facial Droop\saved_model\1'
tf.saved_model.save(model, export_dir)

converter = tf.lite.TFLiteConverter.from_saved_model(export_dir)
        
converter.optimizations = [tf.lite.Optimize.DEFAULT]

tflite_model = converter.convert()
tflite_model_file = r"D:\Competitions\Stroke Project\Code\Facial Droop\converted_model.tflite"

with open(tflite_model_file, "wb") as f:
    f.write(tflite_model)

labels = ['No Stroke', 'Stroke']

with open('labels.txt', 'w') as f:
    f.write('\n'.join(labels))

print("Successful")

print(model.predict(r"D:\Downloads\Webp.net-resizeimage (1).jpg"))
print(model.predict(r"D:\Downloads\Webp.net-resizeimage.jpg"))
