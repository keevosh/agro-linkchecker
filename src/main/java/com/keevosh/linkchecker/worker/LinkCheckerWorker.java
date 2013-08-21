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

    private final static Logger log = LoggerFactory.getLogger(LinkCheckerWorker.class);

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
        log.info("Worker {} starting...", this.getName());

        try {
            FileDto fileDto = null;

            while (work) {
                while ((fileDto = inputQueue.poll(1L, TimeUnit.SECONDS)) != null) {

                    try {
                        if (fileDto.isContainsError()) {
                            log.debug("File with id {} contains already errors, skipping...", fileDto.getIdentifier());
                        } else {
                            fileDto = LinkCheckerService.getInstance().checkFileLocations(fileDto);
                            log.debug("Success processed FileDto : {}", fileDto);
                        }
                    } catch (Exception e) {
                        log.debug("Failed to process FileDto : {}", fileDto);
                    }

                    outputQueue.offer(fileDto, 2, TimeUnit.SECONDS);
                }
                log.info("Nothing found to process waiting 1 sec. No stop signal recieved, will wait more.");
            }

            log.info("Nothing else found in the queue exiting...");
        } catch (InterruptedException e) {
            log.error("Error in PageScrumWorker", e);
        }
    }

    public void stopWorking() {
        this.work = false;
    }
}