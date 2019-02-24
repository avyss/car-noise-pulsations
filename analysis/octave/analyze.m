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

display(['Starting analysys of: ' fileName]);
fileTitle = extract_recording_title(fileName);

data = load_recording_data(fileName);

Fs = data.pressureFs;      

pressureTimes  = data.pressureSamples(:,1);
pressureValues = data.pressureSamples(:,2);
useIndexes = find((pressureTimes >= timeRange(1)) 
                & (pressureTimes <= timeRange(2)));
pressureTimes  = pressureTimes(useIndexes);
pressureValues = pressureValues(useIndexes);

hasSpeedData = (length(data.speedSamples) > 0);
if hasSpeedData
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

hasBearingData = (length(data.bearingSamples) > 0);
if hasBearingData
  bearingTimes = data.bearingSamples(:,1);
  bearingValues = data.bearingSamples(:,2);
  bearingUseIndexes = find((speedTimes >= timeRange(1)) 
                       & (speedTimes <= timeRange(2)));
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
display(['Estimated decimation rate for input pressure values: ', num2str(subsampling_rate)]);

# replace NaNs in the measurements with "last-known" values
pressureValues = overrideNaNs(pressureValues);

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
clear figure;

subplot(2, 1, 1);
hold on;

# plot spectrogram
pkg load signal;
#specgram(pressureValues, 2^nextpow2(window), Fs, window, window-step);
[specS, specF, specT] = specgram(pressureValues, 2^nextpow2(window), Fs, window, window-step);
specT = specT + pressureTimes(1);
specS = abs(specS);
if (max(specS(:)) != 0)
  specS = specS / max(specS(:));   # normalize magnitude so that max is 0 dB.
endif
specS = max(specS, 10^(-40/10));   # clip below -40 dB.
specS = min(specS, 10^(-3/10));    # clip above -3 dB.
imagesc(specT, specF, log(specS));    # display in log scale
set(gca, "ydir", "normal"); # put the 'y' direction in the correct direction

axis([min(pressureTimes) max(pressureTimes)]);
xlabel('Time [sec]')
ylabel('Frequency [Hz]')
title(strrep(fileTitle,'_',':'));
grid on;

# mark frequency with maximum intensity for each window
minFreqCutoffIdx = round(length(specF) / 4);
[m,im] = max(specS(minFreqCutoffIdx:length(specF), :));
pulsationsFrequencies = specF(im + minFreqCutoffIdx - 1);
plot(specT, pulsationsFrequencies, 'o-c');

# analyze speed relation (if speed available)
if !hasSpeedData
  display('No speed data, further processing not possible');
  return;
endif

subplot(2, 1, 2);
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
[ax h1 h2] = plotyy(speedTimes, speedValues, bearingTimes, turn_rates .* turn_rate_dir);
set(h1, 'Marker', '^');
set(h1, 'Color', 'b');
set(h2, 'Marker', '.');
set(h2, 'Color', 'k');
axis(ax(1), [min(pressureTimes) max(pressureTimes)]);
axis(ax(2), [min(pressureTimes) max(pressureTimes)]);
set(ax(2), 'YLim', [-180 180]);
set(ax, {'ycolor'}, {'b';'k'})
xlabel('Time [sec]');
ylabel(ax(1), 'Speed [km/h]');
ylabel(ax(2), 'Trun rate [deg/sec]');
grid on;

figure(2);
clear figure;
hold on;
plotSpeed = interp1(speedTimes, speedValues, specT);
plot(plotSpeed, pulsationsFrequencies, 'x');
xlabel('speed');
ylabel('pulsations F');

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
