/*
 * SIRIUS Nightsky API
 * REST API that provides the full functionality of SIRIUS and its web services as background service. It is intended as entry-point for scripting languages and software integration SDKs.This API is exposed by SIRIUS 6.0.0-SNAPSHOT
 *
 * The version of the OpenAPI document: 2.1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package de.unijena.bioinf.ms.nightsky.sdk.api;

import de.unijena.bioinf.ms.nightsky.sdk.model.GuiInfo;
import org.junit.Test;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API tests for GuiApi
 */
@Ignore
public class GuiApiTest {

    private final GuiApi api = new GuiApi();

    
    /**
     * Close GUI instance of given project-space if available.
     *
     * Close GUI instance of given project-space if available.
     */
    @Test
    public void closeGuiTest()  {
        String projectId = null;
        Boolean closeProject = null;
        Boolean response = api.closeGui(projectId, closeProject);

        // TODO: test validations
    }
    
    /**
     * Get list of currently running gui windows, managed by this SIRIUS instance.
     *
     * Get list of currently running gui windows, managed by this SIRIUS instance.  Note this will not show any Clients that are connected from a separate process!
     */
    @Test
    public void getGuisTest()  {
        List<GuiInfo> response = api.getGuis();

        // TODO: test validations
    }
    
    /**
     * Open GUI instance on specified project-space and bring the GUI window to foreground.
     *
     * Open GUI instance on specified project-space and bring the GUI window to foreground.
     */
    @Test
    public void openGuiTest()  {
        String projectId = null;
        api.openGui(projectId);

        // TODO: test validations
    }
    
}
