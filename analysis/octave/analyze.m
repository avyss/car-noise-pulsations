function analyze(fileName, timeRange)

if nargin() < 2
  timeRange = [-Inf, Inf];
  
  if nargin() < 1
    [fname, fpath] = uigetfile('../../recordings/');
    if (fname == 0) 
        display('processing canceled')
        return;
    endif
    fileName = [fpath, fname];
  endif
endif

param_SpecgramWindowSec = 5.0; # each spectral window covers <n> seconds
param_SpecgramWindowSteps = 3; # <k> overlapping slices per window
param_LowFrequencyCutoffFreqHz = 0.5; # low pressure frequencies to be ignored 
param_LowFrequencyCutoffApplyFilter = false;
param_LowFrequencyCutoffFilterOrder = 5;

display(['Starting analysys of: ' fileName]);
fileTitle = extract_recording_title(fileName);
figTitle = strrep(fileTitle,'_',':');

data = load_recording_data(fileName);

Fs = data.pressureFs;

# extract pressure data in specified time range
pressureTimes  = data.pressureSamples(:,1);
pressureValues = data.pressureSamples(:,2);
useIndexes = find((pressureTimes >= timeRange(1)) 
                & (pressureTimes <= timeRange(2)));
pressureTimes  = pressureTimes(useIndexes);
pressureValues = pressureValues(useIndexes);

# extract speed data in specified time range
if data.hasSpeed
  speedTimes = data.speedSamples(:,1);
  speedValues = data.speedSamples(:,2) * (60*60/1000);
  speedUseIndexes = find((speedTimes >= timeRange(1)) 
                     & (speedTimes <= timeRange(2)));
  speedTimes  = speedTimes(speedUseIndexes);
  speedValues = speedValues(speedUseIndexes);
else 
  speedTimes = [];
  speedValues = [];
endif

# extract bearing data in specified time range
if data.hasBearing
  bearingTimes = data.bearingSamples(:,1);
  bearingValues = data.bearingSamples(:,2);
  bearingUseIndexes = find((bearingTimes >= timeRange(1)) 
                       & (bearingTimes <= timeRange(2)));
  bearingTimes  = bearingTimes(bearingUseIndexes);
  bearingValues = bearingValues(bearingUseIndexes);
else 
  bearingTimes = [];
  bearingValues = [];
endif

# Determine oversampling rate in the measurements: 
#   some data point are just duplications of the previous points, so
#   sub-sample the signal accordingly
nTotalSamples = length(pressureValues);
nanIdx = find(isnan(pressureValues));
nMeaningfulSamples = nTotalSamples - length(nanIdx);
subsampling_rate = round(nTotalSamples / nMeaningfulSamples);
Fs = Fs / subsampling_rate;
display('')
display(['Estimated decimation rate for input pressure values: ', num2str(subsampling_rate)]);
display(['   Effective sampling frequency: ', num2str(Fs), ' Hz']);

# replace NaNs in the measurements with "last-known" values
pressureValues = overrideNaNs(pressureValues);

pressureValues = pressureValues([1 : subsampling_rate : length(pressureValues)]);
pressureTimes  = pressureTimes([1 : subsampling_rate : length(pressureTimes)]);

pkg load signal; %needed for specgram() and for butter()

# suppress the near-DC component of the pressure 
pressureValues = pressureValues - mean(pressureValues);
if param_LowFrequencyCutoffApplyFilter
  # apply low-pass filter to cut frequencies below significance threshold
  [lpf_b, lpf_a] = butter(param_LowFrequencyCutoffFilterOrder, 
                          param_LowFrequencyCutoffFreqHz / (Fs/2) * 0.75, 
                          'high');
  pressureValues = filter(lpf_b, lpf_a, pressureValues);
end

# calculate spectrogram 
window = ceil(param_SpecgramWindowSec * Fs);     
step = ceil(window/param_SpecgramWindowSteps);
[specS, specF, specT] = specgram(pressureValues, 2^nextpow2(window), Fs, window, window-step);
specT = specT + pressureTimes(1);
specS = abs(specS);
if (max(specS(:)) != 0)
  specS = specS / max(specS(:));   # normalize magnitude so that max is 0 dB.
endif
specS = max(specS, 10^(-40/10));   # clip below -40 dB.
specS = min(specS, 10^(-3/10));    # clip above -3 dB.

# find frequency with maximum intensity for each window
minFreqCutoffIdx = min(find(specF >= param_LowFrequencyCutoffFreqHz));
[m, maxIdx] = max(specS(minFreqCutoffIdx : length(specF), :));
pulsationsFrequencies = specF(maxIdx + minFreqCutoffIdx - 1);
significantFreqIdx = find(pulsationsFrequencies > specF(minFreqCutoffIdx));

display('');
display('Processing of pressure frequency spectrum done');
display(['   Frequency resolution: ', num2str(specF(2)), ' Hz']);
display(['   Maximal detectable frequency: ', num2str(max(specF)), ' Hz']);

figure(4);
clf;
title({figTitle, 'Pressure pulsations - spectrogram'});
hold on;
surf(specT, specF, log(specS));
colormap(cool)
xlabel('Time [sec]')
ylabel('Frequency [Hz]')
view(150, 45)

figure(1);
clf;

subplot(3, 1, 1);
title({figTitle, 'Pressure Pulsations Frequencies'});
hold on;

# plot spectrogram
imagesc(specT, specF, log(specS));    # display in log scale
set(gca, "ydir", "normal"); # put the 'y' direction in the correct direction
axis([min(pressureTimes) max(pressureTimes) 0 max(specF)]);
xlabel('Time [sec]')
ylabel('Frequency [Hz]')
grid on;
# plot detected frequencies
plot(specT, pulsationsFrequencies, '.-c');
plot(specT(significantFreqIdx), pulsationsFrequencies(significantFreqIdx), 'oc');


# analyze speed relation (if speed available)
if !data.hasSpeed
  display('No speed data, further processing not possible');
  return;
endif

subplot(3, 1, 2);
title('Speed')
hold on;

# make smooth plot of turn rate even when the bearing crosses 0<->360deg boundary
# present the bearing as a vector on unit circle
# plot the result
plot(speedTimes, speedValues, 'kd-');
axis([min(pressureTimes) max(pressureTimes)]);
xlabel('Time [sec]');
ylabel('Speed [km/h]');
grid on;

subplot(3, 1, 3);
title('Bearing & Turn rate')
hold on;

# make smooth plot of turn rate even when the bearing crosses 0<->360deg boundary
# present the bearing as a vector on unit circle
bearing_x = cos(bearingValues/180*pi);
bearing_y = sin(bearingValues/180*pi);
bearing_deltas_x = [diff(bearing_x); 0];
bearing_deltas_y = [diff(bearing_y); 0];
# turn rate (absolute):
turn_rates = sqrt(bearing_deltas_x.^2 + bearing_deltas_y.^2) ./ pi*180 ./ [diff(bearingTimes); Inf];
# turn rate direction - determine it as the sign of Z coordinate of the cross product 
# between the bearing direction and the delta-bearing direction (z=ax*by - ay*bx)
turn_rate_dir = sign(bearing_x .* bearing_deltas_y - bearing_y .* bearing_deltas_x);
# plot the result
[ax h1 h2] = plotyy(bearingTimes, bearingValues, bearingTimes, turn_rates .* turn_rate_dir);
set(h1, 'Marker', '^');
set(h1, 'Color', 'b');
set(h2, 'Marker', '.');
set(h2, 'Color', 'k');
axis(ax(1), [min(pressureTimes) max(pressureTimes)]);
axis(ax(2), [min(pressureTimes) max(pressureTimes)]);
set(ax(1), 'YLim', [0 360]);
set(ax(2), 'YLim', [-20 20]);
set(ax, {'ycolor'}, {'b';'k'})
xlabel('Time [sec]');
ylabel(ax(1), 'Bearing [deg]');
ylabel(ax(2), 'Trun rate [deg/sec]');
grid on;

figure(2);
clf;
title({figTitle, 'Pulsation frequency vs. Speed'});
hold on;
speedPlotTimes = specT(significantFreqIdx);
speedPlotValues = interp1(speedTimes, speedValues, speedPlotTimes);
plotFreqs = pulsationsFrequencies(significantFreqIdx);
if data.hasWind
  windSpeed = data.wind(2) * (60*60/1000);
  windDirection = data.wind(3);
  windDirDifference = interp1(bearingTimes, bearingValues, speedPlotTimes) - (180-windDirection);
  speedPlotValues = speedPlotValues - windSpeed * cos(windDirDifference/180*pi);
end

plot(speedPlotValues, plotFreqs, 'o');
xlabel('speed');
ylabel('F');

display('');
display(['Finished analysys of: ' fileName]);

endfunction

function r = overrideNaNs(v)
  r = v;
  
  firstNotNanIdx = min(find(!isnan(v)));
  if length(firstNotNanIdx) == 0
    return;
  endif
  
  r(1:firstNotNanIdx) = r(firstNotNanIdx);
  
  for i = firstNotNanIdx + 1 : size(r)
    if isnan(r(i))
        r(i) = r(i-1);
    endif
  endfor
  
endfunction
