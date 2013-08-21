package com.keevosh.linkchecker.repository;

import com.keevosh.linkchecker.dto.FamilyGroupDto;
import com.keevosh.linkchecker.dto.FileDto;
import com.mongodb.Mongo;

import java.net.UnknownHostException;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapreduce.GroupBy;
import org.springframework.data.mongodb.core.mapreduce.GroupByResults;

public class FileDtoRepository {
    private final static Logger log = LoggerFactory.getLogger(FileDtoRepository.class);
    
    // TODO: externalize to properties file
    private final String MONGO_HOST = "localhost";
    
    private final MongoTemplate mongoTemplate;

    private static final class SINGLETON_HOLDER {
        public final static FileDtoRepository self = new FileDtoRepository();
    }

    private FileDtoRepository() {
        try {
         // TODO: externalize to properties file
            mongoTemplate = new MongoTemplate(new Mongo(MONGO_HOST), "urlchecker");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static FileDtoRepository getInstance() {
        return SINGLETON_HOLDER.self;
    }

    public void save(FileDto fileDto) {
        mongoTemplate.save(fileDto);
    }
    
    public void countAndPrintAllSuccessErrorsFromDB() {
        GroupByResults<FamilyGroupDto> results = mongoTemplate.group("fileDto", 
                        GroupBy.key("containsError").initialDocument("{ count: 0 }").reduceFunction("function(doc, prev) { prev.count += 1 }"), 
                        FamilyGroupDto.class);
        for (Iterator<FamilyGroupDto> iterator = results.iterator(); iterator.hasNext();) {
            FamilyGroupDto familyGroupDto = iterator.next();
            log.info("{}", familyGroupDto);
        }
    }
}
