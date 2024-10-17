package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.model.Info;
import io.sirius.ms.sdk.SiriusClient;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class InfoApiTest {

    private InfoApi instance;
    private SiriusClient siriusClient;

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
