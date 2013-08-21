package com.keevosh.linkchecker.service;

import com.keevosh.linkchecker.dto.FileDto;
import com.keevosh.linkchecker.dto.UrlDto;

import java.io.IOException;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.commons.collections.CollectionUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkCheckerService {

    private final static Logger log = LoggerFactory.getLogger(LinkCheckerService.class);

    private static final class SINGLETON_HOLDER {
        public final static LinkCheckerService self = new LinkCheckerService();
    }

    private LinkCheckerService() {}

    public static LinkCheckerService getInstance() {
        return SINGLETON_HOLDER.self;
    }

    public FileDto checkFileLocations(FileDto fileDto) {
        if(CollectionUtils.isEmpty(fileDto.getLocations())) {
            log.debug("File {} contains no locations. Skipping...", fileDto);
            return fileDto;
        }
        
        for(UrlDto location : fileDto.getLocations()) {
            try {
                checkLocation(location);
            } catch (Exception e) {
                log.debug("Error checking location {}", location, e);
                location.setStatusFamily(Family.OTHER);
                location.setResponseStatusCode(-1);
            }
            fileDto.setContainsError(fileDto.isContainsError() || location.getStatusFamily() != Family.SUCCESSFUL);
        }
            
        return fileDto;
    }
    
    private void checkLocation(UrlDto location) throws IOException {
        if(location.getStatusFamily() != null) {
            log.debug("Location {} already checked in previous step and found with status {}. Skipping...", location, location.getStatusFamily());
            return;
        }
        
        log.trace("About to check location {}", location);
        int responseCode = Jsoup.connect(location.getUrl().toString()).followRedirects(false).ignoreHttpErrors(true).ignoreContentType(true).timeout(30000).maxBodySize(1).execute().statusCode();
        Status status = Status.fromStatusCode(responseCode);
        Family responseFamily = status == null ? Family.OTHER : status.getFamily();
        location.setStatusFamily(responseFamily);
        location.setResponseStatusCode(responseCode);
        log.trace("Location checked. Current status {}", location);
    }
}
