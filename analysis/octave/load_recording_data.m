function data = load_recording_data(fileName, fileTitle)
  subDir = fileTitle;
  unzipFilesDir = [tempdir() '/' subDir];
  if exist(unzipFilesDir)
    delete([unzipFilesDir '/*.*']);
  else
    mkdir(unzipFilesDir);
  endif
  
  disp(['unzipping recording data into: ' unzipFilesDir]);
  
  unzip(fileName, unzipFilesDir);
  data.pressureSamples = do_load(unzipFilesDir, 'pressure_samples');
  data.pressureFs      = do_load(unzipFilesDir, 'pressure_fs');
  data.speedSamples    = do_load(unzipFilesDir, 'speed_samples');
  data.speedFs         = do_load(unzipFilesDir, 'speed_fs');
endfunction


function d = do_load(dir, part)
  
  filename = [dir '/' part '.csv'];
  
  try
	    d = load('-ascii', filename);
  catch
    if length(strfind(lasterr(), "seems to be empty!")) > 0
      d = [];
    else
      error(lasterr());
    endif
  end_try_catch
  
endfunction
