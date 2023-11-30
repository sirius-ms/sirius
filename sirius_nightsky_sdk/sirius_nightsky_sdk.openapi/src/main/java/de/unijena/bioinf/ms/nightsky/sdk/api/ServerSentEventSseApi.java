package de.unijena.bioinf.ms.nightsky.sdk.api;

import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;


import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
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
public class ServerSentEventSseApi {
    private ApiClient apiClient;

    public ServerSentEventSseApi() {
        this(new ApiClient());
    }

    @Autowired
    public ServerSentEventSseApi(ApiClient apiClient) {
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
     * @param eventsToListenOn The eventsToListenOn parameter
     * @return ServerSentEvent<String>
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec listenToEventsRequestCreation(List<String> eventsToListenOn) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "eventsToListenOn", eventsToListenOn));

        final String[] localVarAccepts = { 
            "text/event-stream"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<ServerSentEvent<String>> localVarReturnType = new ParameterizedTypeReference<ServerSentEvent<String>>() {};
        return apiClient.invokeAPI("/sse", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param eventsToListenOn The eventsToListenOn parameter
     * @return ServerSentEvent<String>
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ServerSentEvent<String> listenToEvents(List<String> eventsToListenOn) throws WebClientResponseException {
        ParameterizedTypeReference<ServerSentEvent<String>> localVarReturnType = new ParameterizedTypeReference<ServerSentEvent<String>>() {};
        return listenToEventsRequestCreation(eventsToListenOn).bodyToMono(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param eventsToListenOn The eventsToListenOn parameter
     * @return ResponseEntity&lt;ServerSentEvent<String>&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ServerSentEvent<String>> listenToEventsWithHttpInfo(List<String> eventsToListenOn) throws WebClientResponseException {
        ParameterizedTypeReference<ServerSentEvent<String>> localVarReturnType = new ParameterizedTypeReference<ServerSentEvent<String>>() {};
        return listenToEventsRequestCreation(eventsToListenOn).toEntity(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param eventsToListenOn The eventsToListenOn parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec listenToEventsWithResponseSpec(List<String> eventsToListenOn) throws WebClientResponseException {
        return listenToEventsRequestCreation(eventsToListenOn);
    }
}
