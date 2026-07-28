package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import io.sirius.ms.sdk.model.PagedModelString;
import io.sirius.ms.sdk.model.Reaction;
import io.sirius.ms.sdk.model.ReactionRequest;
import io.sirius.ms.sdk.model.ReactionSequence;

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

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.24.0")
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
     * [EXPERIMENTAL] Add a new reaction to the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param reaction The reaction parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addReactionRequestCreation(@jakarta.annotation.Nonnull Reaction reaction) throws WebClientResponseException {
        Object postBody = reaction;
        // verify the required parameter 'reaction' is set
        if (reaction == null) {
            throw new WebClientResponseException("Missing the required parameter 'reaction' when calling addReaction", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/api/reactions/library", HttpMethod.POST, pathParams, localVarQueryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Add a new reaction to the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param reaction The reaction parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void addReaction(@jakarta.annotation.Nonnull Reaction reaction) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        addReactionRequestCreation(reaction).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Add a new reaction to the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param reaction The reaction parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> addReactionWithHttpInfo(@jakarta.annotation.Nonnull Reaction reaction) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return addReactionRequestCreation(reaction).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Add a new reaction to the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param reaction The reaction parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addReactionWithResponseSpec(@jakarta.annotation.Nonnull Reaction reaction) throws WebClientResponseException {
        return addReactionRequestCreation(reaction);
    }

    /**
     * [EXPERIMENTAL] Add a new reaction sequence to the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param reactionSequence The reactionSequence parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addSequenceRequestCreation(@jakarta.annotation.Nonnull ReactionSequence reactionSequence) throws WebClientResponseException {
        Object postBody = reactionSequence;
        // verify the required parameter 'reactionSequence' is set
        if (reactionSequence == null) {
            throw new WebClientResponseException("Missing the required parameter 'reactionSequence' when calling addSequence", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/api/reactions/sequences/library", HttpMethod.POST, pathParams, localVarQueryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Add a new reaction sequence to the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param reactionSequence The reactionSequence parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void addSequence(@jakarta.annotation.Nonnull ReactionSequence reactionSequence) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        addSequenceRequestCreation(reactionSequence).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Add a new reaction sequence to the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param reactionSequence The reactionSequence parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> addSequenceWithHttpInfo(@jakarta.annotation.Nonnull ReactionSequence reactionSequence) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return addSequenceRequestCreation(reactionSequence).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Add a new reaction sequence to the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param reactionSequence The reactionSequence parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addSequenceWithResponseSpec(@jakarta.annotation.Nonnull ReactionSequence reactionSequence) throws WebClientResponseException {
        return addSequenceRequestCreation(reactionSequence);
    }

    /**
     * [EXPERIMENTAL] Apply a sequence of reactions to a list of SMILES strings or structures from a database.
     * [EXPERIMENTAL] Returns the final pool of SMILES strings. [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
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

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "limit", limit));

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
        return apiClient.invokeAPI("/api/reactions", HttpMethod.POST, pathParams, localVarQueryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Apply a sequence of reactions to a list of SMILES strings or structures from a database.
     * [EXPERIMENTAL] Returns the final pool of SMILES strings. [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
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
     * [EXPERIMENTAL] Apply a sequence of reactions to a list of SMILES strings or structures from a database.
     * [EXPERIMENTAL] Returns the final pool of SMILES strings. [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
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
     * [EXPERIMENTAL] Apply a sequence of reactions to a list of SMILES strings or structures from a database.
     * [EXPERIMENTAL] Returns the final pool of SMILES strings. [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param reactionRequest The reactionRequest parameter
     * @param limit The limit parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec applyReactionsWithResponseSpec(@jakarta.annotation.Nonnull ReactionRequest reactionRequest, @jakarta.annotation.Nullable Integer limit) throws WebClientResponseException {
        return applyReactionsRequestCreation(reactionRequest, limit);
    }

    /**
     * [EXPERIMENTAL] Delete a reaction from the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param name The name parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteReactionRequestCreation(@jakarta.annotation.Nonnull String name) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new WebClientResponseException("Missing the required parameter 'name' when calling deleteReaction", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("name", name);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/api/reactions/library/{name}", HttpMethod.DELETE, pathParams, localVarQueryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Delete a reaction from the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param name The name parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteReaction(@jakarta.annotation.Nonnull String name) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteReactionRequestCreation(name).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Delete a reaction from the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param name The name parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteReactionWithHttpInfo(@jakarta.annotation.Nonnull String name) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteReactionRequestCreation(name).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Delete a reaction from the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param name The name parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteReactionWithResponseSpec(@jakarta.annotation.Nonnull String name) throws WebClientResponseException {
        return deleteReactionRequestCreation(name);
    }

    /**
     * [EXPERIMENTAL] Delete a reaction sequence from the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param name The name parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteSequenceRequestCreation(@jakarta.annotation.Nonnull String name) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new WebClientResponseException("Missing the required parameter 'name' when calling deleteSequence", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("name", name);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/api/reactions/sequences/library/{name}", HttpMethod.DELETE, pathParams, localVarQueryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Delete a reaction sequence from the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param name The name parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteSequence(@jakarta.annotation.Nonnull String name) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteSequenceRequestCreation(name).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Delete a reaction sequence from the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param name The name parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteSequenceWithHttpInfo(@jakarta.annotation.Nonnull String name) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteSequenceRequestCreation(name).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Delete a reaction sequence from the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param name The name parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteSequenceWithResponseSpec(@jakarta.annotation.Nonnull String name) throws WebClientResponseException {
        return deleteSequenceRequestCreation(name);
    }

    /**
     * [EXPERIMENTAL] Get a specific reaction from the library by name.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param name The name parameter
     * @return Reaction
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getReactionRequestCreation(@jakarta.annotation.Nonnull String name) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new WebClientResponseException("Missing the required parameter 'name' when calling getReaction", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("name", name);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
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

        ParameterizedTypeReference<Reaction> localVarReturnType = new ParameterizedTypeReference<Reaction>() {};
        return apiClient.invokeAPI("/api/reactions/library/{name}", HttpMethod.GET, pathParams, localVarQueryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Get a specific reaction from the library by name.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param name The name parameter
     * @return Reaction
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Reaction getReaction(@jakarta.annotation.Nonnull String name) throws WebClientResponseException {
        ParameterizedTypeReference<Reaction> localVarReturnType = new ParameterizedTypeReference<Reaction>() {};
        return getReactionRequestCreation(name).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get a specific reaction from the library by name.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param name The name parameter
     * @return ResponseEntity&lt;Reaction&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Reaction> getReactionWithHttpInfo(@jakarta.annotation.Nonnull String name) throws WebClientResponseException {
        ParameterizedTypeReference<Reaction> localVarReturnType = new ParameterizedTypeReference<Reaction>() {};
        return getReactionRequestCreation(name).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get a specific reaction from the library by name.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param name The name parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getReactionWithResponseSpec(@jakarta.annotation.Nonnull String name) throws WebClientResponseException {
        return getReactionRequestCreation(name);
    }

    /**
     * [EXPERIMENTAL] Get all reactions from the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @return List&lt;Reaction&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getReactionsRequestCreation() throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
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

        ParameterizedTypeReference<Reaction> localVarReturnType = new ParameterizedTypeReference<Reaction>() {};
        return apiClient.invokeAPI("/api/reactions", HttpMethod.GET, pathParams, localVarQueryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Get all reactions from the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @return List&lt;Reaction&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Reaction> getReactions() throws WebClientResponseException {
        ParameterizedTypeReference<Reaction> localVarReturnType = new ParameterizedTypeReference<Reaction>() {};
        return getReactionsRequestCreation().bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [EXPERIMENTAL] Get all reactions from the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @return ResponseEntity&lt;List&lt;Reaction&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Reaction>> getReactionsWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<Reaction> localVarReturnType = new ParameterizedTypeReference<Reaction>() {};
        return getReactionsRequestCreation().toEntityList(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get all reactions from the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getReactionsWithResponseSpec() throws WebClientResponseException {
        return getReactionsRequestCreation();
    }

    /**
     * [EXPERIMENTAL] Get a specific reaction sequence from the library by name.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param name The name parameter
     * @return ReactionSequence
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getSequenceRequestCreation(@jakarta.annotation.Nonnull String name) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new WebClientResponseException("Missing the required parameter 'name' when calling getSequence", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("name", name);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
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

        ParameterizedTypeReference<ReactionSequence> localVarReturnType = new ParameterizedTypeReference<ReactionSequence>() {};
        return apiClient.invokeAPI("/api/reactions/sequences/{name}", HttpMethod.GET, pathParams, localVarQueryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Get a specific reaction sequence from the library by name.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param name The name parameter
     * @return ReactionSequence
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ReactionSequence getSequence(@jakarta.annotation.Nonnull String name) throws WebClientResponseException {
        ParameterizedTypeReference<ReactionSequence> localVarReturnType = new ParameterizedTypeReference<ReactionSequence>() {};
        return getSequenceRequestCreation(name).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get a specific reaction sequence from the library by name.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param name The name parameter
     * @return ResponseEntity&lt;ReactionSequence&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ReactionSequence> getSequenceWithHttpInfo(@jakarta.annotation.Nonnull String name) throws WebClientResponseException {
        ParameterizedTypeReference<ReactionSequence> localVarReturnType = new ParameterizedTypeReference<ReactionSequence>() {};
        return getSequenceRequestCreation(name).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get a specific reaction sequence from the library by name.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param name The name parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getSequenceWithResponseSpec(@jakarta.annotation.Nonnull String name) throws WebClientResponseException {
        return getSequenceRequestCreation(name);
    }

    /**
     * [EXPERIMENTAL] Get all reaction sequences from the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @return List&lt;ReactionSequence&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getSequencesRequestCreation() throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
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

        ParameterizedTypeReference<ReactionSequence> localVarReturnType = new ParameterizedTypeReference<ReactionSequence>() {};
        return apiClient.invokeAPI("/api/reactions/sequences", HttpMethod.GET, pathParams, localVarQueryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Get all reaction sequences from the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @return List&lt;ReactionSequence&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<ReactionSequence> getSequences() throws WebClientResponseException {
        ParameterizedTypeReference<ReactionSequence> localVarReturnType = new ParameterizedTypeReference<ReactionSequence>() {};
        return getSequencesRequestCreation().bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [EXPERIMENTAL] Get all reaction sequences from the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @return ResponseEntity&lt;List&lt;ReactionSequence&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<ReactionSequence>> getSequencesWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<ReactionSequence> localVarReturnType = new ParameterizedTypeReference<ReactionSequence>() {};
        return getSequencesRequestCreation().toEntityList(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get all reaction sequences from the library.
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getSequencesWithResponseSpec() throws WebClientResponseException {
        return getSequencesRequestCreation();
    }
}
