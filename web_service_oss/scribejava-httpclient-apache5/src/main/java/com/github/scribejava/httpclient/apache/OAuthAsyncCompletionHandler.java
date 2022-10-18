package com.github.scribejava.httpclient.apache;

import com.github.scribejava.core.model.OAuthAsyncRequestCallback;
import com.github.scribejava.core.model.OAuthRequest.ResponseConverter;
import com.github.scribejava.core.model.Response;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.Header;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;


public class OAuthAsyncCompletionHandler<T> implements FutureCallback<SimpleHttpResponse> {

    private final OAuthAsyncRequestCallback<T> callback;
    private final ResponseConverter<T> converter;
    private final CountDownLatch latch;
    private T result;
    private Exception exception;

    public OAuthAsyncCompletionHandler(OAuthAsyncRequestCallback<T> callback, ResponseConverter<T> converter) {
        this.callback = callback;
        this.converter = converter;
        this.latch = new CountDownLatch(1);
    }

    @Override
    public void completed(SimpleHttpResponse httpResponse) {
        try {
            final Map<String, String> headersMap = new HashMap<>();
            for (Header header : httpResponse.getHeaders()) {
                headersMap.put(header.getName(), header.getValue());
            }


            final InputStream contentStream = new ByteArrayInputStream(httpResponse.getBodyBytes());;
            final Response response = new Response(httpResponse.getCode(), httpResponse.getReasonPhrase(), headersMap,
                    contentStream, contentStream);

            @SuppressWarnings("unchecked")
            final T t = converter == null ? (T) response : converter.convert(response);
            result = t;
            if (callback != null) {
                callback.onCompleted(result);
            }
        } catch (IOException | RuntimeException e) {
            exception = e;
            if (callback != null) {
                callback.onThrowable(e);
            }
        } finally {
            latch.countDown();
        }
    }

    @Override
    public void failed(Exception e) {
        exception = e;
        try {
            if (callback != null) {
                callback.onThrowable(e);
            }
        } finally {
            latch.countDown();
        }
    }

    @Override
    public void cancelled() {
        exception = new CancellationException();
        try {
            if (callback != null) {
                callback.onThrowable(exception);
            }
        } finally {
            latch.countDown();
        }
    }

    public T getResult() throws InterruptedException, ExecutionException {
        latch.await();
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return result;
    }

    public T getResult(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

        if (!latch.await(timeout, unit)) {
            throw new TimeoutException();
        }
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return result;
    }
}
