from csv import reader
from matplotlib import pyplot as plt
from matplotlib.ticker import PercentFormatter

band = input('Enter the band name: ').strip().lower()
readingNum = int(input('Enter the Reading Number: '))
fileName = f'D:\Competitions\Stroke Project\EEG Data\Readings\Reading {readingNum}\BandpowerResults.csv'

xData = []
yData = []

coeff = {6: 1.2, 7: 1.7, 8: 2.1, 9: 1.5, 10: 1.4, 12: 1.3}

with open(fileName, 'r') as file:
    fileReader = tuple(reader(file, delimiter = ','))
    startTime = float(fileReader[0][0])

    for row in fileReader:
        time, delta, theta, alpha, beta = map(float, row)
        bandpowerSum = delta + theta + alpha + beta

        timePassed = time - startTime

        delta /= bandpowerSum
        theta /= bandpowerSum
        alpha /= bandpowerSum
        beta /= bandpowerSum

        xData.append(timePassed / 1000)
        bandValue = {'alpha': alpha, 'beta': beta, 'theta': theta, 'delta': delta}
        yData.append(bandValue[band])

band = band[0].upper() + band[1:]
if readingNum in coeff.keys():
    yData = [r * coeff[readingNum] for i, r in enumerate(yData)]
plt.plot(xData, yData)
plt.ylim(0, 1)
plt.title(f'Relative {band} Bandpower over time')
plt.xlabel('Time (s)')
plt.ylabel(f'Relative {band} Bandpower')
plt.gca().yaxis.set_major_formatter(PercentFormatter(1))
plt.show()