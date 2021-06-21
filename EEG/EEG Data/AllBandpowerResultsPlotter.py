from csv import reader
from matplotlib import pyplot as plt
from matplotlib.ticker import PercentFormatter

readingNum = int(input('Enter the Reading Number: '))
fileName = f'D:\Competitions\Stroke Project\EEG Data\Readings\Reading {readingNum}\BandpowerResults.csv'

coeff = {6: 1.2, 7: 1.7, 8: 2.1, 9: 1.5, 10: 1.4, 12: 1.3}

def getData(bandName, fileName):
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
			yData.append(bandValue[bandName])

	return [r - 50 for i, r in enumerate(xData) if i % 1 == 0 ], [r for i, r in enumerate(yData) if i % 1 == 0 ]

plt.rcParams['font.weight'] = 'bold'
plt.rcParams['axes.labelweight'] = 'bold'
plt.title(r"$\bf{{{Relative}}}$ $\bf{{{Bandpowers}}}$ $\bf{{{of}}}$ $\bf{{{EEG}}}$ $\bf{{{Bands}}}$ $\bf{{{over}}}$ $\bf{{{time}}}$")
plt.plot(*getData('alpha', fileName), label = 'alpha', lw = 3)
plt.plot(*getData('beta', fileName), label = 'beta', lw = 3)
plt.plot(*getData('theta', fileName), label = 'theta', lw = 3)
plt.plot(*getData('delta', fileName), label = 'delta', lw = 3)
plt.ylim(0, 0.6)
plt.xlim(0, 250)
plt.xlabel('Time (s)')
plt.ylabel(f'Relative Bandpower')
plt.gca().yaxis.set_major_formatter(PercentFormatter(1))
plt.legend()
plt.show()