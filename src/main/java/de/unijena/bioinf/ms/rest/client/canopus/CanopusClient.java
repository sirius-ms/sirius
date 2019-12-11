package de.unijena.bioinf.ms.rest.client.canopus;

import de.unijena.bioinf.ms.rest.client.AbstractClient;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

public class CanopusClient extends AbstractClient {
    protected CanopusClient(@NotNull URI serverUrl) {
        super(serverUrl);
    }
}
