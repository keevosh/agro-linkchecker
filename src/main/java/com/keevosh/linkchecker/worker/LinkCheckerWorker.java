/**
 * 
 */
package com.keevosh.linkchecker.worker;

import com.keevosh.linkchecker.dto.FileDto;
import com.keevosh.linkchecker.service.LinkCheckerService;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tassos
 */
public class LinkCheckerWorker extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(LinkCheckerWorker.class);
    private static final long POLL_TIMEOUT = 1;
    private static final long OFFER_TIMEOUT = 5;

    private final LinkedBlockingQueue<FileDto> inputQueue;
    private final LinkedBlockingQueue<FileDto> outputQueue;
    private boolean work = true;

    /**
     * @param inputQueue
     * @param outputQueue
     */
    public LinkCheckerWorker(int id, LinkedBlockingQueue<FileDto> inputQueue, LinkedBlockingQueue<FileDto> outputQueue) {
        super("LinkCheckerWorker " + id);
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        LOG.info("Worker {} starting...", this.getName());

        try {
            FileDto fileDto = null;

            while (work) {
                while ((fileDto = inputQueue.poll(POLL_TIMEOUT, TimeUnit.SECONDS)) != null) {
                    try {
                        fileDto = LinkCheckerService.getInstance().checkFileLocations(fileDto);
                        LOG.debug("Success processed FileDto : {}", fileDto);
                    } catch (Exception e) {
                        LOG.debug("Failed to process FileDto : {}", fileDto);
                    }

                    outputQueue.offer(fileDto, OFFER_TIMEOUT, TimeUnit.SECONDS);
                }
                LOG.info("Nothing found to process waiting 1 sec. No stop signal recieved, will wait more.");
            }

            LOG.info("Nothing else found in the queue exiting...");
        } catch (InterruptedException e) {
            LOG.error("Error in PageScrumWorker", e);
        }
    }

    public void stopWorking() {
        this.work = false;
    }
}