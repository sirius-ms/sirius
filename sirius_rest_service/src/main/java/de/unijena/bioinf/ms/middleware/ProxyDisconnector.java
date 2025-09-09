package de.unijena.bioinf.ms.middleware;

import de.unijena.bioinf.rest.ProxyManager;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class ProxyDisconnector {
    @PreDestroy
    private void disconnect(){
        ProxyManager.disconnect();
    }
}
