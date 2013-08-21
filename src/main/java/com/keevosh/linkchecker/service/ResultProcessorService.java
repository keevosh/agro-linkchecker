package com.keevosh.linkchecker.service;

import com.keevosh.linkchecker.LinkCheckerOptions;
import com.keevosh.linkchecker.dto.FileDto;
import com.keevosh.linkchecker.repository.FileDtoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultProcessorService {

    private final static Logger log = LoggerFactory.getLogger(ResultProcessorService.class);

    private static final class SINGLETON_HOLDER {
        public final static ResultProcessorService self = new ResultProcessorService();
    }

    private ResultProcessorService() {}

    public static ResultProcessorService getInstance() {
        return SINGLETON_HOLDER.self;
    }

    public void processResult(FileDto fileDto, LinkCheckerOptions options) throws Exception {
        log.trace("Ready to process result {} with options {}", fileDto, options);
        if(options.supportMode) {
            FileProcessorService.getInstance().updateFile(fileDto, options);
        }
        else {
            FileProcessorService.getInstance().copyFile(fileDto.getFilePath(), options, !fileDto.isContainsError());
        }
        
        FileDtoRepository.getInstance().save(fileDto);
    }
}
