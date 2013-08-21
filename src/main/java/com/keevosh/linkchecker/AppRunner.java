package com.keevosh.linkchecker;

import com.keevosh.linkchecker.dto.FileDto;
import com.keevosh.linkchecker.repository.FileDtoRepository;
import com.keevosh.linkchecker.service.FileProcessorService;
import com.keevosh.linkchecker.worker.LinkCheckerWorker;
import com.keevosh.linkchecker.worker.ResultProcessorWorker;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import net.zettadata.simpleparser.SimpleMetadataFactory;

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

public class AppRunner {

    private final static Logger log = LoggerFactory.getLogger(AppRunner.class);

    private final static String[] supportedFileFortmats = { SimpleMetadataFactory.AKIF, SimpleMetadataFactory.AGRIF };

    /**
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        LinkCheckerOptions options = checkOptions(args);
        if (options == null) {
            return;
        }

        log.info("Starting the linkchecker with the given options {}", options);

        File rootFolderPath = FileUtils.getFile(options.rootFolderPath);
        if (rootFolderPath == null || !rootFolderPath.isDirectory()) {
            log.error("The specified rootFolderPath does not exist or is not a folder. Exiting....");
            return;
        }

        Collection<File> files = FileUtils.listFiles(rootFolderPath, new String[] { "json" }, true);

        if (CollectionUtils.isEmpty(files)) {
            log.error("The specified rootFolderPath is empty or does not contains any json files. Exiting....");
            return;
        }

        log.info("Found {} files to process.", files.size());

        final LinkedBlockingQueue<FileDto> inputQueue = new LinkedBlockingQueue<FileDto>(Math.min(files.size(), 2000));
        final LinkedBlockingQueue<FileDto> outputQueue = new LinkedBlockingQueue<FileDto>(Math.min(files.size(), 2000));
        List<ResultProcessorWorker> resultProcessorsList = new ArrayList<ResultProcessorWorker>();
        List<LinkCheckerWorker> linkCheckerWorkersList = new ArrayList<LinkCheckerWorker>();

        // TODO: externalize to properties file
        for (int i = 0; i < 5; i++) {
            ResultProcessorWorker w = new ResultProcessorWorker(i + 1, outputQueue, options);
            resultProcessorsList.add(w);
            w.start();
        }

        // TODO: externalize to properties file
        for (int i = 0; i < 15; i++) {
            LinkCheckerWorker w = new LinkCheckerWorker(i + 1, inputQueue, outputQueue);
            linkCheckerWorkersList.add(w);
            w.start();
        }

        log.info("Starting offer the files to input queue...");

        for (File file : files) {
            FileDto dto = null;
            try {
                dto = FileProcessorService.getInstance().readFile(file.getPath(), options);
            } catch (Exception e) {
                log.error("Error reading file {}. Skipping...", file, e);
                continue;
            }
            if (dto == null) {
                log.error("Error reading file {}. Skipping...", file);
                continue;
            }
            inputQueue.offer(dto, 30, TimeUnit.SECONDS);
        }

        log.info("All files offered to the input queue for process");

        while (inputQueue.size() > 0) {
            log.info("Remaining jobs to be processes {}", inputQueue.size());
            Thread.sleep(3000);
        }

        log.info("All files in input queue are processed. Stopping gracefully the LinkCheckerWorkers");
        for (LinkCheckerWorker w : linkCheckerWorkersList)
            w.stopWorking();

        while (outputQueue.size() > 0) {
            log.info("Remaining job results to be processes {}", outputQueue.size());
            Thread.sleep(3000);
        }

        log.info("All files in input queue are processed. Stopping gracefully the ResultProcessorWorker");
        for (ResultProcessorWorker w : resultProcessorsList)
            w.stopWorking();

        // wait till all workers are shut down
        Thread.sleep(3000);

        FileDtoRepository.getInstance().countAndPrintAllSuccessErrorsFromDB();

        log.info("All files processed successfully.");
    }

    public static LinkCheckerOptions checkOptions(String[] args) {
        LinkCheckerOptions linkCheckerOptions = new LinkCheckerOptions();
        HelpFormatter formatter = new HelpFormatter();
        CommandLineParser parser = new DefaultParser();

        Option help = new Option("help", "print this message");
        Option mode = Option.builder("mode").argName("mode").hasArg().desc("the mode that the linkchecker is about to run. use 'support' for Suport mode, use 'live' for new incoming files.").required().build();
        Option fileFormat = Option.builder("format").argName("fileFormat").hasArg().desc("the file format (AKIF, AGRIF etc) that the linkchecker is going to check.").required().build();
        Option rootFolderPath = Option.builder("rootFolder").argName("rootFolderPath").hasArg().desc("the folder where the files are located.").required().build();
        Option successFolderPath = Option.builder("successFolder").argName("successFolderPath").hasArg().desc("the folder where the OK files will be transfered in case of Support mode.").required(false).build();
        Option errorFolderPath = Option.builder("errorFolder").argName("errorFolderPath").hasArg().desc("the folder where the NOT OK files will be transfered in case of Support mode.").required(false).build();

        Options options = new Options();
        options.addOption(help);
        options.addOption(mode);
        options.addOption(fileFormat);
        options.addOption(rootFolderPath);
        options.addOption(successFolderPath);
        options.addOption(errorFolderPath);

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            // Check the required options
            for (Option o : options.getOptions())
                if (o.isRequired() && !line.hasOption(o.getOpt())) {
                    log.error("Required options are missing. Set all the required options to proceed.");
                    formatter.printHelp("LinkChecker", options, true);
                    return null;
                }

            linkCheckerOptions.supportMode = line.getOptionValue(mode.getOpt()).equals("support");
            linkCheckerOptions.fileFormat = line.getOptionValue(fileFormat.getOpt());
            linkCheckerOptions.rootFolderPath = line.getOptionValue(rootFolderPath.getOpt());
            linkCheckerOptions.successFolderPath = !linkCheckerOptions.supportMode ? line.getOptionValue(successFolderPath.getOpt()) : null;
            linkCheckerOptions.errorFolderPath = !linkCheckerOptions.supportMode ? line.getOptionValue(errorFolderPath.getOpt()) : null;

            // Check that in case of active mode, the successPath and the
            // errorPath are set
            if (!linkCheckerOptions.supportMode && (linkCheckerOptions.successFolderPath == null || linkCheckerOptions.errorFolderPath == null)) {
                log.error("Success and error paths are required in case of active mode. Set them to proceed.");
                formatter.printHelp("LinkChecker", options, true);
                return null;
            }

            // check that file format is valid
            Arrays.sort(supportedFileFortmats);
            if (Arrays.binarySearch(supportedFileFortmats, linkCheckerOptions.fileFormat) < 0) {
                log.error("File format {} is not supported. Select one of {} to proceed.", linkCheckerOptions.fileFormat, supportedFileFortmats);
                formatter.printHelp("LinkChecker", options, true);
                return null;
            }
        } catch (ParseException e) {
            formatter.printHelp("EmailScrumer", options, true);
            log.trace("Parsing failed.  Reason: ", e);
            return null;
        }

        return linkCheckerOptions;
    }
}
