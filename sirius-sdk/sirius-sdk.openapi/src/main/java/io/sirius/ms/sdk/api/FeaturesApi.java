package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import io.sirius.ms.sdk.model.AlignedFeature;
import io.sirius.ms.sdk.model.AlignedFeatureOptField;
import io.sirius.ms.sdk.model.AlignedFeatureQualityExperimental;
import io.sirius.ms.sdk.model.AnnotatedMsMsData;
import io.sirius.ms.sdk.model.AnnotatedSpectrum;
import io.sirius.ms.sdk.model.CanopusPrediction;
import io.sirius.ms.sdk.model.CompoundClasses;
import io.sirius.ms.sdk.model.Feature;
import io.sirius.ms.sdk.model.FeatureImport;
import io.sirius.ms.sdk.model.FormulaCandidate;
import io.sirius.ms.sdk.model.FormulaCandidateOptField;
import io.sirius.ms.sdk.model.FragmentationTree;
import io.sirius.ms.sdk.model.InstrumentProfile;
import io.sirius.ms.sdk.model.IsotopePatternAnnotation;
import io.sirius.ms.sdk.model.LipidAnnotation;
import io.sirius.ms.sdk.model.MsData;
import io.sirius.ms.sdk.model.PagedModelAlignedFeature;
import io.sirius.ms.sdk.model.PagedModelFeature;
import io.sirius.ms.sdk.model.PagedModelFormulaCandidate;
import io.sirius.ms.sdk.model.PagedModelSpectralLibraryMatch;
import io.sirius.ms.sdk.model.PagedModelStructureCandidateFormula;
import io.sirius.ms.sdk.model.PagedModelStructureCandidateScored;
import io.sirius.ms.sdk.model.QuantMeasure;
import io.sirius.ms.sdk.model.QuantTable;
import io.sirius.ms.sdk.model.QuantTableOptField;
import io.sirius.ms.sdk.model.SearchableField;
import io.sirius.ms.sdk.model.SpectralLibraryMatch;
import io.sirius.ms.sdk.model.SpectralLibraryMatchOptField;
import io.sirius.ms.sdk.model.SpectralLibraryMatchSummary;
import io.sirius.ms.sdk.model.StructureCandidateFormula;
import io.sirius.ms.sdk.model.StructureCandidateOptField;
import io.sirius.ms.sdk.model.StructureCandidateScored;
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
public class FeaturesApi {
    private ApiClient apiClient;

    public FeaturesApi() {
        this(new ApiClient());
    }

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
    private ResponseSpec addAlignedFeaturesRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<FeatureImport> featureImport, @jakarta.annotation.Nullable InstrumentProfile profile, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
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
    public List<AlignedFeature> addAlignedFeatures(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<FeatureImport> featureImport, @jakarta.annotation.Nullable InstrumentProfile profile, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
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
    public ResponseEntity<List<AlignedFeature>> addAlignedFeaturesWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<FeatureImport> featureImport, @jakarta.annotation.Nullable InstrumentProfile profile, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
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
    public ResponseSpec addAlignedFeaturesWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<FeatureImport> featureImport, @jakarta.annotation.Nullable InstrumentProfile profile, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        return addAlignedFeaturesRequestCreation(projectId, featureImport, profile, optFields);
    }

    /**
     * [INTERNAL] Add molecular structures (as SMILES) to the list of de novo structures.
     * [INTERNAL] Add molecular structures (as SMILES) to the list of de novo structures. This starts a scoring job to incorporate the structures in the de novo results list.
     * <p><b>200</b> - StructureCandidate of this feature candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param smiles smiles
     * @param skipExistenceCheck if true, skips a check if this compound is an existing candidate, potentially leading to duplicate structures.
     * @return List&lt;StructureCandidateFormula&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addDeNovoStructureCandidateRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable String smiles, @jakarta.annotation.Nullable Boolean skipExistenceCheck) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addDeNovoStructureCandidate", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling addDeNovoStructureCandidate", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "smiles", smiles));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "skipExistenceCheck", skipExistenceCheck));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<StructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<StructureCandidateFormula>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/denovo-structures/add-candidate", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [INTERNAL] Add molecular structures (as SMILES) to the list of de novo structures.
     * [INTERNAL] Add molecular structures (as SMILES) to the list of de novo structures. This starts a scoring job to incorporate the structures in the de novo results list.
     * <p><b>200</b> - StructureCandidate of this feature candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param smiles smiles
     * @param skipExistenceCheck if true, skips a check if this compound is an existing candidate, potentially leading to duplicate structures.
     * @return List&lt;StructureCandidateFormula&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<StructureCandidateFormula> addDeNovoStructureCandidate(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable String smiles, @jakarta.annotation.Nullable Boolean skipExistenceCheck) throws WebClientResponseException {
        ParameterizedTypeReference<StructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<StructureCandidateFormula>() {};
        return addDeNovoStructureCandidateRequestCreation(projectId, alignedFeatureId, smiles, skipExistenceCheck).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [INTERNAL] Add molecular structures (as SMILES) to the list of de novo structures.
     * [INTERNAL] Add molecular structures (as SMILES) to the list of de novo structures. This starts a scoring job to incorporate the structures in the de novo results list.
     * <p><b>200</b> - StructureCandidate of this feature candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param smiles smiles
     * @param skipExistenceCheck if true, skips a check if this compound is an existing candidate, potentially leading to duplicate structures.
     * @return ResponseEntity&lt;List&lt;StructureCandidateFormula&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<StructureCandidateFormula>> addDeNovoStructureCandidateWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable String smiles, @jakarta.annotation.Nullable Boolean skipExistenceCheck) throws WebClientResponseException {
        ParameterizedTypeReference<StructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<StructureCandidateFormula>() {};
        return addDeNovoStructureCandidateRequestCreation(projectId, alignedFeatureId, smiles, skipExistenceCheck).toEntityList(localVarReturnType).block();
    }

    /**
     * [INTERNAL] Add molecular structures (as SMILES) to the list of de novo structures.
     * [INTERNAL] Add molecular structures (as SMILES) to the list of de novo structures. This starts a scoring job to incorporate the structures in the de novo results list.
     * <p><b>200</b> - StructureCandidate of this feature candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param smiles smiles
     * @param skipExistenceCheck if true, skips a check if this compound is an existing candidate, potentially leading to duplicate structures.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addDeNovoStructureCandidateWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable String smiles, @jakarta.annotation.Nullable Boolean skipExistenceCheck) throws WebClientResponseException {
        return addDeNovoStructureCandidateRequestCreation(projectId, alignedFeatureId, smiles, skipExistenceCheck);
    }

    /**
     * [EXPERIMENTAL] Add tags to a feature (aligned over runs) in the project
     * [EXPERIMENTAL] Add tags to a feature (aligned over runs) in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param alignedFeatureId feature to add tags to.
     * @param tag tags to add.
     * @return List&lt;Tag&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addTagsToAlignedFeatureExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull List<Tag> tag) throws WebClientResponseException {
        Object postBody = tag;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addTagsToAlignedFeatureExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling addTagsToAlignedFeatureExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tag' is set
        if (tag == null) {
            throw new WebClientResponseException("Missing the required parameter 'tag' when calling addTagsToAlignedFeatureExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/tags/{alignedFeatureId}", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Add tags to a feature (aligned over runs) in the project
     * [EXPERIMENTAL] Add tags to a feature (aligned over runs) in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param alignedFeatureId feature to add tags to.
     * @param tag tags to add.
     * @return List&lt;Tag&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Tag> addTagsToAlignedFeatureExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull List<Tag> tag) throws WebClientResponseException {
        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return addTagsToAlignedFeatureExperimentalRequestCreation(projectId, alignedFeatureId, tag).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [EXPERIMENTAL] Add tags to a feature (aligned over runs) in the project
     * [EXPERIMENTAL] Add tags to a feature (aligned over runs) in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param alignedFeatureId feature to add tags to.
     * @param tag tags to add.
     * @return ResponseEntity&lt;List&lt;Tag&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Tag>> addTagsToAlignedFeatureExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull List<Tag> tag) throws WebClientResponseException {
        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return addTagsToAlignedFeatureExperimentalRequestCreation(projectId, alignedFeatureId, tag).toEntityList(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Add tags to a feature (aligned over runs) in the project
     * [EXPERIMENTAL] Add tags to a feature (aligned over runs) in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - the tags that have been added
     * @param projectId project-space to add to.
     * @param alignedFeatureId feature to add tags to.
     * @param tag tags to add.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addTagsToAlignedFeatureExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull List<Tag> tag) throws WebClientResponseException {
        return addTagsToAlignedFeatureExperimentalRequestCreation(projectId, alignedFeatureId, tag);
    }

    /**
     * [EXPERIMENTAL] Add tags to a feature (aligned over runs) in the project
     * [EXPERIMENTAL] Add tags to a feature (aligned over runs) in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to add to.
     * @param tagSubmission tags with the id of feature they shall be added to.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec addTagsToAlignedFeaturesExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<TagSubmission> tagSubmission) throws WebClientResponseException {
        Object postBody = tagSubmission;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling addTagsToAlignedFeaturesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tagSubmission' is set
        if (tagSubmission == null) {
            throw new WebClientResponseException("Missing the required parameter 'tagSubmission' when calling addTagsToAlignedFeaturesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/tags", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Add tags to a feature (aligned over runs) in the project
     * [EXPERIMENTAL] Add tags to a feature (aligned over runs) in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to add to.
     * @param tagSubmission tags with the id of feature they shall be added to.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void addTagsToAlignedFeaturesExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<TagSubmission> tagSubmission) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        addTagsToAlignedFeaturesExperimentalRequestCreation(projectId, tagSubmission).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Add tags to a feature (aligned over runs) in the project
     * [EXPERIMENTAL] Add tags to a feature (aligned over runs) in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to add to.
     * @param tagSubmission tags with the id of feature they shall be added to.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> addTagsToAlignedFeaturesExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<TagSubmission> tagSubmission) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return addTagsToAlignedFeaturesExperimentalRequestCreation(projectId, tagSubmission).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Add tags to a feature (aligned over runs) in the project
     * [EXPERIMENTAL] Add tags to a feature (aligned over runs) in the project. Tags with the same name will be overwritten.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to add to.
     * @param tagSubmission tags with the id of feature they shall be added to.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec addTagsToAlignedFeaturesExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<TagSubmission> tagSubmission) throws WebClientResponseException {
        return addTagsToAlignedFeaturesExperimentalRequestCreation(projectId, tagSubmission);
    }

    /**
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * Delete feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteAlignedFeatureRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId) throws WebClientResponseException {
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
    public void deleteAlignedFeature(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId) throws WebClientResponseException {
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
    public ResponseEntity<Void> deleteAlignedFeatureWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId) throws WebClientResponseException {
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
    public ResponseSpec deleteAlignedFeatureWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId) throws WebClientResponseException {
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
    private ResponseSpec deleteAlignedFeaturesRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<String> requestBody) throws WebClientResponseException {
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
    public void deleteAlignedFeatures(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<String> requestBody) throws WebClientResponseException {
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
    public ResponseEntity<Void> deleteAlignedFeaturesWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<String> requestBody) throws WebClientResponseException {
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
    public ResponseSpec deleteAlignedFeaturesWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull List<String> requestBody) throws WebClientResponseException {
        return deleteAlignedFeaturesRequestCreation(projectId, requestBody);
    }

    /**
     * Delete all features (aligned over runs) that match the given lucene search query from the specified  project-space.
     * Delete all features (aligned over runs) that match the given lucene search query from the specified  project-space. The query is resolved server-side against the search index, so callers do not need to  page the matching ids to the client and send them back for deletion.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param searchQuery tag/text/range query in lucene syntax; must be non-empty (a blank query would match                     every feature).
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteAlignedFeaturesByQueryExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String searchQuery) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteAlignedFeaturesByQueryExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'searchQuery' is set
        if (searchQuery == null) {
            throw new WebClientResponseException("Missing the required parameter 'searchQuery' when calling deleteAlignedFeaturesByQueryExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "searchQuery", searchQuery));

        final String[] localVarAccepts = { };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/delete-by-query", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete all features (aligned over runs) that match the given lucene search query from the specified  project-space.
     * Delete all features (aligned over runs) that match the given lucene search query from the specified  project-space. The query is resolved server-side against the search index, so callers do not need to  page the matching ids to the client and send them back for deletion.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param searchQuery tag/text/range query in lucene syntax; must be non-empty (a blank query would match                     every feature).
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteAlignedFeaturesByQueryExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String searchQuery) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteAlignedFeaturesByQueryExperimentalRequestCreation(projectId, searchQuery).bodyToMono(localVarReturnType).block();
    }

    /**
     * Delete all features (aligned over runs) that match the given lucene search query from the specified  project-space.
     * Delete all features (aligned over runs) that match the given lucene search query from the specified  project-space. The query is resolved server-side against the search index, so callers do not need to  page the matching ids to the client and send them back for deletion.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param searchQuery tag/text/range query in lucene syntax; must be non-empty (a blank query would match                     every feature).
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteAlignedFeaturesByQueryExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String searchQuery) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteAlignedFeaturesByQueryExperimentalRequestCreation(projectId, searchQuery).toEntity(localVarReturnType).block();
    }

    /**
     * Delete all features (aligned over runs) that match the given lucene search query from the specified  project-space.
     * Delete all features (aligned over runs) that match the given lucene search query from the specified  project-space. The query is resolved server-side against the search index, so callers do not need to  page the matching ids to the client and send them back for deletion.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param searchQuery tag/text/range query in lucene syntax; must be non-empty (a blank query would match                     every feature).
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteAlignedFeaturesByQueryExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String searchQuery) throws WebClientResponseException {
        return deleteAlignedFeaturesByQueryExperimentalRequestCreation(projectId, searchQuery);
    }

    /**
     * [EXPERIMENTAL] Returns the adduct network for a given alignedFeatureId together with all merged traces contained in the network
     * [EXPERIMENTAL] Returns the adduct network for a given alignedFeatureId together with all merged traces contained in the network.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param alignedFeatureId one feature that is considered the main feature of the adduct network
     * @return TraceSetExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAdductNetworkWithMergedTracesExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getAdductNetworkWithMergedTracesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getAdductNetworkWithMergedTracesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
     * [EXPERIMENTAL] Returns the adduct network for a given alignedFeatureId together with all merged traces contained in the network
     * [EXPERIMENTAL] Returns the adduct network for a given alignedFeatureId together with all merged traces contained in the network.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param alignedFeatureId one feature that is considered the main feature of the adduct network
     * @return TraceSetExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public TraceSetExperimental getAdductNetworkWithMergedTracesExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<TraceSetExperimental> localVarReturnType = new ParameterizedTypeReference<TraceSetExperimental>() {};
        return getAdductNetworkWithMergedTracesExperimentalRequestCreation(projectId, alignedFeatureId).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Returns the adduct network for a given alignedFeatureId together with all merged traces contained in the network
     * [EXPERIMENTAL] Returns the adduct network for a given alignedFeatureId together with all merged traces contained in the network.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param alignedFeatureId one feature that is considered the main feature of the adduct network
     * @return ResponseEntity&lt;TraceSetExperimental&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<TraceSetExperimental> getAdductNetworkWithMergedTracesExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<TraceSetExperimental> localVarReturnType = new ParameterizedTypeReference<TraceSetExperimental>() {};
        return getAdductNetworkWithMergedTracesExperimentalRequestCreation(projectId, alignedFeatureId).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Returns the adduct network for a given alignedFeatureId together with all merged traces contained in the network
     * [EXPERIMENTAL] Returns the adduct network for a given alignedFeatureId together with all merged traces contained in the network.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param alignedFeatureId one feature that is considered the main feature of the adduct network
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAdductNetworkWithMergedTracesExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId) throws WebClientResponseException {
        return getAdductNetworkWithMergedTracesExperimentalRequestCreation(projectId, alignedFeatureId);
    }

    /**
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeature with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return AlignedFeature
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAlignedFeatureRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
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

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "msDataSearchPrepared", msDataSearchPrepared));
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
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return AlignedFeature
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public AlignedFeature getAlignedFeature(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeature> localVarReturnType = new ParameterizedTypeReference<AlignedFeature>() {};
        return getAlignedFeatureRequestCreation(projectId, alignedFeatureId, msDataSearchPrepared, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeature with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;AlignedFeature&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AlignedFeature> getAlignedFeatureWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeature> localVarReturnType = new ParameterizedTypeReference<AlignedFeature>() {};
        return getAlignedFeatureRequestCreation(projectId, alignedFeatureId, msDataSearchPrepared, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * Get feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeature with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAlignedFeatureWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        return getAlignedFeatureRequestCreation(projectId, alignedFeatureId, msDataSearchPrepared, optFields);
    }

    /**
     * [EXPERIMENTAL] Returns data quality information for given feature (alignedFeatureId)  
     * [EXPERIMENTAL] Returns data quality information for given feature (alignedFeatureId)  &lt;p&gt;  Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @return AlignedFeatureQualityExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAlignedFeatureQualityExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getAlignedFeatureQualityExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getAlignedFeatureQualityExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
     * [EXPERIMENTAL] Returns data quality information for given feature (alignedFeatureId)  
     * [EXPERIMENTAL] Returns data quality information for given feature (alignedFeatureId)  &lt;p&gt;  Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @return AlignedFeatureQualityExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public AlignedFeatureQualityExperimental getAlignedFeatureQualityExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeatureQualityExperimental> localVarReturnType = new ParameterizedTypeReference<AlignedFeatureQualityExperimental>() {};
        return getAlignedFeatureQualityExperimentalRequestCreation(projectId, alignedFeatureId).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Returns data quality information for given feature (alignedFeatureId)  
     * [EXPERIMENTAL] Returns data quality information for given feature (alignedFeatureId)  &lt;p&gt;  Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @return ResponseEntity&lt;AlignedFeatureQualityExperimental&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AlignedFeatureQualityExperimental> getAlignedFeatureQualityExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeatureQualityExperimental> localVarReturnType = new ParameterizedTypeReference<AlignedFeatureQualityExperimental>() {};
        return getAlignedFeatureQualityExperimentalRequestCreation(projectId, alignedFeatureId).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Returns data quality information for given feature (alignedFeatureId)  
     * [EXPERIMENTAL] Returns data quality information for given feature (alignedFeatureId)  &lt;p&gt;  Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAlignedFeatureQualityExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId) throws WebClientResponseException {
        return getAlignedFeatureQualityExperimentalRequestCreation(projectId, alignedFeatureId);
    }

    /**
     * [DEPRECATED] Get all available features (aligned over runs) in the given project-space
     * [DEPRECATED] Get all available features (aligned over runs) in the given project-space.  &lt;p&gt;  [DEPRECATED] Use /aligned-features/page instead. Loading all features at once does not scale for large  projects. This endpoint will be removed in the next major version of this API.
     * <p><b>200</b> - AlignedFeatures with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;AlignedFeature&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec getAlignedFeaturesRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
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

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "msDataSearchPrepared", msDataSearchPrepared));
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
     * [DEPRECATED] Get all available features (aligned over runs) in the given project-space
     * [DEPRECATED] Get all available features (aligned over runs) in the given project-space.  &lt;p&gt;  [DEPRECATED] Use /aligned-features/page instead. Loading all features at once does not scale for large  projects. This endpoint will be removed in the next major version of this API.
     * <p><b>200</b> - AlignedFeatures with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;AlignedFeature&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<AlignedFeature> getAlignedFeatures(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeature> localVarReturnType = new ParameterizedTypeReference<AlignedFeature>() {};
        return getAlignedFeaturesRequestCreation(projectId, msDataSearchPrepared, optFields).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [DEPRECATED] Get all available features (aligned over runs) in the given project-space
     * [DEPRECATED] Get all available features (aligned over runs) in the given project-space.  &lt;p&gt;  [DEPRECATED] Use /aligned-features/page instead. Loading all features at once does not scale for large  projects. This endpoint will be removed in the next major version of this API.
     * <p><b>200</b> - AlignedFeatures with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;List&lt;AlignedFeature&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<AlignedFeature>> getAlignedFeaturesWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeature> localVarReturnType = new ParameterizedTypeReference<AlignedFeature>() {};
        return getAlignedFeaturesRequestCreation(projectId, msDataSearchPrepared, optFields).toEntityList(localVarReturnType).block();
    }

    /**
     * [DEPRECATED] Get all available features (aligned over runs) in the given project-space
     * [DEPRECATED] Get all available features (aligned over runs) in the given project-space.  &lt;p&gt;  [DEPRECATED] Use /aligned-features/page instead. Loading all features at once does not scale for large  projects. This endpoint will be removed in the next major version of this API.
     * <p><b>200</b> - AlignedFeatures with additional annotations and MS/MS data (if specified).
     * @param projectId project-space to read from.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAlignedFeaturesWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        return getAlignedFeaturesRequestCreation(projectId, msDataSearchPrepared, optFields);
    }

    /**
     * [EXPERIMENTAL] Get features (aligned over runs) by tag group
     * [EXPERIMENTAL] Get features (aligned over runs) by tag group.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - tagged features (aligned over runs)
     * @param projectId project-space to delete from.
     * @param groupName tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelAlignedFeature
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAlignedFeaturesByGroupExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String groupName, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getAlignedFeaturesByGroupExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'groupName' is set
        if (groupName == null) {
            throw new WebClientResponseException("Missing the required parameter 'groupName' when calling getAlignedFeaturesByGroupExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "msDataSearchPrepared", msDataSearchPrepared));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PagedModelAlignedFeature> localVarReturnType = new ParameterizedTypeReference<PagedModelAlignedFeature>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/grouped", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Get features (aligned over runs) by tag group
     * [EXPERIMENTAL] Get features (aligned over runs) by tag group.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - tagged features (aligned over runs)
     * @param projectId project-space to delete from.
     * @param groupName tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelAlignedFeature
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelAlignedFeature getAlignedFeaturesByGroupExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String groupName, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelAlignedFeature> localVarReturnType = new ParameterizedTypeReference<PagedModelAlignedFeature>() {};
        return getAlignedFeaturesByGroupExperimentalRequestCreation(projectId, groupName, page, size, sort, msDataSearchPrepared, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get features (aligned over runs) by tag group
     * [EXPERIMENTAL] Get features (aligned over runs) by tag group.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - tagged features (aligned over runs)
     * @param projectId project-space to delete from.
     * @param groupName tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PagedModelAlignedFeature&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelAlignedFeature> getAlignedFeaturesByGroupExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String groupName, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelAlignedFeature> localVarReturnType = new ParameterizedTypeReference<PagedModelAlignedFeature>() {};
        return getAlignedFeaturesByGroupExperimentalRequestCreation(projectId, groupName, page, size, sort, msDataSearchPrepared, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get features (aligned over runs) by tag group
     * [EXPERIMENTAL] Get features (aligned over runs) by tag group.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - tagged features (aligned over runs)
     * @param projectId project-space to delete from.
     * @param groupName tag group name.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAlignedFeaturesByGroupExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String groupName, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        return getAlignedFeaturesByGroupExperimentalRequestCreation(projectId, groupName, page, size, sort, msDataSearchPrepared, optFields);
    }

    /**
     * Get features (aligned over runs) in the given project-space
     * Get features (aligned over runs) in the given project-space.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name.  &lt;/p&gt;   Searchable fields are the indexed properties of the feature (e.g. &lt;code&gt;ionMass&lt;/code&gt;, &lt;code&gt;name&lt;/code&gt;,  &lt;code&gt;quality&lt;/code&gt;, &lt;code&gt;hasMsMs&lt;/code&gt;), its annotations addressed via dot notation  (e.g. &lt;code&gt;topAnnotations.structureAnnotation.structureName&lt;/code&gt;), and project tags prefixed with the  namespace &lt;code&gt;tags.&lt;/code&gt; (e.g. &lt;code&gt;tags.MyTag&lt;/code&gt;).  Use the &lt;code&gt;searchable-fields&lt;/code&gt; endpoint (getAlignedFeaturesSearchableFields) to list all fields that  can be searched, including their value type, whether they support word based (full text) search, and  whether results can be sorted by them.  Possible value types are &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;double&lt;/strong&gt;,  &lt;strong&gt;boolean&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt;.   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;tags.MyTagA:sample&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;name:&amp;quot;Bicuculline methiodide&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;tags.MyTagA:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;ionMass&amp;lt;300&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;ionMass:[300 TO 400]&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;boolean&lt;/strong&gt;: boolean fields are matched as follows: e.g. &lt;code&gt;hasMsMs:true&lt;/code&gt;, &lt;code&gt;tags.MyTagA:false&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;value-less&lt;/strong&gt;: tags without values (See TagDefinition) are matched as follows: e.g. &lt;code&gt;tags.MyTagA:*&lt;/code&gt; or &lt;code&gt;tags.MyTagA:true&lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;ionMass:[300 TO 400] AND quality:GOOD AND topAnnotations.compoundClassAnnotation.npcPathway:&amp;quot;Alkaloids&amp;quot; AND tags.city:&amp;quot;new york&amp;quot; OR tags.MyDateTag:[2023-10-01 TO 2023-12-24] OR tags.time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;
     * <p><b>200</b> - tagged features (aligned over runs)
     * @param projectId project space to get features (aligned over runs) from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery Optional search query in lucene syntax. Omit this parameter to page over all features.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelAlignedFeature
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAlignedFeaturesPageRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getAlignedFeaturesPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "searchQuery", searchQuery));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "msDataSearchPrepared", msDataSearchPrepared));
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
     * Get features (aligned over runs) in the given project-space
     * Get features (aligned over runs) in the given project-space.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name.  &lt;/p&gt;   Searchable fields are the indexed properties of the feature (e.g. &lt;code&gt;ionMass&lt;/code&gt;, &lt;code&gt;name&lt;/code&gt;,  &lt;code&gt;quality&lt;/code&gt;, &lt;code&gt;hasMsMs&lt;/code&gt;), its annotations addressed via dot notation  (e.g. &lt;code&gt;topAnnotations.structureAnnotation.structureName&lt;/code&gt;), and project tags prefixed with the  namespace &lt;code&gt;tags.&lt;/code&gt; (e.g. &lt;code&gt;tags.MyTag&lt;/code&gt;).  Use the &lt;code&gt;searchable-fields&lt;/code&gt; endpoint (getAlignedFeaturesSearchableFields) to list all fields that  can be searched, including their value type, whether they support word based (full text) search, and  whether results can be sorted by them.  Possible value types are &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;double&lt;/strong&gt;,  &lt;strong&gt;boolean&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt;.   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;tags.MyTagA:sample&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;name:&amp;quot;Bicuculline methiodide&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;tags.MyTagA:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;ionMass&amp;lt;300&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;ionMass:[300 TO 400]&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;boolean&lt;/strong&gt;: boolean fields are matched as follows: e.g. &lt;code&gt;hasMsMs:true&lt;/code&gt;, &lt;code&gt;tags.MyTagA:false&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;value-less&lt;/strong&gt;: tags without values (See TagDefinition) are matched as follows: e.g. &lt;code&gt;tags.MyTagA:*&lt;/code&gt; or &lt;code&gt;tags.MyTagA:true&lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;ionMass:[300 TO 400] AND quality:GOOD AND topAnnotations.compoundClassAnnotation.npcPathway:&amp;quot;Alkaloids&amp;quot; AND tags.city:&amp;quot;new york&amp;quot; OR tags.MyDateTag:[2023-10-01 TO 2023-12-24] OR tags.time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;
     * <p><b>200</b> - tagged features (aligned over runs)
     * @param projectId project space to get features (aligned over runs) from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery Optional search query in lucene syntax. Omit this parameter to page over all features.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelAlignedFeature
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelAlignedFeature getAlignedFeaturesPage(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelAlignedFeature> localVarReturnType = new ParameterizedTypeReference<PagedModelAlignedFeature>() {};
        return getAlignedFeaturesPageRequestCreation(projectId, page, size, sort, searchQuery, msDataSearchPrepared, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get features (aligned over runs) in the given project-space
     * Get features (aligned over runs) in the given project-space.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name.  &lt;/p&gt;   Searchable fields are the indexed properties of the feature (e.g. &lt;code&gt;ionMass&lt;/code&gt;, &lt;code&gt;name&lt;/code&gt;,  &lt;code&gt;quality&lt;/code&gt;, &lt;code&gt;hasMsMs&lt;/code&gt;), its annotations addressed via dot notation  (e.g. &lt;code&gt;topAnnotations.structureAnnotation.structureName&lt;/code&gt;), and project tags prefixed with the  namespace &lt;code&gt;tags.&lt;/code&gt; (e.g. &lt;code&gt;tags.MyTag&lt;/code&gt;).  Use the &lt;code&gt;searchable-fields&lt;/code&gt; endpoint (getAlignedFeaturesSearchableFields) to list all fields that  can be searched, including their value type, whether they support word based (full text) search, and  whether results can be sorted by them.  Possible value types are &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;double&lt;/strong&gt;,  &lt;strong&gt;boolean&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt;.   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;tags.MyTagA:sample&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;name:&amp;quot;Bicuculline methiodide&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;tags.MyTagA:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;ionMass&amp;lt;300&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;ionMass:[300 TO 400]&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;boolean&lt;/strong&gt;: boolean fields are matched as follows: e.g. &lt;code&gt;hasMsMs:true&lt;/code&gt;, &lt;code&gt;tags.MyTagA:false&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;value-less&lt;/strong&gt;: tags without values (See TagDefinition) are matched as follows: e.g. &lt;code&gt;tags.MyTagA:*&lt;/code&gt; or &lt;code&gt;tags.MyTagA:true&lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;ionMass:[300 TO 400] AND quality:GOOD AND topAnnotations.compoundClassAnnotation.npcPathway:&amp;quot;Alkaloids&amp;quot; AND tags.city:&amp;quot;new york&amp;quot; OR tags.MyDateTag:[2023-10-01 TO 2023-12-24] OR tags.time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;
     * <p><b>200</b> - tagged features (aligned over runs)
     * @param projectId project space to get features (aligned over runs) from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery Optional search query in lucene syntax. Omit this parameter to page over all features.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PagedModelAlignedFeature&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelAlignedFeature> getAlignedFeaturesPageWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelAlignedFeature> localVarReturnType = new ParameterizedTypeReference<PagedModelAlignedFeature>() {};
        return getAlignedFeaturesPageRequestCreation(projectId, page, size, sort, searchQuery, msDataSearchPrepared, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Get features (aligned over runs) in the given project-space
     * Get features (aligned over runs) in the given project-space.   &lt;h2&gt;Supported filter syntax&lt;/h2&gt;   &lt;p&gt;The filter string must contain one or more clauses. A clause is prefíxed  by a field name.  &lt;/p&gt;   Searchable fields are the indexed properties of the feature (e.g. &lt;code&gt;ionMass&lt;/code&gt;, &lt;code&gt;name&lt;/code&gt;,  &lt;code&gt;quality&lt;/code&gt;, &lt;code&gt;hasMsMs&lt;/code&gt;), its annotations addressed via dot notation  (e.g. &lt;code&gt;topAnnotations.structureAnnotation.structureName&lt;/code&gt;), and project tags prefixed with the  namespace &lt;code&gt;tags.&lt;/code&gt; (e.g. &lt;code&gt;tags.MyTag&lt;/code&gt;).  Use the &lt;code&gt;searchable-fields&lt;/code&gt; endpoint (getAlignedFeaturesSearchableFields) to list all fields that  can be searched, including their value type, whether they support word based (full text) search, and  whether results can be sorted by them.  Possible value types are &lt;strong&gt;text&lt;/strong&gt;, &lt;strong&gt;integer&lt;/strong&gt;, &lt;strong&gt;double&lt;/strong&gt;,  &lt;strong&gt;boolean&lt;/strong&gt;, &lt;strong&gt;date&lt;/strong&gt;, or &lt;strong&gt;time&lt;/strong&gt;.   &lt;p&gt;The format of the &lt;strong&gt;date&lt;/strong&gt; type is &lt;code&gt;yyyy-MM-dd&lt;/code&gt; and of the &lt;strong&gt;time&lt;/strong&gt; type is &lt;code&gt;HH\\:mm\\:ss&lt;/code&gt;.&lt;/p&gt;   &lt;p&gt;A clause may be:&lt;/p&gt;  &lt;ul&gt;      &lt;li&gt;a &lt;strong&gt;term&lt;/strong&gt;: field name followed by a colon and the search term, e.g. &lt;code&gt;tags.MyTagA:sample&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;phrase&lt;/strong&gt;: field name followed by a colon and the search phrase in doublequotes, e.g. &lt;code&gt;name:&amp;quot;Bicuculline methiodide&amp;quot;&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;regular expression&lt;/strong&gt;: field name followed by a colon and the regex in slashes, e.g. &lt;code&gt;tags.MyTagA:/[mb]oat/&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;comparison&lt;/strong&gt;: field name followed by a comparison operator and a value, e.g. &lt;code&gt;ionMass&amp;lt;300&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;range&lt;/strong&gt;: field name followed by a colon and an open (indiced by &lt;code&gt;[ &lt;/code&gt; and &lt;code&gt;] &lt;/code&gt;) or (semi-)closed range (indiced by &lt;code&gt;{&lt;/code&gt; and &lt;code&gt;}&lt;/code&gt;), e.g. &lt;code&gt;ionMass:[300 TO 400]&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;boolean&lt;/strong&gt;: boolean fields are matched as follows: e.g. &lt;code&gt;hasMsMs:true&lt;/code&gt;, &lt;code&gt;tags.MyTagA:false&lt;/code&gt;&lt;/li&gt;      &lt;li&gt;a &lt;strong&gt;value-less&lt;/strong&gt;: tags without values (See TagDefinition) are matched as follows: e.g. &lt;code&gt;tags.MyTagA:*&lt;/code&gt; or &lt;code&gt;tags.MyTagA:true&lt;/code&gt;&lt;/li&gt;  &lt;/ul&gt;   &lt;p&gt;Clauses may be &lt;strong&gt;grouped&lt;/strong&gt; with brackets &lt;code&gt;( &lt;/code&gt; and &lt;code&gt;) &lt;/code&gt; and / or &lt;strong&gt;joined&lt;/strong&gt; with &lt;code&gt;AND&lt;/code&gt; or &lt;code&gt;OR &lt;/code&gt; (or &lt;code&gt;&amp;amp;&amp;amp; &lt;/code&gt; and &lt;code&gt;|| &lt;/code&gt;)&lt;/p&gt;   &lt;h3&gt;Example&lt;/h3&gt;   &lt;p&gt;The syntax allows to build complex filter queries such as:&lt;/p&gt;   &lt;p&gt;&lt;code&gt;ionMass:[300 TO 400] AND quality:GOOD AND topAnnotations.compoundClassAnnotation.npcPathway:&amp;quot;Alkaloids&amp;quot; AND tags.city:&amp;quot;new york&amp;quot; OR tags.MyDateTag:[2023-10-01 TO 2023-12-24] OR tags.time&amp;lt;10\\:00\\:00 &lt;/code&gt;&lt;/p&gt;
     * <p><b>200</b> - tagged features (aligned over runs)
     * @param projectId project space to get features (aligned over runs) from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param searchQuery Optional search query in lucene syntax. Omit this parameter to page over all features.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAlignedFeaturesPageWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<AlignedFeatureOptField> optFields) throws WebClientResponseException {
        return getAlignedFeaturesPageRequestCreation(projectId, page, size, sort, searchQuery, msDataSearchPrepared, optFields);
    }

    /**
     * Get all fields that can be used in the searchQuery parameter of feature (aligned over runs) endpoints
     * Get all fields that can be used in the searchQuery parameter of feature (aligned over runs) endpoints.  &lt;p&gt;  Use this to build valid lucene queries: the field type determines which clauses are supported  (e.g. range queries like &lt;code&gt;ionMass:[300 TO 400]&lt;/code&gt; for numeric fields, word based search for  full-text fields). Includes the dynamic tag fields (&lt;code&gt;tags.&amp;lt;tagName&amp;gt;&lt;/code&gt;) of this project.  An empty list means there are no searchable fields.
     * <p><b>200</b> - fields usable in searchQuery parameters of feature endpoints.
     * @param projectId project space to read the searchable feature fields from.
     * @return List&lt;SearchableField&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAlignedFeaturesSearchableFieldsRequestCreation(@jakarta.annotation.Nonnull String projectId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getAlignedFeaturesSearchableFields", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

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

        ParameterizedTypeReference<SearchableField> localVarReturnType = new ParameterizedTypeReference<SearchableField>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/searchable-fields", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get all fields that can be used in the searchQuery parameter of feature (aligned over runs) endpoints
     * Get all fields that can be used in the searchQuery parameter of feature (aligned over runs) endpoints.  &lt;p&gt;  Use this to build valid lucene queries: the field type determines which clauses are supported  (e.g. range queries like &lt;code&gt;ionMass:[300 TO 400]&lt;/code&gt; for numeric fields, word based search for  full-text fields). Includes the dynamic tag fields (&lt;code&gt;tags.&amp;lt;tagName&amp;gt;&lt;/code&gt;) of this project.  An empty list means there are no searchable fields.
     * <p><b>200</b> - fields usable in searchQuery parameters of feature endpoints.
     * @param projectId project space to read the searchable feature fields from.
     * @return List&lt;SearchableField&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<SearchableField> getAlignedFeaturesSearchableFields(@jakarta.annotation.Nonnull String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableField> localVarReturnType = new ParameterizedTypeReference<SearchableField>() {};
        return getAlignedFeaturesSearchableFieldsRequestCreation(projectId).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Get all fields that can be used in the searchQuery parameter of feature (aligned over runs) endpoints
     * Get all fields that can be used in the searchQuery parameter of feature (aligned over runs) endpoints.  &lt;p&gt;  Use this to build valid lucene queries: the field type determines which clauses are supported  (e.g. range queries like &lt;code&gt;ionMass:[300 TO 400]&lt;/code&gt; for numeric fields, word based search for  full-text fields). Includes the dynamic tag fields (&lt;code&gt;tags.&amp;lt;tagName&amp;gt;&lt;/code&gt;) of this project.  An empty list means there are no searchable fields.
     * <p><b>200</b> - fields usable in searchQuery parameters of feature endpoints.
     * @param projectId project space to read the searchable feature fields from.
     * @return ResponseEntity&lt;List&lt;SearchableField&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<SearchableField>> getAlignedFeaturesSearchableFieldsWithHttpInfo(@jakarta.annotation.Nonnull String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<SearchableField> localVarReturnType = new ParameterizedTypeReference<SearchableField>() {};
        return getAlignedFeaturesSearchableFieldsRequestCreation(projectId).toEntityList(localVarReturnType).block();
    }

    /**
     * Get all fields that can be used in the searchQuery parameter of feature (aligned over runs) endpoints
     * Get all fields that can be used in the searchQuery parameter of feature (aligned over runs) endpoints.  &lt;p&gt;  Use this to build valid lucene queries: the field type determines which clauses are supported  (e.g. range queries like &lt;code&gt;ionMass:[300 TO 400]&lt;/code&gt; for numeric fields, word based search for  full-text fields). Includes the dynamic tag fields (&lt;code&gt;tags.&amp;lt;tagName&amp;gt;&lt;/code&gt;) of this project.  An empty list means there are no searchable fields.
     * <p><b>200</b> - fields usable in searchQuery parameters of feature endpoints.
     * @param projectId project space to read the searchable feature fields from.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAlignedFeaturesSearchableFieldsWithResponseSpec(@jakarta.annotation.Nonnull String projectId) throws WebClientResponseException {
        return getAlignedFeaturesSearchableFieldsRequestCreation(projectId);
    }

    /**
     * Return Best matching compound classes for given formulaId
     * Return Best matching compound classes for given formulaId.  &lt;p&gt;  Set of the highest scoring compound classes (CANOPUS) on each hierarchy level of  the ClassyFire and NPC ontology,
     * <p><b>200</b> - Best matching Predicted compound classes
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return CompoundClasses
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getBestMatchingCompoundClassesRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
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
     * Return Best matching compound classes for given formulaId
     * Return Best matching compound classes for given formulaId.  &lt;p&gt;  Set of the highest scoring compound classes (CANOPUS) on each hierarchy level of  the ClassyFire and NPC ontology,
     * <p><b>200</b> - Best matching Predicted compound classes
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return CompoundClasses
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public CompoundClasses getBestMatchingCompoundClasses(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<CompoundClasses> localVarReturnType = new ParameterizedTypeReference<CompoundClasses>() {};
        return getBestMatchingCompoundClassesRequestCreation(projectId, alignedFeatureId, formulaId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Return Best matching compound classes for given formulaId
     * Return Best matching compound classes for given formulaId.  &lt;p&gt;  Set of the highest scoring compound classes (CANOPUS) on each hierarchy level of  the ClassyFire and NPC ontology,
     * <p><b>200</b> - Best matching Predicted compound classes
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseEntity&lt;CompoundClasses&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<CompoundClasses> getBestMatchingCompoundClassesWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<CompoundClasses> localVarReturnType = new ParameterizedTypeReference<CompoundClasses>() {};
        return getBestMatchingCompoundClassesRequestCreation(projectId, alignedFeatureId, formulaId).toEntity(localVarReturnType).block();
    }

    /**
     * Return Best matching compound classes for given formulaId
     * Return Best matching compound classes for given formulaId.  &lt;p&gt;  Set of the highest scoring compound classes (CANOPUS) on each hierarchy level of  the ClassyFire and NPC ontology,
     * <p><b>200</b> - Best matching Predicted compound classes
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getBestMatchingCompoundClassesWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
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
    private ResponseSpec getCanopusPredictionRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
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
    public CanopusPrediction getCanopusPrediction(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
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
    public ResponseEntity<CanopusPrediction> getCanopusPredictionWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
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
    public ResponseSpec getCanopusPredictionWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
        return getCanopusPredictionRequestCreation(projectId, alignedFeatureId, formulaId);
    }

    /**
     * [DEPRECATED] List of de novo structure candidates (e
     * [DEPRECATED] List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  &lt;p&gt;  [DEPRECATED] Use /denovo-structures/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;StructureCandidateFormula&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec getDeNovoStructureCandidatesRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
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
     * [DEPRECATED] List of de novo structure candidates (e
     * [DEPRECATED] List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  &lt;p&gt;  [DEPRECATED] Use /denovo-structures/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;StructureCandidateFormula&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<StructureCandidateFormula> getDeNovoStructureCandidates(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<StructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<StructureCandidateFormula>() {};
        return getDeNovoStructureCandidatesRequestCreation(projectId, alignedFeatureId, optFields).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [DEPRECATED] List of de novo structure candidates (e
     * [DEPRECATED] List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  &lt;p&gt;  [DEPRECATED] Use /denovo-structures/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;List&lt;StructureCandidateFormula&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<StructureCandidateFormula>> getDeNovoStructureCandidatesWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<StructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<StructureCandidateFormula>() {};
        return getDeNovoStructureCandidatesRequestCreation(projectId, alignedFeatureId, optFields).toEntityList(localVarReturnType).block();
    }

    /**
     * [DEPRECATED] List of de novo structure candidates (e
     * [DEPRECATED] List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  &lt;p&gt;  [DEPRECATED] Use /denovo-structures/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getDeNovoStructureCandidatesWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        return getDeNovoStructureCandidatesRequestCreation(projectId, alignedFeatureId, optFields);
    }

    /**
     * [DEPRECATED] List of de novo structure candidates (e
     * [DEPRECATED] List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  &lt;p&gt;  [DEPRECATED] Use /formulas/{formulaId}/denovo-structures/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;StructureCandidateScored&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec getDeNovoStructureCandidatesByFormulaRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
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
     * [DEPRECATED] List of de novo structure candidates (e
     * [DEPRECATED] List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  &lt;p&gt;  [DEPRECATED] Use /formulas/{formulaId}/denovo-structures/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;StructureCandidateScored&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<StructureCandidateScored> getDeNovoStructureCandidatesByFormula(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<StructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<StructureCandidateScored>() {};
        return getDeNovoStructureCandidatesByFormulaRequestCreation(projectId, alignedFeatureId, formulaId, optFields).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [DEPRECATED] List of de novo structure candidates (e
     * [DEPRECATED] List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  &lt;p&gt;  [DEPRECATED] Use /formulas/{formulaId}/denovo-structures/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;List&lt;StructureCandidateScored&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<StructureCandidateScored>> getDeNovoStructureCandidatesByFormulaWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<StructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<StructureCandidateScored>() {};
        return getDeNovoStructureCandidatesByFormulaRequestCreation(projectId, alignedFeatureId, formulaId, optFields).toEntityList(localVarReturnType).block();
    }

    /**
     * [DEPRECATED] List of de novo structure candidates (e
     * [DEPRECATED] List of de novo structure candidates (e.g. generated by MsNovelist) ranked by CSI:FingerID score for the given &#39;formulaId&#39; with minimal information.  &lt;p&gt;  [DEPRECATED] Use /formulas/{formulaId}/denovo-structures/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  StructureCandidates can be enriched with molecular fingerprint.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getDeNovoStructureCandidatesByFormulaWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
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
    private ResponseSpec getDeNovoStructureCandidatesByFormulaPageRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getDeNovoStructureCandidatesByFormulaPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getDeNovoStructureCandidatesByFormulaPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getDeNovoStructureCandidatesByFormulaPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
    public PagedModelStructureCandidateScored getDeNovoStructureCandidatesByFormulaPage(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelStructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateScored>() {};
        return getDeNovoStructureCandidatesByFormulaPageRequestCreation(projectId, alignedFeatureId, formulaId, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
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
    public ResponseEntity<PagedModelStructureCandidateScored> getDeNovoStructureCandidatesByFormulaPageWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelStructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateScored>() {};
        return getDeNovoStructureCandidatesByFormulaPageRequestCreation(projectId, alignedFeatureId, formulaId, page, size, sort, optFields).toEntity(localVarReturnType).block();
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
    public ResponseSpec getDeNovoStructureCandidatesByFormulaPageWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        return getDeNovoStructureCandidatesByFormulaPageRequestCreation(projectId, alignedFeatureId, formulaId, page, size, sort, optFields);
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
    private ResponseSpec getDeNovoStructureCandidatesPageRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getDeNovoStructureCandidatesPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getDeNovoStructureCandidatesPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
    public PagedModelStructureCandidateFormula getDeNovoStructureCandidatesPage(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelStructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateFormula>() {};
        return getDeNovoStructureCandidatesPageRequestCreation(projectId, alignedFeatureId, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
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
    public ResponseEntity<PagedModelStructureCandidateFormula> getDeNovoStructureCandidatesPageWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelStructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateFormula>() {};
        return getDeNovoStructureCandidatesPageRequestCreation(projectId, alignedFeatureId, page, size, sort, optFields).toEntity(localVarReturnType).block();
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
    public ResponseSpec getDeNovoStructureCandidatesPageWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        return getDeNovoStructureCandidatesPageRequestCreation(projectId, alignedFeatureId, page, size, sort, optFields);
    }

    /**
     * Returns the full quantification table of features
     * Returns the full quantification table of features.  &lt;p&gt;  The quantification table contains the quantities of the features within all runs they are contained in.  Rows refer to features, columns to runs, both given as ids and names.  &lt;p&gt;  The optional search query allows to quantify an arbitrary subset of the project. It uses the same lucene  syntax as the paged listing endpoints. Omit it to quantify all objects of the project.
     * <p><b>200</b> - Quant table of the features of this project
     * @param projectId project-space to read from.
     * @param searchQuery Optional search query in lucene syntax. Omit this parameter to quantify all features.
     * @param type quantification type.
     * @param optFields The optFields parameter
     * @return QuantTable
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFeatureQuantTableRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable QuantMeasure type, @jakarta.annotation.Nullable List<QuantTableOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getFeatureQuantTable", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<QuantTable> localVarReturnType = new ParameterizedTypeReference<QuantTable>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/quant-table", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Returns the full quantification table of features
     * Returns the full quantification table of features.  &lt;p&gt;  The quantification table contains the quantities of the features within all runs they are contained in.  Rows refer to features, columns to runs, both given as ids and names.  &lt;p&gt;  The optional search query allows to quantify an arbitrary subset of the project. It uses the same lucene  syntax as the paged listing endpoints. Omit it to quantify all objects of the project.
     * <p><b>200</b> - Quant table of the features of this project
     * @param projectId project-space to read from.
     * @param searchQuery Optional search query in lucene syntax. Omit this parameter to quantify all features.
     * @param type quantification type.
     * @param optFields The optFields parameter
     * @return QuantTable
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public QuantTable getFeatureQuantTable(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable QuantMeasure type, @jakarta.annotation.Nullable List<QuantTableOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<QuantTable> localVarReturnType = new ParameterizedTypeReference<QuantTable>() {};
        return getFeatureQuantTableRequestCreation(projectId, searchQuery, type, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Returns the full quantification table of features
     * Returns the full quantification table of features.  &lt;p&gt;  The quantification table contains the quantities of the features within all runs they are contained in.  Rows refer to features, columns to runs, both given as ids and names.  &lt;p&gt;  The optional search query allows to quantify an arbitrary subset of the project. It uses the same lucene  syntax as the paged listing endpoints. Omit it to quantify all objects of the project.
     * <p><b>200</b> - Quant table of the features of this project
     * @param projectId project-space to read from.
     * @param searchQuery Optional search query in lucene syntax. Omit this parameter to quantify all features.
     * @param type quantification type.
     * @param optFields The optFields parameter
     * @return ResponseEntity&lt;QuantTable&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<QuantTable> getFeatureQuantTableWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable QuantMeasure type, @jakarta.annotation.Nullable List<QuantTableOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<QuantTable> localVarReturnType = new ParameterizedTypeReference<QuantTable>() {};
        return getFeatureQuantTableRequestCreation(projectId, searchQuery, type, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Returns the full quantification table of features
     * Returns the full quantification table of features.  &lt;p&gt;  The quantification table contains the quantities of the features within all runs they are contained in.  Rows refer to features, columns to runs, both given as ids and names.  &lt;p&gt;  The optional search query allows to quantify an arbitrary subset of the project. It uses the same lucene  syntax as the paged listing endpoints. Omit it to quantify all objects of the project.
     * <p><b>200</b> - Quant table of the features of this project
     * @param projectId project-space to read from.
     * @param searchQuery Optional search query in lucene syntax. Omit this parameter to quantify all features.
     * @param type quantification type.
     * @param optFields The optFields parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFeatureQuantTableWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nullable String searchQuery, @jakarta.annotation.Nullable QuantMeasure type, @jakarta.annotation.Nullable List<QuantTableOptField> optFields) throws WebClientResponseException {
        return getFeatureQuantTableRequestCreation(projectId, searchQuery, type, optFields);
    }

    /**
     * [INTERNAL] Returns a quantification table that contains the given feature (alignedFeatureId) as its only row
     * [INTERNAL] Returns a quantification table that contains the given feature (alignedFeatureId) as its only row.  &lt;p&gt;  The columns of the row refer to the runs the feature has been quantified in, given as run ids and run names.  &lt;p&gt;  [INTERNAL] This endpoint is for internal use and not intended to become part of the stable API specification at any time. This endpoint can change (or be removed) at any time, even in minor updates.  [DEPRECATED] Will be replaced by the quantification table endpoint with filter query support, which allows  to request the quantification of an arbitrary subset of the project.
     * <p><b>200</b> - Quant table row for this feature
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature which quantity should be read out
     * @param type quantification type. APEX_INTENSITY is the intensity of the feature at its apex,                          AREA_UNDER_CURVE the area under its curve.
     * @param optFields The optFields parameter
     * @return QuantTable
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec getFeatureQuantTableRowRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable QuantMeasure type, @jakarta.annotation.Nullable List<QuantTableOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getFeatureQuantTableRow", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getFeatureQuantTableRow", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<QuantTable> localVarReturnType = new ParameterizedTypeReference<QuantTable>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/quant-table-row", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [INTERNAL] Returns a quantification table that contains the given feature (alignedFeatureId) as its only row
     * [INTERNAL] Returns a quantification table that contains the given feature (alignedFeatureId) as its only row.  &lt;p&gt;  The columns of the row refer to the runs the feature has been quantified in, given as run ids and run names.  &lt;p&gt;  [INTERNAL] This endpoint is for internal use and not intended to become part of the stable API specification at any time. This endpoint can change (or be removed) at any time, even in minor updates.  [DEPRECATED] Will be replaced by the quantification table endpoint with filter query support, which allows  to request the quantification of an arbitrary subset of the project.
     * <p><b>200</b> - Quant table row for this feature
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature which quantity should be read out
     * @param type quantification type. APEX_INTENSITY is the intensity of the feature at its apex,                          AREA_UNDER_CURVE the area under its curve.
     * @param optFields The optFields parameter
     * @return QuantTable
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public QuantTable getFeatureQuantTableRow(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable QuantMeasure type, @jakarta.annotation.Nullable List<QuantTableOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<QuantTable> localVarReturnType = new ParameterizedTypeReference<QuantTable>() {};
        return getFeatureQuantTableRowRequestCreation(projectId, alignedFeatureId, type, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * [INTERNAL] Returns a quantification table that contains the given feature (alignedFeatureId) as its only row
     * [INTERNAL] Returns a quantification table that contains the given feature (alignedFeatureId) as its only row.  &lt;p&gt;  The columns of the row refer to the runs the feature has been quantified in, given as run ids and run names.  &lt;p&gt;  [INTERNAL] This endpoint is for internal use and not intended to become part of the stable API specification at any time. This endpoint can change (or be removed) at any time, even in minor updates.  [DEPRECATED] Will be replaced by the quantification table endpoint with filter query support, which allows  to request the quantification of an arbitrary subset of the project.
     * <p><b>200</b> - Quant table row for this feature
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature which quantity should be read out
     * @param type quantification type. APEX_INTENSITY is the intensity of the feature at its apex,                          AREA_UNDER_CURVE the area under its curve.
     * @param optFields The optFields parameter
     * @return ResponseEntity&lt;QuantTable&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<QuantTable> getFeatureQuantTableRowWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable QuantMeasure type, @jakarta.annotation.Nullable List<QuantTableOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<QuantTable> localVarReturnType = new ParameterizedTypeReference<QuantTable>() {};
        return getFeatureQuantTableRowRequestCreation(projectId, alignedFeatureId, type, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * [INTERNAL] Returns a quantification table that contains the given feature (alignedFeatureId) as its only row
     * [INTERNAL] Returns a quantification table that contains the given feature (alignedFeatureId) as its only row.  &lt;p&gt;  The columns of the row refer to the runs the feature has been quantified in, given as run ids and run names.  &lt;p&gt;  [INTERNAL] This endpoint is for internal use and not intended to become part of the stable API specification at any time. This endpoint can change (or be removed) at any time, even in minor updates.  [DEPRECATED] Will be replaced by the quantification table endpoint with filter query support, which allows  to request the quantification of an arbitrary subset of the project.
     * <p><b>200</b> - Quant table row for this feature
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature which quantity should be read out
     * @param type quantification type. APEX_INTENSITY is the intensity of the feature at its apex,                          AREA_UNDER_CURVE the area under its curve.
     * @param optFields The optFields parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFeatureQuantTableRowWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable QuantMeasure type, @jakarta.annotation.Nullable List<QuantTableOptField> optFields) throws WebClientResponseException {
        return getFeatureQuantTableRowRequestCreation(projectId, alignedFeatureId, type, optFields);
    }

    /**
     * [DEPRECATED] Get all features the given feature (aligned over runs) was aligned from
     * [DEPRECATED] Get all features the given feature (aligned over runs) was aligned from.  &lt;p&gt;  [DEPRECATED] Use /features/page instead. Loading all features at once does not scale for a project with many  runs, since a feature is aligned from one feature per run it was detected in.
     * <p><b>200</b> - Features of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) whose features to read.
     * @return List&lt;Feature&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec getFeaturesRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getFeatures", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getFeatures", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<Feature> localVarReturnType = new ParameterizedTypeReference<Feature>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/features", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [DEPRECATED] Get all features the given feature (aligned over runs) was aligned from
     * [DEPRECATED] Get all features the given feature (aligned over runs) was aligned from.  &lt;p&gt;  [DEPRECATED] Use /features/page instead. Loading all features at once does not scale for a project with many  runs, since a feature is aligned from one feature per run it was detected in.
     * <p><b>200</b> - Features of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) whose features to read.
     * @return List&lt;Feature&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Feature> getFeatures(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<Feature> localVarReturnType = new ParameterizedTypeReference<Feature>() {};
        return getFeaturesRequestCreation(projectId, alignedFeatureId).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [DEPRECATED] Get all features the given feature (aligned over runs) was aligned from
     * [DEPRECATED] Get all features the given feature (aligned over runs) was aligned from.  &lt;p&gt;  [DEPRECATED] Use /features/page instead. Loading all features at once does not scale for a project with many  runs, since a feature is aligned from one feature per run it was detected in.
     * <p><b>200</b> - Features of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) whose features to read.
     * @return ResponseEntity&lt;List&lt;Feature&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Feature>> getFeaturesWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<Feature> localVarReturnType = new ParameterizedTypeReference<Feature>() {};
        return getFeaturesRequestCreation(projectId, alignedFeatureId).toEntityList(localVarReturnType).block();
    }

    /**
     * [DEPRECATED] Get all features the given feature (aligned over runs) was aligned from
     * [DEPRECATED] Get all features the given feature (aligned over runs) was aligned from.  &lt;p&gt;  [DEPRECATED] Use /features/page instead. Loading all features at once does not scale for a project with many  runs, since a feature is aligned from one feature per run it was detected in.
     * <p><b>200</b> - Features of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) whose features to read.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFeaturesWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId) throws WebClientResponseException {
        return getFeaturesRequestCreation(projectId, alignedFeatureId);
    }

    /**
     * Page of features the given feature (aligned over runs) was aligned from, one per run it has been detected in
     * Page of features the given feature (aligned over runs) was aligned from, one per run it has been detected in.  &lt;p&gt;  A feature carries what is specific to the run it was detected in: where it sits on that run&#39;s retention time  axis, the m/z it was measured at there, and how much of it was measured. Use the run id to relate it to the run  it belongs to; a feature of a project that was imported from preprocessed data belongs to no run and has none.
     * <p><b>200</b> - Features of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) whose features to read.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return PagedModelFeature
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFeaturesPageRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getFeaturesPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getFeaturesPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PagedModelFeature> localVarReturnType = new ParameterizedTypeReference<PagedModelFeature>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/features/page", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Page of features the given feature (aligned over runs) was aligned from, one per run it has been detected in
     * Page of features the given feature (aligned over runs) was aligned from, one per run it has been detected in.  &lt;p&gt;  A feature carries what is specific to the run it was detected in: where it sits on that run&#39;s retention time  axis, the m/z it was measured at there, and how much of it was measured. Use the run id to relate it to the run  it belongs to; a feature of a project that was imported from preprocessed data belongs to no run and has none.
     * <p><b>200</b> - Features of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) whose features to read.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return PagedModelFeature
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelFeature getFeaturesPage(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelFeature> localVarReturnType = new ParameterizedTypeReference<PagedModelFeature>() {};
        return getFeaturesPageRequestCreation(projectId, alignedFeatureId, page, size, sort).bodyToMono(localVarReturnType).block();
    }

    /**
     * Page of features the given feature (aligned over runs) was aligned from, one per run it has been detected in
     * Page of features the given feature (aligned over runs) was aligned from, one per run it has been detected in.  &lt;p&gt;  A feature carries what is specific to the run it was detected in: where it sits on that run&#39;s retention time  axis, the m/z it was measured at there, and how much of it was measured. Use the run id to relate it to the run  it belongs to; a feature of a project that was imported from preprocessed data belongs to no run and has none.
     * <p><b>200</b> - Features of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) whose features to read.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return ResponseEntity&lt;PagedModelFeature&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelFeature> getFeaturesPageWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelFeature> localVarReturnType = new ParameterizedTypeReference<PagedModelFeature>() {};
        return getFeaturesPageRequestCreation(projectId, alignedFeatureId, page, size, sort).toEntity(localVarReturnType).block();
    }

    /**
     * Page of features the given feature (aligned over runs) was aligned from, one per run it has been detected in
     * Page of features the given feature (aligned over runs) was aligned from, one per run it has been detected in.  &lt;p&gt;  A feature carries what is specific to the run it was detected in: where it sits on that run&#39;s retention time  axis, the m/z it was measured at there, and how much of it was measured. Use the run id to relate it to the run  it belongs to; a feature of a project that was imported from preprocessed data belongs to no run and has none.
     * <p><b>200</b> - Features of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) whose features to read.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFeaturesPageWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort) throws WebClientResponseException {
        return getFeaturesPageRequestCreation(projectId, alignedFeatureId, page, size, sort);
    }

    /**
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier (formulaId)  
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier (formulaId)  &lt;p&gt;  This fingerprint is used to perform structure database search and predict compound classes.
     * <p><b>200</b> - probabilistic fingerprint predicted by CSI:FingerID
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return List&lt;Double&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFingerprintPredictionRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
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
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier (formulaId)  
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier (formulaId)  &lt;p&gt;  This fingerprint is used to perform structure database search and predict compound classes.
     * <p><b>200</b> - probabilistic fingerprint predicted by CSI:FingerID
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return List&lt;Double&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Double> getFingerprintPrediction(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<Double> localVarReturnType = new ParameterizedTypeReference<Double>() {};
        return getFingerprintPredictionRequestCreation(projectId, alignedFeatureId, formulaId).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier (formulaId)  
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier (formulaId)  &lt;p&gt;  This fingerprint is used to perform structure database search and predict compound classes.
     * <p><b>200</b> - probabilistic fingerprint predicted by CSI:FingerID
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseEntity&lt;List&lt;Double&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Double>> getFingerprintPredictionWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<Double> localVarReturnType = new ParameterizedTypeReference<Double>() {};
        return getFingerprintPredictionRequestCreation(projectId, alignedFeatureId, formulaId).toEntityList(localVarReturnType).block();
    }

    /**
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier (formulaId)  
     * Returns predicted fingerprint (CSI:FingerID) for the given formula result identifier (formulaId)  &lt;p&gt;  This fingerprint is used to perform structure database search and predict compound classes.
     * <p><b>200</b> - probabilistic fingerprint predicted by CSI:FingerID
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFingerprintPredictionWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
        return getFingerprintPredictionRequestCreation(projectId, alignedFeatureId, formulaId);
    }

    /**
     * Returns MS/MS Spectrum annotated with fragments and losses for provided formulaId
     * Returns MS/MS Spectrum annotated with fragments and losses for provided formulaId.  &lt;p&gt;  Returns MS/MS Spectrum (Merged MS/MS and measured MS/MS) which is annotated with fragments and losses  for the given formula result identifier  These annotations are only available if a fragmentation tree and the structure candidate are available.
     * <p><b>200</b> - Fragmentation spectra annotated with fragment formulas and losses.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                          Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                          peak assignments and reference spectra.
     * @return AnnotatedMsMsData
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFormulaAnnotatedMsMsDataRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared) throws WebClientResponseException {
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

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "msDataSearchPrepared", msDataSearchPrepared));

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
     * Returns MS/MS Spectrum annotated with fragments and losses for provided formulaId
     * Returns MS/MS Spectrum annotated with fragments and losses for provided formulaId.  &lt;p&gt;  Returns MS/MS Spectrum (Merged MS/MS and measured MS/MS) which is annotated with fragments and losses  for the given formula result identifier  These annotations are only available if a fragmentation tree and the structure candidate are available.
     * <p><b>200</b> - Fragmentation spectra annotated with fragment formulas and losses.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                          Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                          peak assignments and reference spectra.
     * @return AnnotatedMsMsData
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public AnnotatedMsMsData getFormulaAnnotatedMsMsData(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared) throws WebClientResponseException {
        ParameterizedTypeReference<AnnotatedMsMsData> localVarReturnType = new ParameterizedTypeReference<AnnotatedMsMsData>() {};
        return getFormulaAnnotatedMsMsDataRequestCreation(projectId, alignedFeatureId, formulaId, msDataSearchPrepared).bodyToMono(localVarReturnType).block();
    }

    /**
     * Returns MS/MS Spectrum annotated with fragments and losses for provided formulaId
     * Returns MS/MS Spectrum annotated with fragments and losses for provided formulaId.  &lt;p&gt;  Returns MS/MS Spectrum (Merged MS/MS and measured MS/MS) which is annotated with fragments and losses  for the given formula result identifier  These annotations are only available if a fragmentation tree and the structure candidate are available.
     * <p><b>200</b> - Fragmentation spectra annotated with fragment formulas and losses.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                          Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                          peak assignments and reference spectra.
     * @return ResponseEntity&lt;AnnotatedMsMsData&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AnnotatedMsMsData> getFormulaAnnotatedMsMsDataWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared) throws WebClientResponseException {
        ParameterizedTypeReference<AnnotatedMsMsData> localVarReturnType = new ParameterizedTypeReference<AnnotatedMsMsData>() {};
        return getFormulaAnnotatedMsMsDataRequestCreation(projectId, alignedFeatureId, formulaId, msDataSearchPrepared).toEntity(localVarReturnType).block();
    }

    /**
     * Returns MS/MS Spectrum annotated with fragments and losses for provided formulaId
     * Returns MS/MS Spectrum annotated with fragments and losses for provided formulaId.  &lt;p&gt;  Returns MS/MS Spectrum (Merged MS/MS and measured MS/MS) which is annotated with fragments and losses  for the given formula result identifier  These annotations are only available if a fragmentation tree and the structure candidate are available.
     * <p><b>200</b> - Fragmentation spectra annotated with fragment formulas and losses.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                          Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                          peak assignments and reference spectra.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFormulaAnnotatedMsMsDataWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared) throws WebClientResponseException {
        return getFormulaAnnotatedMsMsDataRequestCreation(projectId, alignedFeatureId, formulaId, msDataSearchPrepared);
    }

    /**
     * Returns a fragmentation spectrum (e
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier  &lt;p&gt;  These annotations are only available if a fragmentation tree is available.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragment formulas and losses.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param spectrumIndex index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex &lt; 0 (default)
     * @param searchPrepared Returns all fragment spectra in a preprocessed form as used for fast                          Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                          peak assignments and reference spectra.
     * @return AnnotatedSpectrum
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFormulaAnnotatedSpectrumRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Integer spectrumIndex, @jakarta.annotation.Nullable Boolean searchPrepared) throws WebClientResponseException {
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
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "searchPrepared", searchPrepared));

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
     * Returns a fragmentation spectrum (e
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier  &lt;p&gt;  These annotations are only available if a fragmentation tree is available.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragment formulas and losses.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param spectrumIndex index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex &lt; 0 (default)
     * @param searchPrepared Returns all fragment spectra in a preprocessed form as used for fast                          Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                          peak assignments and reference spectra.
     * @return AnnotatedSpectrum
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public AnnotatedSpectrum getFormulaAnnotatedSpectrum(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Integer spectrumIndex, @jakarta.annotation.Nullable Boolean searchPrepared) throws WebClientResponseException {
        ParameterizedTypeReference<AnnotatedSpectrum> localVarReturnType = new ParameterizedTypeReference<AnnotatedSpectrum>() {};
        return getFormulaAnnotatedSpectrumRequestCreation(projectId, alignedFeatureId, formulaId, spectrumIndex, searchPrepared).bodyToMono(localVarReturnType).block();
    }

    /**
     * Returns a fragmentation spectrum (e
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier  &lt;p&gt;  These annotations are only available if a fragmentation tree is available.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragment formulas and losses.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param spectrumIndex index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex &lt; 0 (default)
     * @param searchPrepared Returns all fragment spectra in a preprocessed form as used for fast                          Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                          peak assignments and reference spectra.
     * @return ResponseEntity&lt;AnnotatedSpectrum&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AnnotatedSpectrum> getFormulaAnnotatedSpectrumWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Integer spectrumIndex, @jakarta.annotation.Nullable Boolean searchPrepared) throws WebClientResponseException {
        ParameterizedTypeReference<AnnotatedSpectrum> localVarReturnType = new ParameterizedTypeReference<AnnotatedSpectrum>() {};
        return getFormulaAnnotatedSpectrumRequestCreation(projectId, alignedFeatureId, formulaId, spectrumIndex, searchPrepared).toEntity(localVarReturnType).block();
    }

    /**
     * Returns a fragmentation spectrum (e
     * Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the given formula result identifier  &lt;p&gt;  These annotations are only available if a fragmentation tree is available.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragment formulas and losses.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param spectrumIndex index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex &lt; 0 (default)
     * @param searchPrepared Returns all fragment spectra in a preprocessed form as used for fast                          Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                          peak assignments and reference spectra.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFormulaAnnotatedSpectrumWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Integer spectrumIndex, @jakarta.annotation.Nullable Boolean searchPrepared) throws WebClientResponseException {
        return getFormulaAnnotatedSpectrumRequestCreation(projectId, alignedFeatureId, formulaId, spectrumIndex, searchPrepared);
    }

    /**
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.  Can be enriched with an optional results overview and formula candidate information.
     * <p><b>200</b> - FormulaCandidate of this feature (aligned over runs) with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return FormulaCandidate
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFormulaCandidateRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
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

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "msDataSearchPrepared", msDataSearchPrepared));
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
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return FormulaCandidate
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public FormulaCandidate getFormulaCandidate(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<FormulaCandidate> localVarReturnType = new ParameterizedTypeReference<FormulaCandidate>() {};
        return getFormulaCandidateRequestCreation(projectId, alignedFeatureId, formulaId, msDataSearchPrepared, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.  Can be enriched with an optional results overview and formula candidate information.
     * <p><b>200</b> - FormulaCandidate of this feature (aligned over runs) with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;FormulaCandidate&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<FormulaCandidate> getFormulaCandidateWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<FormulaCandidate> localVarReturnType = new ParameterizedTypeReference<FormulaCandidate>() {};
        return getFormulaCandidateRequestCreation(projectId, alignedFeatureId, formulaId, msDataSearchPrepared, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.
     * FormulaResultContainers for the given &#39;formulaId&#39; with minimal information.  Can be enriched with an optional results overview and formula candidate information.
     * <p><b>200</b> - FormulaCandidate of this feature (aligned over runs) with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFormulaCandidateWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        return getFormulaCandidateRequestCreation(projectId, alignedFeatureId, formulaId, msDataSearchPrepared, optFields);
    }

    /**
     * [DEPRECATED] List of FormulaResultContainers available for this feature with minimal information
     * [DEPRECATED] List of FormulaResultContainers available for this feature with minimal information.  &lt;p&gt;  [DEPRECATED] Use /formulas/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  Can be enriched with an optional results overview.
     * <p><b>200</b> - All FormulaCandidate of this feature with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;FormulaCandidate&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec getFormulaCandidatesRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
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

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "msDataSearchPrepared", msDataSearchPrepared));
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
     * [DEPRECATED] List of FormulaResultContainers available for this feature with minimal information
     * [DEPRECATED] List of FormulaResultContainers available for this feature with minimal information.  &lt;p&gt;  [DEPRECATED] Use /formulas/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  Can be enriched with an optional results overview.
     * <p><b>200</b> - All FormulaCandidate of this feature with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;FormulaCandidate&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<FormulaCandidate> getFormulaCandidates(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<FormulaCandidate> localVarReturnType = new ParameterizedTypeReference<FormulaCandidate>() {};
        return getFormulaCandidatesRequestCreation(projectId, alignedFeatureId, msDataSearchPrepared, optFields).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [DEPRECATED] List of FormulaResultContainers available for this feature with minimal information
     * [DEPRECATED] List of FormulaResultContainers available for this feature with minimal information.  &lt;p&gt;  [DEPRECATED] Use /formulas/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  Can be enriched with an optional results overview.
     * <p><b>200</b> - All FormulaCandidate of this feature with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;List&lt;FormulaCandidate&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<FormulaCandidate>> getFormulaCandidatesWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<FormulaCandidate> localVarReturnType = new ParameterizedTypeReference<FormulaCandidate>() {};
        return getFormulaCandidatesRequestCreation(projectId, alignedFeatureId, msDataSearchPrepared, optFields).toEntityList(localVarReturnType).block();
    }

    /**
     * [DEPRECATED] List of FormulaResultContainers available for this feature with minimal information
     * [DEPRECATED] List of FormulaResultContainers available for this feature with minimal information.  &lt;p&gt;  [DEPRECATED] Use /formulas/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  Can be enriched with an optional results overview.
     * <p><b>200</b> - All FormulaCandidate of this feature with.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFormulaCandidatesWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        return getFormulaCandidatesRequestCreation(projectId, alignedFeatureId, msDataSearchPrepared, optFields);
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
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelFormulaCandidate
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFormulaCandidatesPageRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getFormulaCandidatesPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getFormulaCandidatesPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "msDataSearchPrepared", msDataSearchPrepared));
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
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return PagedModelFormulaCandidate
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PagedModelFormulaCandidate getFormulaCandidatesPage(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelFormulaCandidate> localVarReturnType = new ParameterizedTypeReference<PagedModelFormulaCandidate>() {};
        return getFormulaCandidatesPageRequestCreation(projectId, alignedFeatureId, page, size, sort, msDataSearchPrepared, optFields).bodyToMono(localVarReturnType).block();
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
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;PagedModelFormulaCandidate&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PagedModelFormulaCandidate> getFormulaCandidatesPageWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelFormulaCandidate> localVarReturnType = new ParameterizedTypeReference<PagedModelFormulaCandidate>() {};
        return getFormulaCandidatesPageRequestCreation(projectId, alignedFeatureId, page, size, sort, msDataSearchPrepared, optFields).toEntity(localVarReturnType).block();
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
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                             Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                             peak assignments and reference spectra.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFormulaCandidatesPageWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Boolean msDataSearchPrepared, @jakarta.annotation.Nullable List<FormulaCandidateOptField> optFields) throws WebClientResponseException {
        return getFormulaCandidatesPageRequestCreation(projectId, alignedFeatureId, page, size, sort, msDataSearchPrepared, optFields);
    }

    /**
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier  
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier  &lt;p&gt;  This tree is used to rank formula candidates (treeScore).
     * <p><b>200</b> - Fragmentation Tree
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return FragmentationTree
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFragTreeRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
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
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier  
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier  &lt;p&gt;  This tree is used to rank formula candidates (treeScore).
     * <p><b>200</b> - Fragmentation Tree
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return FragmentationTree
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public FragmentationTree getFragTree(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<FragmentationTree> localVarReturnType = new ParameterizedTypeReference<FragmentationTree>() {};
        return getFragTreeRequestCreation(projectId, alignedFeatureId, formulaId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier  
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier  &lt;p&gt;  This tree is used to rank formula candidates (treeScore).
     * <p><b>200</b> - Fragmentation Tree
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseEntity&lt;FragmentationTree&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<FragmentationTree> getFragTreeWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<FragmentationTree> localVarReturnType = new ParameterizedTypeReference<FragmentationTree>() {};
        return getFragTreeRequestCreation(projectId, alignedFeatureId, formulaId).toEntity(localVarReturnType).block();
    }

    /**
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier  
     * Returns fragmentation tree (SIRIUS) for the given formula result identifier  &lt;p&gt;  This tree is used to rank formula candidates (treeScore).
     * <p><b>200</b> - Fragmentation Tree
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFragTreeWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
        return getFragTreeRequestCreation(projectId, alignedFeatureId, formulaId);
    }

    /**
     * Returns Isotope pattern information for given formulaId  
     * Returns Isotope pattern information for given formulaId  &lt;p&gt;  Returns Isotope pattern information (simulated isotope pattern, measured isotope pattern, isotope pattern highlighting)  for the given formula result identifier. This simulated isotope pattern is used to rank formula candidates (treeScore).
     * <p><b>200</b> - Isotope pattern information
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return IsotopePatternAnnotation
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getIsotopePatternAnnotationRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
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
     * Returns Isotope pattern information for given formulaId  
     * Returns Isotope pattern information for given formulaId  &lt;p&gt;  Returns Isotope pattern information (simulated isotope pattern, measured isotope pattern, isotope pattern highlighting)  for the given formula result identifier. This simulated isotope pattern is used to rank formula candidates (treeScore).
     * <p><b>200</b> - Isotope pattern information
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return IsotopePatternAnnotation
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public IsotopePatternAnnotation getIsotopePatternAnnotation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<IsotopePatternAnnotation> localVarReturnType = new ParameterizedTypeReference<IsotopePatternAnnotation>() {};
        return getIsotopePatternAnnotationRequestCreation(projectId, alignedFeatureId, formulaId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Returns Isotope pattern information for given formulaId  
     * Returns Isotope pattern information for given formulaId  &lt;p&gt;  Returns Isotope pattern information (simulated isotope pattern, measured isotope pattern, isotope pattern highlighting)  for the given formula result identifier. This simulated isotope pattern is used to rank formula candidates (treeScore).
     * <p><b>200</b> - Isotope pattern information
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseEntity&lt;IsotopePatternAnnotation&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<IsotopePatternAnnotation> getIsotopePatternAnnotationWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<IsotopePatternAnnotation> localVarReturnType = new ParameterizedTypeReference<IsotopePatternAnnotation>() {};
        return getIsotopePatternAnnotationRequestCreation(projectId, alignedFeatureId, formulaId).toEntity(localVarReturnType).block();
    }

    /**
     * Returns Isotope pattern information for given formulaId  
     * Returns Isotope pattern information for given formulaId  &lt;p&gt;  Returns Isotope pattern information (simulated isotope pattern, measured isotope pattern, isotope pattern highlighting)  for the given formula result identifier. This simulated isotope pattern is used to rank formula candidates (treeScore).
     * <p><b>200</b> - Isotope pattern information
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getIsotopePatternAnnotationWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
        return getIsotopePatternAnnotationRequestCreation(projectId, alignedFeatureId, formulaId);
    }

    /**
     * Returns Lipid annotation (ElGordo) for the given formulaId
     * Returns Lipid annotation (ElGordo) for the given formulaId.  &lt;p&gt;  ElGordo lipid annotation runs as part of the SIRIUS formula identification step.
     * <p><b>200</b> - LipidAnnotation
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return LipidAnnotation
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getLipidAnnotationRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
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
     * Returns Lipid annotation (ElGordo) for the given formulaId
     * Returns Lipid annotation (ElGordo) for the given formulaId.  &lt;p&gt;  ElGordo lipid annotation runs as part of the SIRIUS formula identification step.
     * <p><b>200</b> - LipidAnnotation
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return LipidAnnotation
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public LipidAnnotation getLipidAnnotation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<LipidAnnotation> localVarReturnType = new ParameterizedTypeReference<LipidAnnotation>() {};
        return getLipidAnnotationRequestCreation(projectId, alignedFeatureId, formulaId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Returns Lipid annotation (ElGordo) for the given formulaId
     * Returns Lipid annotation (ElGordo) for the given formulaId.  &lt;p&gt;  ElGordo lipid annotation runs as part of the SIRIUS formula identification step.
     * <p><b>200</b> - LipidAnnotation
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseEntity&lt;LipidAnnotation&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<LipidAnnotation> getLipidAnnotationWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<LipidAnnotation> localVarReturnType = new ParameterizedTypeReference<LipidAnnotation>() {};
        return getLipidAnnotationRequestCreation(projectId, alignedFeatureId, formulaId).toEntity(localVarReturnType).block();
    }

    /**
     * Returns Lipid annotation (ElGordo) for the given formulaId
     * Returns Lipid annotation (ElGordo) for the given formulaId.  &lt;p&gt;  ElGordo lipid annotation runs as part of the SIRIUS formula identification step.
     * <p><b>200</b> - LipidAnnotation
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getLipidAnnotationWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
        return getLipidAnnotationRequestCreation(projectId, alignedFeatureId, formulaId);
    }

    /**
     * Mass Spec data (input data) for the given &#39;alignedFeatureId&#39; .
     * Mass Spec data (input data) for the given &#39;alignedFeatureId&#39; .
     * <p><b>200</b> - Mass Spec data of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the Mass Spec data belongs to.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                          Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                          peak assignments and reference spectra.
     * @return MsData
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getMsDataRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared) throws WebClientResponseException {
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

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "msDataSearchPrepared", msDataSearchPrepared));

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
     * @param alignedFeatureId feature (aligned over runs) the Mass Spec data belongs to.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                          Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                          peak assignments and reference spectra.
     * @return MsData
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public MsData getMsData(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared) throws WebClientResponseException {
        ParameterizedTypeReference<MsData> localVarReturnType = new ParameterizedTypeReference<MsData>() {};
        return getMsDataRequestCreation(projectId, alignedFeatureId, msDataSearchPrepared).bodyToMono(localVarReturnType).block();
    }

    /**
     * Mass Spec data (input data) for the given &#39;alignedFeatureId&#39; .
     * Mass Spec data (input data) for the given &#39;alignedFeatureId&#39; .
     * <p><b>200</b> - Mass Spec data of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the Mass Spec data belongs to.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                          Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                          peak assignments and reference spectra.
     * @return ResponseEntity&lt;MsData&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<MsData> getMsDataWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared) throws WebClientResponseException {
        ParameterizedTypeReference<MsData> localVarReturnType = new ParameterizedTypeReference<MsData>() {};
        return getMsDataRequestCreation(projectId, alignedFeatureId, msDataSearchPrepared).toEntity(localVarReturnType).block();
    }

    /**
     * Mass Spec data (input data) for the given &#39;alignedFeatureId&#39; .
     * Mass Spec data (input data) for the given &#39;alignedFeatureId&#39; .
     * <p><b>200</b> - Mass Spec data of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the Mass Spec data belongs to.
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                          Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                          peak assignments and reference spectra.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getMsDataWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Boolean msDataSearchPrepared) throws WebClientResponseException {
        return getMsDataRequestCreation(projectId, alignedFeatureId, msDataSearchPrepared);
    }

    /**
     * [INTERNAL] Returns fragmentation tree (SIRIUS) for the given formula result identifier in SIRIUS&#39; internal format
     * [INTERNAL] Returns fragmentation tree (SIRIUS) for the given formula result identifier in SIRIUS&#39; internal format.  &lt;p&gt;  [INTERNAL]: This is an internal api endpoint and not part of the official public API. It might be changed or removed at any time.
     * <p><b>200</b> - Fragmentation Tree in internal format.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return String
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getSiriusFragTreeInternalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
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
     * [INTERNAL] Returns fragmentation tree (SIRIUS) for the given formula result identifier in SIRIUS&#39; internal format
     * [INTERNAL] Returns fragmentation tree (SIRIUS) for the given formula result identifier in SIRIUS&#39; internal format.  &lt;p&gt;  [INTERNAL]: This is an internal api endpoint and not part of the official public API. It might be changed or removed at any time.
     * <p><b>200</b> - Fragmentation Tree in internal format.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return String
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public String getSiriusFragTreeInternal(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return getSiriusFragTreeInternalRequestCreation(projectId, alignedFeatureId, formulaId).bodyToMono(localVarReturnType).block();
    }

    /**
     * [INTERNAL] Returns fragmentation tree (SIRIUS) for the given formula result identifier in SIRIUS&#39; internal format
     * [INTERNAL] Returns fragmentation tree (SIRIUS) for the given formula result identifier in SIRIUS&#39; internal format.  &lt;p&gt;  [INTERNAL]: This is an internal api endpoint and not part of the official public API. It might be changed or removed at any time.
     * <p><b>200</b> - Fragmentation Tree in internal format.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseEntity&lt;String&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<String> getSiriusFragTreeInternalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return getSiriusFragTreeInternalRequestCreation(projectId, alignedFeatureId, formulaId).toEntity(localVarReturnType).block();
    }

    /**
     * [INTERNAL] Returns fragmentation tree (SIRIUS) for the given formula result identifier in SIRIUS&#39; internal format
     * [INTERNAL] Returns fragmentation tree (SIRIUS) for the given formula result identifier in SIRIUS&#39; internal format.  &lt;p&gt;  [INTERNAL]: This is an internal api endpoint and not part of the official public API. It might be changed or removed at any time.
     * <p><b>200</b> - Fragmentation Tree in internal format.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getSiriusFragTreeInternalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId) throws WebClientResponseException {
        return getSiriusFragTreeInternalRequestCreation(projectId, alignedFeatureId, formulaId);
    }

    /**
     * Spectral library match for the given &#39;alignedFeatureId&#39;.
     * Spectral library match for the given &#39;alignedFeatureId&#39;.
     * <p><b>200</b> - Spectral library match with requested mathcId.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param matchId id of the library match to be returned.
     * @param optFields The optFields parameter
     * @return SpectralLibraryMatch
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getSpectralLibraryMatchRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String matchId, @jakarta.annotation.Nullable List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
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
     * Spectral library match for the given &#39;alignedFeatureId&#39;.
     * Spectral library match for the given &#39;alignedFeatureId&#39;.
     * <p><b>200</b> - Spectral library match with requested mathcId.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param matchId id of the library match to be returned.
     * @param optFields The optFields parameter
     * @return SpectralLibraryMatch
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public SpectralLibraryMatch getSpectralLibraryMatch(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String matchId, @jakarta.annotation.Nullable List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<SpectralLibraryMatch> localVarReturnType = new ParameterizedTypeReference<SpectralLibraryMatch>() {};
        return getSpectralLibraryMatchRequestCreation(projectId, alignedFeatureId, matchId, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Spectral library match for the given &#39;alignedFeatureId&#39;.
     * Spectral library match for the given &#39;alignedFeatureId&#39;.
     * <p><b>200</b> - Spectral library match with requested mathcId.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param matchId id of the library match to be returned.
     * @param optFields The optFields parameter
     * @return ResponseEntity&lt;SpectralLibraryMatch&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<SpectralLibraryMatch> getSpectralLibraryMatchWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String matchId, @jakarta.annotation.Nullable List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<SpectralLibraryMatch> localVarReturnType = new ParameterizedTypeReference<SpectralLibraryMatch>() {};
        return getSpectralLibraryMatchRequestCreation(projectId, alignedFeatureId, matchId, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Spectral library match for the given &#39;alignedFeatureId&#39;.
     * Spectral library match for the given &#39;alignedFeatureId&#39;.
     * <p><b>200</b> - Spectral library match with requested mathcId.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param matchId id of the library match to be returned.
     * @param optFields The optFields parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getSpectralLibraryMatchWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String matchId, @jakarta.annotation.Nullable List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        return getSpectralLibraryMatchRequestCreation(projectId, alignedFeatureId, matchId, optFields);
    }

    /**
     * [DEPRECATED] List of spectral library matches for the given &#39;alignedFeatureId&#39;
     * [DEPRECATED] List of spectral library matches for the given &#39;alignedFeatureId&#39;.  &lt;p&gt;  [DEPRECATED] Use /spectral-library-matches/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.
     * <p><b>200</b> - Spectral library matches of this feature (aligned over runs).
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param minSharedPeaks The minSharedPeaks parameter
     * @param minSimilarity The minSimilarity parameter
     * @param inchiKey The inchiKey parameter
     * @param optFields The optFields parameter
     * @return List&lt;SpectralLibraryMatch&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec getSpectralLibraryMatchesRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer minSharedPeaks, @jakarta.annotation.Nullable Double minSimilarity, @jakarta.annotation.Nullable String inchiKey, @jakarta.annotation.Nullable List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
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
     * [DEPRECATED] List of spectral library matches for the given &#39;alignedFeatureId&#39;
     * [DEPRECATED] List of spectral library matches for the given &#39;alignedFeatureId&#39;.  &lt;p&gt;  [DEPRECATED] Use /spectral-library-matches/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.
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
    public List<SpectralLibraryMatch> getSpectralLibraryMatches(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer minSharedPeaks, @jakarta.annotation.Nullable Double minSimilarity, @jakarta.annotation.Nullable String inchiKey, @jakarta.annotation.Nullable List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<SpectralLibraryMatch> localVarReturnType = new ParameterizedTypeReference<SpectralLibraryMatch>() {};
        return getSpectralLibraryMatchesRequestCreation(projectId, alignedFeatureId, minSharedPeaks, minSimilarity, inchiKey, optFields).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [DEPRECATED] List of spectral library matches for the given &#39;alignedFeatureId&#39;
     * [DEPRECATED] List of spectral library matches for the given &#39;alignedFeatureId&#39;.  &lt;p&gt;  [DEPRECATED] Use /spectral-library-matches/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.
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
    public ResponseEntity<List<SpectralLibraryMatch>> getSpectralLibraryMatchesWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer minSharedPeaks, @jakarta.annotation.Nullable Double minSimilarity, @jakarta.annotation.Nullable String inchiKey, @jakarta.annotation.Nullable List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<SpectralLibraryMatch> localVarReturnType = new ParameterizedTypeReference<SpectralLibraryMatch>() {};
        return getSpectralLibraryMatchesRequestCreation(projectId, alignedFeatureId, minSharedPeaks, minSimilarity, inchiKey, optFields).toEntityList(localVarReturnType).block();
    }

    /**
     * [DEPRECATED] List of spectral library matches for the given &#39;alignedFeatureId&#39;
     * [DEPRECATED] List of spectral library matches for the given &#39;alignedFeatureId&#39;.  &lt;p&gt;  [DEPRECATED] Use /spectral-library-matches/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.
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
    public ResponseSpec getSpectralLibraryMatchesWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer minSharedPeaks, @jakarta.annotation.Nullable Double minSimilarity, @jakarta.annotation.Nullable String inchiKey, @jakarta.annotation.Nullable List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
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
    private ResponseSpec getSpectralLibraryMatchesPageRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Integer minSharedPeaks, @jakarta.annotation.Nullable Double minSimilarity, @jakarta.annotation.Nullable String inchiKey, @jakarta.annotation.Nullable List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getSpectralLibraryMatchesPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getSpectralLibraryMatchesPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
    public PagedModelSpectralLibraryMatch getSpectralLibraryMatchesPage(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Integer minSharedPeaks, @jakarta.annotation.Nullable Double minSimilarity, @jakarta.annotation.Nullable String inchiKey, @jakarta.annotation.Nullable List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelSpectralLibraryMatch> localVarReturnType = new ParameterizedTypeReference<PagedModelSpectralLibraryMatch>() {};
        return getSpectralLibraryMatchesPageRequestCreation(projectId, alignedFeatureId, page, size, sort, minSharedPeaks, minSimilarity, inchiKey, optFields).bodyToMono(localVarReturnType).block();
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
    public ResponseEntity<PagedModelSpectralLibraryMatch> getSpectralLibraryMatchesPageWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Integer minSharedPeaks, @jakarta.annotation.Nullable Double minSimilarity, @jakarta.annotation.Nullable String inchiKey, @jakarta.annotation.Nullable List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelSpectralLibraryMatch> localVarReturnType = new ParameterizedTypeReference<PagedModelSpectralLibraryMatch>() {};
        return getSpectralLibraryMatchesPageRequestCreation(projectId, alignedFeatureId, page, size, sort, minSharedPeaks, minSimilarity, inchiKey, optFields).toEntity(localVarReturnType).block();
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
    public ResponseSpec getSpectralLibraryMatchesPageWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable Integer minSharedPeaks, @jakarta.annotation.Nullable Double minSimilarity, @jakarta.annotation.Nullable String inchiKey, @jakarta.annotation.Nullable List<SpectralLibraryMatchOptField> optFields) throws WebClientResponseException {
        return getSpectralLibraryMatchesPageRequestCreation(projectId, alignedFeatureId, page, size, sort, minSharedPeaks, minSimilarity, inchiKey, optFields);
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
    private ResponseSpec getSpectralLibraryMatchesSummaryRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer minSharedPeaks, @jakarta.annotation.Nullable Double minSimilarity, @jakarta.annotation.Nullable String inchiKey) throws WebClientResponseException {
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
    public SpectralLibraryMatchSummary getSpectralLibraryMatchesSummary(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer minSharedPeaks, @jakarta.annotation.Nullable Double minSimilarity, @jakarta.annotation.Nullable String inchiKey) throws WebClientResponseException {
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
    public ResponseEntity<SpectralLibraryMatchSummary> getSpectralLibraryMatchesSummaryWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer minSharedPeaks, @jakarta.annotation.Nullable Double minSimilarity, @jakarta.annotation.Nullable String inchiKey) throws WebClientResponseException {
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
    public ResponseSpec getSpectralLibraryMatchesSummaryWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer minSharedPeaks, @jakarta.annotation.Nullable Double minSimilarity, @jakarta.annotation.Nullable String inchiKey) throws WebClientResponseException {
        return getSpectralLibraryMatchesSummaryRequestCreation(projectId, alignedFeatureId, minSharedPeaks, minSimilarity, inchiKey);
    }

    /**
     * [EXPERIMENTAL] Returns MS/MS Data annotated with fragments and losses for given formulaId and inChIKey
     * [EXPERIMENTAL] Returns MS/MS Data annotated with fragments and losses for given formulaId and inChIKey.  &lt;p&gt;  Returns MS/MS Data (Merged MS/MS and list of measured MS/MS ) which are annotated with fragments and losses  for the given formula result identifier and structure candidate inChIKey.  These annotations are only available if a fragmentation tree and the structure candidate are available.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragments and sub-structures.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param inchiKey 2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                          Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                          peak assignments and reference spectra.
     * @return AnnotatedMsMsData
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getStructureAnnotatedMsDataExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nonnull String inchiKey, @jakarta.annotation.Nullable Boolean msDataSearchPrepared) throws WebClientResponseException {
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

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "msDataSearchPrepared", msDataSearchPrepared));

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
     * [EXPERIMENTAL] Returns MS/MS Data annotated with fragments and losses for given formulaId and inChIKey
     * [EXPERIMENTAL] Returns MS/MS Data annotated with fragments and losses for given formulaId and inChIKey.  &lt;p&gt;  Returns MS/MS Data (Merged MS/MS and list of measured MS/MS ) which are annotated with fragments and losses  for the given formula result identifier and structure candidate inChIKey.  These annotations are only available if a fragmentation tree and the structure candidate are available.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragments and sub-structures.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param inchiKey 2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                          Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                          peak assignments and reference spectra.
     * @return AnnotatedMsMsData
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public AnnotatedMsMsData getStructureAnnotatedMsDataExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nonnull String inchiKey, @jakarta.annotation.Nullable Boolean msDataSearchPrepared) throws WebClientResponseException {
        ParameterizedTypeReference<AnnotatedMsMsData> localVarReturnType = new ParameterizedTypeReference<AnnotatedMsMsData>() {};
        return getStructureAnnotatedMsDataExperimentalRequestCreation(projectId, alignedFeatureId, formulaId, inchiKey, msDataSearchPrepared).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Returns MS/MS Data annotated with fragments and losses for given formulaId and inChIKey
     * [EXPERIMENTAL] Returns MS/MS Data annotated with fragments and losses for given formulaId and inChIKey.  &lt;p&gt;  Returns MS/MS Data (Merged MS/MS and list of measured MS/MS ) which are annotated with fragments and losses  for the given formula result identifier and structure candidate inChIKey.  These annotations are only available if a fragmentation tree and the structure candidate are available.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragments and sub-structures.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param inchiKey 2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                          Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                          peak assignments and reference spectra.
     * @return ResponseEntity&lt;AnnotatedMsMsData&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AnnotatedMsMsData> getStructureAnnotatedMsDataExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nonnull String inchiKey, @jakarta.annotation.Nullable Boolean msDataSearchPrepared) throws WebClientResponseException {
        ParameterizedTypeReference<AnnotatedMsMsData> localVarReturnType = new ParameterizedTypeReference<AnnotatedMsMsData>() {};
        return getStructureAnnotatedMsDataExperimentalRequestCreation(projectId, alignedFeatureId, formulaId, inchiKey, msDataSearchPrepared).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Returns MS/MS Data annotated with fragments and losses for given formulaId and inChIKey
     * [EXPERIMENTAL] Returns MS/MS Data annotated with fragments and losses for given formulaId and inChIKey.  &lt;p&gt;  Returns MS/MS Data (Merged MS/MS and list of measured MS/MS ) which are annotated with fragments and losses  for the given formula result identifier and structure candidate inChIKey.  These annotations are only available if a fragmentation tree and the structure candidate are available.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragments and sub-structures.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param inchiKey 2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @param msDataSearchPrepared Returns all fragment spectra in a preprocessed form as used for fast                          Cosine/Modified Cosine computation. Gives you spectra compatible with SpectralLibraryMatch                          peak assignments and reference spectra.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getStructureAnnotatedMsDataExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nonnull String inchiKey, @jakarta.annotation.Nullable Boolean msDataSearchPrepared) throws WebClientResponseException {
        return getStructureAnnotatedMsDataExperimentalRequestCreation(projectId, alignedFeatureId, formulaId, inchiKey, msDataSearchPrepared);
    }

    /**
     * [EXPERIMENTAL] Spectral library match for the given &#39;alignedFeatureId&#39; with additional molecular formula and substructure annotations
     * [EXPERIMENTAL] Spectral library match for the given &#39;alignedFeatureId&#39; with additional molecular formula and substructure annotations.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Spectral library match with requested mathcId.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param matchId id of the library match to be returned.
     * @return AnnotatedSpectrum
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getStructureAnnotatedSpectralLibraryMatchExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String matchId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getStructureAnnotatedSpectralLibraryMatchExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getStructureAnnotatedSpectralLibraryMatchExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'matchId' is set
        if (matchId == null) {
            throw new WebClientResponseException("Missing the required parameter 'matchId' when calling getStructureAnnotatedSpectralLibraryMatchExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<AnnotatedSpectrum> localVarReturnType = new ParameterizedTypeReference<AnnotatedSpectrum>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/spectral-library-matches/{matchId}/annotated", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Spectral library match for the given &#39;alignedFeatureId&#39; with additional molecular formula and substructure annotations
     * [EXPERIMENTAL] Spectral library match for the given &#39;alignedFeatureId&#39; with additional molecular formula and substructure annotations.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Spectral library match with requested mathcId.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param matchId id of the library match to be returned.
     * @return AnnotatedSpectrum
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public AnnotatedSpectrum getStructureAnnotatedSpectralLibraryMatchExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String matchId) throws WebClientResponseException {
        ParameterizedTypeReference<AnnotatedSpectrum> localVarReturnType = new ParameterizedTypeReference<AnnotatedSpectrum>() {};
        return getStructureAnnotatedSpectralLibraryMatchExperimentalRequestCreation(projectId, alignedFeatureId, matchId).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Spectral library match for the given &#39;alignedFeatureId&#39; with additional molecular formula and substructure annotations
     * [EXPERIMENTAL] Spectral library match for the given &#39;alignedFeatureId&#39; with additional molecular formula and substructure annotations.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Spectral library match with requested mathcId.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param matchId id of the library match to be returned.
     * @return ResponseEntity&lt;AnnotatedSpectrum&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AnnotatedSpectrum> getStructureAnnotatedSpectralLibraryMatchExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String matchId) throws WebClientResponseException {
        ParameterizedTypeReference<AnnotatedSpectrum> localVarReturnType = new ParameterizedTypeReference<AnnotatedSpectrum>() {};
        return getStructureAnnotatedSpectralLibraryMatchExperimentalRequestCreation(projectId, alignedFeatureId, matchId).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Spectral library match for the given &#39;alignedFeatureId&#39; with additional molecular formula and substructure annotations
     * [EXPERIMENTAL] Spectral library match for the given &#39;alignedFeatureId&#39; with additional molecular formula and substructure annotations.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Spectral library match with requested mathcId.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param matchId id of the library match to be returned.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getStructureAnnotatedSpectralLibraryMatchExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String matchId) throws WebClientResponseException {
        return getStructureAnnotatedSpectralLibraryMatchExperimentalRequestCreation(projectId, alignedFeatureId, matchId);
    }

    /**
     * [EXPERIMENTAL] Returns a fragmentation spectrum annotated with fragments and losses for the given formulaId and inChIKey  
     * [EXPERIMENTAL] Returns a fragmentation spectrum annotated with fragments and losses for the given formulaId and inChIKey  &lt;p&gt;  Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the selected formula result  These annotations are only available if a fragmentation tree is available.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragments and sub-structures.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param inchiKey 2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @param spectrumIndex index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex &lt; 0 (default)
     * @param searchPrepared The searchPrepared parameter
     * @return AnnotatedSpectrum
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getStructureAnnotatedSpectrumExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nonnull String inchiKey, @jakarta.annotation.Nullable Integer spectrumIndex, @jakarta.annotation.Nullable Boolean searchPrepared) throws WebClientResponseException {
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
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "searchPrepared", searchPrepared));

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
     * [EXPERIMENTAL] Returns a fragmentation spectrum annotated with fragments and losses for the given formulaId and inChIKey  
     * [EXPERIMENTAL] Returns a fragmentation spectrum annotated with fragments and losses for the given formulaId and inChIKey  &lt;p&gt;  Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the selected formula result  These annotations are only available if a fragmentation tree is available.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragments and sub-structures.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param inchiKey 2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @param spectrumIndex index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex &lt; 0 (default)
     * @param searchPrepared The searchPrepared parameter
     * @return AnnotatedSpectrum
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public AnnotatedSpectrum getStructureAnnotatedSpectrumExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nonnull String inchiKey, @jakarta.annotation.Nullable Integer spectrumIndex, @jakarta.annotation.Nullable Boolean searchPrepared) throws WebClientResponseException {
        ParameterizedTypeReference<AnnotatedSpectrum> localVarReturnType = new ParameterizedTypeReference<AnnotatedSpectrum>() {};
        return getStructureAnnotatedSpectrumExperimentalRequestCreation(projectId, alignedFeatureId, formulaId, inchiKey, spectrumIndex, searchPrepared).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Returns a fragmentation spectrum annotated with fragments and losses for the given formulaId and inChIKey  
     * [EXPERIMENTAL] Returns a fragmentation spectrum annotated with fragments and losses for the given formulaId and inChIKey  &lt;p&gt;  Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the selected formula result  These annotations are only available if a fragmentation tree is available.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragments and sub-structures.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param inchiKey 2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @param spectrumIndex index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex &lt; 0 (default)
     * @param searchPrepared The searchPrepared parameter
     * @return ResponseEntity&lt;AnnotatedSpectrum&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AnnotatedSpectrum> getStructureAnnotatedSpectrumExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nonnull String inchiKey, @jakarta.annotation.Nullable Integer spectrumIndex, @jakarta.annotation.Nullable Boolean searchPrepared) throws WebClientResponseException {
        ParameterizedTypeReference<AnnotatedSpectrum> localVarReturnType = new ParameterizedTypeReference<AnnotatedSpectrum>() {};
        return getStructureAnnotatedSpectrumExperimentalRequestCreation(projectId, alignedFeatureId, formulaId, inchiKey, spectrumIndex, searchPrepared).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Returns a fragmentation spectrum annotated with fragments and losses for the given formulaId and inChIKey  
     * [EXPERIMENTAL] Returns a fragmentation spectrum annotated with fragments and losses for the given formulaId and inChIKey  &lt;p&gt;  Returns a fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses for the selected formula result  These annotations are only available if a fragmentation tree is available.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Fragmentation spectrum annotated with fragments and sub-structures.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param inchiKey 2d InChIKey of the structure candidate to be used to annotate the spectrum annotation
     * @param spectrumIndex index of the spectrum to be annotated. Merged MS/MS will be used if spectrumIndex &lt; 0 (default)
     * @param searchPrepared The searchPrepared parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getStructureAnnotatedSpectrumExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nonnull String inchiKey, @jakarta.annotation.Nullable Integer spectrumIndex, @jakarta.annotation.Nullable Boolean searchPrepared) throws WebClientResponseException {
        return getStructureAnnotatedSpectrumExperimentalRequestCreation(projectId, alignedFeatureId, formulaId, inchiKey, spectrumIndex, searchPrepared);
    }

    /**
     * [DEPRECATED] List of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information
     * [DEPRECATED] List of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  &lt;p&gt;  [DEPRECATED] Use /db-structures/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;StructureCandidateFormula&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec getStructureCandidatesRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
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
     * [DEPRECATED] List of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information
     * [DEPRECATED] List of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  &lt;p&gt;  [DEPRECATED] Use /db-structures/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;StructureCandidateFormula&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<StructureCandidateFormula> getStructureCandidates(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<StructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<StructureCandidateFormula>() {};
        return getStructureCandidatesRequestCreation(projectId, alignedFeatureId, optFields).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [DEPRECATED] List of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information
     * [DEPRECATED] List of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  &lt;p&gt;  [DEPRECATED] Use /db-structures/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;List&lt;StructureCandidateFormula&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<StructureCandidateFormula>> getStructureCandidatesWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<StructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<StructureCandidateFormula>() {};
        return getStructureCandidatesRequestCreation(projectId, alignedFeatureId, optFields).toEntityList(localVarReturnType).block();
    }

    /**
     * [DEPRECATED] List of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information
     * [DEPRECATED] List of structure database search candidates ranked by CSI:FingerID score for the given &#39;alignedFeatureId&#39; with minimal information.  &lt;p&gt;  [DEPRECATED] Use /db-structures/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this feature (aligned over runs) candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the structure candidates belong to.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getStructureCandidatesWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        return getStructureCandidatesRequestCreation(projectId, alignedFeatureId, optFields);
    }

    /**
     * [DEPRECATED] List of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information
     * [DEPRECATED] List of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.  &lt;p&gt;  [DEPRECATED] Use /formulas/{formulaId}/db-structures/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;StructureCandidateScored&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     * @deprecated
     */
    @Deprecated
    private ResponseSpec getStructureCandidatesByFormulaRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
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
     * [DEPRECATED] List of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information
     * [DEPRECATED] List of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.  &lt;p&gt;  [DEPRECATED] Use /formulas/{formulaId}/db-structures/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return List&lt;StructureCandidateScored&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<StructureCandidateScored> getStructureCandidatesByFormula(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<StructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<StructureCandidateScored>() {};
        return getStructureCandidatesByFormulaRequestCreation(projectId, alignedFeatureId, formulaId, optFields).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [DEPRECATED] List of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information
     * [DEPRECATED] List of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.  &lt;p&gt;  [DEPRECATED] Use /formulas/{formulaId}/db-structures/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseEntity&lt;List&lt;StructureCandidateScored&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<StructureCandidateScored>> getStructureCandidatesByFormulaWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<StructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<StructureCandidateScored>() {};
        return getStructureCandidatesByFormulaRequestCreation(projectId, alignedFeatureId, formulaId, optFields).toEntityList(localVarReturnType).block();
    }

    /**
     * [DEPRECATED] List of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information
     * [DEPRECATED] List of CSI:FingerID structure database search candidates for the given &#39;formulaId&#39; with minimal information.  &lt;p&gt;  [DEPRECATED] Use /formulas/{formulaId}/db-structures/page instead. Loading all entries at once does not scale for large  result sets. This endpoint will be removed in the next major version of this API.  StructureCandidates can be enriched with molecular fingerprint, structure database links.
     * <p><b>200</b> - StructureCandidate of this formula candidate with specified optional fields.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature (aligned over runs) the formula result belongs to.
     * @param formulaId identifier of the requested formula result
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getStructureCandidatesByFormulaWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
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
    private ResponseSpec getStructureCandidatesByFormulaPageRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getStructureCandidatesByFormulaPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getStructureCandidatesByFormulaPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'formulaId' is set
        if (formulaId == null) {
            throw new WebClientResponseException("Missing the required parameter 'formulaId' when calling getStructureCandidatesByFormulaPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
    public PagedModelStructureCandidateScored getStructureCandidatesByFormulaPage(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelStructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateScored>() {};
        return getStructureCandidatesByFormulaPageRequestCreation(projectId, alignedFeatureId, formulaId, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
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
    public ResponseEntity<PagedModelStructureCandidateScored> getStructureCandidatesByFormulaPageWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelStructureCandidateScored> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateScored>() {};
        return getStructureCandidatesByFormulaPageRequestCreation(projectId, alignedFeatureId, formulaId, page, size, sort, optFields).toEntity(localVarReturnType).block();
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
    public ResponseSpec getStructureCandidatesByFormulaPageWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String formulaId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        return getStructureCandidatesByFormulaPageRequestCreation(projectId, alignedFeatureId, formulaId, page, size, sort, optFields);
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
    private ResponseSpec getStructureCandidatesPageRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getStructureCandidatesPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getStructureCandidatesPage", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
    public PagedModelStructureCandidateFormula getStructureCandidatesPage(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelStructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateFormula>() {};
        return getStructureCandidatesPageRequestCreation(projectId, alignedFeatureId, page, size, sort, optFields).bodyToMono(localVarReturnType).block();
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
    public ResponseEntity<PagedModelStructureCandidateFormula> getStructureCandidatesPageWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<PagedModelStructureCandidateFormula> localVarReturnType = new ParameterizedTypeReference<PagedModelStructureCandidateFormula>() {};
        return getStructureCandidatesPageRequestCreation(projectId, alignedFeatureId, page, size, sort, optFields).toEntity(localVarReturnType).block();
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
    public ResponseSpec getStructureCandidatesPageWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size, @jakarta.annotation.Nullable List<String> sort, @jakarta.annotation.Nullable List<StructureCandidateOptField> optFields) throws WebClientResponseException {
        return getStructureCandidatesPageRequestCreation(projectId, alignedFeatureId, page, size, sort, optFields);
    }

    /**
     * [EXPERIMENTAL] Get all tags associated with this Object
     * [EXPERIMENTAL] Get all tags associated with this Object
     * <p><b>200</b> - the tags of the requested object
     * @param projectId project-space to get from.
     * @param objectId object to get tags for.
     * @return List&lt;Tag&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getTagsForAlignedFeaturesExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String objectId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getTagsForAlignedFeaturesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'objectId' is set
        if (objectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'objectId' when calling getTagsForAlignedFeaturesExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/tags/{objectId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Get all tags associated with this Object
     * [EXPERIMENTAL] Get all tags associated with this Object
     * <p><b>200</b> - the tags of the requested object
     * @param projectId project-space to get from.
     * @param objectId object to get tags for.
     * @return List&lt;Tag&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Tag> getTagsForAlignedFeaturesExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String objectId) throws WebClientResponseException {
        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return getTagsForAlignedFeaturesExperimentalRequestCreation(projectId, objectId).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * [EXPERIMENTAL] Get all tags associated with this Object
     * [EXPERIMENTAL] Get all tags associated with this Object
     * <p><b>200</b> - the tags of the requested object
     * @param projectId project-space to get from.
     * @param objectId object to get tags for.
     * @return ResponseEntity&lt;List&lt;Tag&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Tag>> getTagsForAlignedFeaturesExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String objectId) throws WebClientResponseException {
        ParameterizedTypeReference<Tag> localVarReturnType = new ParameterizedTypeReference<Tag>() {};
        return getTagsForAlignedFeaturesExperimentalRequestCreation(projectId, objectId).toEntityList(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Get all tags associated with this Object
     * [EXPERIMENTAL] Get all tags associated with this Object
     * <p><b>200</b> - the tags of the requested object
     * @param projectId project-space to get from.
     * @param objectId object to get tags for.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getTagsForAlignedFeaturesExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String objectId) throws WebClientResponseException {
        return getTagsForAlignedFeaturesExperimentalRequestCreation(projectId, objectId);
    }

    /**
     * [EXPERIMENTAL] Returns the traces of the given feature (alignedFeatureId)
     * [EXPERIMENTAL] Returns the traces of the given feature (alignedFeatureId).  &lt;p&gt;  Returns the traces of the given feature. A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.  By default, this method only returns traces of samples the aligned feature appears in. When includeAll is set,  it also includes samples in which the same trace appears in.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Traces of the given feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature which intensities should be read out
     * @param includeAll when true, return all samples that belong to the same merged trace. when false, only return samples which contain the aligned feature.
     * @return TraceSetExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getTracesExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Boolean includeAll) throws WebClientResponseException {
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
     * [EXPERIMENTAL] Returns the traces of the given feature (alignedFeatureId)
     * [EXPERIMENTAL] Returns the traces of the given feature (alignedFeatureId).  &lt;p&gt;  Returns the traces of the given feature. A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.  By default, this method only returns traces of samples the aligned feature appears in. When includeAll is set,  it also includes samples in which the same trace appears in.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Traces of the given feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature which intensities should be read out
     * @param includeAll when true, return all samples that belong to the same merged trace. when false, only return samples which contain the aligned feature.
     * @return TraceSetExperimental
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public TraceSetExperimental getTracesExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Boolean includeAll) throws WebClientResponseException {
        ParameterizedTypeReference<TraceSetExperimental> localVarReturnType = new ParameterizedTypeReference<TraceSetExperimental>() {};
        return getTracesExperimentalRequestCreation(projectId, alignedFeatureId, includeAll).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Returns the traces of the given feature (alignedFeatureId)
     * [EXPERIMENTAL] Returns the traces of the given feature (alignedFeatureId).  &lt;p&gt;  Returns the traces of the given feature. A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.  By default, this method only returns traces of samples the aligned feature appears in. When includeAll is set,  it also includes samples in which the same trace appears in.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Traces of the given feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature which intensities should be read out
     * @param includeAll when true, return all samples that belong to the same merged trace. when false, only return samples which contain the aligned feature.
     * @return ResponseEntity&lt;TraceSetExperimental&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<TraceSetExperimental> getTracesExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Boolean includeAll) throws WebClientResponseException {
        ParameterizedTypeReference<TraceSetExperimental> localVarReturnType = new ParameterizedTypeReference<TraceSetExperimental>() {};
        return getTracesExperimentalRequestCreation(projectId, alignedFeatureId, includeAll).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Returns the traces of the given feature (alignedFeatureId)
     * [EXPERIMENTAL] Returns the traces of the given feature (alignedFeatureId).  &lt;p&gt;  Returns the traces of the given feature. A trace consists of m/z and intensity values over the retention  time axis. All the returned traces are &#39;projected&#39;, which means they refer not to the original retention time axis,  but to a recalibrated axis. This means the data points in the trace are not exactly the same as in the raw data.  However, this also means that all traces can be directly compared against each other, as they all lie in the same  retention time axis.  By default, this method only returns traces of samples the aligned feature appears in. When includeAll is set,  it also includes samples in which the same trace appears in.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - Traces of the given feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId feature which intensities should be read out
     * @param includeAll when true, return all samples that belong to the same merged trace. when false, only return samples which contain the aligned feature.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getTracesExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nullable Boolean includeAll) throws WebClientResponseException {
        return getTracesExperimentalRequestCreation(projectId, alignedFeatureId, includeAll);
    }

    /**
     * [EXPERIMENTAL] Delete tag with the given name from the feature (aligned over runs) with the specified ID in the specified project-space
     * [EXPERIMENTAL] Delete tag with the given name from the feature (aligned over runs) with the specified ID in the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param alignedFeatureId feature (aligned over runs) to delete tag from.
     * @param tagName name of the tag to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec removeTagFromAlignedFeatureExperimentalRequestCreation(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String tagName) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling removeTagFromAlignedFeatureExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling removeTagFromAlignedFeatureExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'tagName' is set
        if (tagName == null) {
            throw new WebClientResponseException("Missing the required parameter 'tagName' when calling removeTagFromAlignedFeatureExperimental", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("alignedFeatureId", alignedFeatureId);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/tags/{alignedFeatureId}/{tagName}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * [EXPERIMENTAL] Delete tag with the given name from the feature (aligned over runs) with the specified ID in the specified project-space
     * [EXPERIMENTAL] Delete tag with the given name from the feature (aligned over runs) with the specified ID in the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param alignedFeatureId feature (aligned over runs) to delete tag from.
     * @param tagName name of the tag to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void removeTagFromAlignedFeatureExperimental(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String tagName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        removeTagFromAlignedFeatureExperimentalRequestCreation(projectId, alignedFeatureId, tagName).bodyToMono(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Delete tag with the given name from the feature (aligned over runs) with the specified ID in the specified project-space
     * [EXPERIMENTAL] Delete tag with the given name from the feature (aligned over runs) with the specified ID in the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param alignedFeatureId feature (aligned over runs) to delete tag from.
     * @param tagName name of the tag to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> removeTagFromAlignedFeatureExperimentalWithHttpInfo(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String tagName) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return removeTagFromAlignedFeatureExperimentalRequestCreation(projectId, alignedFeatureId, tagName).toEntity(localVarReturnType).block();
    }

    /**
     * [EXPERIMENTAL] Delete tag with the given name from the feature (aligned over runs) with the specified ID in the specified project-space
     * [EXPERIMENTAL] Delete tag with the given name from the feature (aligned over runs) with the specified ID in the specified project-space.  &lt;p&gt;  [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param alignedFeatureId feature (aligned over runs) to delete tag from.
     * @param tagName name of the tag to delete.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec removeTagFromAlignedFeatureExperimentalWithResponseSpec(@jakarta.annotation.Nonnull String projectId, @jakarta.annotation.Nonnull String alignedFeatureId, @jakarta.annotation.Nonnull String tagName) throws WebClientResponseException {
        return removeTagFromAlignedFeatureExperimentalRequestCreation(projectId, alignedFeatureId, tagName);
    }
}
