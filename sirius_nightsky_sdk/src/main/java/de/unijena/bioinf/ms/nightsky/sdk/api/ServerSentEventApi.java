/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.nightsky.sdk.api;

import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ServerSentEventApi {
    private ApiClient apiClient;

    public ServerSentEventApi() {
        this(new ApiClient());
    }

    public ServerSentEventApi(ApiClient apiClient) {
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
     * @return ServerSentEvent&ltString&gt
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
     * @return ServerSentEvent&lt;String&gt;
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
     * @return ResponseEntity&lt;ServerSentEvent&lt;String&gt;&gt;
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
