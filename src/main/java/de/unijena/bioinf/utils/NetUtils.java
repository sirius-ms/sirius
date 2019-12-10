package de.unijena.bioinf.utils;

import de.unijena.bioinf.ms.rest.AbstractClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class NetUtils {
    public static final Logger LOG = LoggerFactory.getLogger(NetUtils.class);

    public static void tryAndWait(NetRunnable tryToDo) {
        tryAndWait(() -> {
            tryToDo.run();
            return true;
        });
    }

    public static <R> R tryAndWait(NetSupplier<R> tryToDo) {
        try {
            return tryAndWait(tryToDo, Long.MAX_VALUE);
        } catch (TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void tryAndWait(NetRunnable tryToDo, long timeout) throws InterruptedException, TimeoutException {
        tryAndWait(() -> {
            tryToDo.run();
            return true;
        }, timeout);
    }

    public static <R> R tryAndWait(NetSupplier<R> tryToDo, long timeout) throws InterruptedException, TimeoutException {
        long waitTime = 5000;
        while (timeout > 0) {
            try {
                return tryToDo.get();
            } catch (IOException retry) {
                waitTime = Math.min(waitTime * 2, 120000);
                timeout -= waitTime;


                if(AbstractClient.DEBUG){
                    LOG.warn("Error when try to connect to Server. Try again in " + waitTime / 1000d + "s", retry);
                }else {
                    LOG.warn("Error when try to connect to Server. Try again in " + waitTime / 1000d + "s \n Cause: " + retry.getMessage());
                    LOG.debug("Error when try to connect to Server. Try again in " + waitTime / 1000d + "s", retry);
                }

                Thread.sleep(waitTime);
            }
        }
        throw new TimeoutException("Stop trying because of Timeout!");
    }

    @FunctionalInterface
    public interface NetSupplier<R> {
        R get() throws InterruptedException, TimeoutException, IOException;
    }

    @FunctionalInterface
    public interface NetRunnable {
        void run() throws InterruptedException, TimeoutException, IOException;
    }
}
