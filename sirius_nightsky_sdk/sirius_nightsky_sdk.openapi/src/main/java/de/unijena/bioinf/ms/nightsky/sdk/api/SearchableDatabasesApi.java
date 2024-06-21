package de.unijena.bioinf.ms.nightsky.sdk.api;

import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;

import java.io.File;
import de.unijena.bioinf.ms.nightsky.sdk.model.Job;
import de.unijena.bioinf.ms.nightsky.sdk.model.JobOptField;
import de.unijena.bioinf.ms.nightsky.sdk.model.SearchableDatabase;
import de.unijena.bioinf.ms.nightsky.sdk.model.SearchableDatabaseParameters;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.6.0")
public class SearchableDatabasesApi {
    private ApiClient apiClient;

    public SearchableDatabasesApi() {
        this(new ApiClient());
    }

    @Autowired
    public SearchableDatabasesApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param requestBody The requestBody parameter
     * @return List&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec addDatabasesRequestCreation(List<String> requestBody) throws WebClientResponseException {
        Object postBody = requestBody;
        // verify the required parameter 'requestBody' is set
        if (requestBody == null) {
            throw new WebClientResponseException("Missing the required parameter 'requestBody' when calling addDatabases", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return apiClient.invokeAPI("/api/databases", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param requestBody The requestBody parameter
     * @return List&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<SearchableDatabase> addDatabases(List<String> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return addDatabasesRequestCreation(requestBody).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param requestBody The requestBody parameter
     * @return ResponseEntity&lt;List&lt;SearchableDatabase&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<SearchableDatabase>> addDatabasesWithHttpInfo(List<String> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return addDatabasesRequestCreation(requestBody).toEntityList(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param requestBody The requestBody parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addDatabasesWithResponseSpec(List<String> requestBody) throws WebClientResponseException {
        return addDatabasesRequestCreation(requestBody);
    }
    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param searchableDatabaseParameters The searchableDatabaseParameters parameter
     * @return SearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec createDatabaseRequestCreation(String databaseId, SearchableDatabaseParameters searchableDatabaseParameters) throws WebClientResponseException {
        Object postBody = searchableDatabaseParameters;
        // verify the required parameter 'databaseId' is set
        if (databaseId == null) {
            throw new WebClientResponseException("Missing the required parameter 'databaseId' when calling createDatabase", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("databaseId", databaseId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return apiClient.invokeAPI("/api/databases/{databaseId}", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param searchableDatabaseParameters The searchableDatabaseParameters parameter
     * @return SearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public SearchableDatabase createDatabase(String databaseId, SearchableDatabaseParameters searchableDatabaseParameters) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return createDatabaseRequestCreation(databaseId, searchableDatabaseParameters).bodyToMono(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param searchableDatabaseParameters The searchableDatabaseParameters parameter
     * @return ResponseEntity&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<SearchableDatabase> createDatabaseWithHttpInfo(String databaseId, SearchableDatabaseParameters searchableDatabaseParameters) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return createDatabaseRequestCreation(databaseId, searchableDatabaseParameters).toEntity(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param searchableDatabaseParameters The searchableDatabaseParameters parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec createDatabaseWithResponseSpec(String databaseId, SearchableDatabaseParameters searchableDatabaseParameters) throws WebClientResponseException {
        return createDatabaseRequestCreation(databaseId, searchableDatabaseParameters);
    }
    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param includeStats The includeStats parameter
     * @return List&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCustomDatabasesRequestCreation(Boolean includeStats) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "includeStats", includeStats));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return apiClient.invokeAPI("/api/databases/custom", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param includeStats The includeStats parameter
     * @return List&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<SearchableDatabase> getCustomDatabases(Boolean includeStats) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return getCustomDatabasesRequestCreation(includeStats).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param includeStats The includeStats parameter
     * @return ResponseEntity&lt;List&lt;SearchableDatabase&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<SearchableDatabase>> getCustomDatabasesWithHttpInfo(Boolean includeStats) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return getCustomDatabasesRequestCreation(includeStats).toEntityList(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param includeStats The includeStats parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCustomDatabasesWithResponseSpec(Boolean includeStats) throws WebClientResponseException {
        return getCustomDatabasesRequestCreation(includeStats);
    }
    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param includeStats The includeStats parameter
     * @return SearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getDatabaseRequestCreation(String databaseId, Boolean includeStats) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'databaseId' is set
        if (databaseId == null) {
            throw new WebClientResponseException("Missing the required parameter 'databaseId' when calling getDatabase", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("databaseId", databaseId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "includeStats", includeStats));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return apiClient.invokeAPI("/api/databases/{databaseId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param includeStats The includeStats parameter
     * @return SearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public SearchableDatabase getDatabase(String databaseId, Boolean includeStats) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return getDatabaseRequestCreation(databaseId, includeStats).bodyToMono(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param includeStats The includeStats parameter
     * @return ResponseEntity&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<SearchableDatabase> getDatabaseWithHttpInfo(String databaseId, Boolean includeStats) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return getDatabaseRequestCreation(databaseId, includeStats).toEntity(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param includeStats The includeStats parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getDatabaseWithResponseSpec(String databaseId, Boolean includeStats) throws WebClientResponseException {
        return getDatabaseRequestCreation(databaseId, includeStats);
    }
    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param includeStats The includeStats parameter
     * @return List&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getDatabasesRequestCreation(Boolean includeStats) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "includeStats", includeStats));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return apiClient.invokeAPI("/api/databases", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param includeStats The includeStats parameter
     * @return List&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<SearchableDatabase> getDatabases(Boolean includeStats) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return getDatabasesRequestCreation(includeStats).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param includeStats The includeStats parameter
     * @return ResponseEntity&lt;List&lt;SearchableDatabase&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<SearchableDatabase>> getDatabasesWithHttpInfo(Boolean includeStats) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return getDatabasesRequestCreation(includeStats).toEntityList(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param includeStats The includeStats parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getDatabasesWithResponseSpec(Boolean includeStats) throws WebClientResponseException {
        return getDatabasesRequestCreation(includeStats);
    }
    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param includeStats The includeStats parameter
     * @return List&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getIncludedDatabasesRequestCreation(Boolean includeStats) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "includeStats", includeStats));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return apiClient.invokeAPI("/api/databases/included", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param includeStats The includeStats parameter
     * @return List&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<SearchableDatabase> getIncludedDatabases(Boolean includeStats) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return getIncludedDatabasesRequestCreation(includeStats).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param includeStats The includeStats parameter
     * @return ResponseEntity&lt;List&lt;SearchableDatabase&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<SearchableDatabase>> getIncludedDatabasesWithHttpInfo(Boolean includeStats) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return getIncludedDatabasesRequestCreation(includeStats).toEntityList(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param includeStats The includeStats parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getIncludedDatabasesWithResponseSpec(Boolean includeStats) throws WebClientResponseException {
        return getIncludedDatabasesRequestCreation(includeStats);
    }
    /**
     * Start import of structure and spectra files into the specified database.
     * Start import of structure and spectra files into the specified database.
     * <p><b>200</b> - Job of the import command to be executed.
     * @param databaseId database to import into
     * @param bufferSize The bufferSize parameter
     * @param inputFiles The inputFiles parameter
     * @return SearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec importIntoDatabaseRequestCreation(String databaseId, Integer bufferSize, List<File> inputFiles) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'databaseId' is set
        if (databaseId == null) {
            throw new WebClientResponseException("Missing the required parameter 'databaseId' when calling importIntoDatabase", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("databaseId", databaseId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "bufferSize", bufferSize));
        
        if (inputFiles != null)
            formParams.addAll("inputFiles", inputFiles.stream().map(FileSystemResource::new).collect(Collectors.toList()));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "multipart/form-data"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return apiClient.invokeAPI("/api/databases/{databaseId}/import/from-files", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Start import of structure and spectra files into the specified database.
     * Start import of structure and spectra files into the specified database.
     * <p><b>200</b> - Job of the import command to be executed.
     * @param databaseId database to import into
     * @param bufferSize The bufferSize parameter
     * @param inputFiles The inputFiles parameter
     * @return SearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public SearchableDatabase importIntoDatabase(String databaseId, Integer bufferSize, List<File> inputFiles) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return importIntoDatabaseRequestCreation(databaseId, bufferSize, inputFiles).bodyToMono(localVarReturnType).block();
    }

    /**
     * Start import of structure and spectra files into the specified database.
     * Start import of structure and spectra files into the specified database.
     * <p><b>200</b> - Job of the import command to be executed.
     * @param databaseId database to import into
     * @param bufferSize The bufferSize parameter
     * @param inputFiles The inputFiles parameter
     * @return ResponseEntity&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<SearchableDatabase> importIntoDatabaseWithHttpInfo(String databaseId, Integer bufferSize, List<File> inputFiles) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return importIntoDatabaseRequestCreation(databaseId, bufferSize, inputFiles).toEntity(localVarReturnType).block();
    }

    /**
     * Start import of structure and spectra files into the specified database.
     * Start import of structure and spectra files into the specified database.
     * <p><b>200</b> - Job of the import command to be executed.
     * @param databaseId database to import into
     * @param bufferSize The bufferSize parameter
     * @param inputFiles The inputFiles parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec importIntoDatabaseWithResponseSpec(String databaseId, Integer bufferSize, List<File> inputFiles) throws WebClientResponseException {
        return importIntoDatabaseRequestCreation(databaseId, bufferSize, inputFiles);
    }
    /**
     * Start import of structure and spectra files into the specified database.
     * Start import of structure and spectra files into the specified database.
     * <p><b>200</b> - Job of the import command to be executed.
     * @param databaseId database to import into
     * @param bufferSize The bufferSize parameter
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param inputFiles The inputFiles parameter
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec importIntoDatabaseAsJobRequestCreation(String databaseId, Integer bufferSize, List<JobOptField> optFields, List<File> inputFiles) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'databaseId' is set
        if (databaseId == null) {
            throw new WebClientResponseException("Missing the required parameter 'databaseId' when calling importIntoDatabaseAsJob", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("databaseId", databaseId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "bufferSize", bufferSize));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        if (inputFiles != null)
            formParams.addAll("inputFiles", inputFiles.stream().map(FileSystemResource::new).collect(Collectors.toList()));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "multipart/form-data"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return apiClient.invokeAPI("/api/databases/{databaseId}/import/from-files-job", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Start import of structure and spectra files into the specified database.
     * Start import of structure and spectra files into the specified database.
     * <p><b>200</b> - Job of the import command to be executed.
     * @param databaseId database to import into
     * @param bufferSize The bufferSize parameter
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param inputFiles The inputFiles parameter
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Job importIntoDatabaseAsJob(String databaseId, Integer bufferSize, List<JobOptField> optFields, List<File> inputFiles) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return importIntoDatabaseAsJobRequestCreation(databaseId, bufferSize, optFields, inputFiles).bodyToMono(localVarReturnType).block();
    }

    /**
     * Start import of structure and spectra files into the specified database.
     * Start import of structure and spectra files into the specified database.
     * <p><b>200</b> - Job of the import command to be executed.
     * @param databaseId database to import into
     * @param bufferSize The bufferSize parameter
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param inputFiles The inputFiles parameter
     * @return ResponseEntity&lt;Job&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Job> importIntoDatabaseAsJobWithHttpInfo(String databaseId, Integer bufferSize, List<JobOptField> optFields, List<File> inputFiles) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return importIntoDatabaseAsJobRequestCreation(databaseId, bufferSize, optFields, inputFiles).toEntity(localVarReturnType).block();
    }

    /**
     * Start import of structure and spectra files into the specified database.
     * Start import of structure and spectra files into the specified database.
     * <p><b>200</b> - Job of the import command to be executed.
     * @param databaseId database to import into
     * @param bufferSize The bufferSize parameter
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param inputFiles The inputFiles parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec importIntoDatabaseAsJobWithResponseSpec(String databaseId, Integer bufferSize, List<JobOptField> optFields, List<File> inputFiles) throws WebClientResponseException {
        return importIntoDatabaseAsJobRequestCreation(databaseId, bufferSize, optFields, inputFiles);
    }
    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param delete The delete parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec removeDatabaseRequestCreation(String databaseId, Boolean delete) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'databaseId' is set
        if (databaseId == null) {
            throw new WebClientResponseException("Missing the required parameter 'databaseId' when calling removeDatabase", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("databaseId", databaseId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "delete", delete));
        
        final String[] localVarAccepts = { };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/api/databases/{databaseId}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param delete The delete parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void removeDatabase(String databaseId, Boolean delete) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        removeDatabaseRequestCreation(databaseId, delete).bodyToMono(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param delete The delete parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> removeDatabaseWithHttpInfo(String databaseId, Boolean delete) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return removeDatabaseRequestCreation(databaseId, delete).toEntity(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param delete The delete parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec removeDatabaseWithResponseSpec(String databaseId, Boolean delete) throws WebClientResponseException {
        return removeDatabaseRequestCreation(databaseId, delete);
    }
    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param searchableDatabaseParameters The searchableDatabaseParameters parameter
     * @return SearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec updateDatabaseRequestCreation(String databaseId, SearchableDatabaseParameters searchableDatabaseParameters) throws WebClientResponseException {
        Object postBody = searchableDatabaseParameters;
        // verify the required parameter 'databaseId' is set
        if (databaseId == null) {
            throw new WebClientResponseException("Missing the required parameter 'databaseId' when calling updateDatabase", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("databaseId", databaseId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return apiClient.invokeAPI("/api/databases/{databaseId}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param searchableDatabaseParameters The searchableDatabaseParameters parameter
     * @return SearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public SearchableDatabase updateDatabase(String databaseId, SearchableDatabaseParameters searchableDatabaseParameters) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return updateDatabaseRequestCreation(databaseId, searchableDatabaseParameters).bodyToMono(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param searchableDatabaseParameters The searchableDatabaseParameters parameter
     * @return ResponseEntity&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<SearchableDatabase> updateDatabaseWithHttpInfo(String databaseId, SearchableDatabaseParameters searchableDatabaseParameters) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return updateDatabaseRequestCreation(databaseId, searchableDatabaseParameters).toEntity(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param searchableDatabaseParameters The searchableDatabaseParameters parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec updateDatabaseWithResponseSpec(String databaseId, SearchableDatabaseParameters searchableDatabaseParameters) throws WebClientResponseException {
        return updateDatabaseRequestCreation(databaseId, searchableDatabaseParameters);
    }
}
