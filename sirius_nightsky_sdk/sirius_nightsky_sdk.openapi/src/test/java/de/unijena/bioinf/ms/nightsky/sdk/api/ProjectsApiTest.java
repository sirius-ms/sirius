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

import java.io.File;
import de.unijena.bioinf.ms.nightsky.sdk.model.ImportResult;
import de.unijena.bioinf.ms.nightsky.sdk.model.Job;
import de.unijena.bioinf.ms.nightsky.sdk.model.JobOptField;
import de.unijena.bioinf.ms.nightsky.sdk.model.ProjectInfo;
import de.unijena.bioinf.ms.nightsky.sdk.model.ProjectInfoOptField;
import org.junit.Test;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API tests for ProjectsApi
 */
@Ignore
public class ProjectsApiTest {

    private final ProjectsApi api = new ProjectsApi();

    
    /**
     * Close project-space and remove it from application
     *
     * Close project-space and remove it from application. Project will NOT be deleted from disk.  &lt;p&gt;  ATTENTION: This will cancel and remove all jobs running on this Project before closing it.  If there are many jobs, this might take some time.
     */
    @Test
    public void closeProjectSpaceTest()  {
        String projectId = null;
        api.closeProjectSpace(projectId);

        // TODO: test validations
    }
    
    /**
     * Move an existing (opened) project-space to another location.
     *
     * Move an existing (opened) project-space to another location.
     */
    @Test
    public void copyProjectSpaceTest()  {
        String projectId = null;
        String pathToCopiedProject = null;
        String copyProjectId = null;
        List<ProjectInfoOptField> optFields = null;
        ProjectInfo response = api.copyProjectSpace(projectId, pathToCopiedProject, copyProjectId, optFields);

        // TODO: test validations
    }
    
    /**
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     *
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     */
    @Test
    public void createProjectSpaceTest()  {
        String projectId = null;
        String pathToProject = null;
        ProjectInfo response = api.createProjectSpace(projectId, pathToProject);

        // TODO: test validations
    }
    
    /**
     * Get CANOPUS prediction vector definition for ClassyFire classes
     *
     * 
     */
    @Test
    public void getCanopusClassyFireDataTest()  {
        String projectId = null;
        Integer charge = null;
        String response = api.getCanopusClassyFireData(projectId, charge);

        // TODO: test validations
    }
    
    /**
     * Get CANOPUS prediction vector definition for NPC classes
     *
     * 
     */
    @Test
    public void getCanopusNpcDataTest()  {
        String projectId = null;
        Integer charge = null;
        String response = api.getCanopusNpcData(projectId, charge);

        // TODO: test validations
    }
    
    /**
     * Get CSI:FingerID fingerprint (prediction vector) definition
     *
     * 
     */
    @Test
    public void getFingerIdDataTest()  {
        String projectId = null;
        Integer charge = null;
        String response = api.getFingerIdData(projectId, charge);

        // TODO: test validations
    }
    
    /**
     * Get project space info by its projectId.
     *
     * Get project space info by its projectId.
     */
    @Test
    public void getProjectSpaceTest()  {
        String projectId = null;
        List<ProjectInfoOptField> optFields = null;
        ProjectInfo response = api.getProjectSpace(projectId, optFields);

        // TODO: test validations
    }
    
    /**
     * List opened project spaces.
     *
     * List opened project spaces.
     */
    @Test
    public void getProjectSpacesTest()  {
        List<ProjectInfo> response = api.getProjectSpaces();

        // TODO: test validations
    }
    
    /**
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)
     *
     * Import and Align full MS-Runs from various formats into the specified project  Possible formats (mzML, mzXML)
     */
    @Test
    public void importMsRunDataTest()  {
        String projectId = null;
        Boolean alignRuns = null;
        Boolean allowMs1Only = null;
        List<File> inputFiles = null;
        ImportResult response = api.importMsRunData(projectId, alignRuns, allowMs1Only, inputFiles);

        // TODO: test validations
    }
    
    /**
     * Import and Align full MS-Runs from various formats into the specified project as background job.
     *
     * Import and Align full MS-Runs from various formats into the specified project as background job.  Possible formats (mzML, mzXML)
     */
    @Test
    public void importMsRunDataAsJobTest()  {
        String projectId = null;
        Boolean alignRuns = null;
        Boolean allowMs1Only = null;
        List<JobOptField> optFields = null;
        List<File> inputFiles = null;
        Job response = api.importMsRunDataAsJob(projectId, alignRuns, allowMs1Only, optFields, inputFiles);

        // TODO: test validations
    }
    
    /**
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)
     *
     * Import already preprocessed ms/ms data from various formats into the specified project  Possible formats (ms, mgf, cef, msp)
     */
    @Test
    public void importPreprocessedDataTest()  {
        String projectId = null;
        Boolean ignoreFormulas = null;
        Boolean allowMs1Only = null;
        List<File> inputFiles = null;
        ImportResult response = api.importPreprocessedData(projectId, ignoreFormulas, allowMs1Only, inputFiles);

        // TODO: test validations
    }
    
    /**
     * Import ms/ms data from the given format into the specified project-space as background job.
     *
     * Import ms/ms data from the given format into the specified project-space as background job.  Possible formats (ms, mgf, cef, msp)
     */
    @Test
    public void importPreprocessedDataAsJobTest()  {
        String projectId = null;
        Boolean ignoreFormulas = null;
        Boolean allowMs1Only = null;
        List<JobOptField> optFields = null;
        List<File> inputFiles = null;
        Job response = api.importPreprocessedDataAsJob(projectId, ignoreFormulas, allowMs1Only, optFields, inputFiles);

        // TODO: test validations
    }
    
    /**
     * Open an existing project-space and make it accessible via the given projectId.
     *
     * Open an existing project-space and make it accessible via the given projectId.
     */
    @Test
    public void openProjectSpaceTest()  {
        String projectId = null;
        String pathToProject = null;
        List<ProjectInfoOptField> optFields = null;
        ProjectInfo response = api.openProjectSpace(projectId, pathToProject, optFields);

        // TODO: test validations
    }
    
}
