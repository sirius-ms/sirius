package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import io.sirius.ms.sdk.model.LibraryInfo;

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
public class LibrariesApi {
    private ApiClient apiClient;

    public LibrariesApi() {
        this(new ApiClient());
    }

    @Autowired
    public LibrariesApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Get SIRIUS libraries.
     * Get SIRIUS libraries.
     * <p><b>200</b> - list of libraries available for downloading.
     * @return List&lt;LibraryInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getLibrariesRequestCreation() throws WebClientResponseException {
        Object postBody = null;
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
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<LibraryInfo> localVarReturnType = new ParameterizedTypeReference<LibraryInfo>() {};
        return apiClient.invokeAPI("/api/libraries", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get SIRIUS libraries.
     * Get SIRIUS libraries.
     * <p><b>200</b> - list of libraries available for downloading.
     * @return List&lt;LibraryInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<LibraryInfo> getLibraries() throws WebClientResponseException {
        ParameterizedTypeReference<LibraryInfo> localVarReturnType = new ParameterizedTypeReference<LibraryInfo>() {};
        return getLibrariesRequestCreation().bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Get SIRIUS libraries.
     * Get SIRIUS libraries.
     * <p><b>200</b> - list of libraries available for downloading.
     * @return ResponseEntity&lt;List&lt;LibraryInfo&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<LibraryInfo>> getLibrariesWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<LibraryInfo> localVarReturnType = new ParameterizedTypeReference<LibraryInfo>() {};
        return getLibrariesRequestCreation().toEntityList(localVarReturnType).block();
    }

    /**
     * Get SIRIUS libraries.
     * Get SIRIUS libraries.
     * <p><b>200</b> - list of libraries available for downloading.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getLibrariesWithResponseSpec() throws WebClientResponseException {
        return getLibrariesRequestCreation();
    }
}
