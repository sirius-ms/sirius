package de.unijena.bioinf.ms.nightsky.sdk.api;

import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;

import de.unijena.bioinf.ms.nightsky.sdk.model.AlignedFeatureQuality;
import de.unijena.bioinf.ms.nightsky.sdk.model.PageAlignedFeatureQuality;

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

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class ExperimentalDataQualityApi {
    private ApiClient apiClient;

    public ExperimentalDataQualityApi() {
        this(new ApiClient());
    }

    @Autowired
    public ExperimentalDataQualityApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * List of data quality information for features (aligned over runs) in the given project-space.
     * List of data quality information for features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @return List&lt;AlignedFeatureQuality&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAlignedFeaturesQualityRequestCreation(String projectId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getAlignedFeaturesQuality", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<AlignedFeatureQuality> localVarReturnType = new ParameterizedTypeReference<AlignedFeatureQuality>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features-quality", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * List of data quality information for features (aligned over runs) in the given project-space.
     * List of data quality information for features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @return List&lt;AlignedFeatureQuality&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<AlignedFeatureQuality> getAlignedFeaturesQuality(String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeatureQuality> localVarReturnType = new ParameterizedTypeReference<AlignedFeatureQuality>() {};
        return getAlignedFeaturesQualityRequestCreation(projectId).bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * List of data quality information for features (aligned over runs) in the given project-space.
     * List of data quality information for features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @return ResponseEntity&lt;List&lt;AlignedFeatureQuality&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<AlignedFeatureQuality>> getAlignedFeaturesQualityWithHttpInfo(String projectId) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeatureQuality> localVarReturnType = new ParameterizedTypeReference<AlignedFeatureQuality>() {};
        return getAlignedFeaturesQualityRequestCreation(projectId).toEntityList(localVarReturnType).block();
    }

    /**
     * List of data quality information for features (aligned over runs) in the given project-space.
     * List of data quality information for features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAlignedFeaturesQualityWithResponseSpec(String projectId) throws WebClientResponseException {
        return getAlignedFeaturesQualityRequestCreation(projectId);
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
    private ResponseSpec getAlignedFeaturesQuality1RequestCreation(String projectId, String alignedFeatureId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getAlignedFeaturesQuality1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'alignedFeatureId' is set
        if (alignedFeatureId == null) {
            throw new WebClientResponseException("Missing the required parameter 'alignedFeatureId' when calling getAlignedFeaturesQuality1", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features-quality/{alignedFeatureId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
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
    public AlignedFeatureQuality getAlignedFeaturesQuality1(String projectId, String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeatureQuality> localVarReturnType = new ParameterizedTypeReference<AlignedFeatureQuality>() {};
        return getAlignedFeaturesQuality1RequestCreation(projectId, alignedFeatureId).bodyToMono(localVarReturnType).block();
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
    public ResponseEntity<AlignedFeatureQuality> getAlignedFeaturesQuality1WithHttpInfo(String projectId, String alignedFeatureId) throws WebClientResponseException {
        ParameterizedTypeReference<AlignedFeatureQuality> localVarReturnType = new ParameterizedTypeReference<AlignedFeatureQuality>() {};
        return getAlignedFeaturesQuality1RequestCreation(projectId, alignedFeatureId).toEntity(localVarReturnType).block();
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
    public ResponseSpec getAlignedFeaturesQuality1WithResponseSpec(String projectId, String alignedFeatureId) throws WebClientResponseException {
        return getAlignedFeaturesQuality1RequestCreation(projectId, alignedFeatureId);
    }
    /**
     * Page of data quality information for features (aligned over runs) in the given project-space.
     * Page of data quality information for features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return PageAlignedFeatureQuality
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAlignedFeaturesQualityPagedRequestCreation(String projectId, Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'projectId' is set
        if (projectId == null) {
            throw new WebClientResponseException("Missing the required parameter 'projectId' when calling getAlignedFeaturesQualityPaged", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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

        ParameterizedTypeReference<PageAlignedFeatureQuality> localVarReturnType = new ParameterizedTypeReference<PageAlignedFeatureQuality>() {};
        return apiClient.invokeAPI("/api/projects/{projectId}/aligned-features-quality/page", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Page of data quality information for features (aligned over runs) in the given project-space.
     * Page of data quality information for features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return PageAlignedFeatureQuality
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public PageAlignedFeatureQuality getAlignedFeaturesQualityPaged(String projectId, Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        ParameterizedTypeReference<PageAlignedFeatureQuality> localVarReturnType = new ParameterizedTypeReference<PageAlignedFeatureQuality>() {};
        return getAlignedFeaturesQualityPagedRequestCreation(projectId, page, size, sort).bodyToMono(localVarReturnType).block();
    }

    /**
     * Page of data quality information for features (aligned over runs) in the given project-space.
     * Page of data quality information for features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return ResponseEntity&lt;PageAlignedFeatureQuality&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PageAlignedFeatureQuality> getAlignedFeaturesQualityPagedWithHttpInfo(String projectId, Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        ParameterizedTypeReference<PageAlignedFeatureQuality> localVarReturnType = new ParameterizedTypeReference<PageAlignedFeatureQuality>() {};
        return getAlignedFeaturesQualityPagedRequestCreation(projectId, page, size, sort).toEntity(localVarReturnType).block();
    }

    /**
     * Page of data quality information for features (aligned over runs) in the given project-space.
     * Page of data quality information for features (aligned over runs) in the given project-space.
     * <p><b>200</b> - AlignedFeatureQuality quality information of the respective feature.
     * @param projectId project-space to read from.
     * @param page Zero-based page index (0..N)
     * @param size The size of the page to be returned
     * @param sort Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAlignedFeaturesQualityPagedWithResponseSpec(String projectId, Integer page, Integer size, List<String> sort) throws WebClientResponseException {
        return getAlignedFeaturesQualityPagedRequestCreation(projectId, page, size, sort);
    }
}
