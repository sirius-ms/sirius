package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import io.sirius.ms.sdk.model.AlignedFeatureOptField;
import io.sirius.ms.sdk.model.Compound;
import io.sirius.ms.sdk.model.CompoundImport;
import io.sirius.ms.sdk.model.CompoundOptField;
import io.sirius.ms.sdk.model.InstrumentProfile;
import io.sirius.ms.sdk.model.PagedModelCompound;
import io.sirius.ms.sdk.model.QuantMeasure;
import io.sirius.ms.sdk.model.QuantTable;
import io.sirius.ms.sdk.model.Tag;
import io.sirius.ms.sdk.model.TagSubmission;
import io.sirius.ms.sdk.model.TraceSetExperimental;

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
public class CompoundsApi {
    private ApiClient apiClient;

    public CompoundsApi() {
        this(new ApiClient());
    }

    public CompoundsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Import Compounds and its contained features.
     * Import Compounds and its contained features. Compounds and Features must not exist in the project.  Otherwise, they will exist twice.
     * <p><b>200</b> - the Compounds that have been imported with specified optional fields
     * @param projectId project-space to import into.
     * @param compoundImport the compound data to be imported
     * @param profile profile describing the instrument used to measure the data. Used to merge spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; to override defaults.
     * @param optFieldsFeatures set of optional fields of the nested features to be included. Use &#39;none&#39; to override defaults.
     * @return List&lt;Compound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addCompoundsRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<CompoundImport> compoundImport, @jakarta.annotation.Nullable InstrumentProfile profile, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        Object postBody = compoundImport;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addCompounds", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundImport' is set
        if (compoundImport == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundImport' when calling addCompounds", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "profile", profile));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFieldsFeatures", optFieldsFeatures));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Import Compounds and its contained features.
     * Import Compounds and its contained features. Compounds and Features must not exist in the project.  Otherwise, they will exist twice.
     * <p><b>200</b> - the Compounds that have been imported with specified optional fields
     * @param projectId project-space to import into.
     * @param compoundImport the compound data to be imported
     * @param profile profile describing the instrument used to measure the data. Used to merge spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; to override defaults.
     * @param optFieldsFeatures set of optional fields of the nested features to be included. Use &#39;none&#39; to override defaults.
     * @return List&lt;Compound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Compound> addCompounds(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<CompoundImport> compoundImport, @jakarta.annotation.Nullable InstrumentProfile profile, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return addCompoundsRequestCreation(projectId, compoundImport, profile, optFields, optFieldsFeatures).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Import Compounds and its contained features.
     * Import Compounds and its contained features. Compounds and Features must not exist in the project.  Otherwise, they will exist twice.
     * <p><b>200</b> - the Compounds that have been imported with specified optional fields
     * @param projectId project-space to import into.
     * @param compoundImport the compound data to be imported
     * @param profile profile describing the instrument used to measure the data. Used to merge spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; to override defaults.
     * @param optFieldsFeatures set of optional fields of the nested features to be included. Use &#39;none&#39; to override defaults.
     * @return ResponseEntity&lt;List&lt;Compound&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Compound>> addCompoundsWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<CompoundImport> compoundImport, @jakarta.annotation.Nullable InstrumentProfile profile, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return addCompoundsRequestCreation(projectId, compoundImport, profile, optFields, optFieldsFeatures).toEntityList(localVarReturnType).block();
    }

    /**
     * Import Compounds and its contained features.
     * Import Compounds and its contained features. Compounds and Features must not exist in the project.  Otherwise, they will exist twice.
     * <p><b>200</b> - the Compounds that have been imported with specified optional fields
     * @param projectId project-space to import into.
     * @param compoundImport the compound data to be imported
     * @param profile profile describing the instrument used to measure the data. Used to merge spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; to override defaults.
     * @param optFieldsFeatures set of optional fields of the nested features to be included. Use &#39;none&#39; to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addCompoundsWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<CompoundImport> compoundImport, @jakarta.annotation.Nullable InstrumentProfile profile, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        return addCompoundsRequestCreation(projectId, compoundImport, profile, optFields, optFieldsFeatures);
    }

    /**
     * [EXPERIMENTAL] Tags with the same name will be overwritten
     * [EXPERIMENTAL] Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param compoundId compound (group of ion identities) to add tags to.
     * @param tag tags to add.
     * @return List&lt;Tag&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addTagsToCompoundExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nonnull List<Tag> tag) throws WebClientResponseException {
        Object postBody = tag;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addTagsToCompoundExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling addTagsToCompoundExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tag' is set
        if (tag == null) {
            throw new WebClientResponseException("Missing the required parameter 'tag' when calling addTagsToCompoundExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("compoundId", compoundId);

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

        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/tags/{compoundId}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Tags with the same name will be overwritten
     * [EXPERIMENTAL] Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param compoundId compound (group of ion identities) to add tags to.
     * @param tag tags to add.
     * @return List&lt;Tag&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Tag> addTagsToCompoundExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nonnull List<Tag> tag) throws WebClientResponseException {
        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return addTagsToCompoundExperimentalRequestCreation(projectId, compoundId, tag).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [EXPERIMENTAL] Tags with the same name will be overwritten
     * [EXPERIMENTAL] Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param compoundId compound (group of ion identities) to add tags to.
     * @param tag tags to add.
     * @return ResponseEntity&lt;List&lt;Tag&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Tag>> addTagsToCompoundExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nonnull List<Tag> tag) throws WebClientResponseException {
        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return addTagsToCompoundExperimentalRequestCreation(projectId, compoundId, tag).toEntityList(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Tags with the same name will be overwritten
     * [EXPERIMENTAL] Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param compoundId compound (group of ion identities) to add tags to.
     * @param tag tags to add.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addTagsToCompoundExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nonnull List<Tag> tag) throws WebClientResponseException {
        return addTagsToCompoundExperimentalRequestCreation(projectId, compoundId, tag);
    }

    /**
     * [EXPERIMENTAL] Add tags to a compound (group of ion identities) in the project
     * [EXPERIMENTAL] Add tags to a compound (group of ion identities) in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to add to.
     * @param tagSubmission tags with the id of compound (group of ion identities) they shall be added to.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addTagsToCompoundsExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<TagSubmission> tagSubmission) throws WebClientResponseException {
        Object postBody = tagSubmission;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addTagsToCompoundsExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tagSubmission' is set
        if (tagSubmission == null) {
            throw new WebClientResponseException("Missing the required parameter 'tagSubmission' when calling addTagsToCompoundsExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
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
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/tags", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Add tags to a compound (group of ion identities) in the project
     * [EXPERIMENTAL] Add tags to a compound (group of ion identities) in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to add to.
     * @param tagSubmission tags with the id of compound (group of ion identities) they shall be added to.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void addTagsToCompoundsExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<TagSubmission> tagSubmission) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        addTagsToCompoundsExperimentalRequestCreation(projectId, tagSubmission).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Add tags to a compound (group of ion identities) in the project
     * [EXPERIMENTAL] Add tags to a compound (group of ion identities) in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to add to.
     * @param tagSubmission tags with the id of compound (group of ion identities) they shall be added to.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> addTagsToCompoundsExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<TagSubmission> tagSubmission) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return addTagsToCompoundsExperimentalRequestCreation(projectId, tagSubmission).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Add tags to a compound (group of ion identities) in the project
     * [EXPERIMENTAL] Add tags to a compound (group of ion identities) in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to add to.
     * @param tagSubmission tags with the id of compound (group of ion identities) they shall be added to.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addTagsToCompoundsExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<TagSubmission> tagSubmission) throws WebClientResponseException {
        return addTagsToCompoundsExperimentalRequestCreation(projectId, tagSubmission);
    }

    /**
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId identifier of the compound to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteCompoundRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteCompound", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling deleteCompound", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("compoundId", compoundId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/{compoundId}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId identifier of the compound to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteCompound(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteCompoundRequestCreation(projectId, compoundId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId identifier of the compound to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteCompoundWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteCompoundRequestCreation(projectId, compoundId).toEntity(localVarReturnType).block();
    }

    /**
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId identifier of the compound to delete.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteCompoundWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId) throws WebClientResponseException {
        return deleteCompoundRequestCreation(projectId, compoundId);
    }

    /**
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param compoundId identifier of the compound (group of ion identities) to access.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return Compound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompound", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling getCompound", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("compoundId", compoundId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "msDataSearchPrepared", msDataSearchPrepared));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFieldsFeatures", optFieldsFeatures));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/{compoundId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param compoundId identifier of the compound (group of ion identities) to access.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return Compound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Compound getCompound(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return getCompoundRequestCreation(projectId, compoundId, msDataSearchPrepared, optFields, optFieldsFeatures).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param compoundId identifier of the compound (group of ion identities) to access.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseEntity&lt;Compound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Compound> getCompoundWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return getCompoundRequestCreation(projectId, compoundId, msDataSearchPrepared, optFields, optFieldsFeatures).toEntity(localVarReturnType).block();
    }

    /**
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param compoundId identifier of the compound (group of ion identities) to access.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        return getCompoundRequestCreation(projectId, compoundId, msDataSearchPrepared, optFields, optFieldsFeatures);
    }

    /**
     * Returns the full quantification table of compounds
     * Returns the full quantification table of compounds.  &lt;p&gt;  The quantification table contains the quantities of the compounds within all runs they are contained in.  Rows refer to compounds, columns to runs, both given as ids and names.  &lt;p&gt;  Compounds are not indexed yet, so the optional search query may only refer to the compound id, e.g.  &lt;code&gt;compoundId:1 OR compoundId:2&lt;/code&gt; or &lt;code&gt;NOT compoundId:3&lt;/code&gt;. Such a query is answered with the same  semantics the search index would apply. Any query referring to other fields is rejected. Omit the query to  quantify all compounds.
     * <p><b>200</b> - Quant table of the compounds of this project
     * @param projectId project-space to read from.
     * @param searchQuery Optional query in lucene syntax selecting compounds by id. Omit this parameter to quantify all compounds.
     * @param type quantification type.
     * @return QuantTable
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundQuantTableRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable QuantMeasure type) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompoundQuantTable", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "searchQuery", searchQuery));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "type", type));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<QuantTable> localVarReturnType = new ParameterizedTypeReference<QuantTable>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/quant-table", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Returns the full quantification table of compounds
     * Returns the full quantification table of compounds.  &lt;p&gt;  The quantification table contains the quantities of the compounds within all runs they are contained in.  Rows refer to compounds, columns to runs, both given as ids and names.  &lt;p&gt;  Compounds are not indexed yet, so the optional search query may only refer to the compound id, e.g.  &lt;code&gt;compoundId:1 OR compoundId:2&lt;/code&gt; or &lt;code&gt;NOT compoundId:3&lt;/code&gt;. Such a query is answered with the same  semantics the search index would apply. Any query referring to other fields is rejected. Omit the query to  quantify all compounds.
     * <p><b>200</b> - Quant table of the compounds of this project
     * @param projectId project-space to read from.
     * @param searchQuery Optional query in lucene syntax selecting compounds by id. Omit this parameter to quantify all compounds.
     * @param type quantification type.
     * @return QuantTable
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public QuantTable getCompoundQuantTable(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable QuantMeasure type) throws WebClientResponseException {
        ParameterizedTypeReference<QuantTable> localVarReturnType = new ParameterizedTypeReference<QuantTable>() {};
        return getCompoundQuantTableRequestCreation(projectId, searchQuery, type).bodyToMono(localVarReturnType).block();
    }

    /**
     * Returns the full quantification table of compounds
     * Returns the full quantification table of compounds.  &lt;p&gt;  The quantification table contains the quantities of the compounds within all runs they are contained in.  Rows refer to compounds, columns to runs, both given as ids and names.  &lt;p&gt;  Compounds are not indexed yet, so the optional search query may only refer to the compound id, e.g.  &lt;code&gt;compoundId:1 OR compoundId:2&lt;/code&gt; or &lt;code&gt;NOT compoundId:3&lt;/code&gt;. Such a query is answered with the same  semantics the search index would apply. Any query referring to other fields is rejected. Omit the query to  quantify all compounds.
     * <p><b>200</b> - Quant table of the compounds of this project
     * @param projectId project-space to read from.
     * @param searchQuery Optional query in lucene syntax selecting compounds by id. Omit this parameter to quantify all compounds.
     * @param type quantification type.
     * @return ResponseEntity&lt;QuantTable&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<QuantTable> getCompoundQuantTableWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable QuantMeasure type) throws WebClientResponseException {
        ParameterizedTypeReference<QuantTable> localVarReturnType = new ParameterizedTypeReference<QuantTable>() {};
        return getCompoundQuantTableRequestCreation(projectId, searchQuery, type).toEntity(localVarReturnType).block();
    }

    /**
     * Returns the full quantification table of compounds
     * Returns the full quantification table of compounds.  &lt;p&gt;  The quantification table contains the quantities of the compounds within all runs they are contained in.  Rows refer to compounds, columns to runs, both given as ids and names.  &lt;p&gt;  Compounds are not indexed yet, so the optional search query may only refer to the compound id, e.g.  &lt;code&gt;compoundId:1 OR compoundId:2&lt;/code&gt; or &lt;code&gt;NOT compoundId:3&lt;/code&gt;. Such a query is answered with the same  semantics the search index would apply. Any query referring to other fields is rejected. Omit the query to  quantify all compounds.
     * <p><b>200</b> - Quant table of the compounds of this project
     * @param projectId project-space to read from.
     * @param searchQuery Optional query in lucene syntax selecting compounds by id. Omit this parameter to quantify all compounds.
     * @param type quantification type.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundQuantTableWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable QuantMeasure type) throws WebClientResponseException {
        return getCompoundQuantTableRequestCreation(projectId, searchQuery, type);
    }

    /**
     * [INTERNAL] Returns a quantification table that contains the given compound as its only row
     * [INTERNAL] Returns a quantification table that contains the given compound as its only row.  &lt;p&gt;  The columns of the row refer to the runs the compound has been quantified in, given as run ids and run names.  &lt;p&gt;  [INTERNAL] This endpoint is for internal use and not intended to become part of the stable API specification at any time. This endpoint can change (or be removed) at any time, even in minor updates.  [DEPRECATED] Will be replaced by the quantification table endpoint with filter query support, which allows  to request the quantification of an arbitrary subset of the project.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param compoundId compound which should be read out
     * @param type quantification type.
     * @return QuantTable
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec getCompoundQuantTableRowRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nullable QuantMeasure type) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompoundQuantTableRow", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling getCompoundQuantTableRow", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("compoundId", compoundId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "type", type));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<QuantTable> localVarReturnType = new ParameterizedTypeReference<QuantTable>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/{compoundId}/quant-table-row", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [INTERNAL] Returns a quantification table that contains the given compound as its only row
     * [INTERNAL] Returns a quantification table that contains the given compound as its only row.  &lt;p&gt;  The columns of the row refer to the runs the compound has been quantified in, given as run ids and run names.  &lt;p&gt;  [INTERNAL] This endpoint is for internal use and not intended to become part of the stable API specification at any time. This endpoint can change (or be removed) at any time, even in minor updates.  [DEPRECATED] Will be replaced by the quantification table endpoint with filter query support, which allows  to request the quantification of an arbitrary subset of the project.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param compoundId compound which should be read out
     * @param type quantification type.
     * @return QuantTable
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public QuantTable getCompoundQuantTableRow(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nullable QuantMeasure type) throws WebClientResponseException {
        ParameterizedTypeReference<QuantTable> localVarReturnType = new ParameterizedTypeReference<QuantTable>() {};
        return getCompoundQuantTableRowRequestCreation(projectId, compoundId, type).bodyToMono(localVarReturnType).block();
    }

    /**
     * [INTERNAL] Returns a quantification table that contains the given compound as its only row
     * [INTERNAL] Returns a quantification table that contains the given compound as its only row.  &lt;p&gt;  The columns of the row refer to the runs the compound has been quantified in, given as run ids and run names.  &lt;p&gt;  [INTERNAL] This endpoint is for internal use and not intended to become part of the stable API specification at any time. This endpoint can change (or be removed) at any time, even in minor updates.  [DEPRECATED] Will be replaced by the quantification table endpoint with filter query support, which allows  to request the quantification of an arbitrary subset of the project.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param compoundId compound which should be read out
     * @param type quantification type.
     * @return ResponseEntity&lt;QuantTable&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<QuantTable> getCompoundQuantTableRowWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nullable QuantMeasure type) throws WebClientResponseException {
        ParameterizedTypeReference<QuantTable> localVarReturnType = new ParameterizedTypeReference<QuantTable>() {};
        return getCompoundQuantTableRowRequestCreation(projectId, compoundId, type).toEntity(localVarReturnType).block();
    }

    /**
     * [INTERNAL] Returns a quantification table that contains the given compound as its only row
     * [INTERNAL] Returns a quantification table that contains the given compound as its only row.  &lt;p&gt;  The columns of the row refer to the runs the compound has been quantified in, given as run ids and run names.  &lt;p&gt;  [INTERNAL] This endpoint is for internal use and not intended to become part of the stable API specification at any time. This endpoint can change (or be removed) at any time, even in minor updates.  [DEPRECATED] Will be replaced by the quantification table endpoint with filter query support, which allows  to request the quantification of an arbitrary subset of the project.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param compoundId compound which should be read out
     * @param type quantification type.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundQuantTableRowWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nullable QuantMeasure type) throws WebClientResponseException {
        return getCompoundQuantTableRowRequestCreation(projectId, compoundId, type);
    }

    /**
     * [EXPERIMENTAL] Returns the traces of the given compound
     * [EXPERIMENTAL] Returns the traces of the given compound.  &lt;p&gt;  A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
     * <p><b>200</b> - Traces of the given compound.
     * @param projectId project-space to read from.
     * @param compoundId compound which intensities should be read out
     * @param featureId The featureId parameter
     * @return TraceSetExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundTracesExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nullable String featureId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompoundTracesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling getCompoundTracesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("compoundId", compoundId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "featureId", featureId));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<TraceSetExperimental> localVarReturnType = new ParameterizedTypeReference<TraceSetExperimental>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/{compoundId}/traces", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Returns the traces of the given compound
     * [EXPERIMENTAL] Returns the traces of the given compound.  &lt;p&gt;  A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
     * <p><b>200</b> - Traces of the given compound.
     * @param projectId project-space to read from.
     * @param compoundId compound which intensities should be read out
     * @param featureId The featureId parameter
     * @return TraceSetExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public TraceSetExperimental getCompoundTracesExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nullable String featureId) throws WebClientResponseException {
        ParameterizedTypeReference<TraceSetExperimental> localVarReturnType = new ParameterizedTypeReference<TraceSetExperimental>() {};
        return getCompoundTracesExperimentalRequestCreation(projectId, compoundId, featureId).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Returns the traces of the given compound
     * [EXPERIMENTAL] Returns the traces of the given compound.  &lt;p&gt;  A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
     * <p><b>200</b> - Traces of the given compound.
     * @param projectId project-space to read from.
     * @param compoundId compound which intensities should be read out
     * @param featureId The featureId parameter
     * @return ResponseEntity&lt;TraceSetExperimental&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<TraceSetExperimental> getCompoundTracesExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nullable String featureId) throws WebClientResponseException {
        ParameterizedTypeReference<TraceSetExperimental> localVarReturnType = new ParameterizedTypeReference<TraceSetExperimental>() {};
        return getCompoundTracesExperimentalRequestCreation(projectId, compoundId, featureId).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Returns the traces of the given compound
     * [EXPERIMENTAL] Returns the traces of the given compound.  &lt;p&gt;  A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.*
     * <p><b>200</b> - Traces of the given compound.
     * @param projectId project-space to read from.
     * @param compoundId compound which intensities should be read out
     * @param featureId The featureId parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundTracesExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nullable String featureId) throws WebClientResponseException {
        return getCompoundTracesExperimentalRequestCreation(projectId, compoundId, featureId);
    }

    /**
     * [DEPRECATED] List of all available compounds (group of ion identities) in the given project-space
     * [DEPRECATED] List of all available compounds (group of ion identities) in the given project-space.  &lt;p&gt;  [DEPRECATED] Use /compounds/page instead. Loading all compounds at once does not scale for large projects.  This endpoint will be removed in the next major version of this API.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return List&lt;Compound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec getCompoundsRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompounds", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "msDataSearchPrepared", msDataSearchPrepared));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFieldsFeatures", optFieldsFeatures));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [DEPRECATED] List of all available compounds (group of ion identities) in the given project-space
     * [DEPRECATED] List of all available compounds (group of ion identities) in the given project-space.  &lt;p&gt;  [DEPRECATED] Use /compounds/page instead. Loading all compounds at once does not scale for large projects.  This endpoint will be removed in the next major version of this API.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return List&lt;Compound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Compound> getCompounds(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return getCompoundsRequestCreation(projectId, msDataSearchPrepared, optFields, optFieldsFeatures).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [DEPRECATED] List of all available compounds (group of ion identities) in the given project-space
     * [DEPRECATED] List of all available compounds (group of ion identities) in the given project-space.  &lt;p&gt;  [DEPRECATED] Use /compounds/page instead. Loading all compounds at once does not scale for large projects.  This endpoint will be removed in the next major version of this API.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseEntity&lt;List&lt;Compound&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Compound>> getCompoundsWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return getCompoundsRequestCreation(projectId, msDataSearchPrepared, optFields, optFieldsFeatures).toEntityList(localVarReturnType).block();
    }

    /**
     * [DEPRECATED] List of all available compounds (group of ion identities) in the given project-space
     * [DEPRECATED] List of all available compounds (group of ion identities) in the given project-space.  &lt;p&gt;  [DEPRECATED] Use /compounds/page instead. Loading all compounds at once does not scale for large projects.  This endpoint will be removed in the next major version of this API.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundsWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        return getCompoundsRequestCreation(projectId, msDataSearchPrepared, optFields, optFieldsFeatures);
    }

    /**
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag group
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag group.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - tagged compounds (group of ion identities)
     * @param projectId project-space to delete from.
     * @param groupName tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param msDataAsCosineQuery Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return PagedModelCompound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundsByGroupExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String groupName, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Boolean msDataAsCosineQuery, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompoundsByGroupExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'groupName' is set
        if (groupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'groupName' when calling getCompoundsByGroupExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "groupName", groupName));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "sort", sort));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "msDataAsCosineQuery", msDataAsCosineQuery));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFieldsFeatures", optFieldsFeatures));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/grouped", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag group
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag group.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - tagged compounds (group of ion identities)
     * @param projectId project-space to delete from.
     * @param groupName tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param msDataAsCosineQuery Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return PagedModelCompound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelCompound getCompoundsByGroupExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String groupName, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Boolean msDataAsCosineQuery, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return getCompoundsByGroupExperimentalRequestCreation(projectId, groupName, page, size, sort, msDataAsCosineQuery, optFields, optFieldsFeatures).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag group
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag group.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - tagged compounds (group of ion identities)
     * @param projectId project-space to delete from.
     * @param groupName tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param msDataAsCosineQuery Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseEntity&lt;PagedModelCompound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelCompound> getCompoundsByGroupExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String groupName, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Boolean msDataAsCosineQuery, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return getCompoundsByGroupExperimentalRequestCreation(projectId, groupName, page, size, sort, msDataAsCosineQuery, optFields, optFieldsFeatures).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag group
     * [EXPERIMENTAL] Get compounds (group of ion identities) by tag group.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - tagged compounds (group of ion identities)
     * @param projectId project-space to delete from.
     * @param groupName tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param msDataAsCosineQuery Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundsByGroupExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String groupName, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Boolean msDataAsCosineQuery, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        return getCompoundsByGroupExperimentalRequestCreation(projectId, groupName, page, size, sort, msDataAsCosineQuery, optFields, optFieldsFeatures);
    }

    /**
     * Page of available compounds (group of ion identities) in the given project-space
     * Page of available compounds (group of ion identities) in the given project-space.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name.  &lt;/p&gt;  &lt;p&gt;  Currently the only searchable fields are names of tags (&lt;code&gt;tagName&lt;/code&gt;) followed by a clause that is valued for the value type of the tag (See TagDefinition).  Tag name based field need to be prefixed with the namespace &lt;code&gt;tags.&lt;/code&gt;.  Possible value types of tags are &lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;tags.MyTagA:sample&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;tags.MyTagA:&amp;quot;Some Text&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;tags.MyTagA:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;tags.MyTagB&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;tags.MyTagB:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;tags.city:&amp;quot;new york&amp;quot; AND tags.ATextTag:/[mb]oat/ AND tags.count:[1 TO *] OR tags.realNumberTag&amp;lt;&#x3D;3.2 OR tags.MyDateTag:2024-01-01 OR tags.MyDateTag:[2023-10-01 TO 2023-12-24] OR tags.MyDateTag&amp;lt;2022-01-01 OR tags.time:12\\:00\\:00 OR tags.time:[12\\:00\\:00 TO 14\\:00\\:00] OR tags.time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;  &lt;p&gt;  &lt;strong&gt;Note:&lt;/strong&gt; compound-level indexing is not implemented yet, so this endpoint always reads from the  project database. Passing a non-empty &lt;code&gt;searchQuery&lt;/code&gt; is therefore not supported and responds with  405 METHOD_NOT_ALLOWED. Omit the parameter to page over all compounds.
     * <p><b>200</b> - tagged compounds (group of ion identities)
     * @param projectId project-space to read from.
     * @param searchQuery Optional search query in lucene syntax. Not yet supported for compounds; a non-empty                      query responds with 405 METHOD_NOT_ALLOWED. Omit this parameter to page over all compounds.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return PagedModelCompound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundsPageRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompoundsPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "searchQuery", searchQuery));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "sort", sort));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "msDataSearchPrepared", msDataSearchPrepared));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFieldsFeatures", optFieldsFeatures));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/page", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Page of available compounds (group of ion identities) in the given project-space
     * Page of available compounds (group of ion identities) in the given project-space.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name.  &lt;/p&gt;  &lt;p&gt;  Currently the only searchable fields are names of tags (&lt;code&gt;tagName&lt;/code&gt;) followed by a clause that is valued for the value type of the tag (See TagDefinition).  Tag name based field need to be prefixed with the namespace &lt;code&gt;tags.&lt;/code&gt;.  Possible value types of tags are &lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;tags.MyTagA:sample&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;tags.MyTagA:&amp;quot;Some Text&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;tags.MyTagA:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;tags.MyTagB&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;tags.MyTagB:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;tags.city:&amp;quot;new york&amp;quot; AND tags.ATextTag:/[mb]oat/ AND tags.count:[1 TO *] OR tags.realNumberTag&amp;lt;&#x3D;3.2 OR tags.MyDateTag:2024-01-01 OR tags.MyDateTag:[2023-10-01 TO 2023-12-24] OR tags.MyDateTag&amp;lt;2022-01-01 OR tags.time:12\\:00\\:00 OR tags.time:[12\\:00\\:00 TO 14\\:00\\:00] OR tags.time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;  &lt;p&gt;  &lt;strong&gt;Note:&lt;/strong&gt; compound-level indexing is not implemented yet, so this endpoint always reads from the  project database. Passing a non-empty &lt;code&gt;searchQuery&lt;/code&gt; is therefore not supported and responds with  405 METHOD_NOT_ALLOWED. Omit the parameter to page over all compounds.
     * <p><b>200</b> - tagged compounds (group of ion identities)
     * @param projectId project-space to read from.
     * @param searchQuery Optional search query in lucene syntax. Not yet supported for compounds; a non-empty                      query responds with 405 METHOD_NOT_ALLOWED. Omit this parameter to page over all compounds.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return PagedModelCompound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelCompound getCompoundsPage(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return getCompoundsPageRequestCreation(projectId, searchQuery, page, size, sort, msDataSearchPrepared, optFields, optFieldsFeatures).bodyToMono(localVarReturnType).block();
    }

    /**
     * Page of available compounds (group of ion identities) in the given project-space
     * Page of available compounds (group of ion identities) in the given project-space.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name.  &lt;/p&gt;  &lt;p&gt;  Currently the only searchable fields are names of tags (&lt;code&gt;tagName&lt;/code&gt;) followed by a clause that is valued for the value type of the tag (See TagDefinition).  Tag name based field need to be prefixed with the namespace &lt;code&gt;tags.&lt;/code&gt;.  Possible value types of tags are &lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;tags.MyTagA:sample&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;tags.MyTagA:&amp;quot;Some Text&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;tags.MyTagA:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;tags.MyTagB&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;tags.MyTagB:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;tags.city:&amp;quot;new york&amp;quot; AND tags.ATextTag:/[mb]oat/ AND tags.count:[1 TO *] OR tags.realNumberTag&amp;lt;&#x3D;3.2 OR tags.MyDateTag:2024-01-01 OR tags.MyDateTag:[2023-10-01 TO 2023-12-24] OR tags.MyDateTag&amp;lt;2022-01-01 OR tags.time:12\\:00\\:00 OR tags.time:[12\\:00\\:00 TO 14\\:00\\:00] OR tags.time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;  &lt;p&gt;  &lt;strong&gt;Note:&lt;/strong&gt; compound-level indexing is not implemented yet, so this endpoint always reads from the  project database. Passing a non-empty &lt;code&gt;searchQuery&lt;/code&gt; is therefore not supported and responds with  405 METHOD_NOT_ALLOWED. Omit the parameter to page over all compounds.
     * <p><b>200</b> - tagged compounds (group of ion identities)
     * @param projectId project-space to read from.
     * @param searchQuery Optional search query in lucene syntax. Not yet supported for compounds; a non-empty                      query responds with 405 METHOD_NOT_ALLOWED. Omit this parameter to page over all compounds.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseEntity&lt;PagedModelCompound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelCompound> getCompoundsPageWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelCompound> localVarReturnType = new ParameterizedTypeReference<PagedModelCompound>() {};
        return getCompoundsPageRequestCreation(projectId, searchQuery, page, size, sort, msDataSearchPrepared, optFields, optFieldsFeatures).toEntity(localVarReturnType).block();
    }

    /**
     * Page of available compounds (group of ion identities) in the given project-space
     * Page of available compounds (group of ion identities) in the given project-space.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name.  &lt;/p&gt;  &lt;p&gt;  Currently the only searchable fields are names of tags (&lt;code&gt;tagName&lt;/code&gt;) followed by a clause that is valued for the value type of the tag (See TagDefinition).  Tag name based field need to be prefixed with the namespace &lt;code&gt;tags.&lt;/code&gt;.  Possible value types of tags are &lt;strong&gt;bool&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;real&lt;/strong&gt;, &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt; - tag value   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;tags.MyTagA:sample&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;tags.MyTagA:&amp;quot;Some Text&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;tags.MyTagA:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;tags.MyTagB&amp;lt;3&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;tags.MyTagB:[* TO 3] &lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;tags.city:&amp;quot;new york&amp;quot; AND tags.ATextTag:/[mb]oat/ AND tags.count:[1 TO *] OR tags.realNumberTag&amp;lt;&#x3D;3.2 OR tags.MyDateTag:2024-01-01 OR tags.MyDateTag:[2023-10-01 TO 2023-12-24] OR tags.MyDateTag&amp;lt;2022-01-01 OR tags.time:12\\:00\\:00 OR tags.time:[12\\:00\\:00 TO 14\\:00\\:00] OR tags.time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;  &lt;p&gt;  &lt;strong&gt;Note:&lt;/strong&gt; compound-level indexing is not implemented yet, so this endpoint always reads from the  project database. Passing a non-empty &lt;code&gt;searchQuery&lt;/code&gt; is therefore not supported and responds with  405 METHOD_NOT_ALLOWED. Omit the parameter to page over all compounds.
     * <p><b>200</b> - tagged compounds (group of ion identities)
     * @param projectId project-space to read from.
     * @param searchQuery Optional search query in lucene syntax. Not yet supported for compounds; a non-empty                      query responds with 405 METHOD_NOT_ALLOWED. Omit this parameter to page over all compounds.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundsPageWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<CompoundOptField> optFields, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        return getCompoundsPageRequestCreation(projectId, searchQuery, page, size, sort, msDataSearchPrepared, optFields, optFieldsFeatures);
    }

    /**
     * [EXPERIMENTAL] Get all tags associated with this Compound
     * [EXPERIMENTAL] Get all tags associated with this Compound
     * <p><b>200</b> - the tags of the requested Compound
     * @param projectId project-space to get from.
     * @param objectId CompoundId to get tags for.
     * @return List&lt;Tag&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getTagsForCompoundExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String objectId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getTagsForCompoundExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'objectId' is set
        if (objectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'objectId' when calling getTagsForCompoundExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("objectId", objectId);

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

        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/tags/{objectId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Get all tags associated with this Compound
     * [EXPERIMENTAL] Get all tags associated with this Compound
     * <p><b>200</b> - the tags of the requested Compound
     * @param projectId project-space to get from.
     * @param objectId CompoundId to get tags for.
     * @return List&lt;Tag&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Tag> getTagsForCompoundExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String objectId) throws WebClientResponseException {
        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return getTagsForCompoundExperimentalRequestCreation(projectId, objectId).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [EXPERIMENTAL] Get all tags associated with this Compound
     * [EXPERIMENTAL] Get all tags associated with this Compound
     * <p><b>200</b> - the tags of the requested Compound
     * @param projectId project-space to get from.
     * @param objectId CompoundId to get tags for.
     * @return ResponseEntity&lt;List&lt;Tag&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Tag>> getTagsForCompoundExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String objectId) throws WebClientResponseException {
        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return getTagsForCompoundExperimentalRequestCreation(projectId, objectId).toEntityList(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get all tags associated with this Compound
     * [EXPERIMENTAL] Get all tags associated with this Compound
     * <p><b>200</b> - the tags of the requested Compound
     * @param projectId project-space to get from.
     * @param objectId CompoundId to get tags for.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getTagsForCompoundExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String objectId) throws WebClientResponseException {
        return getTagsForCompoundExperimentalRequestCreation(projectId, objectId);
    }

    /**
     * [EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space
     * [EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId compound (group of ion identities) to delete tag from.
     * @param tagName name of the tag to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec removeTagFromCompoundExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nonnull String tagName) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling removeTagFromCompoundExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling removeTagFromCompoundExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tagName' is set
        if (tagName == null) {
            throw new WebClientResponseException("Missing the required parameter 'tagName' when calling removeTagFromCompoundExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("compoundId", compoundId);
        pathParams.put("tagName", tagName);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/tags/{compoundId}/{tagName}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space
     * [EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId compound (group of ion identities) to delete tag from.
     * @param tagName name of the tag to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void removeTagFromCompoundExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nonnull String tagName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        removeTagFromCompoundExperimentalRequestCreation(projectId, compoundId, tagName).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space
     * [EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId compound (group of ion identities) to delete tag from.
     * @param tagName name of the tag to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> removeTagFromCompoundExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nonnull String tagName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return removeTagFromCompoundExperimentalRequestCreation(projectId, compoundId, tagName).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space
     * [EXPERIMENTAL] Delete tag with the given name from the compound (group of ion identities) with the specified ID in the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId compound (group of ion identities) to delete tag from.
     * @param tagName name of the tag to delete.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec removeTagFromCompoundExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String compoundId, @jakarta.annotation.Nonnull String tagName) throws WebClientResponseException {
        return removeTagFromCompoundExperimentalRequestCreation(projectId, compoundId, tagName);
    }
}
