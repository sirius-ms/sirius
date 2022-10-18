package com.github.scribejava.httpclient.apache;

import com.github.scribejava.core.httpclient.HttpClientConfig;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;


public class ApacheHttpClientConfig implements HttpClientConfig {

    private final HttpAsyncClientBuilder httpAsyncClientBuilder;

    public ApacheHttpClientConfig(HttpAsyncClientBuilder httpAsyncClientBuilder) {
        this.httpAsyncClientBuilder = httpAsyncClientBuilder;
    }

    public HttpAsyncClientBuilder getHttpAsyncClientBuilder() {
        return httpAsyncClientBuilder;
    }

    @Override
    public HttpClientConfig createDefaultConfig() {
        return defaultConfig();
    }

    public static ApacheHttpClientConfig defaultConfig() {
        return new ApacheHttpClientConfig(HttpAsyncClientBuilder.create());
    }
}
