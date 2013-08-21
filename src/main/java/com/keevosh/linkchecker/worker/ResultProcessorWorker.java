/**
 * 
 */
package com.keevosh.linkchecker.worker;

import com.keevosh.linkchecker.LinkCheckerOptions;
import com.keevosh.linkchecker.dto.FileDto;
import com.keevosh.linkchecker.service.ResultProcessorService;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tassos
 */
public class ResultProcessorWorker extends Thread {

    private final static Logger log = LoggerFactory.getLogger(ResultProcessorWorker.class);

    private final LinkedBlockingQueue<FileDto> inputQueue;
    private final LinkCheckerOptions options;
    private long successCounter = 0;
    private long errorCounter = 0;
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
        log.info("ResultProcessorWorker {} starting...", this.getName());

        try {
            FileDto article = null;

            while (work) {
                while ((article = inputQueue.poll(2L, TimeUnit.SECONDS)) != null) {
                    try {
                        if (article.isContainsError()) {
                            errorCounter++;
                        } else {
                            successCounter++;
                        }
                        ResultProcessorService.getInstance().processResult(article, options);
                        log.trace("Success processed ArticleDto : {}", article);
                    } catch (Exception e) {
                        log.error("Failed to process result ArticleDto : {}", article, e);
                    }
                }
                log.info("Nothing found to process waiting 2 sec. No stop signal recieved, will wait more.");
            }

            log.info("Nothing else found in the queue exiting... Processed {} files. Found {} ok and {} errors.", new Object[] { successCounter + errorCounter, successCounter, errorCounter });
        } catch (InterruptedException e) {
            log.error("Error in ResultProcessorWorker", e);
        }
    }

    public void stopWorking() {
        this.work = false;
    }
}