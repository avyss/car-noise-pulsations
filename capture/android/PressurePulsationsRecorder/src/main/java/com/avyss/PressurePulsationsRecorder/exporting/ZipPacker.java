package com.avyss.PressurePulsationsRecorder.exporting;

import android.util.Log;

import com.avyss.PressurePulsationsRecorder.acquisition.AbstractSampleCollector;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipPacker implements Closeable {

    private static final String DATA_FILE_EXT = ".csv";

    private File zipFile;
    private ZipOutputStream zos;

    public ZipPacker(File containerDirectory, String fileName) {
        zipFile = new File(containerDirectory, fileName);
        zipFile.deleteOnExit();

        try {
            zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
        } catch(FileNotFoundException e) {
            Log.d("results", "can't write to file " + zipFile.getName());
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void close() throws IOException {
        zos.finish();
        zos.close();
        Log.d("results", "stored file " + zipFile.getName());
    }

    public File getZipFile() {
        return zipFile;
    }

    public void addSamples(String collectorName, AbstractSampleCollector sampleCollector) throws IOException {
        List<String> samplingRateLines = Collections.singletonList(Float.toString(sampleCollector.getSamplingRate()));

        writePart(collectorName + "_fs", samplingRateLines.iterator());

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
                return Float.toString(time) + "," + Float.toString(value);
            }

            @Override
            public void remove() {}
            @Override
            public void forEachRemaining(Consumer action) {}
        };

        writePart(collectorName + "_samples", samplesLines);
    }

    private void writePart(String partName, Iterator<String> linesIterator) throws IOException {

        ZipEntry entry = new ZipEntry(partName + DATA_FILE_EXT);
        zos.putNextEntry(entry);

        try {

            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(zos));
            try {

                while (linesIterator.hasNext()) {
                    w.write(linesIterator.next());
                    w.newLine();
                }

                w.flush();
            } finally {
                // don't call the w.close(), as it will cause the zip stream to close too early
            }
        } catch (IOException e) {
            Log.e("results", "can't store " + partName, e);
            throw e;
        } finally {
            Log.d("results", "stored part " + partName);
        }

    }

}
