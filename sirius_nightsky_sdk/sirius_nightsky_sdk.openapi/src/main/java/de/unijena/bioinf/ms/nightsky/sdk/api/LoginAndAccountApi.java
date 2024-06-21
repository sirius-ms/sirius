package de.unijena.bioinf.ms.nightsky.sdk.api;

import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;

import de.unijena.bioinf.ms.nightsky.sdk.model.AccountCredentials;
import de.unijena.bioinf.ms.nightsky.sdk.model.AccountInfo;
import de.unijena.bioinf.ms.nightsky.sdk.model.Subscription;

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
public class LoginAndAccountApi {
    private ApiClient apiClient;

    public LoginAndAccountApi() {
        this(new ApiClient());
    }

    @Autowired
    public LoginAndAccountApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Get information about the account currently logged in.
     * Get information about the account currently logged in. Fails if not logged in.
     * <p><b>200</b> - Basic information about the account that has been logged in and its subscriptions.
     * @param includeSubs include available and active subscriptions in {@link AccountInfo AccountInfo}.
     * @return AccountInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getAccountInfoRequestCreation(Boolean includeSubs) throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "includeSubs", includeSubs));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<AccountInfo> localVarReturnType = new ParameterizedTypeReference<AccountInfo>() {};
        return apiClient.invokeAPI("/api/account/", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get information about the account currently logged in.
     * Get information about the account currently logged in. Fails if not logged in.
     * <p><b>200</b> - Basic information about the account that has been logged in and its subscriptions.
     * @param includeSubs include available and active subscriptions in {@link AccountInfo AccountInfo}.
     * @return AccountInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public AccountInfo getAccountInfo(Boolean includeSubs) throws WebClientResponseException {
        ParameterizedTypeReference<AccountInfo> localVarReturnType = new ParameterizedTypeReference<AccountInfo>() {};
        return getAccountInfoRequestCreation(includeSubs).bodyToMono(localVarReturnType).block();
    }

    /**
     * Get information about the account currently logged in.
     * Get information about the account currently logged in. Fails if not logged in.
     * <p><b>200</b> - Basic information about the account that has been logged in and its subscriptions.
     * @param includeSubs include available and active subscriptions in {@link AccountInfo AccountInfo}.
     * @return ResponseEntity&lt;AccountInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AccountInfo> getAccountInfoWithHttpInfo(Boolean includeSubs) throws WebClientResponseException {
        ParameterizedTypeReference<AccountInfo> localVarReturnType = new ParameterizedTypeReference<AccountInfo>() {};
        return getAccountInfoRequestCreation(includeSubs).toEntity(localVarReturnType).block();
    }

    /**
     * Get information about the account currently logged in.
     * Get information about the account currently logged in. Fails if not logged in.
     * <p><b>200</b> - Basic information about the account that has been logged in and its subscriptions.
     * @param includeSubs include available and active subscriptions in {@link AccountInfo AccountInfo}.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getAccountInfoWithResponseSpec(Boolean includeSubs) throws WebClientResponseException {
        return getAccountInfoRequestCreation(includeSubs);
    }
    /**
     * Get SignUp URL (For signUp via web browser)
     * Get SignUp URL (For signUp via web browser)
     * <p><b>200</b> - OK
     * @return String
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getSignUpURLRequestCreation() throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "text/plain;charset=UTF-8"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return apiClient.invokeAPI("/api/account/signUpURL", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get SignUp URL (For signUp via web browser)
     * Get SignUp URL (For signUp via web browser)
     * <p><b>200</b> - OK
     * @return String
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public String getSignUpURL() throws WebClientResponseException {
        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return getSignUpURLRequestCreation().bodyToMono(localVarReturnType).block();
    }

    /**
     * Get SignUp URL (For signUp via web browser)
     * Get SignUp URL (For signUp via web browser)
     * <p><b>200</b> - OK
     * @return ResponseEntity&lt;String&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<String> getSignUpURLWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return getSignUpURLRequestCreation().toEntity(localVarReturnType).block();
    }

    /**
     * Get SignUp URL (For signUp via web browser)
     * Get SignUp URL (For signUp via web browser)
     * <p><b>200</b> - OK
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getSignUpURLWithResponseSpec() throws WebClientResponseException {
        return getSignUpURLRequestCreation();
    }
    /**
     * Get available subscriptions of the account currently logged in.
     * Get available subscriptions of the account currently logged in. Fails if not logged in.
     * <p><b>200</b> - OK
     * @return List&lt;Subscription&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getSubscriptionsRequestCreation() throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

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

        ParameterizedTypeReference<Subscription> localVarReturnType = new ParameterizedTypeReference<Subscription>() {};
        return apiClient.invokeAPI("/api/account/subscriptions", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get available subscriptions of the account currently logged in.
     * Get available subscriptions of the account currently logged in. Fails if not logged in.
     * <p><b>200</b> - OK
     * @return List&lt;Subscription&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public List<Subscription> getSubscriptions() throws WebClientResponseException {
        ParameterizedTypeReference<Subscription> localVarReturnType = new ParameterizedTypeReference<Subscription>() {};
        return getSubscriptionsRequestCreation().bodyToFlux(localVarReturnType).collectList().block();
    }

    /**
     * Get available subscriptions of the account currently logged in.
     * Get available subscriptions of the account currently logged in. Fails if not logged in.
     * <p><b>200</b> - OK
     * @return ResponseEntity&lt;List&lt;Subscription&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Subscription>> getSubscriptionsWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<Subscription> localVarReturnType = new ParameterizedTypeReference<Subscription>() {};
        return getSubscriptionsRequestCreation().toEntityList(localVarReturnType).block();
    }

    /**
     * Get available subscriptions of the account currently logged in.
     * Get available subscriptions of the account currently logged in. Fails if not logged in.
     * <p><b>200</b> - OK
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getSubscriptionsWithResponseSpec() throws WebClientResponseException {
        return getSubscriptionsRequestCreation();
    }
    /**
     * Check if a user is logged in.
     * Check if a user is logged in.
     * <p><b>200</b> - true if the user is logged in
     * @return Boolean
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec isLoggedInRequestCreation() throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

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

        ParameterizedTypeReference<Boolean> localVarReturnType = new ParameterizedTypeReference<Boolean>() {};
        return apiClient.invokeAPI("/api/account/isLoggedIn", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Check if a user is logged in.
     * Check if a user is logged in.
     * <p><b>200</b> - true if the user is logged in
     * @return Boolean
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Boolean isLoggedIn() throws WebClientResponseException {
        ParameterizedTypeReference<Boolean> localVarReturnType = new ParameterizedTypeReference<Boolean>() {};
        return isLoggedInRequestCreation().bodyToMono(localVarReturnType).block();
    }

    /**
     * Check if a user is logged in.
     * Check if a user is logged in.
     * <p><b>200</b> - true if the user is logged in
     * @return ResponseEntity&lt;Boolean&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Boolean> isLoggedInWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<Boolean> localVarReturnType = new ParameterizedTypeReference<Boolean>() {};
        return isLoggedInRequestCreation().toEntity(localVarReturnType).block();
    }

    /**
     * Check if a user is logged in.
     * Check if a user is logged in.
     * <p><b>200</b> - true if the user is logged in
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec isLoggedInWithResponseSpec() throws WebClientResponseException {
        return isLoggedInRequestCreation();
    }
    /**
     * Login into SIRIUS web services and activate default subscription if available.
     * Login into SIRIUS web services and activate default subscription if available.
     * <p><b>200</b> - Basic information about the account that has been logged in and its subscriptions.
     * @param acceptTerms The acceptTerms parameter
     * @param accountCredentials used to log in.
     * @param failWhenLoggedIn if true request fails if an active login already exists.
     * @param includeSubs include available and active subscriptions in {@link AccountInfo AccountInfo}.
     * @return AccountInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec loginRequestCreation(Boolean acceptTerms, AccountCredentials accountCredentials, Boolean failWhenLoggedIn, Boolean includeSubs) throws WebClientResponseException {
        Object postBody = accountCredentials;
        // verify the required parameter 'acceptTerms' is set
        if (acceptTerms == null) {
            throw new WebClientResponseException("Missing the required parameter 'acceptTerms' when calling login", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'accountCredentials' is set
        if (accountCredentials == null) {
            throw new WebClientResponseException("Missing the required parameter 'accountCredentials' when calling login", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "acceptTerms", acceptTerms));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "failWhenLoggedIn", failWhenLoggedIn));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "includeSubs", includeSubs));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<AccountInfo> localVarReturnType = new ParameterizedTypeReference<AccountInfo>() {};
        return apiClient.invokeAPI("/api/account/login", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Login into SIRIUS web services and activate default subscription if available.
     * Login into SIRIUS web services and activate default subscription if available.
     * <p><b>200</b> - Basic information about the account that has been logged in and its subscriptions.
     * @param acceptTerms The acceptTerms parameter
     * @param accountCredentials used to log in.
     * @param failWhenLoggedIn if true request fails if an active login already exists.
     * @param includeSubs include available and active subscriptions in {@link AccountInfo AccountInfo}.
     * @return AccountInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public AccountInfo login(Boolean acceptTerms, AccountCredentials accountCredentials, Boolean failWhenLoggedIn, Boolean includeSubs) throws WebClientResponseException {
        ParameterizedTypeReference<AccountInfo> localVarReturnType = new ParameterizedTypeReference<AccountInfo>() {};
        return loginRequestCreation(acceptTerms, accountCredentials, failWhenLoggedIn, includeSubs).bodyToMono(localVarReturnType).block();
    }

    /**
     * Login into SIRIUS web services and activate default subscription if available.
     * Login into SIRIUS web services and activate default subscription if available.
     * <p><b>200</b> - Basic information about the account that has been logged in and its subscriptions.
     * @param acceptTerms The acceptTerms parameter
     * @param accountCredentials used to log in.
     * @param failWhenLoggedIn if true request fails if an active login already exists.
     * @param includeSubs include available and active subscriptions in {@link AccountInfo AccountInfo}.
     * @return ResponseEntity&lt;AccountInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AccountInfo> loginWithHttpInfo(Boolean acceptTerms, AccountCredentials accountCredentials, Boolean failWhenLoggedIn, Boolean includeSubs) throws WebClientResponseException {
        ParameterizedTypeReference<AccountInfo> localVarReturnType = new ParameterizedTypeReference<AccountInfo>() {};
        return loginRequestCreation(acceptTerms, accountCredentials, failWhenLoggedIn, includeSubs).toEntity(localVarReturnType).block();
    }

    /**
     * Login into SIRIUS web services and activate default subscription if available.
     * Login into SIRIUS web services and activate default subscription if available.
     * <p><b>200</b> - Basic information about the account that has been logged in and its subscriptions.
     * @param acceptTerms The acceptTerms parameter
     * @param accountCredentials used to log in.
     * @param failWhenLoggedIn if true request fails if an active login already exists.
     * @param includeSubs include available and active subscriptions in {@link AccountInfo AccountInfo}.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec loginWithResponseSpec(Boolean acceptTerms, AccountCredentials accountCredentials, Boolean failWhenLoggedIn, Boolean includeSubs) throws WebClientResponseException {
        return loginRequestCreation(acceptTerms, accountCredentials, failWhenLoggedIn, includeSubs);
    }
    /**
     * Logout from SIRIUS web services.
     * Logout from SIRIUS web services.
     * <p><b>200</b> - OK
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec logoutRequestCreation() throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

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
        return apiClient.invokeAPI("/api/account/logout", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Logout from SIRIUS web services.
     * Logout from SIRIUS web services.
     * <p><b>200</b> - OK
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void logout() throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        logoutRequestCreation().bodyToMono(localVarReturnType).block();
    }

    /**
     * Logout from SIRIUS web services.
     * Logout from SIRIUS web services.
     * <p><b>200</b> - OK
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> logoutWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return logoutRequestCreation().toEntity(localVarReturnType).block();
    }

    /**
     * Logout from SIRIUS web services.
     * Logout from SIRIUS web services.
     * <p><b>200</b> - OK
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec logoutWithResponseSpec() throws WebClientResponseException {
        return logoutRequestCreation();
    }
    /**
     * Open User portal in browser.
     * Open User portal in browser. If user is logged in SIRIUS tries to transfer the login state to the browser.
     * <p><b>200</b> - OK
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec openPortalRequestCreation() throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

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
        return apiClient.invokeAPI("/api/account/openPortal", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Open User portal in browser.
     * Open User portal in browser. If user is logged in SIRIUS tries to transfer the login state to the browser.
     * <p><b>200</b> - OK
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public void openPortal() throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        openPortalRequestCreation().bodyToMono(localVarReturnType).block();
    }

    /**
     * Open User portal in browser.
     * Open User portal in browser. If user is logged in SIRIUS tries to transfer the login state to the browser.
     * <p><b>200</b> - OK
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> openPortalWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<Void>() {};
        return openPortalRequestCreation().toEntity(localVarReturnType).block();
    }

    /**
     * Open User portal in browser.
     * Open User portal in browser. If user is logged in SIRIUS tries to transfer the login state to the browser.
     * <p><b>200</b> - OK
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec openPortalWithResponseSpec() throws WebClientResponseException {
        return openPortalRequestCreation();
    }
    /**
     * Select a subscription as active subscription to be used for computations.
     * Select a subscription as active subscription to be used for computations.
     * <p><b>200</b> - Account information with updated active subscription
     * @param sid The sid parameter
     * @return AccountInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec selectSubscriptionRequestCreation(String sid) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'sid' is set
        if (sid == null) {
            throw new WebClientResponseException("Missing the required parameter 'sid' when calling selectSubscription", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "sid", sid));
        
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<AccountInfo> localVarReturnType = new ParameterizedTypeReference<AccountInfo>() {};
        return apiClient.invokeAPI("/api/account/subscriptions/select-active", HttpMethod.PUT, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Select a subscription as active subscription to be used for computations.
     * Select a subscription as active subscription to be used for computations.
     * <p><b>200</b> - Account information with updated active subscription
     * @param sid The sid parameter
     * @return AccountInfo
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public AccountInfo selectSubscription(String sid) throws WebClientResponseException {
        ParameterizedTypeReference<AccountInfo> localVarReturnType = new ParameterizedTypeReference<AccountInfo>() {};
        return selectSubscriptionRequestCreation(sid).bodyToMono(localVarReturnType).block();
    }

    /**
     * Select a subscription as active subscription to be used for computations.
     * Select a subscription as active subscription to be used for computations.
     * <p><b>200</b> - Account information with updated active subscription
     * @param sid The sid parameter
     * @return ResponseEntity&lt;AccountInfo&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AccountInfo> selectSubscriptionWithHttpInfo(String sid) throws WebClientResponseException {
        ParameterizedTypeReference<AccountInfo> localVarReturnType = new ParameterizedTypeReference<AccountInfo>() {};
        return selectSubscriptionRequestCreation(sid).toEntity(localVarReturnType).block();
    }

    /**
     * Select a subscription as active subscription to be used for computations.
     * Select a subscription as active subscription to be used for computations.
     * <p><b>200</b> - Account information with updated active subscription
     * @param sid The sid parameter
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec selectSubscriptionWithResponseSpec(String sid) throws WebClientResponseException {
        return selectSubscriptionRequestCreation(sid);
    }
    /**
     * Open SignUp window in system browser and return signUp link.
     * Open SignUp window in system browser and return signUp link.
     * <p><b>200</b> - OK
     * @return String
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec signUpRequestCreation() throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "text/plain;charset=UTF-8"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return apiClient.invokeAPI("/api/account/signUp", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Open SignUp window in system browser and return signUp link.
     * Open SignUp window in system browser and return signUp link.
     * <p><b>200</b> - OK
     * @return String
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public String signUp() throws WebClientResponseException {
        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return signUpRequestCreation().bodyToMono(localVarReturnType).block();
    }

    /**
     * Open SignUp window in system browser and return signUp link.
     * Open SignUp window in system browser and return signUp link.
     * <p><b>200</b> - OK
     * @return ResponseEntity&lt;String&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<String> signUpWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<String>() {};
        return signUpRequestCreation().toEntity(localVarReturnType).block();
    }

    /**
     * Open SignUp window in system browser and return signUp link.
     * Open SignUp window in system browser and return signUp link.
     * <p><b>200</b> - OK
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec signUpWithResponseSpec() throws WebClientResponseException {
        return signUpRequestCreation();
    }
}
