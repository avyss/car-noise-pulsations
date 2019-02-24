function title = extract_recording_title(fileName)
    fileExtPos = strfind(fileName, '.zip');
    if size(fileExtPos) == 0
        error('recording file name must end with .zip');
        return;
    endif
    
    lastSlashPos = max([0, strfind(fileName, '/'), strfind(fileName, '\')]);
    
    title = fileName(lastSlashPos + 1 : fileExtPos - 1);
endfunction
