package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import io.sirius.ms.sdk.model.AlignedFeature;
import io.sirius.ms.sdk.model.AlignedFeatureOptField;
import io.sirius.ms.sdk.model.AlignedFeatureQualityExperimental;
import io.sirius.ms.sdk.model.AnnotatedMsMsData;
import io.sirius.ms.sdk.model.AnnotatedSpectrum;
import io.sirius.ms.sdk.model.CanopusPrediction;
import io.sirius.ms.sdk.model.CompoundClasses;
import io.sirius.ms.sdk.model.FeatureImport;
import io.sirius.ms.sdk.model.FormulaCandidate;
import io.sirius.ms.sdk.model.FormulaCandidateOptField;
import io.sirius.ms.sdk.model.FragmentationTree;
import io.sirius.ms.sdk.model.InstrumentProfile;
import io.sirius.ms.sdk.model.IsotopePatternAnnotation;
import io.sirius.ms.sdk.model.LipidAnnotation;
import io.sirius.ms.sdk.model.MsData;
import io.sirius.ms.sdk.model.PagedModelAlignedFeature;
import io.sirius.ms.sdk.model.PagedModelFormulaCandidate;
import io.sirius.ms.sdk.model.PagedModelSpectralLibraryMatch;
import io.sirius.ms.sdk.model.PagedModelStructureCandidateFormula;
import io.sirius.ms.sdk.model.PagedModelStructureCandidateScored;
import io.sirius.ms.sdk.model.QuantificationMeasure;
import io.sirius.ms.sdk.model.QuantificationTableExperimental;
import io.sirius.ms.sdk.model.SpectralLibraryMatch;
import io.sirius.ms.sdk.model.SpectralLibraryMatchOptField;
import io.sirius.ms.sdk.model.SpectralLibraryMatchSummary;
import io.sirius.ms.sdk.model.StructureCandidateFormula;
import io.sirius.ms.sdk.model.StructureCandidateOptField;
import io.sirius.ms.sdk.model.StructureCandidateScored;
import io.sirius.ms.sdk.model.TraceSetExperimental;

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
public class FeaturesApi {
    private ApiClient apiClient;

    public FeaturesApi() {
        this(new ApiClient());
    }

    @Autowired
    public FeaturesApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Import (aligned) features into the project.
     * Import (aligned) features into the project. Features must not exist in the project.  Otherwise, they will exist twice.
     * <p><b>200</b> - the Features that have been imported with specified optional fields
     * @param projectId project-space to import into.
     * @param featureImport the feature data to be imported
     * @param profile profile describing the instrument used to measure the data. Used to merge spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; to override defaults.
     * @return List&lt;AlignedFeature&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addAlignedFeaturesRequestCreation(String projectId, List<FeatureImport> featureImport, InstrumentProfile profile, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        Object postBody = featureImport;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addAlignedFeatures", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'featureImport' is set
        if (featureImport == null) {
            throw new WebClientResponseException("Missing the required parameter 'featureImport' when calling addAlignedFeatures", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<AlignedFeature> localVarReturnType = new ParameterizedTypeReference<AlignedFeature>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Import (aligned) features into the project.
     * Import (aligned) features into the project. Features must not exist in the project.  Otherwise, they will exist twice.
     * <p><b>200</b> - the Features that have been imported with specified optional fields
     * @param projectId project-space to import into.
     * @param featureImport the feature data to be imported
     * @param profile profile describing the instrument used to measure the data. Used to merge spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; to override defaults.
     * @return List&lt;AlignedFeature&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<AlignedFeature> addAlignedFeatures(String projectId, List<FeatureImport> featureImport, InstrumentProfile profile, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeature> localVarReturnType = new ParameterizedTypeReference<AlignedFeature>() {};
        return addAlignedFeaturesRequestCreation(projectId, featureImport, profile, optFields).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Import (aligned) features into the project.
     * Import (aligned) features into the project. Features must not exist in the project.  Otherwise, they will exist twice.
     * <p><b>200</b> - the Features that have been imported with specified optional fields
     * @param projectId project-space to import into.
     * @param featureImport the feature data to be imported
     * @param profile profile describing the instrument used to measure the data. Used to merge spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; to override defaults.
     * @return ResponseEntity&lt;List&lt;AlignedFeature&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<AlignedFeature>> addAlignedFeaturesWithHttpInfo(String projectId, List<FeatureImport> featureImport, InstrumentProfile profile, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeature> localVarReturnType = new ParameterizedTypeReference<AlignedFeature>() {};
        return addAlignedFeaturesRequestCreation(projectId, featureImport, profile, optFields).toEntityList(localVarReturnType).block();
    }

    /**
     * Import (aligned) features into the project.
     * Import (aligned) features into the project. Features must not exist in the project.  Otherwise, they will exist twice.
     * <p><b>200</b> - the Features that have been imported with specified optional fields
     * @param projectId project-space to import into.
     * @param featureImport the feature data to be imported
     * @param profile profile describing the instrument used to measure the data. Used to merge spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addAlignedFeaturesWithResponseSpec(String projectId, List<FeatureImport> featureImport, InstrumentProfile profile, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        return addAlignedFeaturesRequestCreation(projectId, featureImport, profile, optFields);
    }
    /**
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteAlignedFeatureRequestCreation(String projectId, String alignedFeatureId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteAlignedFeature", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling deleteAlignedFeature", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

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
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteAlignedFeature(String projectId, String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteAlignedFeatureRequestCreation(projectId, alignedFeatureId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteAlignedFeatureWithHttpInfo(String projectId, String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteAlignedFeatureRequestCreation(projectId, alignedFeatureId).toEntity(localVarReturnType).block();
    }

    /**
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to delete.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteAlignedFeatureWithResponseSpec(String projectId, String alignedFeatureId) throws WebClientResponseException {
        return deleteAlignedFeatureRequestCreation(projectId, alignedFeatureId);
    }
    /**
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param requestBody The requestBody parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteAlignedFeaturesRequestCreation(String projectId, List<String> requestBody) throws WebClientResponseException {
        Object postBody = requestBody;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteAlignedFeatures", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'requestBody' is set
        if (requestBody == null) {
            throw new WebClientResponseException("Missing the required parameter 'requestBody' when calling deleteAlignedFeatures", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/delete", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param requestBody The requestBody parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteAlignedFeatures(String projectId, List<String> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteAlignedFeaturesRequestCreation(projectId, requestBody).bodyToMono(localVarReturnType).block();
    }

    /**
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param requestBody The requestBody parameter
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteAlignedFeaturesWithHttpInfo(String projectId, List<String> requestBody) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteAlignedFeaturesRequestCreation(projectId, requestBody).toEntity(localVarReturnType).block();
    }

    /**
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param requestBody The requestBody parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteAlignedFeaturesWithResponseSpec(String projectId, List<String> requestBody) throws WebClientResponseException {
        return deleteAlignedFeaturesRequestCreation(projectId, requestBody);
    }
    /**
     * Returns the adduct network for a given aligned feature id together with all merged traces contained in the network.
     * Returns the adduct network for a given aligned feature id together with all merged traces contained in the network.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param alignedFeatureId one feature that is considered the main feature of the adduct network
     * @return TraceSetExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAdductNetworkWithMergedTracesRequestCreation(String projectId, String alignedFeatureId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getAdductNetworkWithMergedTraces", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getAdductNetworkWithMergedTraces", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

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

        ParameterizedTypeReference<TraceSetExperimental> localVarReturnType = new ParameterizedTypeReference<TraceSetExperimental>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/adducts", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Returns the adduct network for a given aligned feature id together with all merged traces contained in the network.
     * Returns the adduct network for a given aligned feature id together with all merged traces contained in the network.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param alignedFeatureId one feature that is considered the main feature of the adduct network
     * @return TraceSetExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public TraceSetExperimental getAdductNetworkWithMergedTraces(String projectId, String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<TraceSetExperimental> localVarReturnType = new ParameterizedTypeReference<TraceSetExperimental>() {};
        return getAdductNetworkWithMergedTracesRequestCreation(projectId, alignedFeatureId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Returns the adduct network for a given aligned feature id together with all merged traces contained in the network.
     * Returns the adduct network for a given aligned feature id together with all merged traces contained in the network.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param alignedFeatureId one feature that is considered the main feature of the adduct network
     * @return ResponseEntity&lt;TraceSetExperimental&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<TraceSetExperimental> getAdductNetworkWithMergedTracesWithHttpInfo(String projectId, String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<TraceSetExperimental> localVarReturnType = new ParameterizedTypeReference<TraceSetExperimental>() {};
        return getAdductNetworkWithMergedTracesRequestCreation(projectId, alignedFeatureId).toEntity(localVarReturnType).block();
    }

    /**
     * Returns the adduct network for a given aligned feature id together with all merged traces contained in the network.
     * Returns the adduct network for a given aligned feature id together with all merged traces contained in the network.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param alignedFeatureId one feature that is considered the main feature of the adduct network
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAdductNetworkWithMergedTracesWithResponseSpec(String projectId, String alignedFeatureId) throws WebClientResponseException {
        return getAdductNetworkWithMergedTracesRequestCreation(projectId, alignedFeatureId);
    }
    /**
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeature with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return AlignedFeature
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAlignedFeatureRequestCreation(String projectId, String alignedFeatureId, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getAlignedFeature", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getAlignedFeature", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<AlignedFeature> localVarReturnType = new ParameterizedTypeReference<AlignedFeature>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeature with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return AlignedFeature
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public AlignedFeature getAlignedFeature(String projectId, String alignedFeatureId, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeature> localVarReturnType = new ParameterizedTypeReference<AlignedFeature>() {};
        return getAlignedFeatureRequestCreation(projectId, alignedFeatureId, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeature with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;AlignedFeature&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AlignedFeature> getAlignedFeatureWithHttpInfo(String projectId, String alignedFeatureId, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeature> localVarReturnType = new ParameterizedTypeReference<AlignedFeature>() {};
        return getAlignedFeatureRequestCreation(projectId, alignedFeatureId, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeature with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAlignedFeatureWithResponseSpec(String projectId, String alignedFeatureId, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        return getAlignedFeatureRequestCreation(projectId, alignedFeatureId, optFields);
    }
    /**
     * Get all available features (aligned over runs) in the given project-space.
     * Get all available features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatures with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;AlignedFeature&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAlignedFeaturesRequestCreation(String projectId, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getAlignedFeatures", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<AlignedFeature> localVarReturnType = new ParameterizedTypeReference<AlignedFeature>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get all available features (aligned over runs) in the given project-space.
     * Get all available features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatures with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;AlignedFeature&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<AlignedFeature> getAlignedFeatures(String projectId, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeature> localVarReturnType = new ParameterizedTypeReference<AlignedFeature>() {};
        return getAlignedFeaturesRequestCreation(projectId, optFields).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Get all available features (aligned over runs) in the given project-space.
     * Get all available features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatures with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;List&lt;AlignedFeature&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<AlignedFeature>> getAlignedFeaturesWithHttpInfo(String projectId, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeature> localVarReturnType = new ParameterizedTypeReference<AlignedFeature>() {};
        return getAlignedFeaturesRequestCreation(projectId, optFields).toEntityList(localVarReturnType).block();
    }

    /**
     * Get all available features (aligned over runs) in the given project-space.
     * Get all available features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatures with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAlignedFeaturesWithResponseSpec(String projectId, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        return getAlignedFeaturesRequestCreation(projectId, optFields);
    }
    /**
     * Get all available features (aligned over runs) in the given project-space.
     * Get all available features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatures with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelAlignedFeature
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAlignedFeaturesPagedRequestCreation(String projectId, Integer page, Integer size, List<String> sort, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getAlignedFeaturesPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "sort", sort));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PagedModelAlignedFeature> localVarReturnType = new ParameterizedTypeReference<PagedModelAlignedFeature>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/page", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get all available features (aligned over runs) in the given project-space.
     * Get all available features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatures with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelAlignedFeature
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelAlignedFeature getAlignedFeaturesPaged(String projectId, Integer page, Integer size, List<String> sort, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelAlignedFeature> localVarReturnType = new ParameterizedTypeReference<PagedModelAlignedFeature>() {};
        return getAlignedFeaturesPagedRequestCreation(projectId, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get all available features (aligned over runs) in the given project-space.
     * Get all available features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatures with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PagedModelAlignedFeature&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelAlignedFeature> getAlignedFeaturesPagedWithHttpInfo(String projectId, Integer page, Integer size, List<String> sort, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelAlignedFeature> localVarReturnType = new ParameterizedTypeReference<PagedModelAlignedFeature>() {};
        return getAlignedFeaturesPagedRequestCreation(projectId, page, size, sort, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Get all available features (aligned over runs) in the given project-space.
     * Get all available features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatures with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAlignedFeaturesPagedWithResponseSpec(String projectId, Integer page, Integer size, List<String> sort, List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        return getAlignedFeaturesPagedRequestCreation(projectId, page, size, sort, optFields);
    }
    /**
     * EXPERIMENTAL: This endpoint is experimental and may be changed (or even removed) without notice until it is declared stable.
     * Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @return AlignedFeatureQualityExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAlignedFeaturesQualityExperimentalRequestCreation(String projectId, String alignedFeatureId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getAlignedFeaturesQualityExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getAlignedFeaturesQualityExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

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

        ParameterizedTypeReference<AlignedFeatureQualityExperimental> localVarReturnType = new ParameterizedTypeReference<AlignedFeatureQualityExperimental>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/quality-report", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * EXPERIMENTAL: This endpoint is experimental and may be changed (or even removed) without notice until it is declared stable.
     * Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @return AlignedFeatureQualityExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public AlignedFeatureQualityExperimental getAlignedFeaturesQualityExperimental(String projectId, String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeatureQualityExperimental> localVarReturnType = new ParameterizedTypeReference<AlignedFeatureQualityExperimental>() {};
        return getAlignedFeaturesQualityExperimentalRequestCreation(projectId, alignedFeatureId).bodyToMono(localVarReturnType).block();
    }

    /**
     * EXPERIMENTAL: This endpoint is experimental and may be changed (or even removed) without notice until it is declared stable.
     * Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @return ResponseEntity&lt;AlignedFeatureQualityExperimental&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AlignedFeatureQualityExperimental> getAlignedFeaturesQualityExperimentalWithHttpInfo(String projectId, String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeatureQualityExperimental> localVarReturnType = new ParameterizedTypeReference<AlignedFeatureQualityExperimental>() {};
        return getAlignedFeaturesQualityExperimentalRequestCreation(projectId, alignedFeatureId).toEntity(localVarReturnType).block();
    }

    /**
     * EXPERIMENTAL: This endpoint is experimental and may be changed (or even removed) without notice until it is declared stable.
     * Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAlignedFeaturesQualityExperimentalWithResponseSpec(String projectId, String alignedFeatureId) throws WebClientResponseException {
        return getAlignedFeaturesQualityExperimentalRequestCreation(projectId, alignedFeatureId);
    }
    /**
     * Best matching compound classes,  Set of the highest scoring compound classes (CANOPUS) on each hierarchy level of  the ClassyFire and NPC ontology,
     * Best matching compound classes,  Set of the highest scoring compound classes (CANOPUS) on each hierarchy level of  the ClassyFire and NPC ontology,
     * <p><b>200</b> - Best matching Predicted compound classes
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return CompoundClasses
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getBestMatchingCompoundClassesRequestCreation(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getBestMatchingCompoundClasses", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getBestMatchingCompoundClasses", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getBestMatchingCompoundClasses", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("formulaId", formulaId);

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

        ParameterizedTypeReference<CompoundClasses> localVarReturnType = new ParameterizedTypeReference<CompoundClasses>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/best-compound-classes", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Best matching compound classes,  Set of the highest scoring compound classes (CANOPUS) on each hierarchy level of  the ClassyFire and NPC ontology,
     * Best matching compound classes,  Set of the highest scoring compound classes (CANOPUS) on each hierarchy level of  the ClassyFire and NPC ontology,
     * <p><b>200</b> - Best matching Predicted compound classes
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return CompoundClasses
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public CompoundClasses getBestMatchingCompoundClasses(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<CompoundClasses> localVarReturnType = new ParameterizedTypeReference<CompoundClasses>() {};
        return getBestMatchingCompoundClassesRequestCreation(projectId, alignedFeatureId, formulaId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Best matching compound classes,  Set of the highest scoring compound classes (CANOPUS) on each hierarchy level of  the ClassyFire and NPC ontology,
     * Best matching compound classes,  Set of the highest scoring compound classes (CANOPUS) on each hierarchy level of  the ClassyFire and NPC ontology,
     * <p><b>200</b> - Best matching Predicted compound classes
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseEntity&lt;CompoundClasses&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<CompoundClasses> getBestMatchingCompoundClassesWithHttpInfo(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<CompoundClasses> localVarReturnType = new ParameterizedTypeReference<CompoundClasses>() {};
        return getBestMatchingCompoundClassesRequestCreation(projectId, alignedFeatureId, formulaId).toEntity(localVarReturnType).block();
    }

    /**
     * Best matching compound classes,  Set of the highest scoring compound classes (CANOPUS) on each hierarchy level of  the ClassyFire and NPC ontology,
     * Best matching compound classes,  Set of the highest scoring compound classes (CANOPUS) on each hierarchy level of  the ClassyFire and NPC ontology,
     * <p><b>200</b> - Best matching Predicted compound classes
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getBestMatchingCompoundClassesWithResponseSpec(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        return getBestMatchingCompoundClassesRequestCreation(projectId, alignedFeatureId, formulaId);
    }
    /**
     * All predicted compound classes (CANOPUS) from ClassyFire and NPC and their probabilities,
     * All predicted compound classes (CANOPUS) from ClassyFire and NPC and their probabilities,
     * <p><b>200</b> - Predicted compound classes
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return CanopusPrediction
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCanopusPredictionRequestCreation(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCanopusPrediction", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getCanopusPrediction", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getCanopusPrediction", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("formulaId", formulaId);

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

        ParameterizedTypeReference<CanopusPrediction> localVarReturnType = new ParameterizedTypeReference<CanopusPrediction>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/canopus-prediction", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * All predicted compound classes (CANOPUS) from ClassyFire and NPC and their probabilities,
     * All predicted compound classes (CANOPUS) from ClassyFire and NPC and their probabilities,
     * <p><b>200</b> - Predicted compound classes
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return CanopusPrediction
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public CanopusPrediction getCanopusPrediction(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<CanopusPrediction> localVarReturnType = new ParameterizedTypeReference<CanopusPrediction>() {};
        return getCanopusPredictionRequestCreation(projectId, alignedFeatureId, formulaId).bodyToMono(localVarReturnType).block();
    }

    /**
     * All predicted compound classes (CANOPUS) from ClassyFire and NPC and their probabilities,
     * All predicted compound classes (CANOPUS) from ClassyFire and NPC and their probabilities,
     * <p><b>200</b> - Predicted compound classes
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseEntity&lt;CanopusPrediction&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<CanopusPrediction> getCanopusPredictionWithHttpInfo(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<CanopusPrediction> localVarReturnType = new ParameterizedTypeReference<CanopusPrediction>() {};
        return getCanopusPredictionRequestCreation(projectId, alignedFeatureId, formulaId).toEntity(localVarReturnType).block();
    }

    /**
     * All predicted compound classes (CANOPUS) from ClassyFire and NPC and their probabilities,
     * All predicted compound classes (CANOPUS) from ClassyFire and NPC and their probabilities,
     * <p><b>200</b> - Predicted compound classes
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCanopusPredictionWithResponseSpec(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        return getCanopusPredictionRequestCreation(projectId, alignedFeatureId, formulaId);
    }
    /**
     * List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;StructureCandidateFormula&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getDeNovoStructureCandidatesRequestCreation(String projectId, String alignedFeatureId, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getDeNovoStructureCandidates", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getDeNovoStructureCandidates", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<StructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<StructureCandidateFormula>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/denovo-structures", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;StructureCandidateFormula&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<StructureCandidateFormula> getDeNovoStructureCandidates(String projectId, String alignedFeatureId, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<StructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<StructureCandidateFormula>() {};
        return getDeNovoStructureCandidatesRequestCreation(projectId, alignedFeatureId, optFields).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;List&lt;StructureCandidateFormula&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<StructureCandidateFormula>> getDeNovoStructureCandidatesWithHttpInfo(String projectId, String alignedFeatureId, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<StructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<StructureCandidateFormula>() {};
        return getDeNovoStructureCandidatesRequestCreation(projectId, alignedFeatureId, optFields).toEntityList(localVarReturnType).block();
    }

    /**
     * List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getDeNovoStructureCandidatesWithResponseSpec(String projectId, String alignedFeatureId, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        return getDeNovoStructureCandidatesRequestCreation(projectId, alignedFeatureId, optFields);
    }
    /**
     * List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;StructureCandidateScored&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getDeNovoStructureCandidatesByFormulaRequestCreation(String projectId, String alignedFeatureId, String formulaId, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getDeNovoStructureCandidatesByFormula", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getDeNovoStructureCandidatesByFormula", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getDeNovoStructureCandidatesByFormula", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("formulaId", formulaId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<StructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<StructureCandidateScored>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/denovo-structures", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;StructureCandidateScored&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<StructureCandidateScored> getDeNovoStructureCandidatesByFormula(String projectId, String alignedFeatureId, String formulaId, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<StructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<StructureCandidateScored>() {};
        return getDeNovoStructureCandidatesByFormulaRequestCreation(projectId, alignedFeatureId, formulaId, optFields).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;List&lt;StructureCandidateScored&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<StructureCandidateScored>> getDeNovoStructureCandidatesByFormulaWithHttpInfo(String projectId, String alignedFeatureId, String formulaId, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<StructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<StructureCandidateScored>() {};
        return getDeNovoStructureCandidatesByFormulaRequestCreation(projectId, alignedFeatureId, formulaId, optFields).toEntityList(localVarReturnType).block();
    }

    /**
     * List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getDeNovoStructureCandidatesByFormulaWithResponseSpec(String projectId, String alignedFeatureId, String formulaId, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        return getDeNovoStructureCandidatesByFormulaRequestCreation(projectId, alignedFeatureId, formulaId, optFields);
    }
    /**
     * Page of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * Page of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelStructureCandidateScored
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getDeNovoStructureCandidatesByFormulaPagedRequestCreation(String projectId, String alignedFeatureId, String formulaId, Integer page, Integer size, List<String> sort, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getDeNovoStructureCandidatesByFormulaPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getDeNovoStructureCandidatesByFormulaPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getDeNovoStructureCandidatesByFormulaPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("formulaId", formulaId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "sort", sort));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PagedModelStructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateScored>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/denovo-structures/page", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Page of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * Page of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelStructureCandidateScored
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelStructureCandidateScored getDeNovoStructureCandidatesByFormulaPaged(String projectId, String alignedFeatureId, String formulaId, Integer page, Integer size, List<String> sort, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelStructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateScored>() {};
        return getDeNovoStructureCandidatesByFormulaPagedRequestCreation(projectId, alignedFeatureId, formulaId, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Page of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * Page of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PagedModelStructureCandidateScored&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelStructureCandidateScored> getDeNovoStructureCandidatesByFormulaPagedWithHttpInfo(String projectId, String alignedFeatureId, String formulaId, Integer page, Integer size, List<String> sort, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelStructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateScored>() {};
        return getDeNovoStructureCandidatesByFormulaPagedRequestCreation(projectId, alignedFeatureId, formulaId, page, size, sort, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Page of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * Page of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getDeNovoStructureCandidatesByFormulaPagedWithResponseSpec(String projectId, String alignedFeatureId, String formulaId, Integer page, Integer size, List<String> sort, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        return getDeNovoStructureCandidatesByFormulaPagedRequestCreation(projectId, alignedFeatureId, formulaId, page, size, sort, optFields);
    }
    /**
     * Page of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * Page of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelStructureCandidateFormula
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getDeNovoStructureCandidatesPagedRequestCreation(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getDeNovoStructureCandidatesPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getDeNovoStructureCandidatesPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "sort", sort));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PagedModelStructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateFormula>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/denovo-structures/page", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Page of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * Page of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelStructureCandidateFormula
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelStructureCandidateFormula getDeNovoStructureCandidatesPaged(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelStructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateFormula>() {};
        return getDeNovoStructureCandidatesPagedRequestCreation(projectId, alignedFeatureId, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Page of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * Page of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PagedModelStructureCandidateFormula&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelStructureCandidateFormula> getDeNovoStructureCandidatesPagedWithHttpInfo(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelStructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateFormula>() {};
        return getDeNovoStructureCandidatesPagedRequestCreation(projectId, alignedFeatureId, page, size, sort, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Page of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * Page of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getDeNovoStructureCandidatesPagedWithResponseSpec(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        return getDeNovoStructureCandidatesPagedRequestCreation(projectId, alignedFeatureId, page, size, sort, optFields);
    }
    /**
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier  This fingerprint is used to perform structure database search and predict compound classes.
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier  This fingerprint is used to perform structure database search and predict compound classes.
     * <p><b>200</b> - probabilistic fingerprint predicted by CSI:FingerID
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return List&lt;Double&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFingerprintPredictionRequestCreation(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getFingerprintPrediction", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getFingerprintPrediction", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getFingerprintPrediction", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("formulaId", formulaId);

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

        ParameterizedTypeReference<Double> localVarReturnType = new ParameterizedTypeReference<Double>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/fingerprint", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier  This fingerprint is used to perform structure database search and predict compound classes.
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier  This fingerprint is used to perform structure database search and predict compound classes.
     * <p><b>200</b> - probabilistic fingerprint predicted by CSI:FingerID
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return List&lt;Double&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Double> getFingerprintPrediction(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<Double> localVarReturnType = new ParameterizedTypeReference<Double>() {};
        return getFingerprintPredictionRequestCreation(projectId, alignedFeatureId, formulaId).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier  This fingerprint is used to perform structure database search and predict compound classes.
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier  This fingerprint is used to perform structure database search and predict compound classes.
     * <p><b>200</b> - probabilistic fingerprint predicted by CSI:FingerID
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseEntity&lt;List&lt;Double&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Double>> getFingerprintPredictionWithHttpInfo(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<Double> localVarReturnType = new ParameterizedTypeReference<Double>() {};
        return getFingerprintPredictionRequestCreation(projectId, alignedFeatureId, formulaId).toEntityList(localVarReturnType).block();
    }

    /**
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier  This fingerprint is used to perform structure database search and predict compound classes.
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier  This fingerprint is used to perform structure database search and predict compound classes.
     * <p><b>200</b> - probabilistic fingerprint predicted by CSI:FingerID
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFingerprintPredictionWithResponseSpec(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        return getFingerprintPredictionRequestCreation(projectId, alignedFeatureId, formulaId);
    }
    /**
     * Returns MS/MS Spectrum (Merged MS/MS and measured MS/MS) which is annotated with fragments and losses  for the given formula result identifier  These annotations are only available if a fragmentation tree and the structure candidate are available.
     * Returns MS/MS Spectrum (Merged MS/MS and measured MS/MS) which is annotated with fragments and losses  for the given formula result identifier  These annotations are only available if a fragmentation tree and the structure candidate are available.
     * <p><b>200</b> - Fragmentation spectra annotated with fragment formulas and losses.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return AnnotatedMsMsData
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFormulaAnnotatedMsMsDataRequestCreation(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getFormulaAnnotatedMsMsData", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getFormulaAnnotatedMsMsData", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getFormulaAnnotatedMsMsData", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("formulaId", formulaId);

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

        ParameterizedTypeReference<AnnotatedMsMsData> localVarReturnType = new ParameterizedTypeReference<AnnotatedMsMsData>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/annotated-msmsdata", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Returns MS/MS Spectrum (Merged MS/MS and measured MS/MS) which is annotated with fragments and losses  for the given formula result identifier  These annotations are only available if a fragmentation tree and the structure candidate are available.
     * Returns MS/MS Spectrum (Merged MS/MS and measured MS/MS) which is annotated with fragments and losses  for the given formula result identifier  These annotations are only available if a fragmentation tree and the structure candidate are available.
     * <p><b>200</b> - Fragmentation spectra annotated with fragment formulas and losses.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return AnnotatedMsMsData
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public AnnotatedMsMsData getFormulaAnnotatedMsMsData(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<AnnotatedMsMsData> localVarReturnType = new ParameterizedTypeReference<AnnotatedMsMsData>() {};
        return getFormulaAnnotatedMsMsDataRequestCreation(projectId, alignedFeatureId, formulaId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Returns MS/MS Spectrum (Merged MS/MS and measured MS/MS) which is annotated with fragments and losses  for the given formula result identifier  These annotations are only available if a fragmentation tree and the structure candidate are available.
     * Returns MS/MS Spectrum (Merged MS/MS and measured MS/MS) which is annotated with fragments and losses  for the given formula result identifier  These annotations are only available if a fragmentation tree and the structure candidate are available.
     * <p><b>200</b> - Fragmentation spectra annotated with fragment formulas and losses.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseEntity&lt;AnnotatedMsMsData&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AnnotatedMsMsData> getFormulaAnnotatedMsMsDataWithHttpInfo(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<AnnotatedMsMsData> localVarReturnType = new ParameterizedTypeReference<AnnotatedMsMsData>() {};
        return getFormulaAnnotatedMsMsDataRequestCreation(projectId, alignedFeatureId, formulaId).toEntity(localVarReturnType).block();
    }

    /**
     * Returns MS/MS Spectrum (Merged MS/MS and measured MS/MS) which is annotated with fragments and losses  for the given formula result identifier  These annotations are only available if a fragmentation tree and the structure candidate are available.
     * Returns MS/MS Spectrum (Merged MS/MS and measured MS/MS) which is annotated with fragments and losses  for the given formula result identifier  These annotations are only available if a fragmentation tree and the structure candidate are available.
     * <p><b>200</b> - Fragmentation spectra annotated with fragment formulas and losses.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFormulaAnnotatedMsMsDataWithResponseSpec(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        return getFormulaAnnotatedMsMsDataRequestCreation(projectId, alignedFeatureId, formulaId);
    }
    /**
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier  These annotations are only available if a fragmentation tree is available.
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier  These annotations are only available if a fragmentation tree is available.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragment formulas and losses.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param spectrumIndex index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex &lt; 0 (default)
     * @return AnnotatedSpectrum
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFormulaAnnotatedSpectrumRequestCreation(String projectId, String alignedFeatureId, String formulaId, Integer spectrumIndex) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getFormulaAnnotatedSpectrum", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getFormulaAnnotatedSpectrum", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getFormulaAnnotatedSpectrum", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("formulaId", formulaId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "spectrumIndex", spectrumIndex));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<AnnotatedSpectrum> localVarReturnType = new ParameterizedTypeReference<AnnotatedSpectrum>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/annotated-spectrum", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier  These annotations are only available if a fragmentation tree is available.
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier  These annotations are only available if a fragmentation tree is available.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragment formulas and losses.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param spectrumIndex index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex &lt; 0 (default)
     * @return AnnotatedSpectrum
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public AnnotatedSpectrum getFormulaAnnotatedSpectrum(String projectId, String alignedFeatureId, String formulaId, Integer spectrumIndex) throws WebClientResponseException {
        ParameterizedTypeReference<AnnotatedSpectrum> localVarReturnType = new ParameterizedTypeReference<AnnotatedSpectrum>() {};
        return getFormulaAnnotatedSpectrumRequestCreation(projectId, alignedFeatureId, formulaId, spectrumIndex).bodyToMono(localVarReturnType).block();
    }

    /**
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier  These annotations are only available if a fragmentation tree is available.
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier  These annotations are only available if a fragmentation tree is available.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragment formulas and losses.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param spectrumIndex index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex &lt; 0 (default)
     * @return ResponseEntity&lt;AnnotatedSpectrum&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AnnotatedSpectrum> getFormulaAnnotatedSpectrumWithHttpInfo(String projectId, String alignedFeatureId, String formulaId, Integer spectrumIndex) throws WebClientResponseException {
        ParameterizedTypeReference<AnnotatedSpectrum> localVarReturnType = new ParameterizedTypeReference<AnnotatedSpectrum>() {};
        return getFormulaAnnotatedSpectrumRequestCreation(projectId, alignedFeatureId, formulaId, spectrumIndex).toEntity(localVarReturnType).block();
    }

    /**
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier  These annotations are only available if a fragmentation tree is available.
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier  These annotations are only available if a fragmentation tree is available.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragment formulas and losses.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param spectrumIndex index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex &lt; 0 (default)
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFormulaAnnotatedSpectrumWithResponseSpec(String projectId, String alignedFeatureId, String formulaId, Integer spectrumIndex) throws WebClientResponseException {
        return getFormulaAnnotatedSpectrumRequestCreation(projectId, alignedFeatureId, formulaId, spectrumIndex);
    }
    /**
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.  Can be enriched with an optional results overview and formula candidate information.
     * <p><b>200</b> - FormulaCandidate of this feature (aligned over runs) with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return FormulaCandidate
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFormulaCandidateRequestCreation(String projectId, String alignedFeatureId, String formulaId, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getFormulaCandidate", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getFormulaCandidate", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getFormulaCandidate", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("formulaId", formulaId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<FormulaCandidate> localVarReturnType = new ParameterizedTypeReference<FormulaCandidate>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.  Can be enriched with an optional results overview and formula candidate information.
     * <p><b>200</b> - FormulaCandidate of this feature (aligned over runs) with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return FormulaCandidate
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public FormulaCandidate getFormulaCandidate(String projectId, String alignedFeatureId, String formulaId, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<FormulaCandidate> localVarReturnType = new ParameterizedTypeReference<FormulaCandidate>() {};
        return getFormulaCandidateRequestCreation(projectId, alignedFeatureId, formulaId, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.  Can be enriched with an optional results overview and formula candidate information.
     * <p><b>200</b> - FormulaCandidate of this feature (aligned over runs) with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;FormulaCandidate&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<FormulaCandidate> getFormulaCandidateWithHttpInfo(String projectId, String alignedFeatureId, String formulaId, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<FormulaCandidate> localVarReturnType = new ParameterizedTypeReference<FormulaCandidate>() {};
        return getFormulaCandidateRequestCreation(projectId, alignedFeatureId, formulaId, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.  Can be enriched with an optional results overview and formula candidate information.
     * <p><b>200</b> - FormulaCandidate of this feature (aligned over runs) with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFormulaCandidateWithResponseSpec(String projectId, String alignedFeatureId, String formulaId, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        return getFormulaCandidateRequestCreation(projectId, alignedFeatureId, formulaId, optFields);
    }
    /**
     * List of FormulaResultContainers available for this feature with minimal information.
     * List of FormulaResultContainers available for this feature with minimal information.  Can be enriched with an optional results overview.
     * <p><b>200</b> - All FormulaCandidate of this feature with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;FormulaCandidate&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFormulaCandidatesRequestCreation(String projectId, String alignedFeatureId, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getFormulaCandidates", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getFormulaCandidates", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<FormulaCandidate> localVarReturnType = new ParameterizedTypeReference<FormulaCandidate>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List of FormulaResultContainers available for this feature with minimal information.
     * List of FormulaResultContainers available for this feature with minimal information.  Can be enriched with an optional results overview.
     * <p><b>200</b> - All FormulaCandidate of this feature with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;FormulaCandidate&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<FormulaCandidate> getFormulaCandidates(String projectId, String alignedFeatureId, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<FormulaCandidate> localVarReturnType = new ParameterizedTypeReference<FormulaCandidate>() {};
        return getFormulaCandidatesRequestCreation(projectId, alignedFeatureId, optFields).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * List of FormulaResultContainers available for this feature with minimal information.
     * List of FormulaResultContainers available for this feature with minimal information.  Can be enriched with an optional results overview.
     * <p><b>200</b> - All FormulaCandidate of this feature with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;List&lt;FormulaCandidate&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<FormulaCandidate>> getFormulaCandidatesWithHttpInfo(String projectId, String alignedFeatureId, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<FormulaCandidate> localVarReturnType = new ParameterizedTypeReference<FormulaCandidate>() {};
        return getFormulaCandidatesRequestCreation(projectId, alignedFeatureId, optFields).toEntityList(localVarReturnType).block();
    }

    /**
     * List of FormulaResultContainers available for this feature with minimal information.
     * List of FormulaResultContainers available for this feature with minimal information.  Can be enriched with an optional results overview.
     * <p><b>200</b> - All FormulaCandidate of this feature with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFormulaCandidatesWithResponseSpec(String projectId, String alignedFeatureId, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        return getFormulaCandidatesRequestCreation(projectId, alignedFeatureId, optFields);
    }
    /**
     * Page of FormulaResultContainers available for this feature with minimal information.
     * Page of FormulaResultContainers available for this feature with minimal information.  Can be enriched with an optional results overview.
     * <p><b>200</b> - All FormulaCandidate of this feature with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelFormulaCandidate
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFormulaCandidatesPagedRequestCreation(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getFormulaCandidatesPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getFormulaCandidatesPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "sort", sort));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PagedModelFormulaCandidate> localVarReturnType = new ParameterizedTypeReference<PagedModelFormulaCandidate>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/page", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Page of FormulaResultContainers available for this feature with minimal information.
     * Page of FormulaResultContainers available for this feature with minimal information.  Can be enriched with an optional results overview.
     * <p><b>200</b> - All FormulaCandidate of this feature with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelFormulaCandidate
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelFormulaCandidate getFormulaCandidatesPaged(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelFormulaCandidate> localVarReturnType = new ParameterizedTypeReference<PagedModelFormulaCandidate>() {};
        return getFormulaCandidatesPagedRequestCreation(projectId, alignedFeatureId, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Page of FormulaResultContainers available for this feature with minimal information.
     * Page of FormulaResultContainers available for this feature with minimal information.  Can be enriched with an optional results overview.
     * <p><b>200</b> - All FormulaCandidate of this feature with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PagedModelFormulaCandidate&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelFormulaCandidate> getFormulaCandidatesPagedWithHttpInfo(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelFormulaCandidate> localVarReturnType = new ParameterizedTypeReference<PagedModelFormulaCandidate>() {};
        return getFormulaCandidatesPagedRequestCreation(projectId, alignedFeatureId, page, size, sort, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Page of FormulaResultContainers available for this feature with minimal information.
     * Page of FormulaResultContainers available for this feature with minimal information.  Can be enriched with an optional results overview.
     * <p><b>200</b> - All FormulaCandidate of this feature with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFormulaCandidatesPagedWithResponseSpec(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        return getFormulaCandidatesPagedRequestCreation(projectId, alignedFeatureId, page, size, sort, optFields);
    }
    /**
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier  This tree is used to rank formula candidates (treeScore).
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier  This tree is used to rank formula candidates (treeScore).
     * <p><b>200</b> - Fragmentation Tree
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return FragmentationTree
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFragTreeRequestCreation(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getFragTree", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getFragTree", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getFragTree", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("formulaId", formulaId);

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

        ParameterizedTypeReference<FragmentationTree> localVarReturnType = new ParameterizedTypeReference<FragmentationTree>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/fragtree", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier  This tree is used to rank formula candidates (treeScore).
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier  This tree is used to rank formula candidates (treeScore).
     * <p><b>200</b> - Fragmentation Tree
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return FragmentationTree
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public FragmentationTree getFragTree(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<FragmentationTree> localVarReturnType = new ParameterizedTypeReference<FragmentationTree>() {};
        return getFragTreeRequestCreation(projectId, alignedFeatureId, formulaId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier  This tree is used to rank formula candidates (treeScore).
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier  This tree is used to rank formula candidates (treeScore).
     * <p><b>200</b> - Fragmentation Tree
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseEntity&lt;FragmentationTree&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<FragmentationTree> getFragTreeWithHttpInfo(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<FragmentationTree> localVarReturnType = new ParameterizedTypeReference<FragmentationTree>() {};
        return getFragTreeRequestCreation(projectId, alignedFeatureId, formulaId).toEntity(localVarReturnType).block();
    }

    /**
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier  This tree is used to rank formula candidates (treeScore).
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier  This tree is used to rank formula candidates (treeScore).
     * <p><b>200</b> - Fragmentation Tree
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFragTreeWithResponseSpec(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        return getFragTreeRequestCreation(projectId, alignedFeatureId, formulaId);
    }
    /**
     * Returns Isotope pattern information (simulated isotope pattern, measured isotope pattern, isotope pattern highlighting)  for the given formula result identifier.
     * Returns Isotope pattern information (simulated isotope pattern, measured isotope pattern, isotope pattern highlighting)  for the given formula result identifier. This simulated isotope pattern is used to rank formula candidates (treeScore).
     * <p><b>200</b> - Isotope pattern information
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return IsotopePatternAnnotation
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getIsotopePatternAnnotationRequestCreation(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getIsotopePatternAnnotation", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getIsotopePatternAnnotation", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getIsotopePatternAnnotation", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("formulaId", formulaId);

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

        ParameterizedTypeReference<IsotopePatternAnnotation> localVarReturnType = new ParameterizedTypeReference<IsotopePatternAnnotation>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/isotope-pattern", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Returns Isotope pattern information (simulated isotope pattern, measured isotope pattern, isotope pattern highlighting)  for the given formula result identifier.
     * Returns Isotope pattern information (simulated isotope pattern, measured isotope pattern, isotope pattern highlighting)  for the given formula result identifier. This simulated isotope pattern is used to rank formula candidates (treeScore).
     * <p><b>200</b> - Isotope pattern information
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return IsotopePatternAnnotation
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public IsotopePatternAnnotation getIsotopePatternAnnotation(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<IsotopePatternAnnotation> localVarReturnType = new ParameterizedTypeReference<IsotopePatternAnnotation>() {};
        return getIsotopePatternAnnotationRequestCreation(projectId, alignedFeatureId, formulaId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Returns Isotope pattern information (simulated isotope pattern, measured isotope pattern, isotope pattern highlighting)  for the given formula result identifier.
     * Returns Isotope pattern information (simulated isotope pattern, measured isotope pattern, isotope pattern highlighting)  for the given formula result identifier. This simulated isotope pattern is used to rank formula candidates (treeScore).
     * <p><b>200</b> - Isotope pattern information
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseEntity&lt;IsotopePatternAnnotation&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<IsotopePatternAnnotation> getIsotopePatternAnnotationWithHttpInfo(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<IsotopePatternAnnotation> localVarReturnType = new ParameterizedTypeReference<IsotopePatternAnnotation>() {};
        return getIsotopePatternAnnotationRequestCreation(projectId, alignedFeatureId, formulaId).toEntity(localVarReturnType).block();
    }

    /**
     * Returns Isotope pattern information (simulated isotope pattern, measured isotope pattern, isotope pattern highlighting)  for the given formula result identifier.
     * Returns Isotope pattern information (simulated isotope pattern, measured isotope pattern, isotope pattern highlighting)  for the given formula result identifier. This simulated isotope pattern is used to rank formula candidates (treeScore).
     * <p><b>200</b> - Isotope pattern information
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getIsotopePatternAnnotationWithResponseSpec(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        return getIsotopePatternAnnotationRequestCreation(projectId, alignedFeatureId, formulaId);
    }
    /**
     * Returns Lipid annotation (ElGordo) for the given formula result identifier.
     * Returns Lipid annotation (ElGordo) for the given formula result identifier.  ElGordo lipid annotation runs as part of the SIRIUS formula identification step.
     * <p><b>200</b> - LipidAnnotation
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return LipidAnnotation
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getLipidAnnotationRequestCreation(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getLipidAnnotation", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getLipidAnnotation", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getLipidAnnotation", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("formulaId", formulaId);

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

        ParameterizedTypeReference<LipidAnnotation> localVarReturnType = new ParameterizedTypeReference<LipidAnnotation>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/lipid-annotation", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Returns Lipid annotation (ElGordo) for the given formula result identifier.
     * Returns Lipid annotation (ElGordo) for the given formula result identifier.  ElGordo lipid annotation runs as part of the SIRIUS formula identification step.
     * <p><b>200</b> - LipidAnnotation
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return LipidAnnotation
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public LipidAnnotation getLipidAnnotation(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<LipidAnnotation> localVarReturnType = new ParameterizedTypeReference<LipidAnnotation>() {};
        return getLipidAnnotationRequestCreation(projectId, alignedFeatureId, formulaId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Returns Lipid annotation (ElGordo) for the given formula result identifier.
     * Returns Lipid annotation (ElGordo) for the given formula result identifier.  ElGordo lipid annotation runs as part of the SIRIUS formula identification step.
     * <p><b>200</b> - LipidAnnotation
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseEntity&lt;LipidAnnotation&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<LipidAnnotation> getLipidAnnotationWithHttpInfo(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<LipidAnnotation> localVarReturnType = new ParameterizedTypeReference<LipidAnnotation>() {};
        return getLipidAnnotationRequestCreation(projectId, alignedFeatureId, formulaId).toEntity(localVarReturnType).block();
    }

    /**
     * Returns Lipid annotation (ElGordo) for the given formula result identifier.
     * Returns Lipid annotation (ElGordo) for the given formula result identifier.  ElGordo lipid annotation runs as part of the SIRIUS formula identification step.
     * <p><b>200</b> - LipidAnnotation
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getLipidAnnotationWithResponseSpec(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        return getLipidAnnotationRequestCreation(projectId, alignedFeatureId, formulaId);
    }
    /**
     * Mass Spec data (input data) for the given &#39;alignedFeatureId&#39; .
     * Mass Spec data (input data) for the given &#39;alignedFeatureId&#39; .
     * <p><b>200</b> - Mass Spec data of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the Mass Spec data belong sto.
     * @return MsData
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getMsDataRequestCreation(String projectId, String alignedFeatureId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getMsData", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getMsData", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

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

        ParameterizedTypeReference<MsData> localVarReturnType = new ParameterizedTypeReference<MsData>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/ms-data", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Mass Spec data (input data) for the given &#39;alignedFeatureId&#39; .
     * Mass Spec data (input data) for the given &#39;alignedFeatureId&#39; .
     * <p><b>200</b> - Mass Spec data of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the Mass Spec data belong sto.
     * @return MsData
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public MsData getMsData(String projectId, String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<MsData> localVarReturnType = new ParameterizedTypeReference<MsData>() {};
        return getMsDataRequestCreation(projectId, alignedFeatureId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Mass Spec data (input data) for the given &#39;alignedFeatureId&#39; .
     * Mass Spec data (input data) for the given &#39;alignedFeatureId&#39; .
     * <p><b>200</b> - Mass Spec data of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the Mass Spec data belong sto.
     * @return ResponseEntity&lt;MsData&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<MsData> getMsDataWithHttpInfo(String projectId, String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<MsData> localVarReturnType = new ParameterizedTypeReference<MsData>() {};
        return getMsDataRequestCreation(projectId, alignedFeatureId).toEntity(localVarReturnType).block();
    }

    /**
     * Mass Spec data (input data) for the given &#39;alignedFeatureId&#39; .
     * Mass Spec data (input data) for the given &#39;alignedFeatureId&#39; .
     * <p><b>200</b> - Mass Spec data of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the Mass Spec data belong sto.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getMsDataWithResponseSpec(String projectId, String alignedFeatureId) throws WebClientResponseException {
        return getMsDataRequestCreation(projectId, alignedFeatureId);
    }
    /**
     * EXPERIMENTAL: This endpoint is experimental and may be changed (or even removed) without notice until it is declared stable.
     * Returns a single quantification table row for the given feature. The quantification table contains the intensity of the feature within all  samples it is contained in.
     * <p><b>200</b> - Quant table row for this feature
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature which intensities should be read out
     * @param type quantification type. Currently, only APEX_HEIGHT is supported, which is the intensity of the feature at its apex.
     * @return QuantificationTableExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getQuantificationExperimentalRequestCreation(String projectId, String alignedFeatureId, QuantificationMeasure type) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getQuantificationExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getQuantificationExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

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

        ParameterizedTypeReference<QuantificationTableExperimental> localVarReturnType = new ParameterizedTypeReference<QuantificationTableExperimental>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/quantification", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * EXPERIMENTAL: This endpoint is experimental and may be changed (or even removed) without notice until it is declared stable.
     * Returns a single quantification table row for the given feature. The quantification table contains the intensity of the feature within all  samples it is contained in.
     * <p><b>200</b> - Quant table row for this feature
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature which intensities should be read out
     * @param type quantification type. Currently, only APEX_HEIGHT is supported, which is the intensity of the feature at its apex.
     * @return QuantificationTableExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public QuantificationTableExperimental getQuantificationExperimental(String projectId, String alignedFeatureId, QuantificationMeasure type) throws WebClientResponseException {
        ParameterizedTypeReference<QuantificationTableExperimental> localVarReturnType = new ParameterizedTypeReference<QuantificationTableExperimental>() {};
        return getQuantificationExperimentalRequestCreation(projectId, alignedFeatureId, type).bodyToMono(localVarReturnType).block();
    }

    /**
     * EXPERIMENTAL: This endpoint is experimental and may be changed (or even removed) without notice until it is declared stable.
     * Returns a single quantification table row for the given feature. The quantification table contains the intensity of the feature within all  samples it is contained in.
     * <p><b>200</b> - Quant table row for this feature
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature which intensities should be read out
     * @param type quantification type. Currently, only APEX_HEIGHT is supported, which is the intensity of the feature at its apex.
     * @return ResponseEntity&lt;QuantificationTableExperimental&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<QuantificationTableExperimental> getQuantificationExperimentalWithHttpInfo(String projectId, String alignedFeatureId, QuantificationMeasure type) throws WebClientResponseException {
        ParameterizedTypeReference<QuantificationTableExperimental> localVarReturnType = new ParameterizedTypeReference<QuantificationTableExperimental>() {};
        return getQuantificationExperimentalRequestCreation(projectId, alignedFeatureId, type).toEntity(localVarReturnType).block();
    }

    /**
     * EXPERIMENTAL: This endpoint is experimental and may be changed (or even removed) without notice until it is declared stable.
     * Returns a single quantification table row for the given feature. The quantification table contains the intensity of the feature within all  samples it is contained in.
     * <p><b>200</b> - Quant table row for this feature
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature which intensities should be read out
     * @param type quantification type. Currently, only APEX_HEIGHT is supported, which is the intensity of the feature at its apex.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getQuantificationExperimentalWithResponseSpec(String projectId, String alignedFeatureId, QuantificationMeasure type) throws WebClientResponseException {
        return getQuantificationExperimentalRequestCreation(projectId, alignedFeatureId, type);
    }
    /**
     * INTERNAL: This is an internal api endpoint and not part of the official public API. It might be changed or removed at any time
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier in SIRIUS&#39; internal format.
     * <p><b>200</b> - Fragmentation Tree in internal format.  &lt;p&gt;
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return String
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getSiriusFragTreeInternalRequestCreation(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getSiriusFragTreeInternal", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getSiriusFragTreeInternal", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getSiriusFragTreeInternal", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("formulaId", formulaId);

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

        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/sirius-fragtree", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * INTERNAL: This is an internal api endpoint and not part of the official public API. It might be changed or removed at any time
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier in SIRIUS&#39; internal format.
     * <p><b>200</b> - Fragmentation Tree in internal format.  &lt;p&gt;
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return String
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public String getSiriusFragTreeInternal(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return getSiriusFragTreeInternalRequestCreation(projectId, alignedFeatureId, formulaId).bodyToMono(localVarReturnType).block();
    }

    /**
     * INTERNAL: This is an internal api endpoint and not part of the official public API. It might be changed or removed at any time
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier in SIRIUS&#39; internal format.
     * <p><b>200</b> - Fragmentation Tree in internal format.  &lt;p&gt;
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseEntity&lt;String&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<String> getSiriusFragTreeInternalWithHttpInfo(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return getSiriusFragTreeInternalRequestCreation(projectId, alignedFeatureId, formulaId).toEntity(localVarReturnType).block();
    }

    /**
     * INTERNAL: This is an internal api endpoint and not part of the official public API. It might be changed or removed at any time
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier in SIRIUS&#39; internal format.
     * <p><b>200</b> - Fragmentation Tree in internal format.  &lt;p&gt;
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getSiriusFragTreeInternalWithResponseSpec(String projectId, String alignedFeatureId, String formulaId) throws WebClientResponseException {
        return getSiriusFragTreeInternalRequestCreation(projectId, alignedFeatureId, formulaId);
    }
    /**
     * List of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * List of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * <p><b>200</b> - Spectral library matches of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param matchId The matchId parameter
     * @param optFields The optFields parameter
     * @return SpectralLibraryMatch
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getSpectralLibraryMatchRequestCreation(String projectId, String alignedFeatureId, String matchId, List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getSpectralLibraryMatch", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getSpectralLibraryMatch", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'matchId' is set
        if (matchId == null) {
            throw new WebClientResponseException("Missing the required parameter 'matchId' when calling getSpectralLibraryMatch", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("matchId", matchId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<SpectralLibraryMatch> localVarReturnType = new ParameterizedTypeReference<SpectralLibraryMatch>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/spectral-library-matches/{matchId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * List of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * <p><b>200</b> - Spectral library matches of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param matchId The matchId parameter
     * @param optFields The optFields parameter
     * @return SpectralLibraryMatch
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public SpectralLibraryMatch getSpectralLibraryMatch(String projectId, String alignedFeatureId, String matchId, List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<SpectralLibraryMatch> localVarReturnType = new ParameterizedTypeReference<SpectralLibraryMatch>() {};
        return getSpectralLibraryMatchRequestCreation(projectId, alignedFeatureId, matchId, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * List of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * List of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * <p><b>200</b> - Spectral library matches of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param matchId The matchId parameter
     * @param optFields The optFields parameter
     * @return ResponseEntity&lt;SpectralLibraryMatch&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<SpectralLibraryMatch> getSpectralLibraryMatchWithHttpInfo(String projectId, String alignedFeatureId, String matchId, List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<SpectralLibraryMatch> localVarReturnType = new ParameterizedTypeReference<SpectralLibraryMatch>() {};
        return getSpectralLibraryMatchRequestCreation(projectId, alignedFeatureId, matchId, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * List of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * List of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * <p><b>200</b> - Spectral library matches of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param matchId The matchId parameter
     * @param optFields The optFields parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getSpectralLibraryMatchWithResponseSpec(String projectId, String alignedFeatureId, String matchId, List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        return getSpectralLibraryMatchRequestCreation(projectId, alignedFeatureId, matchId, optFields);
    }
    /**
     * List of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * List of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * <p><b>200</b> - Spectral library matches of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param minSharedPeaks The minSharedPeaks parameter
     * @param minSimilarity The minSimilarity parameter
     * @param inchiKey The inchiKey parameter
     * @param optFields The optFields parameter
     * @return List&lt;SpectralLibraryMatch&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getSpectralLibraryMatchesRequestCreation(String projectId, String alignedFeatureId, Integer minSharedPeaks, Double minSimilarity, String inchiKey, List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getSpectralLibraryMatches", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getSpectralLibraryMatches", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "minSharedPeaks", minSharedPeaks));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "minSimilarity", minSimilarity));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "inchiKey", inchiKey));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<SpectralLibraryMatch> localVarReturnType = new ParameterizedTypeReference<SpectralLibraryMatch>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/spectral-library-matches", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * List of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * <p><b>200</b> - Spectral library matches of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param minSharedPeaks The minSharedPeaks parameter
     * @param minSimilarity The minSimilarity parameter
     * @param inchiKey The inchiKey parameter
     * @param optFields The optFields parameter
     * @return List&lt;SpectralLibraryMatch&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<SpectralLibraryMatch> getSpectralLibraryMatches(String projectId, String alignedFeatureId, Integer minSharedPeaks, Double minSimilarity, String inchiKey, List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<SpectralLibraryMatch> localVarReturnType = new ParameterizedTypeReference<SpectralLibraryMatch>() {};
        return getSpectralLibraryMatchesRequestCreation(projectId, alignedFeatureId, minSharedPeaks, minSimilarity, inchiKey, optFields).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * List of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * List of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * <p><b>200</b> - Spectral library matches of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param minSharedPeaks The minSharedPeaks parameter
     * @param minSimilarity The minSimilarity parameter
     * @param inchiKey The inchiKey parameter
     * @param optFields The optFields parameter
     * @return ResponseEntity&lt;List&lt;SpectralLibraryMatch&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<SpectralLibraryMatch>> getSpectralLibraryMatchesWithHttpInfo(String projectId, String alignedFeatureId, Integer minSharedPeaks, Double minSimilarity, String inchiKey, List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<SpectralLibraryMatch> localVarReturnType = new ParameterizedTypeReference<SpectralLibraryMatch>() {};
        return getSpectralLibraryMatchesRequestCreation(projectId, alignedFeatureId, minSharedPeaks, minSimilarity, inchiKey, optFields).toEntityList(localVarReturnType).block();
    }

    /**
     * List of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * List of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * <p><b>200</b> - Spectral library matches of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param minSharedPeaks The minSharedPeaks parameter
     * @param minSimilarity The minSimilarity parameter
     * @param inchiKey The inchiKey parameter
     * @param optFields The optFields parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getSpectralLibraryMatchesWithResponseSpec(String projectId, String alignedFeatureId, Integer minSharedPeaks, Double minSimilarity, String inchiKey, List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        return getSpectralLibraryMatchesRequestCreation(projectId, alignedFeatureId, minSharedPeaks, minSimilarity, inchiKey, optFields);
    }
    /**
     * Page of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * Page of spectral library matches for the given &#39;alignedFeatureId&#39;.  If a &#39;inchiKey&#39; (2D) is provided, returns only matches for the database compound with the given InChI key.
     * <p><b>200</b> - Spectral library matches of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param minSharedPeaks The minSharedPeaks parameter
     * @param minSimilarity The minSimilarity parameter
     * @param inchiKey The inchiKey parameter
     * @param optFields The optFields parameter
     * @return PagedModelSpectralLibraryMatch
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getSpectralLibraryMatchesPagedRequestCreation(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, Integer minSharedPeaks, Double minSimilarity, String inchiKey, List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getSpectralLibraryMatchesPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getSpectralLibraryMatchesPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "sort", sort));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "minSharedPeaks", minSharedPeaks));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "minSimilarity", minSimilarity));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "inchiKey", inchiKey));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PagedModelSpectralLibraryMatch> localVarReturnType = new ParameterizedTypeReference<PagedModelSpectralLibraryMatch>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/spectral-library-matches/page", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Page of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * Page of spectral library matches for the given &#39;alignedFeatureId&#39;.  If a &#39;inchiKey&#39; (2D) is provided, returns only matches for the database compound with the given InChI key.
     * <p><b>200</b> - Spectral library matches of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param minSharedPeaks The minSharedPeaks parameter
     * @param minSimilarity The minSimilarity parameter
     * @param inchiKey The inchiKey parameter
     * @param optFields The optFields parameter
     * @return PagedModelSpectralLibraryMatch
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelSpectralLibraryMatch getSpectralLibraryMatchesPaged(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, Integer minSharedPeaks, Double minSimilarity, String inchiKey, List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelSpectralLibraryMatch> localVarReturnType = new ParameterizedTypeReference<PagedModelSpectralLibraryMatch>() {};
        return getSpectralLibraryMatchesPagedRequestCreation(projectId, alignedFeatureId, page, size, sort, minSharedPeaks, minSimilarity, inchiKey, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Page of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * Page of spectral library matches for the given &#39;alignedFeatureId&#39;.  If a &#39;inchiKey&#39; (2D) is provided, returns only matches for the database compound with the given InChI key.
     * <p><b>200</b> - Spectral library matches of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param minSharedPeaks The minSharedPeaks parameter
     * @param minSimilarity The minSimilarity parameter
     * @param inchiKey The inchiKey parameter
     * @param optFields The optFields parameter
     * @return ResponseEntity&lt;PagedModelSpectralLibraryMatch&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelSpectralLibraryMatch> getSpectralLibraryMatchesPagedWithHttpInfo(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, Integer minSharedPeaks, Double minSimilarity, String inchiKey, List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelSpectralLibraryMatch> localVarReturnType = new ParameterizedTypeReference<PagedModelSpectralLibraryMatch>() {};
        return getSpectralLibraryMatchesPagedRequestCreation(projectId, alignedFeatureId, page, size, sort, minSharedPeaks, minSimilarity, inchiKey, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Page of spectral library matches for the given &#39;alignedFeatureId&#39;.
     * Page of spectral library matches for the given &#39;alignedFeatureId&#39;.  If a &#39;inchiKey&#39; (2D) is provided, returns only matches for the database compound with the given InChI key.
     * <p><b>200</b> - Spectral library matches of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param minSharedPeaks The minSharedPeaks parameter
     * @param minSimilarity The minSimilarity parameter
     * @param inchiKey The inchiKey parameter
     * @param optFields The optFields parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getSpectralLibraryMatchesPagedWithResponseSpec(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, Integer minSharedPeaks, Double minSimilarity, String inchiKey, List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        return getSpectralLibraryMatchesPagedRequestCreation(projectId, alignedFeatureId, page, size, sort, minSharedPeaks, minSimilarity, inchiKey, optFields);
    }
    /**
     * Summarize matched reference spectra for the given &#39;alignedFeatureId&#39;.
     * Summarize matched reference spectra for the given &#39;alignedFeatureId&#39;.  If a &#39;inchiKey&#39; (2D) is provided, summarizes only contains matches for the database compound with the given InChI key.
     * <p><b>200</b> - Summary object with best match, number of spectral library matches, matched reference spectra and matched database compounds of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param minSharedPeaks min threshold of shared peaks.
     * @param minSimilarity min spectral similarity threshold.
     * @param inchiKey 2D inchi key of the compound in the structure database.
     * @return SpectralLibraryMatchSummary
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getSpectralLibraryMatchesSummaryRequestCreation(String projectId, String alignedFeatureId, Integer minSharedPeaks, Double minSimilarity, String inchiKey) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getSpectralLibraryMatchesSummary", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getSpectralLibraryMatchesSummary", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "minSharedPeaks", minSharedPeaks));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "minSimilarity", minSimilarity));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "inchiKey", inchiKey));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<SpectralLibraryMatchSummary> localVarReturnType = new ParameterizedTypeReference<SpectralLibraryMatchSummary>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/spectral-library-matches/summary", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Summarize matched reference spectra for the given &#39;alignedFeatureId&#39;.
     * Summarize matched reference spectra for the given &#39;alignedFeatureId&#39;.  If a &#39;inchiKey&#39; (2D) is provided, summarizes only contains matches for the database compound with the given InChI key.
     * <p><b>200</b> - Summary object with best match, number of spectral library matches, matched reference spectra and matched database compounds of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param minSharedPeaks min threshold of shared peaks.
     * @param minSimilarity min spectral similarity threshold.
     * @param inchiKey 2D inchi key of the compound in the structure database.
     * @return SpectralLibraryMatchSummary
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public SpectralLibraryMatchSummary getSpectralLibraryMatchesSummary(String projectId, String alignedFeatureId, Integer minSharedPeaks, Double minSimilarity, String inchiKey) throws WebClientResponseException {
        ParameterizedTypeReference<SpectralLibraryMatchSummary> localVarReturnType = new ParameterizedTypeReference<SpectralLibraryMatchSummary>() {};
        return getSpectralLibraryMatchesSummaryRequestCreation(projectId, alignedFeatureId, minSharedPeaks, minSimilarity, inchiKey).bodyToMono(localVarReturnType).block();
    }

    /**
     * Summarize matched reference spectra for the given &#39;alignedFeatureId&#39;.
     * Summarize matched reference spectra for the given &#39;alignedFeatureId&#39;.  If a &#39;inchiKey&#39; (2D) is provided, summarizes only contains matches for the database compound with the given InChI key.
     * <p><b>200</b> - Summary object with best match, number of spectral library matches, matched reference spectra and matched database compounds of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param minSharedPeaks min threshold of shared peaks.
     * @param minSimilarity min spectral similarity threshold.
     * @param inchiKey 2D inchi key of the compound in the structure database.
     * @return ResponseEntity&lt;SpectralLibraryMatchSummary&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<SpectralLibraryMatchSummary> getSpectralLibraryMatchesSummaryWithHttpInfo(String projectId, String alignedFeatureId, Integer minSharedPeaks, Double minSimilarity, String inchiKey) throws WebClientResponseException {
        ParameterizedTypeReference<SpectralLibraryMatchSummary> localVarReturnType = new ParameterizedTypeReference<SpectralLibraryMatchSummary>() {};
        return getSpectralLibraryMatchesSummaryRequestCreation(projectId, alignedFeatureId, minSharedPeaks, minSimilarity, inchiKey).toEntity(localVarReturnType).block();
    }

    /**
     * Summarize matched reference spectra for the given &#39;alignedFeatureId&#39;.
     * Summarize matched reference spectra for the given &#39;alignedFeatureId&#39;.  If a &#39;inchiKey&#39; (2D) is provided, summarizes only contains matches for the database compound with the given InChI key.
     * <p><b>200</b> - Summary object with best match, number of spectral library matches, matched reference spectra and matched database compounds of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param minSharedPeaks min threshold of shared peaks.
     * @param minSimilarity min spectral similarity threshold.
     * @param inchiKey 2D inchi key of the compound in the structure database.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getSpectralLibraryMatchesSummaryWithResponseSpec(String projectId, String alignedFeatureId, Integer minSharedPeaks, Double minSimilarity, String inchiKey) throws WebClientResponseException {
        return getSpectralLibraryMatchesSummaryRequestCreation(projectId, alignedFeatureId, minSharedPeaks, minSimilarity, inchiKey);
    }
    /**
     * EXPERIMENTAL: This endpoint is experimental because it produces return values that are not yet stable.
     * Returns MS/MS Data (Merged MS/MS and list of measured MS/MS ) which are annotated with fragments and losses  for the given formula result identifier and structure candidate inChIKey.  These annotations are only available if a fragmentation tree and the structure candidate are available.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragments and sub-structures.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param inchiKey 2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @return AnnotatedMsMsData
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getStructureAnnotatedMsDataExperimentalRequestCreation(String projectId, String alignedFeatureId, String formulaId, String inchiKey) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getStructureAnnotatedMsDataExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getStructureAnnotatedMsDataExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getStructureAnnotatedMsDataExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'inchiKey' is set
        if (inchiKey == null) {
            throw new WebClientResponseException("Missing the required parameter 'inchiKey' when calling getStructureAnnotatedMsDataExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("formulaId", formulaId);
        pathParams.put("inchiKey", inchiKey);

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

        ParameterizedTypeReference<AnnotatedMsMsData> localVarReturnType = new ParameterizedTypeReference<AnnotatedMsMsData>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/structures/{inchiKey}/annotated-msmsdata", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * EXPERIMENTAL: This endpoint is experimental because it produces return values that are not yet stable.
     * Returns MS/MS Data (Merged MS/MS and list of measured MS/MS ) which are annotated with fragments and losses  for the given formula result identifier and structure candidate inChIKey.  These annotations are only available if a fragmentation tree and the structure candidate are available.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragments and sub-structures.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param inchiKey 2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @return AnnotatedMsMsData
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public AnnotatedMsMsData getStructureAnnotatedMsDataExperimental(String projectId, String alignedFeatureId, String formulaId, String inchiKey) throws WebClientResponseException {
        ParameterizedTypeReference<AnnotatedMsMsData> localVarReturnType = new ParameterizedTypeReference<AnnotatedMsMsData>() {};
        return getStructureAnnotatedMsDataExperimentalRequestCreation(projectId, alignedFeatureId, formulaId, inchiKey).bodyToMono(localVarReturnType).block();
    }

    /**
     * EXPERIMENTAL: This endpoint is experimental because it produces return values that are not yet stable.
     * Returns MS/MS Data (Merged MS/MS and list of measured MS/MS ) which are annotated with fragments and losses  for the given formula result identifier and structure candidate inChIKey.  These annotations are only available if a fragmentation tree and the structure candidate are available.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragments and sub-structures.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param inchiKey 2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @return ResponseEntity&lt;AnnotatedMsMsData&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AnnotatedMsMsData> getStructureAnnotatedMsDataExperimentalWithHttpInfo(String projectId, String alignedFeatureId, String formulaId, String inchiKey) throws WebClientResponseException {
        ParameterizedTypeReference<AnnotatedMsMsData> localVarReturnType = new ParameterizedTypeReference<AnnotatedMsMsData>() {};
        return getStructureAnnotatedMsDataExperimentalRequestCreation(projectId, alignedFeatureId, formulaId, inchiKey).toEntity(localVarReturnType).block();
    }

    /**
     * EXPERIMENTAL: This endpoint is experimental because it produces return values that are not yet stable.
     * Returns MS/MS Data (Merged MS/MS and list of measured MS/MS ) which are annotated with fragments and losses  for the given formula result identifier and structure candidate inChIKey.  These annotations are only available if a fragmentation tree and the structure candidate are available.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragments and sub-structures.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param inchiKey 2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getStructureAnnotatedMsDataExperimentalWithResponseSpec(String projectId, String alignedFeatureId, String formulaId, String inchiKey) throws WebClientResponseException {
        return getStructureAnnotatedMsDataExperimentalRequestCreation(projectId, alignedFeatureId, formulaId, inchiKey);
    }
    /**
     * EXPERIMENTAL: This endpoint is experimental because it produces return values that are not yet stable.
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier  These annotations are only available if a fragmentation tree is available.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragments and sub-structures.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param inchiKey 2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @param spectrumIndex index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex &lt; 0 (default)
     * @return AnnotatedSpectrum
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getStructureAnnotatedSpectrumExperimentalRequestCreation(String projectId, String alignedFeatureId, String formulaId, String inchiKey, Integer spectrumIndex) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getStructureAnnotatedSpectrumExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getStructureAnnotatedSpectrumExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getStructureAnnotatedSpectrumExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'inchiKey' is set
        if (inchiKey == null) {
            throw new WebClientResponseException("Missing the required parameter 'inchiKey' when calling getStructureAnnotatedSpectrumExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("formulaId", formulaId);
        pathParams.put("inchiKey", inchiKey);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "spectrumIndex", spectrumIndex));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<AnnotatedSpectrum> localVarReturnType = new ParameterizedTypeReference<AnnotatedSpectrum>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/structures/{inchiKey}/annotated-spectrum", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * EXPERIMENTAL: This endpoint is experimental because it produces return values that are not yet stable.
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier  These annotations are only available if a fragmentation tree is available.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragments and sub-structures.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param inchiKey 2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @param spectrumIndex index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex &lt; 0 (default)
     * @return AnnotatedSpectrum
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public AnnotatedSpectrum getStructureAnnotatedSpectrumExperimental(String projectId, String alignedFeatureId, String formulaId, String inchiKey, Integer spectrumIndex) throws WebClientResponseException {
        ParameterizedTypeReference<AnnotatedSpectrum> localVarReturnType = new ParameterizedTypeReference<AnnotatedSpectrum>() {};
        return getStructureAnnotatedSpectrumExperimentalRequestCreation(projectId, alignedFeatureId, formulaId, inchiKey, spectrumIndex).bodyToMono(localVarReturnType).block();
    }

    /**
     * EXPERIMENTAL: This endpoint is experimental because it produces return values that are not yet stable.
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier  These annotations are only available if a fragmentation tree is available.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragments and sub-structures.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param inchiKey 2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @param spectrumIndex index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex &lt; 0 (default)
     * @return ResponseEntity&lt;AnnotatedSpectrum&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AnnotatedSpectrum> getStructureAnnotatedSpectrumExperimentalWithHttpInfo(String projectId, String alignedFeatureId, String formulaId, String inchiKey, Integer spectrumIndex) throws WebClientResponseException {
        ParameterizedTypeReference<AnnotatedSpectrum> localVarReturnType = new ParameterizedTypeReference<AnnotatedSpectrum>() {};
        return getStructureAnnotatedSpectrumExperimentalRequestCreation(projectId, alignedFeatureId, formulaId, inchiKey, spectrumIndex).toEntity(localVarReturnType).block();
    }

    /**
     * EXPERIMENTAL: This endpoint is experimental because it produces return values that are not yet stable.
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier  These annotations are only available if a fragmentation tree is available.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragments and sub-structures.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param inchiKey 2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @param spectrumIndex index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex &lt; 0 (default)
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getStructureAnnotatedSpectrumExperimentalWithResponseSpec(String projectId, String alignedFeatureId, String formulaId, String inchiKey, Integer spectrumIndex) throws WebClientResponseException {
        return getStructureAnnotatedSpectrumExperimentalRequestCreation(projectId, alignedFeatureId, formulaId, inchiKey, spectrumIndex);
    }
    /**
     * List of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.
     * List of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;StructureCandidateFormula&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getStructureCandidatesRequestCreation(String projectId, String alignedFeatureId, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getStructureCandidates", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getStructureCandidates", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<StructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<StructureCandidateFormula>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/db-structures", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.
     * List of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;StructureCandidateFormula&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<StructureCandidateFormula> getStructureCandidates(String projectId, String alignedFeatureId, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<StructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<StructureCandidateFormula>() {};
        return getStructureCandidatesRequestCreation(projectId, alignedFeatureId, optFields).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * List of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.
     * List of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;List&lt;StructureCandidateFormula&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<StructureCandidateFormula>> getStructureCandidatesWithHttpInfo(String projectId, String alignedFeatureId, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<StructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<StructureCandidateFormula>() {};
        return getStructureCandidatesRequestCreation(projectId, alignedFeatureId, optFields).toEntityList(localVarReturnType).block();
    }

    /**
     * List of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.
     * List of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getStructureCandidatesWithResponseSpec(String projectId, String alignedFeatureId, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        return getStructureCandidatesRequestCreation(projectId, alignedFeatureId, optFields);
    }
    /**
     * List of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.
     * List of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;StructureCandidateScored&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getStructureCandidatesByFormulaRequestCreation(String projectId, String alignedFeatureId, String formulaId, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getStructureCandidatesByFormula", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getStructureCandidatesByFormula", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getStructureCandidatesByFormula", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("formulaId", formulaId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<StructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<StructureCandidateScored>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/db-structures", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.
     * List of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;StructureCandidateScored&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<StructureCandidateScored> getStructureCandidatesByFormula(String projectId, String alignedFeatureId, String formulaId, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<StructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<StructureCandidateScored>() {};
        return getStructureCandidatesByFormulaRequestCreation(projectId, alignedFeatureId, formulaId, optFields).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * List of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.
     * List of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;List&lt;StructureCandidateScored&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<StructureCandidateScored>> getStructureCandidatesByFormulaWithHttpInfo(String projectId, String alignedFeatureId, String formulaId, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<StructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<StructureCandidateScored>() {};
        return getStructureCandidatesByFormulaRequestCreation(projectId, alignedFeatureId, formulaId, optFields).toEntityList(localVarReturnType).block();
    }

    /**
     * List of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.
     * List of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getStructureCandidatesByFormulaWithResponseSpec(String projectId, String alignedFeatureId, String formulaId, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        return getStructureCandidatesByFormulaRequestCreation(projectId, alignedFeatureId, formulaId, optFields);
    }
    /**
     * Page of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.
     * Page of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelStructureCandidateScored
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getStructureCandidatesByFormulaPagedRequestCreation(String projectId, String alignedFeatureId, String formulaId, Integer page, Integer size, List<String> sort, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getStructureCandidatesByFormulaPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getStructureCandidatesByFormulaPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getStructureCandidatesByFormulaPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
        pathParams.put("formulaId", formulaId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "sort", sort));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PagedModelStructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateScored>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/formulas/{formulaId}/db-structures/page", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Page of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.
     * Page of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelStructureCandidateScored
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelStructureCandidateScored getStructureCandidatesByFormulaPaged(String projectId, String alignedFeatureId, String formulaId, Integer page, Integer size, List<String> sort, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelStructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateScored>() {};
        return getStructureCandidatesByFormulaPagedRequestCreation(projectId, alignedFeatureId, formulaId, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Page of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.
     * Page of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PagedModelStructureCandidateScored&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelStructureCandidateScored> getStructureCandidatesByFormulaPagedWithHttpInfo(String projectId, String alignedFeatureId, String formulaId, Integer page, Integer size, List<String> sort, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelStructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateScored>() {};
        return getStructureCandidatesByFormulaPagedRequestCreation(projectId, alignedFeatureId, formulaId, page, size, sort, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Page of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.
     * Page of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getStructureCandidatesByFormulaPagedWithResponseSpec(String projectId, String alignedFeatureId, String formulaId, Integer page, Integer size, List<String> sort, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        return getStructureCandidatesByFormulaPagedRequestCreation(projectId, alignedFeatureId, formulaId, page, size, sort, optFields);
    }
    /**
     * Page of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.
     * Page of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelStructureCandidateFormula
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getStructureCandidatesPagedRequestCreation(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getStructureCandidatesPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getStructureCandidatesPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "sort", sort));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PagedModelStructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateFormula>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/db-structures/page", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Page of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.
     * Page of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelStructureCandidateFormula
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelStructureCandidateFormula getStructureCandidatesPaged(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelStructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateFormula>() {};
        return getStructureCandidatesPagedRequestCreation(projectId, alignedFeatureId, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Page of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.
     * Page of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PagedModelStructureCandidateFormula&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelStructureCandidateFormula> getStructureCandidatesPagedWithHttpInfo(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelStructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateFormula>() {};
        return getStructureCandidatesPagedRequestCreation(projectId, alignedFeatureId, page, size, sort, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Page of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.
     * Page of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getStructureCandidatesPagedWithResponseSpec(String projectId, String alignedFeatureId, Integer page, Integer size, List<String> sort, List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        return getStructureCandidatesPagedRequestCreation(projectId, alignedFeatureId, page, size, sort, optFields);
    }
    /**
     * EXPERIMENTAL: This endpoint is experimental and may be changed (or even removed) without notice until it is declared stable.
     * Returns the traces of the given feature. A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.  By default, this method only returns traces of samples the aligned feature appears in. When includeAll is set,  it also includes samples in which the same trace appears in.
     * <p><b>200</b> - Traces of the given feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature which intensities should be read out
     * @param includeAll when true, return all samples that belong to the same merged trace. when false, only return samples which contain the aligned feature.
     * @return TraceSetExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getTracesExperimentalRequestCreation(String projectId, String alignedFeatureId, Boolean includeAll) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getTracesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getTracesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "includeAll", includeAll));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<TraceSetExperimental> localVarReturnType = new ParameterizedTypeReference<TraceSetExperimental>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/traces", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * EXPERIMENTAL: This endpoint is experimental and may be changed (or even removed) without notice until it is declared stable.
     * Returns the traces of the given feature. A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.  By default, this method only returns traces of samples the aligned feature appears in. When includeAll is set,  it also includes samples in which the same trace appears in.
     * <p><b>200</b> - Traces of the given feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature which intensities should be read out
     * @param includeAll when true, return all samples that belong to the same merged trace. when false, only return samples which contain the aligned feature.
     * @return TraceSetExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public TraceSetExperimental getTracesExperimental(String projectId, String alignedFeatureId, Boolean includeAll) throws WebClientResponseException {
        ParameterizedTypeReference<TraceSetExperimental> localVarReturnType = new ParameterizedTypeReference<TraceSetExperimental>() {};
        return getTracesExperimentalRequestCreation(projectId, alignedFeatureId, includeAll).bodyToMono(localVarReturnType).block();
    }

    /**
     * EXPERIMENTAL: This endpoint is experimental and may be changed (or even removed) without notice until it is declared stable.
     * Returns the traces of the given feature. A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.  By default, this method only returns traces of samples the aligned feature appears in. When includeAll is set,  it also includes samples in which the same trace appears in.
     * <p><b>200</b> - Traces of the given feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature which intensities should be read out
     * @param includeAll when true, return all samples that belong to the same merged trace. when false, only return samples which contain the aligned feature.
     * @return ResponseEntity&lt;TraceSetExperimental&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<TraceSetExperimental> getTracesExperimentalWithHttpInfo(String projectId, String alignedFeatureId, Boolean includeAll) throws WebClientResponseException {
        ParameterizedTypeReference<TraceSetExperimental> localVarReturnType = new ParameterizedTypeReference<TraceSetExperimental>() {};
        return getTracesExperimentalRequestCreation(projectId, alignedFeatureId, includeAll).toEntity(localVarReturnType).block();
    }

    /**
     * EXPERIMENTAL: This endpoint is experimental and may be changed (or even removed) without notice until it is declared stable.
     * Returns the traces of the given feature. A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.  By default, this method only returns traces of samples the aligned feature appears in. When includeAll is set,  it also includes samples in which the same trace appears in.
     * <p><b>200</b> - Traces of the given feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature which intensities should be read out
     * @param includeAll when true, return all samples that belong to the same merged trace. when false, only return samples which contain the aligned feature.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getTracesExperimentalWithResponseSpec(String projectId, String alignedFeatureId, Boolean includeAll) throws WebClientResponseException {
        return getTracesExperimentalRequestCreation(projectId, alignedFeatureId, includeAll);
    }
}
