package io.github.enerccio.rensu.ocr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RensuOcr implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(RensuOcr.class);

    private ExecutorService executorService;
    private int numThreads = 2;

    public void process(long id, byte[] data, List<OcrProcessor> processors, OcrResultCallback result) {
        log.debug("Calling to process {} with processors {}", id, processors);
        new OcrTaskChain(this, 0, processors, result).run(id, data);
    }

    void enqueue(OcrProcessor processor, long id, byte[] data, OcrTaskChain ocrTaskChain) {
        log.trace("Next processor chain for {}, task chain {}", id, ocrTaskChain);

        executorService.submit(() -> {
            try {
                processor.process(id, data, ocrTaskChain);
            } catch (Exception e) {
                ocrTaskChain.failure(id, e);
            }
        });
    }

    @Override
    public void destroy() throws Exception {
        log.info("Terminating Rensu service");
        executorService.shutdown();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Initializing Rensu service");
        executorService = Executors.newFixedThreadPool(getNumThreads());
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }
}
