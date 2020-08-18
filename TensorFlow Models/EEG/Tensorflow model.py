import tensorflow as tf
import tensorflow.keras as keras
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

df = pd.read_excel(r"D:/Competitions/Stroke Project/EEG Data.xlsx")

data = df.to_numpy()
delta = data[:,7]
theta = data[:,8]
alpha = data[:,9]
beta = data[:,10]
EEG = data[:,12]
DTR = data[:,16]
DAR = data[:,17]

data_modified = np.array([delta, theta, alpha, beta, EEG, DTR, DAR])
train_input_list = []

for i in range(len(data_modified[0])):
    train_input_list.append([])
    for j in range(len(data_modified)):
        train_input_list[i].append(data_modified[j][i])

train_input = np.array(train_input_list)

train_output_modified = df.to_numpy()[:,:1]
label_list = ["control", "stroke"]
train_output_list = []

for i in range(len(train_output_modified)):
    l = []
    l.append(float(label_list.index(train_output_modified[i])))
    if float(label_list.index(train_output_modified[i])) == 0:
        l.append(1.)
    else:
        l.append(0.)
    train_output_list.append(l)
    
train_output = np.array(train_output_list)

model = keras.Sequential([
    tf.keras.layers.Dense(units = 28, input_shape = (7,)),
    tf.keras.layers.Dense(units = 14),
    tf.keras.layers.Dense(units = 7),
    tf.keras.layers.Dense(units = 2, activation = 'softmax')])

BATCH_SIZE = 19

loss='binary_crossentropy'
optimizer = keras.optimizers.Adam(0.1)

model.compile(loss = loss, optimizer = optimizer, metrics = ['accuracy'])
model.summary()

epochs = 100
history = model.fit(train_input, train_output, epochs = epochs)

plt.xlabel('Epoch Number')
plt.ylabel('Loss Magnitude')
plt.plot(history.history['loss'])

prediction = []
print(predict_input1 = train_input[0])
print(predict_input2 = train_input[19])

print(model.predict(predict_input1[np.newaxis,:])[0])
print(model.predict(predict_input2[np.newaxis,:])[0])
