package ru.digitalhabbits.homework2;

import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

import static java.lang.Runtime.getRuntime;
import static java.nio.charset.Charset.defaultCharset;
import static org.slf4j.LoggerFactory.getLogger;

public class FileProcessor {
    private static final Logger logger = getLogger(FileProcessor.class);
    public static final int CHUNK_SIZE = 2 * getRuntime().availableProcessors();

    public void process(@Nonnull String processingFileName, @Nonnull String resultFileName) {
        checkFileExists(processingFileName);

        Exchanger<List<String>> exchanger = new Exchanger<>();
        List<String> lines = new ArrayList<String>(CHUNK_SIZE);
        List<String> linesAfterProcess = new ArrayList<String>(CHUNK_SIZE);

        final File fileIn = new File(processingFileName);
        final File fileOut = new File(resultFileName);

        LineProcessor lineProcessor = new LineCounterProcessor();
        ExecutorService executorService = Executors.newFixedThreadPool(CHUNK_SIZE);

        // запускаем FileWriter в отдельном потоке
        Thread fileWriterThread = new Thread(new FileWriter(exchanger, fileOut, CHUNK_SIZE));
        fileWriterThread.start();

        try (final Scanner scanner = new Scanner(fileIn, defaultCharset())) {
            while (scanner.hasNext()) {
                // вычитываем CHUNK_SIZE строк для параллельной обработки
                while(lines.size()<CHUNK_SIZE && scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    lines.add(line);
                }

                // обрабатывать строку с помощью LineProcessor. Каждый поток обрабатывает свою строку.
                List<Callable<String>> tasks = new ArrayList<>(lines.size());
                for (String l: lines) {
                    Callable<String> c = () -> {return lineProcessor.process(l).toString("%1$s %2$s");};
                    tasks.add(c);
                }
                List<Future<String>> results = executorService.invokeAll(tasks);
                // получение обработанных строк из Future
                for(Future<String> r: results) {
                    linesAfterProcess.add(r.get());
                }

                // добавить обработанные данные в результирующий файл
                try {
                    linesAfterProcess = exchanger.exchange(linesAfterProcess);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                // удаление старых записей из списка
                lines.clear();
            }
        } catch (IOException | InterruptedException | ExecutionException exception) {
            logger.error("", exception);
        }

        // остановить поток writerThread
        fileWriterThread.interrupt();
        executorService.shutdown();
        logger.info("Finish main thread {}", Thread.currentThread().getName());
    }

    private void checkFileExists(@Nonnull String fileName) {
        final File file = new File(fileName);
        if (!file.exists() || file.isDirectory()) {
            throw new IllegalArgumentException("File '" + fileName + "' not exists");
        }
    }
}
