package com.github.scribejava.httpclient.apache;

import com.github.scribejava.core.httpclient.AbstractAsyncOnlyHttpClient;
import com.github.scribejava.core.httpclient.multipart.MultipartPayload;
import com.github.scribejava.core.model.OAuthAsyncRequestCallback;
import com.github.scribejava.core.model.OAuthConstants;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Future;


public class ApacheHttpClient extends AbstractAsyncOnlyHttpClient {

    private final CloseableHttpAsyncClient client;

    public ApacheHttpClient() {
        this(ApacheHttpClientConfig.defaultConfig());
    }

    public ApacheHttpClient(ApacheHttpClientConfig config) {
        this(config.getHttpAsyncClientBuilder());
    }

    public ApacheHttpClient(HttpAsyncClientBuilder builder) {
        this(builder.build());
    }

    public ApacheHttpClient(CloseableHttpAsyncClient client) {
        this.client = client;
        this.client.start();
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    @Override
    public <T> Future<T> executeAsync(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl,
            byte[] bodyContents, OAuthAsyncRequestCallback<T> callback, OAuthRequest.ResponseConverter<T> converter) {
        return doExecuteAsync(userAgent, headers, httpVerb, completeUrl, bodyContents, callback, converter);
    }

    @Override
    public <T> Future<T> executeAsync(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl,
            MultipartPayload bodyContents, OAuthAsyncRequestCallback<T> callback,
            OAuthRequest.ResponseConverter<T> converter) {

        throw new UnsupportedOperationException("ApacheHttpClient does not support MultipartPayload yet.");
    }

    @Override
    public <T> Future<T> executeAsync(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl,
            String bodyContents, OAuthAsyncRequestCallback<T> callback, OAuthRequest.ResponseConverter<T> converter) {
        return doExecuteAsync(userAgent, headers, httpVerb, completeUrl, bodyContents.getBytes(StandardCharsets.UTF_8), callback, converter);
    }

    @Override
    public <T> Future<T> executeAsync(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl,
            File bodyContents, OAuthAsyncRequestCallback<T> callback, OAuthRequest.ResponseConverter<T> converter) {

        try (FileInputStream s = new FileInputStream(bodyContents)){
            return doExecuteAsync(userAgent, headers, httpVerb, completeUrl, s.readAllBytes(), callback, converter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> Future<T> doExecuteAsync(String userAgent, Map<String, String> headers, Verb httpVerb,
                                         String completeUrl, byte[] body, OAuthAsyncRequestCallback<T> callback,
                                         OAuthRequest.ResponseConverter<T> converter) {
        final SimpleRequestBuilder builder = getRequestBuilder(httpVerb);
        builder.setUri(completeUrl);

        if (httpVerb.isPermitBody()) {
            if (!headers.containsKey(CONTENT_TYPE)) {
                builder.addHeader(CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
            }
            builder.setBody(body, ContentType.create(headers.getOrDefault(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())));
        }

        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.addHeader(header.getKey(), header.getValue());
        }

        if (userAgent != null) {
            builder.setHeader(OAuthConstants.USER_AGENT_HEADER_NAME, userAgent);
        }

//        builder.setVersion(new ProtocolVersion("4", 1, 1));

        final OAuthAsyncCompletionHandler<T> handler = new OAuthAsyncCompletionHandler<>(callback, converter);
        final Future<SimpleHttpResponse> future = client.execute(builder.build(), handler);
        return new ApacheHttpFuture<>(future, handler);
    }

    private static SimpleRequestBuilder getRequestBuilder(Verb httpVerb) {
        switch (httpVerb) {
            case GET:
                return SimpleRequestBuilder.get();
            case PUT:
                return SimpleRequestBuilder.put();
            case DELETE:
                return SimpleRequestBuilder.delete();
            case HEAD:
                return SimpleRequestBuilder.head();
            case POST:
                return SimpleRequestBuilder.post();
            case PATCH:
                return SimpleRequestBuilder.patch();
            case TRACE:
                return SimpleRequestBuilder.trace();
            case OPTIONS:
                return SimpleRequestBuilder.options();
            default:
                throw new IllegalArgumentException("message build error: unknown verb type");
        }
    }
}
