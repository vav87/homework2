package ru.digitalhabbits.homework2;

import org.slf4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Exchanger;

import static java.lang.Thread.currentThread;
import static org.slf4j.LoggerFactory.getLogger;

public class FileWriter implements Runnable {
    private static final Logger logger = getLogger(FileWriter.class);

    private Exchanger<List<String>> exchanger;
    private File outFile;
    private List<String> linesToPrint;
    private final int chunkSize;

    public FileWriter(Exchanger<List<String>> exchanger, File outFile, int chunkSize) {
        this.exchanger = exchanger;
        this.outFile = outFile;
        this.chunkSize = chunkSize;
    }

    @Override
    public void run() {

        logger.info("Started writer thread {}", currentThread().getName());
        try(BufferedWriter outputWriter = new BufferedWriter(new java.io.FileWriter(outFile))) {
            linesToPrint = new ArrayList<>(chunkSize);
            while(!currentThread().isInterrupted()) {
                try {
                    linesToPrint = exchanger.exchange(linesToPrint);
                } catch (InterruptedException e) {
                    break;
                }
                for(String line: linesToPrint) {
                    outputWriter.write(line);
                    outputWriter.newLine();
                }
                // очистка списка
                linesToPrint.clear();
            }
        } catch (FileNotFoundException e) {
            System.out.println("Exception in FileWriter2 FileNotFoundException");
        } catch (IOException ioe) {
            System.out.println("Exception in FileWriter2 IOException");
        }
        logger.info("Finish writer thread {}", currentThread().getName());
    }
}
