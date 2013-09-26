package com.keevosh.linkchecker;

import com.keevosh.linkchecker.dto.FileDto;
import com.keevosh.linkchecker.exceptions.LinkCheckerException;
import com.keevosh.linkchecker.metrics.MetricsRegistryHolder;
import com.keevosh.linkchecker.options.LinkCheckerOptions;
import com.keevosh.linkchecker.service.FileProcessorService;
import com.keevosh.linkchecker.service.RedirectionRulesService;
import com.keevosh.linkchecker.worker.LinkCheckerWorker;
import com.keevosh.linkchecker.worker.ResultProcessorWorker;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AppRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AppRunner.class);

    private static final String APP_NAME = "LinkChecker";
    
    private static final long SLEEP_TIMEOUT = 3000;
    
    private static final long SHUTDOWN_TIMEOUT = 60000;
    
    private static final long QUEUE_OFFER_TIMEOUT_SECS = 30;
    
    private static final int PROCESSORS_NUM = 5;
    
    private static final int CHECKERS_NUM = 25;
    
    private static final int MAX_QUEUE_SIZE = 2000;
    
    private static LinkedBlockingQueue<FileDto> inputQueue;
    private static LinkedBlockingQueue<FileDto> outputQueue;
    private static final List<ResultProcessorWorker> RESULT_PROCESSORS = new ArrayList<ResultProcessorWorker>(PROCESSORS_NUM);
    private static final List<LinkCheckerWorker> LINKCHECKER_WORKERS = new ArrayList<LinkCheckerWorker>(CHECKERS_NUM);
    
    /**
     * 
     */
    private AppRunner() {
        super();
    }

    /**
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        LinkCheckerOptions options = checkOptions(args);
        if (options == null) {
            return;
        }

        LOG.info("Starting the linkchecker with the given options {}", options);

        File rootFolderPath = FileUtils.getFile(options.getRootFolderPath());
        if (rootFolderPath == null || !rootFolderPath.isDirectory()) {
            LOG.error("The specified rootFolderPath does not exist or is not a folder. Exiting....");
            return;
        }
        
        RedirectionRulesService.getInstance().initializeRules(options);

        Collection<File> files = FileUtils.listFiles(rootFolderPath, new String[] { "json" }, true);

        if (CollectionUtils.isEmpty(files)) {
            LOG.error("The specified rootFolderPath is empty or does not contains any json files. Exiting....");
            return;
        }

        LOG.info("Found {} files to process.", files.size());

        setupAndStartThreads(files.size(), options);
        
        LOG.info("Starting offer the files to input queue...");

        offerFilesToInputQueue(files, options);

        LOG.info("All files offered to the input queue for process");

        monitorAndShutdownThreads();

        // wait till all workers are shut down
        Thread.sleep(SHUTDOWN_TIMEOUT);

        MetricsRegistryHolder.report();
        
        LOG.info("All files processed successfully.");
    }

    public static void offerFilesToInputQueue(Collection<File> files, LinkCheckerOptions options) throws InterruptedException {
        for (File file : files) {
            FileDto dto = null;
            try {
                dto = FileProcessorService.getInstance().readFile(file.getPath(), options);
                if (dto == null) {
                    throw new LinkCheckerException();
                }
            } catch (Exception e) {
                MetricsRegistryHolder.getCounter("FILES[MALFORMED]").inc();
                LOG.error("Error reading file {}. Skipping...", file, e);
                continue;
            }
           
            MetricsRegistryHolder.getCounter("FILES[ALL]").inc();
            inputQueue.offer(dto, QUEUE_OFFER_TIMEOUT_SECS, TimeUnit.SECONDS);
        }
    }
    
    public static void setupAndStartThreads(int queueSize, LinkCheckerOptions options) {
        inputQueue = new LinkedBlockingQueue<FileDto>(Math.min(queueSize, MAX_QUEUE_SIZE));
        outputQueue = new LinkedBlockingQueue<FileDto>(Math.min(queueSize, MAX_QUEUE_SIZE));
        
        for (int i = 0; i < PROCESSORS_NUM; i++) {
            ResultProcessorWorker w = new ResultProcessorWorker(i + 1, outputQueue, options);
            RESULT_PROCESSORS.add(w);
            w.start();
        }

        for (int i = 0; i < CHECKERS_NUM; i++) {
            LinkCheckerWorker w = new LinkCheckerWorker(i + 1, inputQueue, outputQueue);
            LINKCHECKER_WORKERS.add(w);
            w.start();
        }
    }
    
    public static void monitorAndShutdownThreads() throws InterruptedException {
        while (inputQueue.size() > 0) {
            LOG.info("Remaining jobs to be processes {}", inputQueue.size());
            Thread.sleep(SLEEP_TIMEOUT);
        }

        LOG.info("All files in input queue are processed. Stopping gracefully the LinkCheckerWorkers");
        for (LinkCheckerWorker w : LINKCHECKER_WORKERS) {
            w.stopWorking();
        }

        while (outputQueue.size() > 0) {
            LOG.info("Remaining job results to be processes {}", outputQueue.size());
            Thread.sleep(SLEEP_TIMEOUT);
        }

        LOG.info("All files in input queue are processed. Stopping gracefully the ResultProcessorWorker");
        for (ResultProcessorWorker w : RESULT_PROCESSORS) {
            w.stopWorking();
        }
    }
    
    public static Options getSupportedOptions() {
        Options options = new Options();
        
        options.addOption(new Option(LinkCheckerOptions.SupportedOptions.HELP.getOptionName(), "print this message"));
        options.addOption(Option.builder(LinkCheckerOptions.SupportedOptions.MODE.getOptionName()).argName("mode").hasArg().desc("the mode that the linkchecker is about to run. use 'support' for Suport mode, use 'live' for new incoming files.").required().build());
        options.addOption(Option.builder(LinkCheckerOptions.SupportedOptions.FILE_FORMAT.getOptionName()).argName("fileFormat").hasArg().desc("the file format (AKIF, AGRIF etc) that the linkchecker is going to check.").required().build());
        options.addOption(Option.builder(LinkCheckerOptions.SupportedOptions.ROOT_FOLDER_PATH.getOptionName()).argName("rootFolderPath").hasArg().desc("the folder where the files are located.").required().build());
        options.addOption(Option.builder(LinkCheckerOptions.SupportedOptions.SUCCESS_FOLDER_PATH.getOptionName()).argName("successFolderPath").hasArg().desc("the folder where the OK files will be transfered in case of Active mode.").required(false).build());
        options.addOption(Option.builder(LinkCheckerOptions.SupportedOptions.ERROR_FOLDER_PATH.getOptionName()).argName("errorFolderPath").hasArg().desc("the folder where the NOT OK files will be transfered in case of Active mode.").required(false).build());
        options.addOption(Option.builder(LinkCheckerOptions.SupportedOptions.RULES_PATH.getOptionName()).argName("rulesPath").hasArg().desc("the path for the redirection rules file.").required(false).build());
        
        return options;
    }
    
    public static LinkCheckerOptions checkOptions(String[] args) {
        HelpFormatter formatter = new HelpFormatter();
        CommandLineParser parser = new DefaultParser();

        Options options = getSupportedOptions();

        try {
            CommandLine line = parser.parse(options, args);
            LinkCheckerOptions linkCheckerOptions = LinkCheckerOptions.newInstance(line, options);
            linkCheckerOptions.validate();
            return linkCheckerOptions;
        } catch (ParseException e) {
            formatter.printHelp(APP_NAME, options, true);
            LOG.trace("Parsing failed.  Reason: ", e);
            return null;
        }
    }
}
