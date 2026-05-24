package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import io.sirius.ms.sdk.model.PagedModelString;
import io.sirius.ms.sdk.model.ReactionRequest;

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

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.14.0")
public class ReactionsApi {
    private ApiClient apiClient;

    public ReactionsApi() {
        this(new ApiClient());
    }

    public ReactionsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    
    /**
     * Apply a sequence of reactions to a list of SMILES strings or structures from a database.
     * [EXPERIMENTAL] Returns the final pool of SMILES strings.
     * <p><b>200</b> - OK
     * @param reactionRequest The reactionRequest parameter
     * @param limit The limit parameter
     * @return PagedModelString
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec applyReactionsRequestCreation(@jakarta.annotation.Nonnull ReactionRequest reactionRequest, @jakarta.annotation.Nullable Integer limit) throws WebClientResponseException {
        Object postBody = reactionRequest;
        // verify the required parameter 'reactionRequest' is set
        if (reactionRequest == null) {
            throw new WebClientResponseException("Missing the required parameter 'reactionRequest' when calling applyReactions", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "limit", limit));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PagedModelString> localVarReturnType = new ParameterizedTypeReference<PagedModelString>() {};
        return apiClient.invokeAPI("/api/reactions", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Apply a sequence of reactions to a list of SMILES strings or structures from a database.
     * [EXPERIMENTAL] Returns the final pool of SMILES strings.
     * <p><b>200</b> - OK
     * @param reactionRequest The reactionRequest parameter
     * @param limit The limit parameter
     * @return PagedModelString
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelString applyReactions(@jakarta.annotation.Nonnull ReactionRequest reactionRequest, @jakarta.annotation.Nullable Integer limit) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelString> localVarReturnType = new ParameterizedTypeReference<PagedModelString>() {};
        return applyReactionsRequestCreation(reactionRequest, limit).bodyToMono(localVarReturnType).block();
    }

    /**
     * Apply a sequence of reactions to a list of SMILES strings or structures from a database.
     * [EXPERIMENTAL] Returns the final pool of SMILES strings.
     * <p><b>200</b> - OK
     * @param reactionRequest The reactionRequest parameter
     * @param limit The limit parameter
     * @return ResponseEntity&lt;PagedModelString&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelString> applyReactionsWithHttpInfo(@jakarta.annotation.Nonnull ReactionRequest reactionRequest, @jakarta.annotation.Nullable Integer limit) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelString> localVarReturnType = new ParameterizedTypeReference<PagedModelString>() {};
        return applyReactionsRequestCreation(reactionRequest, limit).toEntity(localVarReturnType).block();
    }

    /**
     * Apply a sequence of reactions to a list of SMILES strings or structures from a database.
     * [EXPERIMENTAL] Returns the final pool of SMILES strings.
     * <p><b>200</b> - OK
     * @param reactionRequest The reactionRequest parameter
     * @param limit The limit parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec applyReactionsWithResponseSpec(@jakarta.annotation.Nonnull ReactionRequest reactionRequest, @jakarta.annotation.Nullable Integer limit) throws WebClientResponseException {
        return applyReactionsRequestCreation(reactionRequest, limit);
    }
}
