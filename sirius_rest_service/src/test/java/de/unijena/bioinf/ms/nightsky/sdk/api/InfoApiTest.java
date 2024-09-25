package de.unijena.bioinf.ms.nightsky.sdk.api;

import de.unijena.bioinf.ms.nightsky.sdk.NightSkyClient;
import de.unijena.bioinf.ms.nightsky.sdk.model.Info;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class InfoApiTest {

    private InfoApi instance;
    private NightSkyClient siriusClient;

    @BeforeEach
    public void setUp() {
        TestSetup.getInstance().loginIfNeeded();siriusClient = TestSetup.getInstance().getSiriusClient();
        instance = siriusClient.infos();
    }


    @Test
    public void instanceTest() {
        assertNotNull(instance);
    }

    @Test
    public void getVersionInfoTest()  {
        Info response = instance.getInfo(null, null);
        assertNotNull(response);
    }
}
