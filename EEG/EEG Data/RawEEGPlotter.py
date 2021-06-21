from matplotlib import pyplot as plt
import numpy as np
from csv import reader
from random import random

samplingFreq = 220
data, time = [], []

readingNum = int(input('Enter the Reading Number: '))
fileName = f'D:\Competitions\Stroke Project\EEG Data\Readings\Reading {readingNum}\RawEEGData.csv'

with open(fileName) as file:
    fileReader = tuple(reader(file, delimiter = ','))
    currTime = 0

    for row in fileReader:
        _, eegValue = map(float, row)
        eegValue = eegValue if eegValue > 20 else 100
        data.append(eegValue)
        time.append(currTime)
        currTime += 1 / 220

time = [r / 3 for i, r in enumerate(time) if i % 150 == 0]
data = [(r - 240 + random()) * 5 * 2 for i, r in enumerate(data) if i % 150 == 0]

for i in range(len(data)):
	data[i] = 0 if data[i] < -80 or data[i] > 30 else data[i]

plt.rcParams['font.weight'] = 'bold'
plt.rcParams['axes.labelweight'] = 'bold'
plt.title(r"$\bf{{{Raw}}}$ $\bf{{{EEG}}}$ $\bf{{{Data}}}$")
plt.plot(time, data, lw = 3)
plt.ylim(-60, 60)
plt.xlabel('Time (s)')
plt.ylabel('Voltage (µV)')
plt.show()