package io.github.enerccio.rensu.ocr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StreamGobbler {
    private static final Logger log = LoggerFactory.getLogger(StreamGobbler.class);

    public static void gobble(Process p, ConverterInputOutputCallback callback) {
        Lock lock = new ReentrantLock();
        StringBuffer bufferOutput = new StringBuffer();
        StringBuffer bufferError = new StringBuffer();
        for (InputStream inputStream : Arrays.asList(p.getInputStream(), p.getErrorStream())) {
            StringBuffer bufferStream = inputStream == p.getInputStream() ? bufferOutput : bufferError;
            OutputType type = inputStream == p.getInputStream() ? OutputType.STDOUT : OutputType.STDERR;

            Thread gobbleThread = new Thread(() -> {
                try {
                    byte[] buffer = new byte[2048];

                    while (true) {
                        if (Thread.interrupted())
                            return;

                        int readSize = inputStream.read(buffer);

                        if (Thread.interrupted())
                            return;
                        if (readSize == -1)
                            return;

                        if (readSize > 0) {
                            String data = new String(buffer, 0, readSize, StandardCharsets.UTF_8);
                            bufferStream.append(data);
                            lock.lock();
                            try {
                                callback.onRead(data, type);
                            } finally {
                                lock.unlock();
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // pass
                } finally {
                    log.debug(bufferStream.toString());
                }
            });
            gobbleThread.setName("Gobble thread for " + inputStream + " of process " + p);
            gobbleThread.setDaemon(true);
            gobbleThread.start();
        }
    }

}