package com.keevosh.linkchecker.service;

import com.keevosh.linkchecker.dto.FileDto;
import com.keevosh.linkchecker.dto.UrlDto;
import com.keevosh.linkchecker.metrics.MetricsRegistryHolder;

import java.io.IOException;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.commons.collections.CollectionUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LinkCheckerService {

    private static final Logger LOG = LoggerFactory.getLogger(LinkCheckerService.class);
    private static final Logger LOG_URL = LoggerFactory.getLogger("url.check.log");
    private static final Logger LOG_REDIRECTION = LoggerFactory.getLogger("url.redirection.log");

    private static final int HTTP_TIMEOUT = 30000;
    private static final int DOMAIN_ERROR_THRESHOLD = 100;

    private static final class HOLDER {
        public static final LinkCheckerService SELF = new LinkCheckerService();
    }

    private LinkCheckerService() {}

    public static LinkCheckerService getInstance() {
        return HOLDER.SELF;
    }

    public FileDto checkFileLocations(FileDto fileDto) {
        if (CollectionUtils.isEmpty(fileDto.getLocations())) {
            LOG.debug("File {} contains no locations. Skipping...", fileDto);
            MetricsRegistryHolder.getCounter("FILE[NO-URL]").inc();
            return fileDto;
        }

        for (UrlDto location : fileDto.getLocations()) {
            try {
                checkLocation(location);
            } catch (IOException e) {
                LOG.debug("Error checking location {}", location, e);
                location.setStatusFamily(Family.OTHER);
                location.setResponseStatusCode(-1);
            } catch (Exception e) {
                LOG.error("Error checking location {}", location, e);
                location.setStatusFamily(Family.OTHER);
                location.setResponseStatusCode(-1);
            }
            fileDto.setContainsError(fileDto.isContainsError() || location.getStatusFamily() != Family.SUCCESSFUL);

            MetricsRegistryHolder.getCounter("URL.STATUS[" + location.getStatusFamily() + "]").inc();
            MetricsRegistryHolder.getCounter("DOMAIN[" + location.getDomain() + "][" + (location.getStatusFamily() != Family.SUCCESSFUL ? (location.getStatusFamily() == Family.REDIRECTION ? "REDIRECTION" : "ERROR") : "SUCCESS") + "]").inc();
            if (location.getStatusFamily() == Family.REDIRECTION) {
                LOG_REDIRECTION.debug("{}, {}, {}, {}, {}", new Object[] { location.getDomain(), location.getUrl(), location.getStatusFamily(), location.getResponseStatusCode(), location.getRedirectsToUrl() });
            } else {
                LOG_URL.debug("{}, {}, {}, {}", new Object[] { location.getDomain(), location.getUrl(), location.getStatusFamily(), location.getResponseStatusCode() });
            }
        }

        return fileDto;
    }

    private void checkLocation(UrlDto location) throws IOException {
        if (location.getStatusFamily() != null) {
            LOG.debug("Location {} already checked in previous step and found with status {}. Skipping...", location, location.getStatusFamily());
            return;
        }

        LOG.trace("About to check location {}", location);

        shouldSkipLocationCheck(location);
        
        int responseCode = Jsoup.connect(location.getUrl().toString()).followRedirects(false).ignoreHttpErrors(true).ignoreContentType(true).timeout(HTTP_TIMEOUT).maxBodySize(1).userAgent("Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2").execute().statusCode();
        Status status = Status.fromStatusCode(responseCode);

        if (status != null && status.getFamily() == Family.REDIRECTION) {
            Connection.Response resp = Jsoup.connect(location.getUrl().toString()).followRedirects(true).ignoreHttpErrors(true).ignoreContentType(true).timeout(HTTP_TIMEOUT).maxBodySize(1).userAgent("Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2").execute();
            location.setRedirectsToUrl(resp.url().toString());
            Integer redirectionRuleStatus = RedirectionRulesService.getInstance().isRedirectionValid(location);
            if(resp.url().toString().equalsIgnoreCase(location.getUrl().toString())) {
                responseCode = resp.statusCode();
            }
            else if(redirectionRuleStatus != null) {
                responseCode = redirectionRuleStatus;
            }
            status = Status.fromStatusCode(responseCode);
        }

        location.setStatusFamily(status == null ? Family.OTHER : status.getFamily());
        location.setResponseStatusCode(responseCode);
        LOG.trace("Location checked. Current status {}", location);
    }
    
    private boolean shouldSkipLocationCheck(UrlDto location) {
        if(MetricsRegistryHolder.getCounter("DOMAIN[" + location.getDomain() + "][ERROR]").getCount() > DOMAIN_ERROR_THRESHOLD) {
            LOG_URL.debug("{}, {}, {}", new Object[] { location.getDomain(), location.getUrl(), "SKIPPED" });
            MetricsRegistryHolder.getCounter("URL.STATUS[SKIPPED]").inc();
            MetricsRegistryHolder.getCounter("DOMAIN[" + location.getDomain() + "][SKIPPED]").inc();
            location.setStatusFamily(Family.OTHER);
            location.setResponseStatusCode(-1);
            return true;
        }
        return false;
    }
}
