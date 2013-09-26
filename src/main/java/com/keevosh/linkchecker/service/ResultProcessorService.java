package com.keevosh.linkchecker.service;

import com.keevosh.linkchecker.dto.FileDto;
import com.keevosh.linkchecker.options.LinkCheckerOptions;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ResultProcessorService {

    private static final Logger LOG = LoggerFactory.getLogger(ResultProcessorService.class);

    private static final class HOLDER {
        public static final ResultProcessorService SELF = new ResultProcessorService();
    }

    private ResultProcessorService() {}

    public static ResultProcessorService getInstance() {
        return HOLDER.SELF;
    }

    public void processResult(FileDto fileDto, LinkCheckerOptions options) throws IOException {
        LOG.trace("Ready to process result {} with options {}", fileDto, options);
        if(options.isSupportMode()) {
            FileProcessorService.getInstance().updateFile(fileDto, options);
        }
        else {
            FileProcessorService.getInstance().copyFile(fileDto.getFilePath(), options, !fileDto.isContainsError());
        }
    }
}
