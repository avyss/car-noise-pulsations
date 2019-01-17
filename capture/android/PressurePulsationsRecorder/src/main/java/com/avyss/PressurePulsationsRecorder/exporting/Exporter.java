package com.avyss.PressurePulsationsRecorder.exporting;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.avyss.PressurePulsationsRecorder.acquisition.RecordingDetails;
import com.avyss.PressurePulsationsRecorder.acquisition.AbstractSampleCollector;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Exporter {

    private static final String FILE_SHARING_AUTH_NAME = "com.avyss.PressurePulsationsRecorder.recordingSharing";

    private final Activity parentActivity;

    public Exporter(Activity parentActivity) {
        this.parentActivity = parentActivity;
    }

    public void exportResults(
            Context context,
            RecordingDetails recDetails,
            AbstractSampleCollector pressureCollector,
            AbstractSampleCollector speedCollector) throws IOException {

        String zipFileName = generateFileName(recDetails);

        ZipPacker zp = new ZipPacker(parentActivity.getBaseContext().getCacheDir(), zipFileName);
        try {
            zp.addSamples("pressure", pressureCollector);
            zp.addSamples("speed", speedCollector);
        } finally {
            zp.close();
        }

        File zipFile = zp.getZipFile();

        shareResults(context, Collections.singletonList(zipFile));

    }

    private String generateFileName(RecordingDetails recDetails) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String startTimestamp = dateFormat.format(recDetails.getRecordingStartTime());

        String fileName = startTimestamp;
        if (!recDetails.getTitle().isEmpty()) {
            fileName += " - " + recDetails.getTitle();
        }

        fileName += ".zip";

        return fileName;
    }

    private void shareResults(Context context, List<File> resultFiles) {

        ArrayList<Uri> fileUris = new ArrayList<>();
        for (File file: resultFiles) {
            Log.d("results", "sharing file " + file.getName());
            Uri contentUri = FileProvider.getUriForFile(context, FILE_SHARING_AUTH_NAME, file);
            context.grantUriPermission(FILE_SHARING_AUTH_NAME, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileUris.add(contentUri);
        }

        Intent intentShareFile = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intentShareFile.setType("application/binary");
        intentShareFile.putExtra(Intent.EXTRA_STREAM, fileUris);

        parentActivity.startActivity(Intent.createChooser(intentShareFile, "Share the recording result"));
    }
}
