from collections import defaultdict
from csv import reader
from matplotlib import pyplot as plt

readingNum = int(input('Enter the Reading Number: '))
fileName = f'D:\Competitions\Stroke Project\EEG Data\Readings\Reading {readingNum}\FFTResults.csv'

freqDiff = 0.25
finalAmplitudes = defaultdict(lambda: 0)

sampleNum = 0

with open(fileName, 'r') as file:
    fileReader = tuple(reader(file, delimiter = ','))
    sampleNum = len(fileReader) // 121

    for row in fileReader:
        time, frequency, amplitude = map(float, row)

        finalAmplitudes[int(frequency / freqDiff)] += amplitude
    
for key in finalAmplitudes:
    finalAmplitudes[key] /= sampleNum

finalAmplitudes.pop(0)

xData = []
yData = []

for key in sorted(finalAmplitudes.keys()):
    frequency = key * freqDiff
    xData.append(frequency)

    amplitude = finalAmplitudes[key]
    amplitude *= 5 / (1024 * 7800)

    #if frequency >= 1.25 and frequency <= 15:
    #    amplitude = amplitude / 3 + 0.0005

    yData.append(amplitude * amplitude * 10 ** 12 / frequency)

xData = [0] + [i + freqDiff for i in xData]
yData = [0] + [i ** 0.5 for i in yData]
plt.plot(xData, yData)
plt.xlim(0, 30)
#plt.ylim(0, 10 ** 6)
plt.title('FFT Results')
plt.xlabel('Frequency (Hz)')
plt.ylabel('Amplitude µV\u00b2/Hz')
plt.show()
