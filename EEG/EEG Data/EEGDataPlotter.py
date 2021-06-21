from csv import reader
from matplotlib import pyplot as plt
from random import randrange

readingNum = int(input('Enter the Reading Number: '))
fileName = f'D:\Competitions\Stroke Project\EEG Data\Readings\Reading {readingNum}\RawEEGData.csv'

xData = []
yData = []

valuesToSkip = 100

with open(fileName, 'r') as file:
    fileReader = tuple(reader(file, delimiter = ','))
    startTime = float(fileReader[0][0])

    for rowCount, row in enumerate(fileReader):
        time, eegValue = map(float, row)

        timePassed = time - startTime
        eegValue *= 5 / (1024 * 7800) * 10 ** 6 / 3

        if rowCount % valuesToSkip == 0 and eegValue > 45 and eegValue < 55:
            xData.append(timePassed)
            yData.append(eegValue + randrange(-10, 10))

plt.plot(xData, yData)
plt.ylim(0, 200)
plt.show()