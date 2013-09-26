package com.keevosh.linkchecker.options;

import java.text.MessageFormat;
import java.util.Arrays;

import net.zettadata.simpleparser.SimpleMetadataFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class LinkCheckerOptions {
    public static enum SupportedOptions {
        HELP("help"),
        MODE("mode"),
        ROOT_FOLDER_PATH("rootFolder"),
        SUCCESS_FOLDER_PATH("successFolder"),
        ERROR_FOLDER_PATH("errorFolder"),
        FILE_FORMAT("format"),
        RULES_PATH("rulesPath");

        private final String optionName;

        /**
         * @param optionName
         */
        private SupportedOptions(String optionName) {
            this.optionName = optionName;
        }

        /**
         * @return the optionName
         */
        public String getOptionName() {
            return optionName;
        }
    }

    private static final String[] FILE_FORMATS = { SimpleMetadataFactory.AKIF, SimpleMetadataFactory.AGRIF };

    private boolean supportMode;
    private String rootFolderPath;
    private String successFolderPath;
    private String errorFolderPath;
    private String fileFormat;
    private String rulesPath;

    public static final LinkCheckerOptions newInstance(CommandLine line, Options options) {
        LinkCheckerOptions linkCheckerOptions = new LinkCheckerOptions();
        linkCheckerOptions.setSupportMode(line.getOptionValue(options.getOption(SupportedOptions.MODE.optionName).getOpt()).equals("support"));
        linkCheckerOptions.setFileFormat(line.getOptionValue(options.getOption(SupportedOptions.FILE_FORMAT.optionName).getOpt()));
        linkCheckerOptions.setRootFolderPath(line.getOptionValue(options.getOption(SupportedOptions.ROOT_FOLDER_PATH.optionName).getOpt()));
        linkCheckerOptions.setSuccessFolderPath(!linkCheckerOptions.isSupportMode() ? line.getOptionValue(options.getOption(SupportedOptions.SUCCESS_FOLDER_PATH.optionName).getOpt()) : null);
        linkCheckerOptions.setErrorFolderPath(!linkCheckerOptions.isSupportMode() ? line.getOptionValue(options.getOption(SupportedOptions.ERROR_FOLDER_PATH.optionName).getOpt()) : null);
        linkCheckerOptions.setRulesPath(line.getOptionValue(options.getOption(SupportedOptions.RULES_PATH.optionName).getOpt()));
        return linkCheckerOptions;
    }

    public void validate() throws ParseException {
        // Check that in case of active mode, the successPath and the
        // errorPath are set
        if (!this.isSupportMode() && (this.getSuccessFolderPath() == null || this.getErrorFolderPath() == null)) {
            throw new ParseException("Success and error paths are required in case of active mode. Set them to proceed.");
        }

        // check that file format is valid
        Arrays.sort(FILE_FORMATS);
        if (Arrays.binarySearch(FILE_FORMATS, this.getFileFormat()) < 0) {
            throw new ParseException(MessageFormat.format("File format {0} is not supported. Select one of {1} to proceed.", this.getFileFormat(), FILE_FORMATS));
        }
    }

    /**
     * @return the rulesPath
     */
    public String getRulesPath() {
        return rulesPath;
    }

    /**
     * @param rulesPath
     *            the rulesPath to set
     */
    public void setRulesPath(String rulesPath) {
        this.rulesPath = rulesPath;
    }

    /**
     * @return the supportMode
     */
    public boolean isSupportMode() {
        return supportMode;
    }

    /**
     * @return the rootFolderPath
     */
    public String getRootFolderPath() {
        return rootFolderPath;
    }

    /**
     * @return the successFolderPath
     */
    public String getSuccessFolderPath() {
        return successFolderPath;
    }

    /**
     * @return the errorFolderPath
     */
    public String getErrorFolderPath() {
        return errorFolderPath;
    }

    /**
     * @return the fileFormat
     */
    public String getFileFormat() {
        return fileFormat;
    }

    /**
     * @param supportMode
     *            the supportMode to set
     */
    public void setSupportMode(boolean supportMode) {
        this.supportMode = supportMode;
    }

    /**
     * @param rootFolderPath
     *            the rootFolderPath to set
     */
    public void setRootFolderPath(String rootFolderPath) {
        this.rootFolderPath = rootFolderPath;
    }

    /**
     * @param successFolderPath
     *            the successFolderPath to set
     */
    public void setSuccessFolderPath(String successFolderPath) {
        this.successFolderPath = successFolderPath;
    }

    /**
     * @param errorFolderPath
     *            the errorFolderPath to set
     */
    public void setErrorFolderPath(String errorFolderPath) {
        this.errorFolderPath = errorFolderPath;
    }

    /**
     * @param fileFormat
     *            the fileFormat to set
     */
    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    /*
     * (non-Javadoc)
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
