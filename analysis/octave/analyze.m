function analyze
#file_title = '2018-12-25 16_47_25 good sample';
#file_title = '2018-12-26 15_23_02 - dtive home by route 6';
#file_title = '2018-12-28 11_19_55 - half-open window';
#file_title = '2018-12-30 10_35_21 - quarter-open window';
#file_title = '2019-01-05 10_35_43 - ups and downs on 6';

file_title = '2019-01-05 09_31_22 - ups and downs';

display(['Starting analysys of: ' file_title]);

data = load_recording_data(file_title);

Fs = data.pressureFs;      

pressureTimes = data.pressureSamples(:,1);
pressureValues = data.pressureSamples(:,2);

# Determine oversampling rate in the measurements: 
#   some data point are just duplications of the previous points, so
#   sub-sample the signal accordingly
nTotalSamples = length(pressureValues);
nonDuplicateSamples = pressureValues([1 : nTotalSamples-1]) - pressureValues([2 : nTotalSamples]);
nNonDuplicates = length(find(nonDuplicateSamples) != 0);
subsampling_rate = round(length(pressureValues) / nNonDuplicates);
display(['Estimated duplication rate in input pressure values: ', num2str(subsampling_rate)]);

pressureValues = pressureValues([1 : subsampling_rate : length(pressureValues)]);
pressureTimes  = pressureTimes([1 : subsampling_rate : length(pressureTimes)]);
Fs = Fs / subsampling_rate;

# Cut the near-DC component of the pressure 
pressureValues = pressureValues - mean(pressureValues);
#[lpf_b, lpf_a]=butter(10, 0.3);
#pressureDC = filter(lpf_b, lpf_a, pressureValues);
#pressureValues = pressureValues - pressureDC;

window_sec = 10; # one spectral slice every 3 sec
window = ceil(window_sec*Fs);     
step = ceil(window/3);

figure(1);
hold off;
subplot(2, 1, 1);

# plot spectrogram
pkg load signal;
specgram(pressureValues, 2^nextpow2(window), Fs, window, window-step);
[specS, specF, specT] = specgram(pressureValues, 2^nextpow2(window), Fs, window, window-step);
axis([min(pressureTimes) max(pressureTimes)]);
xlabel('Time [sec]')
ylabel('Frequency [Hz]')
title(strrep(file_title,'_',':'));
grid on;
hold on;

# mark frequency with maximum intensity for each window
specS = abs(specS);
minFreqCutoffIdx = round(length(specF) / 4);
[m,im] = max(specS(minFreqCutoffIdx:length(specF), :));
pulsationsFrequencies = specF(im + minFreqCutoffIdx - 1);
plot(specT, pulsationsFrequencies, 'o-c');

# plot speed (if available)
if length(data.speedSamples) == 0
  display('No speed data, further processing not possible');
  return;
endif

subplot(2, 1, 2);
speedTimes = data.speedSamples(:,1);
speedValues = data.speedSamples(:,2) * (60*60/1000);
plot(speedTimes, speedValues, '^-');
axis([min(pressureTimes) max(pressureTimes)]);
xlabel('Time [sec]');
ylabel('Speed [km/h]');
grid on;

figure(2);

plot(interp1(speedTimes, speedValues, specT), pulsationsFrequencies, 'x');
xlabel('speed');
ylabel('pulsations F');

display(['Finished analysys of: ' file_title]);
