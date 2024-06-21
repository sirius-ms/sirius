package de.unijena.bioinf.ms.nightsky.sdk.api;

import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;

import de.unijena.bioinf.ms.nightsky.sdk.model.AlignedFeatureQuality;

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
public class ExperimentalApi {
    private ApiClient apiClient;

    public ExperimentalApi() {
        this(new ApiClient());
    }

    @Autowired
    public ExperimentalApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.
     * Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @return AlignedFeatureQuality
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAlignedFeaturesQualityRequestCreation(String projectId, String alignedFeatureId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getAlignedFeaturesQuality", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getAlignedFeaturesQuality", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<AlignedFeatureQuality> localVarReturnType = new ParameterizedTypeReference<AlignedFeatureQuality>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features/{alignedFeatureId}/quality-report", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.
     * Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @return AlignedFeatureQuality
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public AlignedFeatureQuality getAlignedFeaturesQuality(String projectId, String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeatureQuality> localVarReturnType = new ParameterizedTypeReference<AlignedFeatureQuality>() {};
        return getAlignedFeaturesQualityRequestCreation(projectId, alignedFeatureId).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.
     * Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @return ResponseEntity&lt;AlignedFeatureQuality&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AlignedFeatureQuality> getAlignedFeaturesQualityWithHttpInfo(String projectId, String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeatureQuality> localVarReturnType = new ParameterizedTypeReference<AlignedFeatureQuality>() {};
        return getAlignedFeaturesQualityRequestCreation(projectId, alignedFeatureId).toEntity(localVarReturnType).block();
    }

    /**
     * Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.
     * Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAlignedFeaturesQualityWithResponseSpec(String projectId, String alignedFeatureId) throws WebClientResponseException {
        return getAlignedFeaturesQualityRequestCreation(projectId, alignedFeatureId);
    }
}
