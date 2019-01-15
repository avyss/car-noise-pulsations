package com.avyss.PressurePulsationsRecorder.exporting;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.avyss.PressurePulsationsRecorder.acquisition.RecordingDetails;
import com.avyss.PressurePulsationsRecorder.acquisition.AbstractSampleCollector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class Exporter {

    private static final String DEFAULT_FILE_EXT = ".txt";
    private static final String NO_SAMPLES_MARKER = "#-------------NO SAMPLES------------";
    public static final String FILE_SHARING_AUTH_NAME = "com.avyss.PressurePulsationsRecorder.recordingSharing";

    private final Activity parentActivity;

    public Exporter(Activity parentActivity) {
        this.parentActivity = parentActivity;
    }

    public void exportResults(Context context, RecordingDetails recDetails, AbstractSampleCollector pressureCollector, AbstractSampleCollector speedCollector) {
        List<File> resultFiles = new ArrayList<>();
        resultFiles.addAll(saveToFiles(recDetails, pressureCollector, "pressure"));
        resultFiles.addAll(saveToFiles(recDetails, speedCollector, "speed"));

        sendResults(context, recDetails, resultFiles);
    }

    private List<File> saveToFiles(RecordingDetails recDetails, AbstractSampleCollector sampleCollector, String collectorName) {
        ArrayList<File> savedFiles = new ArrayList<>();

        List<String> samplingRateLines = Collections.singletonList(Float.toString(sampleCollector.getSamplingRate()));
        File file1 = saveToFile(recDetails, collectorName + "_fs", samplingRateLines.iterator());
        savedFiles.add(file1);

        if (!sampleCollector.hasSamples()) {

            List<String> samplesValuesLines = Collections.singletonList(NO_SAMPLES_MARKER);
            File file2 = saveToFile(recDetails, collectorName + "_samples", samplesValuesLines.iterator());
            savedFiles.add(file2);

        } else {
            float [] samples = sampleCollector.getCollectedSamples();
            Iterator<String> samplesLines = new Iterator<String>() {
                private int i = 0;
                private float firstSampleTimeDelay = sampleCollector.firstSampleTimeDelay();

                @Override
                public boolean hasNext() {
                    return i < samples.length;
                }

                @Override
                public String next() {
                    float time = (float)i /sampleCollector.getSamplingRate() + firstSampleTimeDelay;
                    float value = samples[i];
                    i++;
                    return Float.toString(time) + ", " + Float.toString(value);
                }

                @Override
                public void remove() {}
                @Override
                public void forEachRemaining(Consumer action) {}
            };
            File file2 = saveToFile(recDetails, collectorName + "_samples", samplesLines);
            savedFiles.add(file2);
        }

        return savedFiles;
    }

    private File saveToFile(RecordingDetails recDetails, String fieldName, Iterator<String> linesIterator) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String startTimestamp = dateFormat.format(recDetails.getRecordingStartTime());

        String fileName = startTimestamp + " - " + recDetails.getTitle() + " - " + fieldName + DEFAULT_FILE_EXT;

        File cacheDir = parentActivity.getBaseContext().getCacheDir();
        File outFile = new File(cacheDir, fileName);
        outFile.deleteOnExit();
        try {
            FileOutputStream fos = new FileOutputStream(outFile);
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(fos));
            try {
                w.append("# Recording timestamp: ").append(startTimestamp);
                w.newLine();
                w.append("# Recording title: ").append(recDetails.getTitle());
                w.newLine();
                w.append("# field: ").append(fieldName);
                w.newLine();

                while (linesIterator.hasNext()) {
                    w.write(linesIterator.next());
                    w.newLine();
                }

                w.flush();
            } finally {
                fos.close();
            }
            //outFile.setReadable(true, false);
        } catch (IOException e) {
            Log.e("results", "can't store", e);
        } finally {
            Log.d("results", "stored file " + fileName);
        }

        return outFile;
    }

    private void sendResults(Context context, RecordingDetails recDetails, List<File> resultFiles) {

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
        //intentShareFile.putExtra(Intent.EXTRA_TEXT, recDetails.getTitle());

        parentActivity.startActivity(Intent.createChooser(intentShareFile, "Share the recording result"));
    }
}
