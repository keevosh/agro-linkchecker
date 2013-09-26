/**
 * 
 */
package com.keevosh.linkchecker.worker;

import com.keevosh.linkchecker.dto.FileDto;
import com.keevosh.linkchecker.metrics.MetricsRegistryHolder;
import com.keevosh.linkchecker.options.LinkCheckerOptions;
import com.keevosh.linkchecker.service.ResultProcessorService;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tassos
 */
public class ResultProcessorWorker extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(ResultProcessorWorker.class);
    private static final long POLL_TIMEOUT = 5;
    
    private final LinkedBlockingQueue<FileDto> inputQueue;
    private final LinkCheckerOptions options;
    private boolean work = true;

    /**
     * @param inputQueue
     * @param outputQueue
     */
    public ResultProcessorWorker(int id, LinkedBlockingQueue<FileDto> inputQueue, LinkCheckerOptions options) {
        super("ResultProcessorWorker " + id);
        this.inputQueue = inputQueue;
        this.options = options;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        LOG.info("ResultProcessorWorker {} starting...", this.getName());

        try {
            FileDto article = null;

            while (work) {
                while ((article = inputQueue.poll(POLL_TIMEOUT, TimeUnit.SECONDS)) != null) {
                    try {
                        if (article.isContainsError()) {
                            MetricsRegistryHolder.getCounter("FILES[ERROR]").inc();
                        } else {
                            MetricsRegistryHolder.getCounter("FILES[SUCCESS]").inc();
                        }
                        ResultProcessorService.getInstance().processResult(article, options);
                        LOG.trace("Success processed ArticleDto : {}", article);
                    } catch (Exception e) {
                        LOG.error("Failed to process result ArticleDto : {}", article, e);
                    }
                }
                LOG.info("Nothing found to process waiting 5 sec. No stop signal recieved, will wait more.");
            }

            LOG.info("Nothing else found in the queue exiting...");
        } catch (InterruptedException e) {
            LOG.error("Error in ResultProcessorWorker", e);
        }
    }

    public void stopWorking() {
        this.work = false;
    }
}