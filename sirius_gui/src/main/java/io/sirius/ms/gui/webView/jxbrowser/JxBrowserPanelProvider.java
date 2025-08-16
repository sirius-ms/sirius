package io.sirius.ms.gui.webView.jxbrowser;

import com.teamdev.jxbrowser.engine.Engine;
import com.teamdev.jxbrowser.engine.EngineOptions;
import com.teamdev.jxbrowser.engine.Theme;
import com.teamdev.jxbrowser.net.HttpHeader;
import com.teamdev.jxbrowser.net.callback.BeforeStartTransactionCallback;
import com.teamdev.jxbrowser.permission.PermissionType;
import com.teamdev.jxbrowser.permission.callback.RequestPermissionCallback;
import de.unijena.bioinf.ms.gui.configs.Colors;
import io.sirius.ms.gui.webView.BrowserPanelProvider;
import io.sirius.ms.gui.webView.LinkInterception;
import it.unimi.dsi.fastutil.Pair;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static com.teamdev.jxbrowser.engine.RenderingMode.OFF_SCREEN;

@Slf4j
public class JxBrowserPanelProvider extends BrowserPanelProvider<JxBrowserPanel> {
    private final Engine jxBrowserEngine;

    public JxBrowserPanelProvider(@NotNull URI baseUrl, @NotNull Engine jxBrowserEngine) {
        super(baseUrl);
        this.jxBrowserEngine = jxBrowserEngine;

    }

    // Create an Engine with the dark theme enabled.
    public JxBrowserPanelProvider(@NotNull URI baseUrl) {
        this(baseUrl, setupEngine());
    }

    private static Engine setupEngine() {
        EngineOptions opts = EngineOptions
                .newBuilder(OFF_SCREEN)
                .licenseKey(new String(Base64.getDecoder().decode(System.getProperty("jxbrowser.license.key")), StandardCharsets.UTF_8))
                .disableTouchMenu()
                .enableIncognito() // no storage dir, all in memory, fresh state after every start.
                .build();

        Engine engine = Engine.newInstance(opts);
        engine.setTheme(Colors.isDarkTheme() ? Theme.DARK : Theme.LIGHT);

        engine.permissions().set(RequestPermissionCallback.class, (params, tell) -> {
            PermissionType type = params.permissionType();
            if (type == PermissionType.CLIPBOARD_READ_WRITE
                    || type == PermissionType.CLIPBOARD_SANITIZED_WRITE) {
                tell.grant();
            } else {
                tell.deny();
            }
        });
        return engine;
    }

    @SafeVarargs
    public final void addDefaultHeaders(Pair<String, String>... httpHeadersToAdd) {
        addDefaultHeaders(Arrays.asList(httpHeadersToAdd), true);
    }

    public void addDefaultHeaders(final List<Pair<String, String>> httpHeadersToAdd, boolean baseURLOnly) {
        jxBrowserEngine.network().set(BeforeStartTransactionCallback.class, (params) -> {
            String requestUrl = params.urlRequest().url();
            if (baseURLOnly && !requestUrl.startsWith(getBaseUrl().toString()))
                return BeforeStartTransactionCallback.Response.proceed();

            // Get the current list of HTTP headers for the request
            List<HttpHeader> httpHeaders = new ArrayList<>(params.httpHeaders());
            //add additional "default" headers.
            httpHeadersToAdd.stream().map(p -> HttpHeader.of(p.key(), p.value()))
                    .forEach(httpHeaders::add);
            return BeforeStartTransactionCallback.Response.override(httpHeaders);
        });
    }

    @Override
    public JxBrowserPanel newBrowserPanel(@NotNull String fullUrlWithParameters, @NotNull LinkInterception linkInterception) {
        log.info("Browser URL: {}", fullUrlWithParameters);
        return new JxBrowserPanel(fullUrlWithParameters, jxBrowserEngine.newBrowser(), linkInterception);
    }

    @Override
    public void destroy() {
        jxBrowserEngine.close();
    }
}
