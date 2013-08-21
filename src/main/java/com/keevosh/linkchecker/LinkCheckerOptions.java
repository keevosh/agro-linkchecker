package com.keevosh.linkchecker;


public class LinkCheckerOptions {
    public boolean supportMode;
    public String rootFolderPath;
    public String successFolderPath;
    public String errorFolderPath;
    public String fileFormat;

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LinkCheckerOptions [supportMode=");
        builder.append(supportMode);
        builder.append(", ");
        if (rootFolderPath != null) {
            builder.append("rootFolderPath=");
            builder.append(rootFolderPath);
            builder.append(", ");
        }
        if (successFolderPath != null) {
            builder.append("successFolderPath=");
            builder.append(successFolderPath);
            builder.append(", ");
        }
        if (errorFolderPath != null) {
            builder.append("errorFolderPath=");
            builder.append(errorFolderPath);
            builder.append(", ");
        }
        if (fileFormat != null) {
            builder.append("fileFormat=");
            builder.append(fileFormat);
        }
        builder.append("]");
        return builder.toString();
    }  
}
