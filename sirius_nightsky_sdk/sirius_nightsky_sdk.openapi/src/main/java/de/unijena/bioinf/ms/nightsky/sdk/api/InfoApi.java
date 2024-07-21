package de.unijena.bioinf.ms.nightsky.sdk.api;

import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;

import de.unijena.bioinf.ms.nightsky.sdk.model.ConnectionCheck;
import de.unijena.bioinf.ms.nightsky.sdk.model.Info;

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
public class InfoApi {
    private ApiClient apiClient;

    public InfoApi() {
        this(new ApiClient());
    }

    @Autowired
    public InfoApi(ApiClient apiClient) {
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
     * @return ConnectionCheck
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getConnectionCheckRequestCreation() throws WebClientResponseException {
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

        ParameterizedTypeReference<ConnectionCheck> localVarReturnType = new ParameterizedTypeReference<ConnectionCheck>() {};
        return apiClient.invokeAPI("/api/connection-status", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @return ConnectionCheck
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ConnectionCheck getConnectionCheck() throws WebClientResponseException {
        ParameterizedTypeReference<ConnectionCheck> localVarReturnType = new ParameterizedTypeReference<ConnectionCheck>() {};
        return getConnectionCheckRequestCreation().bodyToMono(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @return ResponseEntity&lt;ConnectionCheck&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ConnectionCheck> getConnectionCheckWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<ConnectionCheck> localVarReturnType = new ParameterizedTypeReference<ConnectionCheck>() {};
        return getConnectionCheckRequestCreation().toEntity(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getConnectionCheckWithResponseSpec() throws WebClientResponseException {
        return getConnectionCheckRequestCreation();
    }
    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param serverInfo The serverInfo parameter
     * @param updateInfo The updateInfo parameter
     * @return Info
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getInfoRequestCreation(Boolean serverInfo, Boolean updateInfo) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "serverInfo", serverInfo));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "updateInfo", updateInfo));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Info> localVarReturnType = new ParameterizedTypeReference<Info>() {};
        return apiClient.invokeAPI("/api/info", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param serverInfo The serverInfo parameter
     * @param updateInfo The updateInfo parameter
     * @return Info
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Info getInfo(Boolean serverInfo, Boolean updateInfo) throws WebClientResponseException {
        ParameterizedTypeReference<Info> localVarReturnType = new ParameterizedTypeReference<Info>() {};
        return getInfoRequestCreation(serverInfo, updateInfo).bodyToMono(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param serverInfo The serverInfo parameter
     * @param updateInfo The updateInfo parameter
     * @return ResponseEntity&lt;Info&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Info> getInfoWithHttpInfo(Boolean serverInfo, Boolean updateInfo) throws WebClientResponseException {
        ParameterizedTypeReference<Info> localVarReturnType = new ParameterizedTypeReference<Info>() {};
        return getInfoRequestCreation(serverInfo, updateInfo).toEntity(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param serverInfo The serverInfo parameter
     * @param updateInfo The updateInfo parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getInfoWithResponseSpec(Boolean serverInfo, Boolean updateInfo) throws WebClientResponseException {
        return getInfoRequestCreation(serverInfo, updateInfo);
    }
}
