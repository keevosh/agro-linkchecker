package com.keevosh.linkchecker.service;

import com.keevosh.linkchecker.dto.FileDto;
import com.keevosh.linkchecker.dto.UrlDto;
import com.keevosh.linkchecker.exceptions.LinkCheckerException;
import com.keevosh.linkchecker.exceptions.ProtocolNotSupportedException;
import com.keevosh.linkchecker.options.LinkCheckerOptions;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.core.Response.Status.Family;

import net.zettadata.simpleparser.ParserException;
import net.zettadata.simpleparser.SimpleMetadata;
import net.zettadata.simpleparser.SimpleMetadataFactory;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileProcessorService {
    private static final Logger LOG = LoggerFactory.getLogger(FileProcessorService.class);

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private static final class HOLDER {
        public static final FileProcessorService SELF = new FileProcessorService();
    }

    private FileProcessorService() {}

    public static FileProcessorService getInstance() {
        return HOLDER.SELF;
    }

    public FileDto readFile(String filePath, LinkCheckerOptions options) throws ParserException {
        SimpleMetadata tmpMetadata = SimpleMetadataFactory.getSimpleMetadata(options.getFileFormat());
        tmpMetadata.load(filePath);

        if (CollectionUtils.isEmpty(tmpMetadata.getIdentifiers()) || tmpMetadata.getIdentifiers().size() > 1) {
            LOG.error("Unsupported identifiers size {} for file {}. skipping", tmpMetadata.getIdentifiers().size(), filePath);
            return null;
        }

        FileDto dto = new FileDto(filePath, CollectionUtils.get(tmpMetadata.getIdentifiers(), 0).toString(), options.getFileFormat());
        for (String url : tmpMetadata.getLocations()) {
            try {
                if (!url.startsWith("http")) {
                    throw new ProtocolNotSupportedException("Protocol not supported : " + url);
                }

                URL u = new URL(url);
                if (!u.getProtocol().equals("http")) {
                    throw new ProtocolNotSupportedException("Protocol not supported : " + url);
                }
                dto.addLocation(new UrlDto(u.getHost(), url));
            } catch (MalformedURLException e) {
                LOG.debug("Malformed URL {} in file with id : {}", url, dto.getIdentifier());
                dto.addLocation(new UrlDto(null, url, Family.OTHER, -1));
                dto.setContainsError(true);
            }
        }
        return dto;
    }

    public void copyFile(String filePath, LinkCheckerOptions options, boolean success) {
        LOG.trace("Ready to move file {} to {} folder.", filePath, success ? "success" : "error");

        File rootFolder = new File(options.getRootFolderPath());
        File successFolder = new File(options.getSuccessFolderPath());
        File errorFolder = new File(options.getErrorFolderPath());

        String targetFilePath = filePath.replace(rootFolder.getPath(), success ? successFolder.getPath() : errorFolder.getPath());
        try {
            FileUtils.copyFile(new File(filePath), new File(targetFilePath));
        } catch (Exception e) {
            LOG.error("Error while moving {} to {}", new Object[] { filePath, targetFilePath }, e);
        }
    }

    @SuppressWarnings("unchecked")
    public void updateFile(FileDto fileDto, LinkCheckerOptions options) throws IOException {
        File file = new File(fileDto.getFilePath());
        String akifString = FileUtils.readFileToString(file);
        JSONObject akifObject = (JSONObject) JSONValue.parse(akifString);
        if (!fileDto.getIdentifier().equals(akifObject.get("identifier").toString())) {
            throw new LinkCheckerException("Identifiers mismatch!: " + fileDto.getIdentifier() + " differs from " + akifObject.get("identifier"));
        }
        JSONArray expressions0 = (JSONArray) akifObject.get("expressions");
        JSONArray expressions1 = new JSONArray();
        boolean fileChanged = false;
        for (Object expression : expressions0) {
            JSONObject expression0 = (JSONObject) expression;
            JSONArray manifestations0 = (JSONArray) expression0.get("manifestations");
            JSONArray manifestations1 = new JSONArray();
            for (Object manifestation : manifestations0) {
                JSONObject manifestation0 = (JSONObject) manifestation;
                JSONArray items0 = (JSONArray) manifestation0.get("items");
                JSONArray items1 = new JSONArray();
                for (Object item : items0) {
                    JSONObject item0 = (JSONObject) item;
                    String url = (String) item0.get("url");
                    Boolean broken = (Boolean) item0.get("broken");  
                    if(broken != fileDto.isUrlBroken(url)) {
                        item0.put("broken", Boolean.valueOf(fileDto.isUrlBroken(url)));
                        fileChanged = true;
                        items1.add(item0);
                    }
                }
                manifestation0.put("items", items1);
                manifestations1.add(manifestation0);
            }
            expression0.put("manifestations", manifestations1);
            expressions1.add(expression0);
        }
        akifObject.put("expressions", expressions1);
        if(fileChanged) {
            akifObject.put("lastUpdateDate", sdf.format(new Date(System.currentTimeMillis())));
            FileUtils.writeStringToFile(file, akifObject.toJSONString());
        }
    }
}
