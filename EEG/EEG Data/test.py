import numpy as np
data = np.loadtxt('data.txt')

import matplotlib.pyplot as plt

# Define sampling frequency and time vector
sf = 100.
time = np.arange(data.size) / sf

# Plot the signal
plt.plot(time, data)
plt.xlabel('Time (seconds)')
plt.ylabel('Voltage')
plt.xlim([time.min(), time.max()])
plt.title('Raw EEG data')
plt.show()

from scipy import signal

# Define window length (4 seconds)
win = 4 * sf
freqs, psd = signal.welch(data, sf, nperseg=win)

for i in range(len(freqs)):
    if freqs[i] >= 2:
        psd[i] += 25
    psd[i] *= 5000/300

# Plot the power spectrum
plt.figure(figsize=(8, 4))
plt.plot(freqs, psd, color='k', lw=2)
plt.xlabel('Frequency (Hz)')
plt.ylabel('Power spectral density (V^2 / Hz)')
plt.ylim([0, psd.max() * 1.1])
plt.title("Frequency Distribution")
plt.xlim([0.6, 30])
plt.show()
