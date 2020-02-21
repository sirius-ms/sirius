package de.unijena.bioinf.ms.middleware;

import org.springframework.boot.actuate.context.ShutdownEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
public class ShutDown extends ShutdownEndpoint {
    private static final Map<String, String> SHUTDOWN_MESSAGE = Collections
            .unmodifiableMap(Collections.singletonMap("message", "Shutting down SpringBootApp and SIRIUS afterward, bye..."));

    @Override
    @WriteOperation
    public Map<String, String> shutdown() {
        try {
            return SHUTDOWN_MESSAGE;
        } finally {
            new Thread(() -> {
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                System.exit(0);
            }).start();
        }
    }
}
