package com.keevosh.linkchecker.dto;

import javax.ws.rs.core.Response.Status.Family;

import org.springframework.data.annotation.Id;

public class UrlDto {
    @Id
    private String url;
    private String domain;
    private Family statusFamily;
    private int responseStatusCode;

    /**
     * @param domain
     * @param url
     */
    public UrlDto(String domain, String url) {
        super();
        this.domain = domain;
        this.url = url;
    }
    
    /**
     * @param domain
     * @param url
     * @param statusFamily
     * @param responseStatusCode
     */
    public UrlDto(String domain, String url, Family statusFamily, int responseStatusCode) {
        this(domain, url);
        this.statusFamily = statusFamily;
        this.responseStatusCode = responseStatusCode;
    }

    /**
     * @return the domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @param domain
     *            the domain to set
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return the responseStatusCode
     */
    public int getResponseStatusCode() {
        return responseStatusCode;
    }

    /**
     * @param url
     *            the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @param responseStatusCode
     *            the responseStatusCode to set
     */
    public void setResponseStatusCode(int responseStatusCode) {
        this.responseStatusCode = responseStatusCode;
    }

    /**
     * @return the statusFamily
     */
    public Family getStatusFamily() {
        return statusFamily;
    }

    /**
     * @param statusFamily
     *            the statusFamily to set
     */
    public void setStatusFamily(Family statusFamily) {
        this.statusFamily = statusFamily;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UrlDto [");
        if (url != null) {
            builder.append("url=");
            builder.append(url);
            builder.append(", ");
        }
        if (domain != null) {
            builder.append("domain=");
            builder.append(domain);
            builder.append(", ");
        }
        if (statusFamily != null) {
            builder.append("statusFamily=");
            builder.append(statusFamily);
            builder.append(", ");
        }
        builder.append("responseStatusCode=");
        builder.append(responseStatusCode);
        builder.append("]");
        return builder.toString();
    }
}
