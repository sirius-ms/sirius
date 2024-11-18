package io.sirius.ms.sdk.api;

import io.sirius.ms.sdk.client.ApiClient;

import io.sirius.ms.sdk.model.AlignedFeatureOptField;
import io.sirius.ms.sdk.model.Compound;
import io.sirius.ms.sdk.model.CompoundFoldChange;
import io.sirius.ms.sdk.model.CompoundImport;
import io.sirius.ms.sdk.model.CompoundOptField;
import io.sirius.ms.sdk.model.InstrumentProfile;
import io.sirius.ms.sdk.model.Job;
import io.sirius.ms.sdk.model.JobOptField;
import io.sirius.ms.sdk.model.PageCompound;
import io.sirius.ms.sdk.model.PageCompoundFoldChange;
import io.sirius.ms.sdk.model.QuantificationTable;
import io.sirius.ms.sdk.model.TraceSet;

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
public class CompoundsApi {
    private ApiClient apiClient;

    public CompoundsApi() {
        this(new ApiClient());
    }

    @Autowired
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
    private ResponseSpec addCompoundsRequestCreation(String projectId, List<CompoundImport> compoundImport, InstrumentProfile profile, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
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
    public List<Compound> addCompounds(String projectId, List<CompoundImport> compoundImport, InstrumentProfile profile, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
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
    public ResponseEntity<List<Compound>> addCompoundsWithHttpInfo(String projectId, List<CompoundImport> compoundImport, InstrumentProfile profile, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
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
    public ResponseSpec addCompoundsWithResponseSpec(String projectId, List<CompoundImport> compoundImport, InstrumentProfile profile, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        return addCompoundsRequestCreation(projectId, compoundImport, profile, optFields, optFieldsFeatures);
    }
    /**
     * Compute the fold change between two groups
     * Compute the fold change between two groups.   &lt;p&gt;  &lt;h2&gt;EXPERIMENTAL&lt;/h2&gt;  This endpoint is experimental and not part of the stable API specification.  This endpoint can change at any time, even in minor updates.  &lt;/p&gt;   Computes the fold change between the left and right group.
     * <p><b>200</b> - OK
     * @param projectId project-space to compute the fold change in.
     * @param left name of the left group.
     * @param right name of the right group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @param optFields job opt fields.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec computeFoldChangeRequestCreation(String projectId, String left, String right, String aggregation, String quantification, List<JobOptField> optFields) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling computeFoldChange", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'left' is set
        if (left == null) {
            throw new WebClientResponseException("Missing the required parameter 'left' when calling computeFoldChange", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'right' is set
        if (right == null) {
            throw new WebClientResponseException("Missing the required parameter 'right' when calling computeFoldChange", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "left", left));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "right", right));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "aggregation", aggregation));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "quantification", quantification));
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFields", optFields));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/foldchange/compute", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Compute the fold change between two groups
     * Compute the fold change between two groups.   &lt;p&gt;  &lt;h2&gt;EXPERIMENTAL&lt;/h2&gt;  This endpoint is experimental and not part of the stable API specification.  This endpoint can change at any time, even in minor updates.  &lt;/p&gt;   Computes the fold change between the left and right group.
     * <p><b>200</b> - OK
     * @param projectId project-space to compute the fold change in.
     * @param left name of the left group.
     * @param right name of the right group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @param optFields job opt fields.
     * @return Job
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Job computeFoldChange(String projectId, String left, String right, String aggregation, String quantification, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return computeFoldChangeRequestCreation(projectId, left, right, aggregation, quantification, optFields).bodyToMono(localVarReturnType).block();
    }

    /**
     * Compute the fold change between two groups
     * Compute the fold change between two groups.   &lt;p&gt;  &lt;h2&gt;EXPERIMENTAL&lt;/h2&gt;  This endpoint is experimental and not part of the stable API specification.  This endpoint can change at any time, even in minor updates.  &lt;/p&gt;   Computes the fold change between the left and right group.
     * <p><b>200</b> - OK
     * @param projectId project-space to compute the fold change in.
     * @param left name of the left group.
     * @param right name of the right group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @param optFields job opt fields.
     * @return ResponseEntity&lt;Job&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Job> computeFoldChangeWithHttpInfo(String projectId, String left, String right, String aggregation, String quantification, List<JobOptField> optFields) throws WebClientResponseException {
        ParameterizedTypeReference<Job> localVarReturnType = new ParameterizedTypeReference<Job>() {};
        return computeFoldChangeRequestCreation(projectId, left, right, aggregation, quantification, optFields).toEntity(localVarReturnType).block();
    }

    /**
     * Compute the fold change between two groups
     * Compute the fold change between two groups.   &lt;p&gt;  &lt;h2&gt;EXPERIMENTAL&lt;/h2&gt;  This endpoint is experimental and not part of the stable API specification.  This endpoint can change at any time, even in minor updates.  &lt;/p&gt;   Computes the fold change between the left and right group.
     * <p><b>200</b> - OK
     * @param projectId project-space to compute the fold change in.
     * @param left name of the left group.
     * @param right name of the right group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @param optFields job opt fields.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec computeFoldChangeWithResponseSpec(String projectId, String left, String right, String aggregation, String quantification, List<JobOptField> optFields) throws WebClientResponseException {
        return computeFoldChangeRequestCreation(projectId, left, right, aggregation, quantification, optFields);
    }
    /**
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the  specified project-space.
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param compoundId identifier of the compound to delete.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteCompoundRequestCreation(String projectId, String compoundId) throws WebClientResponseException {
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
    public void deleteCompound(String projectId, String compoundId) throws WebClientResponseException {
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
    public ResponseEntity<Void> deleteCompoundWithHttpInfo(String projectId, String compoundId) throws WebClientResponseException {
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
    public ResponseSpec deleteCompoundWithResponseSpec(String projectId, String compoundId) throws WebClientResponseException {
        return deleteCompoundRequestCreation(projectId, compoundId);
    }
    /**
     * Delete fold change
     * Delete fold change.   &lt;p&gt;  &lt;h2&gt;EXPERIMENTAL&lt;/h2&gt;  This endpoint is experimental and not part of the stable API specification.  This endpoint can change at any time, even in minor updates.  &lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param left name of the left group.
     * @param right name of the right group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteFoldChangeRequestCreation(String projectId, String left, String right, String aggregation, String quantification) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling deleteFoldChange", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'left' is set
        if (left == null) {
            throw new WebClientResponseException("Missing the required parameter 'left' when calling deleteFoldChange", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'right' is set
        if (right == null) {
            throw new WebClientResponseException("Missing the required parameter 'right' when calling deleteFoldChange", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "left", left));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "right", right));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "aggregation", aggregation));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "quantification", quantification));
        
        final String[] localVarAccepts = { };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/foldchange", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Delete fold change
     * Delete fold change.   &lt;p&gt;  &lt;h2&gt;EXPERIMENTAL&lt;/h2&gt;  This endpoint is experimental and not part of the stable API specification.  This endpoint can change at any time, even in minor updates.  &lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param left name of the left group.
     * @param right name of the right group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteFoldChange(String projectId, String left, String right, String aggregation, String quantification) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        deleteFoldChangeRequestCreation(projectId, left, right, aggregation, quantification).bodyToMono(localVarReturnType).block();
    }

    /**
     * Delete fold change
     * Delete fold change.   &lt;p&gt;  &lt;h2&gt;EXPERIMENTAL&lt;/h2&gt;  This endpoint is experimental and not part of the stable API specification.  This endpoint can change at any time, even in minor updates.  &lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param left name of the left group.
     * @param right name of the right group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteFoldChangeWithHttpInfo(String projectId, String left, String right, String aggregation, String quantification) throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return deleteFoldChangeRequestCreation(projectId, left, right, aggregation, quantification).toEntity(localVarReturnType).block();
    }

    /**
     * Delete fold change
     * Delete fold change.   &lt;p&gt;  &lt;h2&gt;EXPERIMENTAL&lt;/h2&gt;  This endpoint is experimental and not part of the stable API specification.  This endpoint can change at any time, even in minor updates.  &lt;/p&gt;
     * <p><b>200</b> - OK
     * @param projectId project-space to delete from.
     * @param left name of the left group.
     * @param right name of the right group.
     * @param aggregation aggregation type.
     * @param quantification quantification type.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteFoldChangeWithResponseSpec(String projectId, String left, String right, String aggregation, String quantification) throws WebClientResponseException {
        return deleteFoldChangeRequestCreation(projectId, left, right, aggregation, quantification);
    }
    /**
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param compoundId identifier of the compound (group of ion identities) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return Compound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundRequestCreation(String projectId, String compoundId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
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
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return Compound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Compound getCompound(String projectId, String compoundId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return getCompoundRequestCreation(projectId, compoundId, optFields, optFieldsFeatures).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param compoundId identifier of the compound (group of ion identities) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseEntity&lt;Compound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Compound> getCompoundWithHttpInfo(String projectId, String compoundId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return getCompoundRequestCreation(projectId, compoundId, optFields, optFieldsFeatures).toEntity(localVarReturnType).block();
    }

    /**
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param compoundId identifier of the compound (group of ion identities) to access.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundWithResponseSpec(String projectId, String compoundId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        return getCompoundRequestCreation(projectId, compoundId, optFields, optFieldsFeatures);
    }
    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param compoundId The compoundId parameter
     * @param featureId The featureId parameter
     * @return TraceSet
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundTracesRequestCreation(String projectId, String compoundId, String featureId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompoundTraces", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling getCompoundTraces", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<TraceSet> localVarReturnType = new ParameterizedTypeReference<TraceSet>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/{compoundId}/traces", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param compoundId The compoundId parameter
     * @param featureId The featureId parameter
     * @return TraceSet
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public TraceSet getCompoundTraces(String projectId, String compoundId, String featureId) throws WebClientResponseException {
        ParameterizedTypeReference<TraceSet> localVarReturnType = new ParameterizedTypeReference<TraceSet>() {};
        return getCompoundTracesRequestCreation(projectId, compoundId, featureId).bodyToMono(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param compoundId The compoundId parameter
     * @param featureId The featureId parameter
     * @return ResponseEntity&lt;TraceSet&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<TraceSet> getCompoundTracesWithHttpInfo(String projectId, String compoundId, String featureId) throws WebClientResponseException {
        ParameterizedTypeReference<TraceSet> localVarReturnType = new ParameterizedTypeReference<TraceSet>() {};
        return getCompoundTracesRequestCreation(projectId, compoundId, featureId).toEntity(localVarReturnType).block();
    }

    /**
     * 
     * 
     * <p><b>200</b> - OK
     * @param projectId The projectId parameter
     * @param compoundId The compoundId parameter
     * @param featureId The featureId parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundTracesWithResponseSpec(String projectId, String compoundId, String featureId) throws WebClientResponseException {
        return getCompoundTracesRequestCreation(projectId, compoundId, featureId);
    }
    /**
     * List of all available compounds (group of ion identities) in the given project-space.
     * List of all available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return List&lt;Compound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundsRequestCreation(String projectId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
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
     * List of all available compounds (group of ion identities) in the given project-space.
     * List of all available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return List&lt;Compound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Compound> getCompounds(String projectId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return getCompoundsRequestCreation(projectId, optFields, optFieldsFeatures).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * List of all available compounds (group of ion identities) in the given project-space.
     * List of all available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseEntity&lt;List&lt;Compound&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Compound>> getCompoundsWithHttpInfo(String projectId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<Compound> localVarReturnType = new ParameterizedTypeReference<Compound>() {};
        return getCompoundsRequestCreation(projectId, optFields, optFieldsFeatures).toEntityList(localVarReturnType).block();
    }

    /**
     * List of all available compounds (group of ion identities) in the given project-space.
     * List of all available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundsWithResponseSpec(String projectId, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        return getCompoundsRequestCreation(projectId, optFields, optFieldsFeatures);
    }
    /**
     * Page of available compounds (group of ion identities) in the given project-space.
     * Page of available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return PageCompound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCompoundsPagedRequestCreation(String projectId, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getCompoundsPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "optFieldsFeatures", optFieldsFeatures));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PageCompound> localVarReturnType = new ParameterizedTypeReference<PageCompound>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/page", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Page of available compounds (group of ion identities) in the given project-space.
     * Page of available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return PageCompound
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PageCompound getCompoundsPaged(String projectId, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<PageCompound> localVarReturnType = new ParameterizedTypeReference<PageCompound>() {};
        return getCompoundsPagedRequestCreation(projectId, page, size, sort, optFields, optFieldsFeatures).bodyToMono(localVarReturnType).block();
    }

    /**
     * Page of available compounds (group of ion identities) in the given project-space.
     * Page of available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseEntity&lt;PageCompound&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PageCompound> getCompoundsPagedWithHttpInfo(String projectId, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        ParameterizedTypeReference<PageCompound> localVarReturnType = new ParameterizedTypeReference<PageCompound>() {};
        return getCompoundsPagedRequestCreation(projectId, page, size, sort, optFields, optFieldsFeatures).toEntity(localVarReturnType).block();
    }

    /**
     * Page of available compounds (group of ion identities) in the given project-space.
     * Page of available compounds (group of ion identities) in the given project-space.
     * <p><b>200</b> - Compounds with additional optional fields (if specified).
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @param optFields set of optional fields to be included. Use &#39;none&#39; only to override defaults.
     * @param optFieldsFeatures The optFieldsFeatures parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCompoundsPagedWithResponseSpec(String projectId, Integer page, Integer size, List<String> sort, List<CompoundOptField> optFields, List<AlignedFeatureOptField> optFieldsFeatures) throws WebClientResponseException {
        return getCompoundsPagedRequestCreation(projectId, page, size, sort, optFields, optFieldsFeatures);
    }
    /**
     * List all fold changes that are associated with an object
     * List all fold changes that are associated with an object.   &lt;p&gt;  &lt;h2&gt;EXPERIMENTAL&lt;/h2&gt;  This endpoint is experimental and not part of the stable API specification.  This endpoint can change at any time, even in minor updates.  &lt;/p&gt;
     * <p><b>200</b> - fold changes
     * @param projectId project-space to read from.
     * @param objectId id of the object the fold changes are assigned to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return List&lt;CompoundFoldChange&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getFoldChangeRequestCreation(String projectId, String objectId, Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getFoldChange", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'objectId' is set
        if (objectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'objectId' when calling getFoldChange", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);
        pathParams.put("objectId", objectId);

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

        ParameterizedTypeReference<CompoundFoldChange> localVarReturnType = new ParameterizedTypeReference<CompoundFoldChange>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/foldchange/{objectId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List all fold changes that are associated with an object
     * List all fold changes that are associated with an object.   &lt;p&gt;  &lt;h2&gt;EXPERIMENTAL&lt;/h2&gt;  This endpoint is experimental and not part of the stable API specification.  This endpoint can change at any time, even in minor updates.  &lt;/p&gt;
     * <p><b>200</b> - fold changes
     * @param projectId project-space to read from.
     * @param objectId id of the object the fold changes are assigned to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return List&lt;CompoundFoldChange&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<CompoundFoldChange> getFoldChange(String projectId, String objectId, Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        ParameterizedTypeReference<CompoundFoldChange> localVarReturnType = new ParameterizedTypeReference<CompoundFoldChange>() {};
        return getFoldChangeRequestCreation(projectId, objectId, page, size, sort).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * List all fold changes that are associated with an object
     * List all fold changes that are associated with an object.   &lt;p&gt;  &lt;h2&gt;EXPERIMENTAL&lt;/h2&gt;  This endpoint is experimental and not part of the stable API specification.  This endpoint can change at any time, even in minor updates.  &lt;/p&gt;
     * <p><b>200</b> - fold changes
     * @param projectId project-space to read from.
     * @param objectId id of the object the fold changes are assigned to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return ResponseEntity&lt;List&lt;CompoundFoldChange&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<CompoundFoldChange>> getFoldChangeWithHttpInfo(String projectId, String objectId, Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        ParameterizedTypeReference<CompoundFoldChange> localVarReturnType = new ParameterizedTypeReference<CompoundFoldChange>() {};
        return getFoldChangeRequestCreation(projectId, objectId, page, size, sort).toEntityList(localVarReturnType).block();
    }

    /**
     * List all fold changes that are associated with an object
     * List all fold changes that are associated with an object.   &lt;p&gt;  &lt;h2&gt;EXPERIMENTAL&lt;/h2&gt;  This endpoint is experimental and not part of the stable API specification.  This endpoint can change at any time, even in minor updates.  &lt;/p&gt;
     * <p><b>200</b> - fold changes
     * @param projectId project-space to read from.
     * @param objectId id of the object the fold changes are assigned to.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getFoldChangeWithResponseSpec(String projectId, String objectId, Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        return getFoldChangeRequestCreation(projectId, objectId, page, size, sort);
    }
    /**
     * Returns the full quantification table.
     * Returns the full quantification table. The quantification table contains a quantification of the features within all  runs they are contained in.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param type quantification type.
     * @return QuantificationTable
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getQuantificationRequestCreation(String projectId, String type) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getQuantification", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("projectId", projectId);

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

        ParameterizedTypeReference<QuantificationTable> localVarReturnType = new ParameterizedTypeReference<QuantificationTable>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/quantification", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Returns the full quantification table.
     * Returns the full quantification table. The quantification table contains a quantification of the features within all  runs they are contained in.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param type quantification type.
     * @return QuantificationTable
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public QuantificationTable getQuantification(String projectId, String type) throws WebClientResponseException {
        ParameterizedTypeReference<QuantificationTable> localVarReturnType = new ParameterizedTypeReference<QuantificationTable>() {};
        return getQuantificationRequestCreation(projectId, type).bodyToMono(localVarReturnType).block();
    }

    /**
     * Returns the full quantification table.
     * Returns the full quantification table. The quantification table contains a quantification of the features within all  runs they are contained in.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param type quantification type.
     * @return ResponseEntity&lt;QuantificationTable&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<QuantificationTable> getQuantificationWithHttpInfo(String projectId, String type) throws WebClientResponseException {
        ParameterizedTypeReference<QuantificationTable> localVarReturnType = new ParameterizedTypeReference<QuantificationTable>() {};
        return getQuantificationRequestCreation(projectId, type).toEntity(localVarReturnType).block();
    }

    /**
     * Returns the full quantification table.
     * Returns the full quantification table. The quantification table contains a quantification of the features within all  runs they are contained in.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param type quantification type.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getQuantificationWithResponseSpec(String projectId, String type) throws WebClientResponseException {
        return getQuantificationRequestCreation(projectId, type);
    }
    /**
     * Returns a single quantification table row for the given feature.
     * Returns a single quantification table row for the given feature. The quantification table contains a quantification of the feature within all  samples it is contained in.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param compoundId compound which should be read out
     * @param type quantification type.
     * @return QuantificationTable
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getQuantificationRowRequestCreation(String projectId, String compoundId, String type) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getQuantificationRow", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'compoundId' is set
        if (compoundId == null) {
            throw new WebClientResponseException("Missing the required parameter 'compoundId' when calling getQuantificationRow", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<QuantificationTable> localVarReturnType = new ParameterizedTypeReference<QuantificationTable>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/{compoundId}/quantification", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Returns a single quantification table row for the given feature.
     * Returns a single quantification table row for the given feature. The quantification table contains a quantification of the feature within all  samples it is contained in.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param compoundId compound which should be read out
     * @param type quantification type.
     * @return QuantificationTable
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public QuantificationTable getQuantificationRow(String projectId, String compoundId, String type) throws WebClientResponseException {
        ParameterizedTypeReference<QuantificationTable> localVarReturnType = new ParameterizedTypeReference<QuantificationTable>() {};
        return getQuantificationRowRequestCreation(projectId, compoundId, type).bodyToMono(localVarReturnType).block();
    }

    /**
     * Returns a single quantification table row for the given feature.
     * Returns a single quantification table row for the given feature. The quantification table contains a quantification of the feature within all  samples it is contained in.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param compoundId compound which should be read out
     * @param type quantification type.
     * @return ResponseEntity&lt;QuantificationTable&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<QuantificationTable> getQuantificationRowWithHttpInfo(String projectId, String compoundId, String type) throws WebClientResponseException {
        ParameterizedTypeReference<QuantificationTable> localVarReturnType = new ParameterizedTypeReference<QuantificationTable>() {};
        return getQuantificationRowRequestCreation(projectId, compoundId, type).toEntity(localVarReturnType).block();
    }

    /**
     * Returns a single quantification table row for the given feature.
     * Returns a single quantification table row for the given feature. The quantification table contains a quantification of the feature within all  samples it is contained in.
     * <p><b>200</b> - OK
     * @param projectId project-space to read from.
     * @param compoundId compound which should be read out
     * @param type quantification type.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getQuantificationRowWithResponseSpec(String projectId, String compoundId, String type) throws WebClientResponseException {
        return getQuantificationRowRequestCreation(projectId, compoundId, type);
    }
    /**
     * List all fold changes in the project space
     * List all fold changes in the project space.   &lt;p&gt;  &lt;h2&gt;EXPERIMENTAL&lt;/h2&gt;  This endpoint is experimental and not part of the stable API specification.  This endpoint can change at any time, even in minor updates.  &lt;/p&gt;
     * <p><b>200</b> - fold changes.
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return PageCompoundFoldChange
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec listFoldChangeRequestCreation(String projectId, Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling listFoldChange", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PageCompoundFoldChange> localVarReturnType = new ParameterizedTypeReference<PageCompoundFoldChange>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/compounds/foldchange", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List all fold changes in the project space
     * List all fold changes in the project space.   &lt;p&gt;  &lt;h2&gt;EXPERIMENTAL&lt;/h2&gt;  This endpoint is experimental and not part of the stable API specification.  This endpoint can change at any time, even in minor updates.  &lt;/p&gt;
     * <p><b>200</b> - fold changes.
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return PageCompoundFoldChange
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PageCompoundFoldChange listFoldChange(String projectId, Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        ParameterizedTypeReference<PageCompoundFoldChange> localVarReturnType = new ParameterizedTypeReference<PageCompoundFoldChange>() {};
        return listFoldChangeRequestCreation(projectId, page, size, sort).bodyToMono(localVarReturnType).block();
    }

    /**
     * List all fold changes in the project space
     * List all fold changes in the project space.   &lt;p&gt;  &lt;h2&gt;EXPERIMENTAL&lt;/h2&gt;  This endpoint is experimental and not part of the stable API specification.  This endpoint can change at any time, even in minor updates.  &lt;/p&gt;
     * <p><b>200</b> - fold changes.
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return ResponseEntity&lt;PageCompoundFoldChange&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PageCompoundFoldChange> listFoldChangeWithHttpInfo(String projectId, Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        ParameterizedTypeReference<PageCompoundFoldChange> localVarReturnType = new ParameterizedTypeReference<PageCompoundFoldChange>() {};
        return listFoldChangeRequestCreation(projectId, page, size, sort).toEntity(localVarReturnType).block();
    }

    /**
     * List all fold changes in the project space
     * List all fold changes in the project space.   &lt;p&gt;  &lt;h2&gt;EXPERIMENTAL&lt;/h2&gt;  This endpoint is experimental and not part of the stable API specification.  This endpoint can change at any time, even in minor updates.  &lt;/p&gt;
     * <p><b>200</b> - fold changes.
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec listFoldChangeWithResponseSpec(String projectId, Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        return listFoldChangeRequestCreation(projectId, page, size, sort);
    }
}
