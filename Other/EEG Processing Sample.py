# imports
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from scipy.integrate import simps
from scipy.signal import welch

# load data
# This sample data is from a patient who was sleeping
# There is a large amount of delta waves in the data
data = np.loadtxt('Sample EEG Data 3.txt')

sns.set(font_scale = 1.2)

# Define sampling frequency and time vector
sf = 100.
time = np.arange(data.size) / sf

# Plot the raw data
fig, ax = plt.subplots(1, 1, figsize=(12, 4))
plt.plot(time, data, lw = 1.5, color = 'k')
plt.xlabel('Time (seconds)')
plt.ylabel('Voltage')
plt.xlim([time.min(), time.max()])
plt.title('N3 sleep EEG data (F3)')
sns.despine()
plt.show()

# Define window length (4 seconds)
win = 4 * sf
freqs, psd = welch(data, sf, nperseg=win)

# Define the different bandpower's lower and upper limits
delta_low, delta_high = 0.5, 4
theta_low, theta_high = 4, 8
alpha_low, alpha_high = 8, 12
beta_low, beta_high = 12, 30


# Find intersecting values in frequency vector
idx_delta = np.logical_and(freqs >= delta_low, freqs <= delta_high)
idx_theta = np.logical_and(freqs >= theta_low, freqs <= theta_high)
idx_alpha = np.logical_and(freqs >= alpha_low, freqs <= alpha_high)
idx_beta = np.logical_and(freqs >= beta_low, freqs <= beta_high)

# Plot the power spectral density and fill the delta area
plt.figure(figsize = (7, 4))
plt.plot(freqs, psd, lw = 2, color = 'k')
plt.fill_between(freqs, psd, where = idx_delta, color = 'blue')
plt.fill_between(freqs, psd, where = idx_theta, color = 'green')
plt.fill_between(freqs, psd, where = idx_alpha, color = 'yellow')
plt.fill_between(freqs, psd, where = idx_beta, color = 'red')
plt.xlabel('Frequency (Hz)')
plt.ylabel('Power spectral density (uV^2 / Hz)')
plt.xlim([0, max(freqs)])
plt.ylim([0, psd.max() * 1.1])
plt.title("Welch's periodogram")
sns.despine()
plt.show()

# Frequency resolution
freq_res = freqs[1] - freqs[0]  # = 1 / 4 = 0.25

# Compute the absolute power by approximating the area under the curve
delta_power = simps(psd[idx_delta], dx = freq_res)
theta_power = simps(psd[idx_theta], dx = freq_res)
alpha_power = simps(psd[idx_alpha], dx = freq_res)
beta_power = simps(psd[idx_beta], dx = freq_res)
print('Absolute delta power: %.3f uV^2' % delta_power)
print('Absolute theta power: %.3f uV^2' % theta_power)
print('Absolute alpha power: %.3f uV^2' % alpha_power)
print('Absolute beta power: %.3f uV^2' % beta_power)

# Relative delta power (expressed as a percentage of total power)
total_power = simps(psd, dx = freq_res)
delta_rel_power = delta_power / total_power
print('Relative delta power: %.3f' % delta_rel_power)

# Function to calculate bandpowers
# i.e. alpha, beta, theta, delta, gamma
def bandpower(data, sf, band, window_sec = None, relative = False):
    """Compute the average power of the signal x in a specific frequency band.

    Parameters
    ----------
    data : 1d-array
        Input signal in the time-domain.
    sf : float
        Sampling frequency of the data.
    band : list
        Lower and upper frequencies of the band of interest.
    window_sec : float
        Length of each window in seconds.
        If None, window_sec = (1 / min(band)) * 2
    relative : boolean
        If True, return the relative power (= divided by the total power of the signal).
        If False (default), return the absolute power.

    Return
    ------
    bp : float
        Absolute or relative band power.
    """
    band = np.asarray(band)
    low, high = band

    # Define window length
    if window_sec is not None:
        nperseg = window_sec * sf
    else:
        nperseg = (2 / low) * sf

    # Compute the modified periodogram (Welch)
    freqs, psd = welch(data, sf, nperseg=nperseg)

    # Frequency resolution
    freq_res = freqs[1] - freqs[0]

    # Find closest indices of band in frequency vector
    idx_band = np.logical_and(freqs >= low, freqs <= high)

    # Integral approximation of the spectrum using Simpson's rule.
    bp = simps(psd[idx_band], dx=freq_res)

    if relative:
        bp /= simps(psd, dx=freq_res)
    return bp

# Define the duration of the window to be 4 seconds
win_sec = 4

# Delta/beta ratio based on the absolute power
dar = delta_power / alpha_power
dbr = delta_power / beta_power

print()
print('Delta / alpha ratio: %.3f' % dar)
print('Delta / beta ratio: %.3f' % dbr)
