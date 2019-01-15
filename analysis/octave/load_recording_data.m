function data = load_recording_data(prefix)
  data.pressureSamples = do_load(prefix, 'pressure_samples');
  data.pressureFs      = do_load(prefix, 'pressure_fs');
  data.speedSamples    = do_load(prefix, 'speed_samples');
  data.speedFs         = do_load(prefix, 'speed_fs');
endfunction


function d = do_load(prefix, field)
  
  filename = [prefix ' - ' field '.txt'];
  
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
