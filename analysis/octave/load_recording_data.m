function data = load_recording_data(fileName)
  
  recTitle = extract_recording_title(fileName);
  
  subDir = recTitle;
  
  unzipFilesDir = [tempdir(), filesep(), subDir];
  if exist(unzipFilesDir)
    delete([unzipFilesDir, filesep(), '*.*']);
  else
    mkdir(unzipFilesDir);
  endif
  
  disp('');
  disp(['Unzipping recording data into: ' unzipFilesDir]);
  
  unzip(fileName, unzipFilesDir);
  data.format          = do_load(unzipFilesDir, 'format',           false);
  data.pressureSamples = do_load(unzipFilesDir, 'pressure_samples', true);
  data.pressureFs      = do_load(unzipFilesDir, 'pressure_fs',      true);
  data.speedSamples    = do_load(unzipFilesDir, 'speed_samples',    false);
  data.speedFs         = do_load(unzipFilesDir, 'speed_fs',         true);
  data.hasSpeed        = (length(data.speedSamples) > 0);
  data.bearingSamples  = do_load(unzipFilesDir, 'bearing_samples',  false);
  data.bearingFs       = do_load(unzipFilesDir, 'bearing_fs',       true);
  data.hasBearing      = (length(data.bearingSamples) > 0);
  data.wind            = do_load(unzipFilesDir, 'wind',             false);
  data.hasWind         = (length(data.wind) > 0);

  display('loaded recording data: ');
  display(['   recording format version:  ', num2str(data.format(1)) '.' num2str(data.format(2))]);
  display(['   pressure recording length: ', num2str(floor(max(data.pressureSamples(:, 1)))), ' seconds']);
  
  presenseStr = {'not present', 'present'};
  display(['   speed data:                ', presenseStr{1+data.hasSpeed}]);
  display(['   bearing data:              ', presenseStr{1+data.hasBearing}]);
  display(['   wind data:                 ', presenseStr{1+data.hasWind}]);
  
endfunction


function d = do_load(dir, part, mandatory)
  
  filename = [dir '/' part '.csv'];
  
  try
	    d = load('-ascii', filename);
  catch
    err = lasterr();
    
    if (mandatory)
      error(err);
    endif
    
    # not mandatory 
    if length(strfind(err, "load: unable to find file")) > 0
        d = [];
        return;
    endif
    if length(strfind(err, "seems to be empty!")) > 0
        d = [];
        return;
    endif
    
    error(err)
  end_try_catch
  
endfunction
