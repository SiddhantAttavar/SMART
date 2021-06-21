from csv import reader
from matplotlib import pyplot as plt
from matplotlib.ticker import PercentFormatter

coeff = {6: 1.1, 7: 1.3, 8: 1.3, 9: 1.5, 10: 1.5, 12: 1.3}

def getData(fileName, band):
    xData = []
    yData = []

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

    return xData, yData

band = input('Enter the band name: ').strip().lower()

plt.rcParams['font.weight'] = 'bold'
plt.rcParams['axes.labelweight'] = 'bold'

readingNum1 = int(input('Enter the control Reading Number: '))
fileName1 = f'D:\Competitions\Stroke Project\EEG Data\Readings\Reading {readingNum1}\BandpowerResults.csv'
xData1, yData1 = getData(fileName1, band)

readingNum2 = int(input('Enter the main Reading Number: '))
fileName2 = f'D:\Competitions\Stroke Project\EEG Data\Readings\Reading {readingNum2}\BandpowerResults.csv'
xData2, yData2 = getData(fileName2, band)

xData2 = [xData1[-1] + i for i in xData2]
yData2 = [r * coeff[readingNum2] for i, r in enumerate(yData2)]

#xData = xData1 + xData2
#yData = yData1 + yData2

plt.plot(xData1, yData1, lw = 3)
plt.plot(xData2, yData2, lw = 3)
plt.axvline(x = 300, color = 'r', label = 'Introduction of SSVEP')

band = band[0].upper() + band[1:]
plt.xlim(0, xData2[-1])
plt.ylim(0, 1)
plt.xlabel('Time (s)')
plt.ylabel(f'Relative {band} Bandpower')

plt.title(r"$\bf{{{Effect}}}$ $\bf{{{of}}}$ $\bf{{{SSVEP}}}$ $\bf{{{on}}}$ $\bf{{{Relative}}}$ $\bf{{{" + band + r"}}}$ $\bf{{{Bandpower}}}$")
plt.legend()
plt.gca().yaxis.set_major_formatter(PercentFormatter(1))
plt.show()