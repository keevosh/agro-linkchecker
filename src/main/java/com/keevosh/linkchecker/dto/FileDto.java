package com.keevosh.linkchecker.dto;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response.Status.Family;

import org.springframework.data.annotation.Id;

public class FileDto {
    @Id
    private String identifier;
    private String filePath;
    private long lastCheckTimestamp;
    private String type;
    private boolean containsError = false;
    private List<UrlDto> locations = new ArrayList<UrlDto>();

    /**
     * 
     */
    public FileDto() {
        super();
    }

    /**
     * @param filePath
     * @param identifier
     * @param type
     */
    public FileDto(String filePath, String identifier, String type) {
        this();
        this.filePath = filePath;
        this.identifier = identifier;
        this.type = type;
    }

    /**
     * @return the filePath
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * @return the lastCheckTimestamp
     */
    public long getLastCheckTimestamp() {
        return lastCheckTimestamp;
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param filePath
     *            the filePath to set
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * @param lastCheckTimestamp
     *            the lastCheckTimestamp to set
     */
    public void setLastCheckTimestamp(long lastCheckTimestamp) {
        this.lastCheckTimestamp = lastCheckTimestamp;
    }

    /**
     * @param identifier
     *            the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the locations
     */
    public List<UrlDto> getLocations() {
        return locations;
    }

    /**
     * @param url
     */
    public void addLocation(UrlDto url) {
        locations.add(url);
    }

    /**
     * @return the containsError
     */
    public boolean isContainsError() {
        return containsError;
    }

    /**
     * @param containsError
     *            the containsError to set
     */
    public void setContainsError(boolean containsError) {
        this.containsError = containsError;
    }

    public boolean isUrlBroken(String url) {
        for(UrlDto dto : getLocations()) {
            if(url.equals(dto.getUrl()) && dto.getStatusFamily() == Family.SUCCESSFUL) {
                return false;
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FileDto [");
        if (identifier != null) {
            builder.append("identifier=");
            builder.append(identifier);
            builder.append(", ");
        }
        if (filePath != null) {
            builder.append("filePath=");
            builder.append(filePath);
            builder.append(", ");
        }
        builder.append("lastCheckTimestamp=");
        builder.append(lastCheckTimestamp);
        builder.append(", ");
        if (type != null) {
            builder.append("type=");
            builder.append(type);
            builder.append(", ");
        }
        builder.append("containsError=");
        builder.append(containsError);
        builder.append(", ");
        if (locations != null) {
            builder.append("locations=");
            builder.append(locations);
        }
        builder.append("]");
        return builder.toString();
    }
}