from __future__ import absolute_import, division, print_function, unicode_literals
import tensorflow as tf
from tensorflow.keras.preprocessing.image import ImageDataGenerator
import os
import numpy as np
import matplotlib.pyplot as plt

base_dir = r"D:\Competitions\Stroke Project\Facial Droop Database"
train_dir = r"D:\Competitions\Stroke Project\Facial Droop Database\train"
val_dir = r"D:\Competitions\Stroke Project\Facial Droop Database\val"

train_s = r"D:\Competitions\Stroke Project\Facial Droop Database\train\Stroke"
train_ns = r"D:\Competitions\Stroke Project\Facial Droop Database\train\No Stroke"

val_s = r"D:\Competitions\Stroke Project\Facial Droop Database\val\Stroke"
val_ns = r"D:\Competitions\Stroke Project\Facial Droop Database\val\No Stroke"

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

BATCH_SIZE = 30
IMG_SHAPE = 220

# This function will plot images in the form of a grid with 1 row and 5 columns where images are placed in each column.
def plotImages(images_arr):
    fig, axes = plt.subplots(1, 5, figsize=(20,20))
    axes = axes.flatten()
    for img, ax in zip(images_arr, axes):
        ax.imshow(img)
    plt.tight_layout()
    plt.show()

image_gen = ImageDataGenerator(rescale=1./255, zoom_range=0.2)

train_data_gen = image_gen.flow_from_directory(batch_size=BATCH_SIZE,
                                               directory=train_dir,
                                               shuffle=True,
                                               target_size=(IMG_SHAPE, IMG_SHAPE),
                                               class_mode='binary')

augmented_images = [train_data_gen[0][0][0] for i in range(5)]
plotImages(augmented_images)

image_gen_val = ImageDataGenerator(rescale=1./255)

val_data_gen = image_gen_val.flow_from_directory(batch_size=BATCH_SIZE,
                                                 directory=val_dir,
                                                 target_size=(IMG_SHAPE, IMG_SHAPE),
                                                 class_mode='binary')

model = tf.keras.models.Sequential([
    tf.keras.layers.Conv2D(32, (3,3), activation='relu', input_shape=(IMG_SHAPE, IMG_SHAPE, 3)),
    tf.keras.layers.MaxPooling2D(2, 2),

    tf.keras.layers.Conv2D(64, (3,3), activation='relu'),
    tf.keras.layers.MaxPooling2D(2,2),

    tf.keras.layers.Conv2D(128, (3,3), activation='relu'),
    tf.keras.layers.MaxPooling2D(2,2),

    tf.keras.layers.Conv2D(128, (3,3), activation='relu'),
    tf.keras.layers.MaxPooling2D(2,2),

    tf.keras.layers.Dropout(0.2),
    tf.keras.layers.Flatten(),
    tf.keras.layers.Dense(512, activation='relu'),
    tf.keras.layers.Dense(2)
])

model.compile(optimizer='adam',
              loss=tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True),
              metrics=['accuracy'])

model.summary()

epochs=30
history = model.fit(
    train_data_gen,
    steps_per_epoch=int(np.ceil(num_train / float(BATCH_SIZE))),
    epochs=epochs,
    validation_data=val_data_gen,
    validation_steps=int(np.ceil(num_val / float(BATCH_SIZE)))
)

acc = history.history['accuracy']
val_acc = history.history['val_accuracy']

loss = history.history['loss']
val_loss = history.history['val_loss']

epochs_range = range(epochs)

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
