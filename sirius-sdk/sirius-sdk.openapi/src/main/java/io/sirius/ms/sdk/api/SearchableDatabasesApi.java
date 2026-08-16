package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import io.sirius.ms.sdk.model.BioTransformerParameters;
import io.sirius.ms.sdk.model.DownloadableDatabase;
import java.io.File;
import io.sirius.ms.sdk.model.PagedModelDatabaseStructure;
import io.sirius.ms.sdk.model.SearchableDatabase;
import io.sirius.ms.sdk.model.SearchableDatabaseParameters;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.17.0")
public class SearchableDatabasesApi {
    private ApiClient apiClient;

    public SearchableDatabasesApi() {
        this(new ApiClient());
    }

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
     * [DEPRECATED] This endpoint is based on local file paths and will likely be replaced in future versions of this API.
     * Register existing custom database files with this SIRIUS instance, so that they become searchable.  &lt;p&gt;  Use this to make databases that already exist on disk available again, for example after reinstalling  SIRIUS or when sharing a database file with a colleague. The files are opened in place, not copied.
     * <p><b>200</b> - the databases that were successfully registered. Files that exist but could not be opened are          skipped and are absent from the result.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - A path does not exist or is not a file, is already registered, or its database name is already in use. No database is registered in that case.
     * @param requestBody local file paths of the database files (.siriusdb) to register. Each must exist,                         must not already be registered, and its name must not collide with an existing                         database.
     * @return List&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec addDatabasesRequestCreation(@jakarta.annotation.Nonnull List<String> requestBody) throws WebClientResponseException {
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
            "application/json", "application/problem+json"
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
     * [DEPRECATED] This endpoint is based on local file paths and will likely be replaced in future versions of this API.
     * Register existing custom database files with this SIRIUS instance, so that they become searchable.  &lt;p&gt;  Use this to make databases that already exist on disk available again, for example after reinstalling  SIRIUS or when sharing a database file with a colleague. The files are opened in place, not copied.
     * <p><b>200</b> - the databases that were successfully registered. Files that exist but could not be opened are          skipped and are absent from the result.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - A path does not exist or is not a file, is already registered, or its database name is already in use. No database is registered in that case.
     * @param requestBody local file paths of the database files (.siriusdb) to register. Each must exist,                         must not already be registered, and its name must not collide with an existing                         database.
     * @return List&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<SearchableDatabase> addDatabases(@jakarta.annotation.Nonnull List<String> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return addDatabasesRequestCreation(requestBody).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [DEPRECATED] This endpoint is based on local file paths and will likely be replaced in future versions of this API.
     * Register existing custom database files with this SIRIUS instance, so that they become searchable.  &lt;p&gt;  Use this to make databases that already exist on disk available again, for example after reinstalling  SIRIUS or when sharing a database file with a colleague. The files are opened in place, not copied.
     * <p><b>200</b> - the databases that were successfully registered. Files that exist but could not be opened are          skipped and are absent from the result.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - A path does not exist or is not a file, is already registered, or its database name is already in use. No database is registered in that case.
     * @param requestBody local file paths of the database files (.siriusdb) to register. Each must exist,                         must not already be registered, and its name must not collide with an existing                         database.
     * @return ResponseEntity&lt;List&lt;SearchableDatabase&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<SearchableDatabase>> addDatabasesWithHttpInfo(@jakarta.annotation.Nonnull List<String> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return addDatabasesRequestCreation(requestBody).toEntityList(localVarReturnType).block();
    }

    /**
     * [DEPRECATED] This endpoint is based on local file paths and will likely be replaced in future versions of this API.
     * Register existing custom database files with this SIRIUS instance, so that they become searchable.  &lt;p&gt;  Use this to make databases that already exist on disk available again, for example after reinstalling  SIRIUS or when sharing a database file with a colleague. The files are opened in place, not copied.
     * <p><b>200</b> - the databases that were successfully registered. Files that exist but could not be opened are          skipped and are absent from the result.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - A path does not exist or is not a file, is already registered, or its database name is already in use. No database is registered in that case.
     * @param requestBody local file paths of the database files (.siriusdb) to register. Each must exist,                         must not already be registered, and its name must not collide with an existing                         database.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addDatabasesWithResponseSpec(@jakarta.annotation.Nonnull List<String> requestBody) throws WebClientResponseException {
        return addDatabasesRequestCreation(requestBody);
    }

    /**
     * Create a new, empty custom database
     * Create a new, empty custom database.  &lt;p&gt;  The new database is created on disk and registered with this SIRIUS instance, so it can immediately be  used as a search parameter and imported into via the import endpoint. It contains no structures and no  reference spectra until something is imported.
     * <p><b>200</b> - the created database.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The database id is not a valid database name. It must consist of letters, digits, &#39;-&#39; and &#39;_&#39; only.
     * <p><b>409</b> - A database with this id already exists, or a file already exists at the target location.
     * @param databaseId id of the new database. Must be URL-safe, that is letters, digits, &#39;-&#39; and &#39;_&#39; only,                     and must not be in use by another database.
     * @param searchableDatabaseParameters optional settings for the new database. If omitted, the database is created in the                     default custom database directory with default settings. Supply a location to place                     the database file elsewhere, a displayName for the user interface, and                     matchRtOfReferenceSpectra for in-house libraries whose retention times are comparable                     to the measured samples.
     * @return SearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec createDatabaseRequestCreation(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable SearchableDatabaseParameters searchableDatabaseParameters) throws WebClientResponseException {
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
            "application/json", "application/problem+json"
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
     * Create a new, empty custom database
     * Create a new, empty custom database.  &lt;p&gt;  The new database is created on disk and registered with this SIRIUS instance, so it can immediately be  used as a search parameter and imported into via the import endpoint. It contains no structures and no  reference spectra until something is imported.
     * <p><b>200</b> - the created database.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The database id is not a valid database name. It must consist of letters, digits, &#39;-&#39; and &#39;_&#39; only.
     * <p><b>409</b> - A database with this id already exists, or a file already exists at the target location.
     * @param databaseId id of the new database. Must be URL-safe, that is letters, digits, &#39;-&#39; and &#39;_&#39; only,                     and must not be in use by another database.
     * @param searchableDatabaseParameters optional settings for the new database. If omitted, the database is created in the                     default custom database directory with default settings. Supply a location to place                     the database file elsewhere, a displayName for the user interface, and                     matchRtOfReferenceSpectra for in-house libraries whose retention times are comparable                     to the measured samples.
     * @return SearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public SearchableDatabase createDatabase(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable SearchableDatabaseParameters searchableDatabaseParameters) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return createDatabaseRequestCreation(databaseId, searchableDatabaseParameters).bodyToMono(localVarReturnType).block();
    }

    /**
     * Create a new, empty custom database
     * Create a new, empty custom database.  &lt;p&gt;  The new database is created on disk and registered with this SIRIUS instance, so it can immediately be  used as a search parameter and imported into via the import endpoint. It contains no structures and no  reference spectra until something is imported.
     * <p><b>200</b> - the created database.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The database id is not a valid database name. It must consist of letters, digits, &#39;-&#39; and &#39;_&#39; only.
     * <p><b>409</b> - A database with this id already exists, or a file already exists at the target location.
     * @param databaseId id of the new database. Must be URL-safe, that is letters, digits, &#39;-&#39; and &#39;_&#39; only,                     and must not be in use by another database.
     * @param searchableDatabaseParameters optional settings for the new database. If omitted, the database is created in the                     default custom database directory with default settings. Supply a location to place                     the database file elsewhere, a displayName for the user interface, and                     matchRtOfReferenceSpectra for in-house libraries whose retention times are comparable                     to the measured samples.
     * @return ResponseEntity&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<SearchableDatabase> createDatabaseWithHttpInfo(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable SearchableDatabaseParameters searchableDatabaseParameters) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return createDatabaseRequestCreation(databaseId, searchableDatabaseParameters).toEntity(localVarReturnType).block();
    }

    /**
     * Create a new, empty custom database
     * Create a new, empty custom database.  &lt;p&gt;  The new database is created on disk and registered with this SIRIUS instance, so it can immediately be  used as a search parameter and imported into via the import endpoint. It contains no structures and no  reference spectra until something is imported.
     * <p><b>200</b> - the created database.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The database id is not a valid database name. It must consist of letters, digits, &#39;-&#39; and &#39;_&#39; only.
     * <p><b>409</b> - A database with this id already exists, or a file already exists at the target location.
     * @param databaseId id of the new database. Must be URL-safe, that is letters, digits, &#39;-&#39; and &#39;_&#39; only,                     and must not be in use by another database.
     * @param searchableDatabaseParameters optional settings for the new database. If omitted, the database is created in the                     default custom database directory with default settings. Supply a location to place                     the database file elsewhere, a displayName for the user interface, and                     matchRtOfReferenceSpectra for in-house libraries whose retention times are comparable                     to the measured samples.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec createDatabaseWithResponseSpec(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable SearchableDatabaseParameters searchableDatabaseParameters) throws WebClientResponseException {
        return createDatabaseRequestCreation(databaseId, searchableDatabaseParameters);
    }

    /**
     * List only the custom databases, that is the structure databases and spectral libraries the user has  created or added.
     * List only the custom databases, that is the structure databases and spectral libraries the user has  created or added. These are the databases that can be modified and imported into.
     * <p><b>200</b> - all custom databases known to this SIRIUS instance.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param includeStats if true, the number of structures, formulas and reference spectra is included                           per database. Slower, since the database files have to be read.
     * @param includeWithErrors if true, databases that could not be loaded are listed as well, carrying the                           reason in their errorMessage field.
     * @return List&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCustomDatabasesRequestCreation(@jakarta.annotation.Nullable Boolean includeStats, @jakarta.annotation.Nullable Boolean includeWithErrors) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "includeStats", includeStats));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "includeWithErrors", includeWithErrors));

        final String[] localVarAccepts = { 
            "application/json", "application/problem+json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return apiClient.invokeAPI("/api/databases/custom", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List only the custom databases, that is the structure databases and spectral libraries the user has  created or added.
     * List only the custom databases, that is the structure databases and spectral libraries the user has  created or added. These are the databases that can be modified and imported into.
     * <p><b>200</b> - all custom databases known to this SIRIUS instance.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param includeStats if true, the number of structures, formulas and reference spectra is included                           per database. Slower, since the database files have to be read.
     * @param includeWithErrors if true, databases that could not be loaded are listed as well, carrying the                           reason in their errorMessage field.
     * @return List&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<SearchableDatabase> getCustomDatabases(@jakarta.annotation.Nullable Boolean includeStats, @jakarta.annotation.Nullable Boolean includeWithErrors) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return getCustomDatabasesRequestCreation(includeStats, includeWithErrors).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * List only the custom databases, that is the structure databases and spectral libraries the user has  created or added.
     * List only the custom databases, that is the structure databases and spectral libraries the user has  created or added. These are the databases that can be modified and imported into.
     * <p><b>200</b> - all custom databases known to this SIRIUS instance.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param includeStats if true, the number of structures, formulas and reference spectra is included                           per database. Slower, since the database files have to be read.
     * @param includeWithErrors if true, databases that could not be loaded are listed as well, carrying the                           reason in their errorMessage field.
     * @return ResponseEntity&lt;List&lt;SearchableDatabase&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<SearchableDatabase>> getCustomDatabasesWithHttpInfo(@jakarta.annotation.Nullable Boolean includeStats, @jakarta.annotation.Nullable Boolean includeWithErrors) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return getCustomDatabasesRequestCreation(includeStats, includeWithErrors).toEntityList(localVarReturnType).block();
    }

    /**
     * List only the custom databases, that is the structure databases and spectral libraries the user has  created or added.
     * List only the custom databases, that is the structure databases and spectral libraries the user has  created or added. These are the databases that can be modified and imported into.
     * <p><b>200</b> - all custom databases known to this SIRIUS instance.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param includeStats if true, the number of structures, formulas and reference spectra is included                           per database. Slower, since the database files have to be read.
     * @param includeWithErrors if true, databases that could not be loaded are listed as well, carrying the                           reason in their errorMessage field.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCustomDatabasesWithResponseSpec(@jakarta.annotation.Nullable Boolean includeStats, @jakarta.annotation.Nullable Boolean includeWithErrors) throws WebClientResponseException {
        return getCustomDatabasesRequestCreation(includeStats, includeWithErrors);
    }

    /**
     * Get a single searchable database by its id.
     * Get a single searchable database by its id.
     * <p><b>200</b> - the requested database.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - No database with the given id exists.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the database to retrieve, as reported by the listing endpoints.
     * @param includeStats if true (the default here), the number of structures, formulas and reference spectra                      is included.
     * @return SearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getDatabaseRequestCreation(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable Boolean includeStats) throws WebClientResponseException {
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
            "application/json", "application/problem+json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return apiClient.invokeAPI("/api/databases/{databaseId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get a single searchable database by its id.
     * Get a single searchable database by its id.
     * <p><b>200</b> - the requested database.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - No database with the given id exists.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the database to retrieve, as reported by the listing endpoints.
     * @param includeStats if true (the default here), the number of structures, formulas and reference spectra                      is included.
     * @return SearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public SearchableDatabase getDatabase(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable Boolean includeStats) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return getDatabaseRequestCreation(databaseId, includeStats).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get a single searchable database by its id.
     * Get a single searchable database by its id.
     * <p><b>200</b> - the requested database.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - No database with the given id exists.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the database to retrieve, as reported by the listing endpoints.
     * @param includeStats if true (the default here), the number of structures, formulas and reference spectra                      is included.
     * @return ResponseEntity&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<SearchableDatabase> getDatabaseWithHttpInfo(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable Boolean includeStats) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return getDatabaseRequestCreation(databaseId, includeStats).toEntity(localVarReturnType).block();
    }

    /**
     * Get a single searchable database by its id.
     * Get a single searchable database by its id.
     * <p><b>200</b> - the requested database.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - No database with the given id exists.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the database to retrieve, as reported by the listing endpoints.
     * @param includeStats if true (the default here), the number of structures, formulas and reference spectra                      is included.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getDatabaseWithResponseSpec(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable Boolean includeStats) throws WebClientResponseException {
        return getDatabaseRequestCreation(databaseId, includeStats);
    }

    /**
     * List all searchable databases, both the ones included in SIRIUS and the custom ones added by the user
     * List all searchable databases, both the ones included in SIRIUS and the custom ones added by the user.  &lt;p&gt;  A searchable database provides structures and reference spectra (optional), and can be selected as a search  parameter for structure database search and spectral library search. Note that every imported spectral  library also acts as a structure database.
     * <p><b>200</b> - all databases known to this SIRIUS instance.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param includeStats if true, the number of structures, formulas and reference spectra is included                            per database. Computing these counts touches the database files, so requesting                            them is noticeably slower than a plain listing.
     * @param includeWithErrors if true, databases that could not be loaded are listed as well, carrying the                            reason in their errorMessage field. Use this to show a broken database to the                            user instead of silently hiding it.
     * @return List&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getDatabasesRequestCreation(@jakarta.annotation.Nullable Boolean includeStats, @jakarta.annotation.Nullable Boolean includeWithErrors) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "includeStats", includeStats));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "includeWithErrors", includeWithErrors));

        final String[] localVarAccepts = { 
            "application/json", "application/problem+json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return apiClient.invokeAPI("/api/databases", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List all searchable databases, both the ones included in SIRIUS and the custom ones added by the user
     * List all searchable databases, both the ones included in SIRIUS and the custom ones added by the user.  &lt;p&gt;  A searchable database provides structures and reference spectra (optional), and can be selected as a search  parameter for structure database search and spectral library search. Note that every imported spectral  library also acts as a structure database.
     * <p><b>200</b> - all databases known to this SIRIUS instance.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param includeStats if true, the number of structures, formulas and reference spectra is included                            per database. Computing these counts touches the database files, so requesting                            them is noticeably slower than a plain listing.
     * @param includeWithErrors if true, databases that could not be loaded are listed as well, carrying the                            reason in their errorMessage field. Use this to show a broken database to the                            user instead of silently hiding it.
     * @return List&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<SearchableDatabase> getDatabases(@jakarta.annotation.Nullable Boolean includeStats, @jakarta.annotation.Nullable Boolean includeWithErrors) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return getDatabasesRequestCreation(includeStats, includeWithErrors).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * List all searchable databases, both the ones included in SIRIUS and the custom ones added by the user
     * List all searchable databases, both the ones included in SIRIUS and the custom ones added by the user.  &lt;p&gt;  A searchable database provides structures and reference spectra (optional), and can be selected as a search  parameter for structure database search and spectral library search. Note that every imported spectral  library also acts as a structure database.
     * <p><b>200</b> - all databases known to this SIRIUS instance.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param includeStats if true, the number of structures, formulas and reference spectra is included                            per database. Computing these counts touches the database files, so requesting                            them is noticeably slower than a plain listing.
     * @param includeWithErrors if true, databases that could not be loaded are listed as well, carrying the                            reason in their errorMessage field. Use this to show a broken database to the                            user instead of silently hiding it.
     * @return ResponseEntity&lt;List&lt;SearchableDatabase&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<SearchableDatabase>> getDatabasesWithHttpInfo(@jakarta.annotation.Nullable Boolean includeStats, @jakarta.annotation.Nullable Boolean includeWithErrors) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return getDatabasesRequestCreation(includeStats, includeWithErrors).toEntityList(localVarReturnType).block();
    }

    /**
     * List all searchable databases, both the ones included in SIRIUS and the custom ones added by the user
     * List all searchable databases, both the ones included in SIRIUS and the custom ones added by the user.  &lt;p&gt;  A searchable database provides structures and reference spectra (optional), and can be selected as a search  parameter for structure database search and spectral library search. Note that every imported spectral  library also acts as a structure database.
     * <p><b>200</b> - all databases known to this SIRIUS instance.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param includeStats if true, the number of structures, formulas and reference spectra is included                            per database. Computing these counts touches the database files, so requesting                            them is noticeably slower than a plain listing.
     * @param includeWithErrors if true, databases that could not be loaded are listed as well, carrying the                            reason in their errorMessage field. Use this to show a broken database to the                            user instead of silently hiding it.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getDatabasesWithResponseSpec(@jakarta.annotation.Nullable Boolean includeStats, @jakarta.annotation.Nullable Boolean includeWithErrors) throws WebClientResponseException {
        return getDatabasesRequestCreation(includeStats, includeWithErrors);
    }

    /**
     * Get list of curated custom databases downloadable from the SIRIUS web service for local use
     * Get list of curated custom databases downloadable from the SIRIUS web service for local use.  &lt;p&gt;  [DEPRECATED] This endpoint will likely be removed or changed in future versions of this API.  [INTERNAL] This endpoint is for internal use and not intended to become part of the stable API specification at any time. This endpoint can change (or be removed) at any time, even in minor updates.
     * <p><b>200</b> - list of databases available for downloading.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * @return List&lt;DownloadableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec getDownloadableDatabasesRequestCreation() throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json", "application/problem+json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<DownloadableDatabase> localVarReturnType = new ParameterizedTypeReference<DownloadableDatabase>() {};
        return apiClient.invokeAPI("/api/databases/downloadable", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get list of curated custom databases downloadable from the SIRIUS web service for local use
     * Get list of curated custom databases downloadable from the SIRIUS web service for local use.  &lt;p&gt;  [DEPRECATED] This endpoint will likely be removed or changed in future versions of this API.  [INTERNAL] This endpoint is for internal use and not intended to become part of the stable API specification at any time. This endpoint can change (or be removed) at any time, even in minor updates.
     * <p><b>200</b> - list of databases available for downloading.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * @return List&lt;DownloadableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<DownloadableDatabase> getDownloadableDatabases() throws WebClientResponseException {
        ParameterizedTypeReference<DownloadableDatabase> localVarReturnType = new ParameterizedTypeReference<DownloadableDatabase>() {};
        return getDownloadableDatabasesRequestCreation().bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Get list of curated custom databases downloadable from the SIRIUS web service for local use
     * Get list of curated custom databases downloadable from the SIRIUS web service for local use.  &lt;p&gt;  [DEPRECATED] This endpoint will likely be removed or changed in future versions of this API.  [INTERNAL] This endpoint is for internal use and not intended to become part of the stable API specification at any time. This endpoint can change (or be removed) at any time, even in minor updates.
     * <p><b>200</b> - list of databases available for downloading.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * @return ResponseEntity&lt;List&lt;DownloadableDatabase&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<DownloadableDatabase>> getDownloadableDatabasesWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<DownloadableDatabase> localVarReturnType = new ParameterizedTypeReference<DownloadableDatabase>() {};
        return getDownloadableDatabasesRequestCreation().toEntityList(localVarReturnType).block();
    }

    /**
     * Get list of curated custom databases downloadable from the SIRIUS web service for local use
     * Get list of curated custom databases downloadable from the SIRIUS web service for local use.  &lt;p&gt;  [DEPRECATED] This endpoint will likely be removed or changed in future versions of this API.  [INTERNAL] This endpoint is for internal use and not intended to become part of the stable API specification at any time. This endpoint can change (or be removed) at any time, even in minor updates.
     * <p><b>200</b> - list of databases available for downloading.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getDownloadableDatabasesWithResponseSpec() throws WebClientResponseException {
        return getDownloadableDatabasesRequestCreation();
    }

    /**
     * List only the databases that ship with SIRIUS, such as PubChem and the bio databases.
     * List only the databases that ship with SIRIUS, such as PubChem and the bio databases. These are  read-only: they cannot be imported into, modified or removed.
     * <p><b>200</b> - all databases included in SIRIUS.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param includeStats if true, the number of structures, formulas and reference spectra is included per                      database. Slower, since the database files have to be read.
     * @return List&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getIncludedDatabasesRequestCreation(@jakarta.annotation.Nullable Boolean includeStats) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "includeStats", includeStats));

        final String[] localVarAccepts = { 
            "application/json", "application/problem+json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return apiClient.invokeAPI("/api/databases/included", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List only the databases that ship with SIRIUS, such as PubChem and the bio databases.
     * List only the databases that ship with SIRIUS, such as PubChem and the bio databases. These are  read-only: they cannot be imported into, modified or removed.
     * <p><b>200</b> - all databases included in SIRIUS.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param includeStats if true, the number of structures, formulas and reference spectra is included per                      database. Slower, since the database files have to be read.
     * @return List&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<SearchableDatabase> getIncludedDatabases(@jakarta.annotation.Nullable Boolean includeStats) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return getIncludedDatabasesRequestCreation(includeStats).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * List only the databases that ship with SIRIUS, such as PubChem and the bio databases.
     * List only the databases that ship with SIRIUS, such as PubChem and the bio databases. These are  read-only: they cannot be imported into, modified or removed.
     * <p><b>200</b> - all databases included in SIRIUS.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param includeStats if true, the number of structures, formulas and reference spectra is included per                      database. Slower, since the database files have to be read.
     * @return ResponseEntity&lt;List&lt;SearchableDatabase&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<SearchableDatabase>> getIncludedDatabasesWithHttpInfo(@jakarta.annotation.Nullable Boolean includeStats) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return getIncludedDatabasesRequestCreation(includeStats).toEntityList(localVarReturnType).block();
    }

    /**
     * List only the databases that ship with SIRIUS, such as PubChem and the bio databases.
     * List only the databases that ship with SIRIUS, such as PubChem and the bio databases. These are  read-only: they cannot be imported into, modified or removed.
     * <p><b>200</b> - all databases included in SIRIUS.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param includeStats if true, the number of structures, formulas and reference spectra is included per                      database. Slower, since the database files have to be read.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getIncludedDatabasesWithResponseSpec(@jakarta.annotation.Nullable Boolean includeStats) throws WebClientResponseException {
        return getIncludedDatabasesRequestCreation(includeStats);
    }

    /**
     * [EXPERIMENTAL] Page through the structures contained in a custom database
     * [EXPERIMENTAL] Page through the structures contained in a custom database.  &lt;p&gt;  Returns the stored structures with their name, SMILES, InChI, InChI key, molecular formula and mass.  Only custom databases are supported; the databases included in SIRIUS cannot be enumerated this way.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint  can change at any time, even in minor updates.
     * <p><b>200</b> - a page of the structures in the database.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - No custom database with the given id exists. Databases included in SIRIUS cannot be enumerated.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the custom database to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return PagedModelDatabaseStructure
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getStructuresRequestCreation(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'databaseId' is set
        if (databaseId == null) {
            throw new WebClientResponseException("Missing the required parameter 'databaseId' when calling getStructures", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("databaseId", databaseId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "sort", sort));

        final String[] localVarAccepts = { 
            "application/json", "application/problem+json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PagedModelDatabaseStructure> localVarReturnType = new ParameterizedTypeReference<PagedModelDatabaseStructure>() {};
        return apiClient.invokeAPI("/api/databases/{databaseId}/structures", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Page through the structures contained in a custom database
     * [EXPERIMENTAL] Page through the structures contained in a custom database.  &lt;p&gt;  Returns the stored structures with their name, SMILES, InChI, InChI key, molecular formula and mass.  Only custom databases are supported; the databases included in SIRIUS cannot be enumerated this way.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint  can change at any time, even in minor updates.
     * <p><b>200</b> - a page of the structures in the database.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - No custom database with the given id exists. Databases included in SIRIUS cannot be enumerated.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the custom database to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return PagedModelDatabaseStructure
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelDatabaseStructure getStructures(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelDatabaseStructure> localVarReturnType = new ParameterizedTypeReference<PagedModelDatabaseStructure>() {};
        return getStructuresRequestCreation(databaseId, page, size, sort).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Page through the structures contained in a custom database
     * [EXPERIMENTAL] Page through the structures contained in a custom database.  &lt;p&gt;  Returns the stored structures with their name, SMILES, InChI, InChI key, molecular formula and mass.  Only custom databases are supported; the databases included in SIRIUS cannot be enumerated this way.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint  can change at any time, even in minor updates.
     * <p><b>200</b> - a page of the structures in the database.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - No custom database with the given id exists. Databases included in SIRIUS cannot be enumerated.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the custom database to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return ResponseEntity&lt;PagedModelDatabaseStructure&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelDatabaseStructure> getStructuresWithHttpInfo(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelDatabaseStructure> localVarReturnType = new ParameterizedTypeReference<PagedModelDatabaseStructure>() {};
        return getStructuresRequestCreation(databaseId, page, size, sort).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Page through the structures contained in a custom database
     * [EXPERIMENTAL] Page through the structures contained in a custom database.  &lt;p&gt;  Returns the stored structures with their name, SMILES, InChI, InChI key, molecular formula and mass.  Only custom databases are supported; the databases included in SIRIUS cannot be enumerated this way.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint  can change at any time, even in minor updates.
     * <p><b>200</b> - a page of the structures in the database.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - No custom database with the given id exists. Databases included in SIRIUS cannot be enumerated.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the custom database to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getStructuresWithResponseSpec(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort) throws WebClientResponseException {
        return getStructuresRequestCreation(databaseId, page, size, sort);
    }

    /**
     * Start import of structure and spectra files into the specified database.
     * Start import of structure and spectra files into the specified database.
     * <p><b>200</b> - the affected database, including its updated statistics.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - No database with the given id exists.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the custom database to import into. Must exist.
     * @param inputFiles files to import into project
     * @param bufferSize number of compounds to keep in memory before writing them to the                                  database. Raise it to speed up large imports on machines with enough RAM.
     * @param bioTransformerParameters The bioTransformerParameters parameter
     * @return SearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec importIntoDatabaseRequestCreation(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nonnull List<File> inputFiles, @jakarta.annotation.Nullable Integer bufferSize, @jakarta.annotation.Nullable BioTransformerParameters bioTransformerParameters) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'databaseId' is set
        if (databaseId == null) {
            throw new WebClientResponseException("Missing the required parameter 'databaseId' when calling importIntoDatabase", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'inputFiles' is set
        if (inputFiles == null) {
            throw new WebClientResponseException("Missing the required parameter 'inputFiles' when calling importIntoDatabase", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        if (bioTransformerParameters != null)
            formParams.add("bioTransformerParameters", bioTransformerParameters);

        final String[] localVarAccepts = { 
            "application/json", "application/problem+json"
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
     * <p><b>200</b> - the affected database, including its updated statistics.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - No database with the given id exists.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the custom database to import into. Must exist.
     * @param inputFiles files to import into project
     * @param bufferSize number of compounds to keep in memory before writing them to the                                  database. Raise it to speed up large imports on machines with enough RAM.
     * @param bioTransformerParameters The bioTransformerParameters parameter
     * @return SearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public SearchableDatabase importIntoDatabase(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nonnull List<File> inputFiles, @jakarta.annotation.Nullable Integer bufferSize, @jakarta.annotation.Nullable BioTransformerParameters bioTransformerParameters) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return importIntoDatabaseRequestCreation(databaseId, inputFiles, bufferSize, bioTransformerParameters).bodyToMono(localVarReturnType).block();
    }

    /**
     * Start import of structure and spectra files into the specified database.
     * Start import of structure and spectra files into the specified database.
     * <p><b>200</b> - the affected database, including its updated statistics.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - No database with the given id exists.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the custom database to import into. Must exist.
     * @param inputFiles files to import into project
     * @param bufferSize number of compounds to keep in memory before writing them to the                                  database. Raise it to speed up large imports on machines with enough RAM.
     * @param bioTransformerParameters The bioTransformerParameters parameter
     * @return ResponseEntity&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<SearchableDatabase> importIntoDatabaseWithHttpInfo(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nonnull List<File> inputFiles, @jakarta.annotation.Nullable Integer bufferSize, @jakarta.annotation.Nullable BioTransformerParameters bioTransformerParameters) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return importIntoDatabaseRequestCreation(databaseId, inputFiles, bufferSize, bioTransformerParameters).toEntity(localVarReturnType).block();
    }

    /**
     * Start import of structure and spectra files into the specified database.
     * Start import of structure and spectra files into the specified database.
     * <p><b>200</b> - the affected database, including its updated statistics.
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>404</b> - No database with the given id exists.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the custom database to import into. Must exist.
     * @param inputFiles files to import into project
     * @param bufferSize number of compounds to keep in memory before writing them to the                                  database. Raise it to speed up large imports on machines with enough RAM.
     * @param bioTransformerParameters The bioTransformerParameters parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec importIntoDatabaseWithResponseSpec(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nonnull List<File> inputFiles, @jakarta.annotation.Nullable Integer bufferSize, @jakarta.annotation.Nullable BioTransformerParameters bioTransformerParameters) throws WebClientResponseException {
        return importIntoDatabaseRequestCreation(databaseId, inputFiles, bufferSize, bioTransformerParameters);
    }

    /**
     * Remove a custom database from this SIRIUS instance, and optionally delete it from disk
     * Remove a custom database from this SIRIUS instance, and optionally delete it from disk.  &lt;p&gt;  This is idempotent: removing a database that is not registered succeeds and does nothing, so a client  does not have to check first.
     * <p><b>200</b> - OK
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the database to remove.
     * @param delete if true, the database file is deleted from disk and the data is lost. If false (the                    default), only the registration is removed and the file is kept, so the database can                    be registered again later.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec removeDatabaseRequestCreation(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable Boolean delete) throws WebClientResponseException {
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
     * Remove a custom database from this SIRIUS instance, and optionally delete it from disk
     * Remove a custom database from this SIRIUS instance, and optionally delete it from disk.  &lt;p&gt;  This is idempotent: removing a database that is not registered succeeds and does nothing, so a client  does not have to check first.
     * <p><b>200</b> - OK
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the database to remove.
     * @param delete if true, the database file is deleted from disk and the data is lost. If false (the                    default), only the registration is removed and the file is kept, so the database can                    be registered again later.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void removeDatabase(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable Boolean delete) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        removeDatabaseRequestCreation(databaseId, delete).bodyToMono(localVarReturnType).block();
    }

    /**
     * Remove a custom database from this SIRIUS instance, and optionally delete it from disk
     * Remove a custom database from this SIRIUS instance, and optionally delete it from disk.  &lt;p&gt;  This is idempotent: removing a database that is not registered succeeds and does nothing, so a client  does not have to check first.
     * <p><b>200</b> - OK
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the database to remove.
     * @param delete if true, the database file is deleted from disk and the data is lost. If false (the                    default), only the registration is removed and the file is kept, so the database can                    be registered again later.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> removeDatabaseWithHttpInfo(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable Boolean delete) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return removeDatabaseRequestCreation(databaseId, delete).toEntity(localVarReturnType).block();
    }

    /**
     * Remove a custom database from this SIRIUS instance, and optionally delete it from disk
     * Remove a custom database from this SIRIUS instance, and optionally delete it from disk.  &lt;p&gt;  This is idempotent: removing a database that is not registered succeeds and does nothing, so a client  does not have to check first.
     * <p><b>200</b> - OK
     * <p><b>500</b> - Unexpected server-side error. The problem detail carries the reason.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the database to remove.
     * @param delete if true, the database file is deleted from disk and the data is lost. If false (the                    default), only the registration is removed and the file is kept, so the database can                    be registered again later.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec removeDatabaseWithResponseSpec(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable Boolean delete) throws WebClientResponseException {
        return removeDatabaseRequestCreation(databaseId, delete);
    }

    /**
     * Change the settings of an existing custom database
     * Change the settings of an existing custom database.  &lt;p&gt;  NOT IMPLEMENTED YET: changing the display name and the retention time matching flag of an existing database  is not supported so far, and every request currently fails. The request and response shape is settled  though, so a client can be written against this endpoint today: it will start succeeding in a future  version without any change on the client side.  &lt;p&gt;  Until then, create a new database with the desired settings and import into it.
     * <p><b>200</b> - the updated database.
     * <p><b>500</b> - Currently always, since updating custom databases is not implemented yet. This will become a normal server-side error once the endpoint is implemented.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the database to update.
     * @param searchableDatabaseParameters the settings to apply.
     * @return SearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec updateDatabaseRequestCreation(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable SearchableDatabaseParameters searchableDatabaseParameters) throws WebClientResponseException {
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
            "application/json", "application/problem+json"
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
     * Change the settings of an existing custom database
     * Change the settings of an existing custom database.  &lt;p&gt;  NOT IMPLEMENTED YET: changing the display name and the retention time matching flag of an existing database  is not supported so far, and every request currently fails. The request and response shape is settled  though, so a client can be written against this endpoint today: it will start succeeding in a future  version without any change on the client side.  &lt;p&gt;  Until then, create a new database with the desired settings and import into it.
     * <p><b>200</b> - the updated database.
     * <p><b>500</b> - Currently always, since updating custom databases is not implemented yet. This will become a normal server-side error once the endpoint is implemented.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the database to update.
     * @param searchableDatabaseParameters the settings to apply.
     * @return SearchableDatabase
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public SearchableDatabase updateDatabase(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable SearchableDatabaseParameters searchableDatabaseParameters) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return updateDatabaseRequestCreation(databaseId, searchableDatabaseParameters).bodyToMono(localVarReturnType).block();
    }

    /**
     * Change the settings of an existing custom database
     * Change the settings of an existing custom database.  &lt;p&gt;  NOT IMPLEMENTED YET: changing the display name and the retention time matching flag of an existing database  is not supported so far, and every request currently fails. The request and response shape is settled  though, so a client can be written against this endpoint today: it will start succeeding in a future  version without any change on the client side.  &lt;p&gt;  Until then, create a new database with the desired settings and import into it.
     * <p><b>200</b> - the updated database.
     * <p><b>500</b> - Currently always, since updating custom databases is not implemented yet. This will become a normal server-side error once the endpoint is implemented.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the database to update.
     * @param searchableDatabaseParameters the settings to apply.
     * @return ResponseEntity&lt;SearchableDatabase&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<SearchableDatabase> updateDatabaseWithHttpInfo(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable SearchableDatabaseParameters searchableDatabaseParameters) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableDatabase> localVarReturnType = new ParameterizedTypeReference<SearchableDatabase>() {};
        return updateDatabaseRequestCreation(databaseId, searchableDatabaseParameters).toEntity(localVarReturnType).block();
    }

    /**
     * Change the settings of an existing custom database
     * Change the settings of an existing custom database.  &lt;p&gt;  NOT IMPLEMENTED YET: changing the display name and the retention time matching flag of an existing database  is not supported so far, and every request currently fails. The request and response shape is settled  though, so a client can be written against this endpoint today: it will start succeeding in a future  version without any change on the client side.  &lt;p&gt;  Until then, create a new database with the desired settings and import into it.
     * <p><b>200</b> - the updated database.
     * <p><b>500</b> - Currently always, since updating custom databases is not implemented yet. This will become a normal server-side error once the endpoint is implemented.
     * <p><b>400</b> - The request body or a parameter is malformed or violates a constraint.
     * @param databaseId id of the database to update.
     * @param searchableDatabaseParameters the settings to apply.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec updateDatabaseWithResponseSpec(@jakarta.annotation.Nonnull String databaseId, @jakarta.annotation.Nullable SearchableDatabaseParameters searchableDatabaseParameters) throws WebClientResponseException {
        return updateDatabaseRequestCreation(databaseId, searchableDatabaseParameters);
    }
}
