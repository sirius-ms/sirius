package de.unijena.bioinf.ms.nightsky.sdk.api;

import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;

import de.unijena.bioinf.ms.nightsky.sdk.model.PageSearchableDatabase;
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

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
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
     * @param databaseId The databaseId parameter
     * @param pathToProject The pathToProject parameter
     * @return SearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addDatabaseRequestCreation(String databaseId, String pathToProject) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'databaseId' is set
        if (databaseId == null) {
            throw new WebClientResponseException("Missing the required parameter 'databaseId' when calling addDatabase", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'pathToProject' is set
        if (pathToProject == null) {
            throw new WebClientResponseException("Missing the required parameter 'pathToProject' when calling addDatabase", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("databaseId", databaseId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "pathToProject", pathToProject));

        final String[] localVarAccepts = { 
            "*/*"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return apiClient.invokeAPI("/api/databases/{databaseId}/add", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param pathToProject The pathToProject parameter
     * @return SearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public SearchableDatabase addDatabase(String databaseId, String pathToProject) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return addDatabaseRequestCreation(databaseId, pathToProject).bodyToMono(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param pathToProject The pathToProject parameter
     * @return ResponseEntity&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<SearchableDatabase> addDatabaseWithHttpInfo(String databaseId, String pathToProject) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return addDatabaseRequestCreation(databaseId, pathToProject).toEntity(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param databaseId The databaseId parameter
     * @param pathToProject The pathToProject parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addDatabaseWithResponseSpec(String databaseId, String pathToProject) throws WebClientResponseException {
        return addDatabaseRequestCreation(databaseId, pathToProject);
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
            "*/*"
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
            "*/*"
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
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param includeStats The includeStats parameter
     * @return PageSearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getDatabasesRequestCreation(Integer page, Integer size, List<String> sort, Boolean includeStats) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "sort", sort));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "includeStats", includeStats));

        final String[] localVarAccepts = { 
            "*/*"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PageSearchableDatabase> localVarReturnType = new ParameterizedTypeReference<PageSearchableDatabase>() {};
        return apiClient.invokeAPI("/api/databases", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param includeStats The includeStats parameter
     * @return PageSearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PageSearchableDatabase getDatabases(Integer page, Integer size, List<String> sort, Boolean includeStats) throws WebClientResponseException {
        ParameterizedTypeReference<PageSearchableDatabase> localVarReturnType = new ParameterizedTypeReference<PageSearchableDatabase>() {};
        return getDatabasesRequestCreation(page, size, sort, includeStats).bodyToMono(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param includeStats The includeStats parameter
     * @return ResponseEntity&lt;PageSearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PageSearchableDatabase> getDatabasesWithHttpInfo(Integer page, Integer size, List<String> sort, Boolean includeStats) throws WebClientResponseException {
        ParameterizedTypeReference<PageSearchableDatabase> localVarReturnType = new ParameterizedTypeReference<PageSearchableDatabase>() {};
        return getDatabasesRequestCreation(page, size, sort, includeStats).toEntity(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param includeStats The includeStats parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getDatabasesWithResponseSpec(Integer page, Integer size, List<String> sort, Boolean includeStats) throws WebClientResponseException {
        return getDatabasesRequestCreation(page, size, sort, includeStats);
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
            "*/*"
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
