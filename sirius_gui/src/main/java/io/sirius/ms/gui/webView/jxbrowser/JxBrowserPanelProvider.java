package io.sirius.ms.gui.webView.jxbrowser;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.engine.Engine;
import com.teamdev.jxbrowser.engine.EngineOptions;
import com.teamdev.jxbrowser.engine.RenderingMode;
import com.teamdev.jxbrowser.engine.Theme;
import com.teamdev.jxbrowser.net.HttpHeader;
import com.teamdev.jxbrowser.net.callback.BeforeStartTransactionCallback;
import com.teamdev.jxbrowser.permission.PermissionType;
import com.teamdev.jxbrowser.permission.callback.RequestPermissionCallback;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.properties.PropertyManager;
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

import static com.teamdev.jxbrowser.engine.RenderingMode.HARDWARE_ACCELERATED;
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

    /**
     * Whether the web views render on the GPU. Off by default, see {@link #renderingMode()}. Read once per
     * start: an engine is built with one rendering mode and keeps it, so changing this takes a restart.
     */
    public static final String HARDWARE_ACCELERATION_KEY = "io.sirius-ms.browser.rendering.hardwareAccelerated";

    /**
     * How the web views are rendered.
     * <p>
     * Hardware-accelerated rendering puts the page on a GPU surface composited over the window. It is the faster way to
     * draw a big plot, and it flashes black whenever such a view is opened: the surface is created when the
     * view is shown and keeps its clear colour - black - until the page has produced its first frame. Nothing
     * can be painted behind it, since it is composited on top; a background on the panel, a background on the
     * browser, and painting the page before it is shown were all tried, and none of them is behind that
     * surface. Being told when the first frame arrives is not possible either - jxbrowser has a {@code Painted}
     * event, but only as internal API.
     * <p>
     * Off-screen rendering draws through Java2D instead, so a view that has not painted yet is just the panel
     * behind it and there is nothing to flash. That is the default: a view that opens quietly beats a faster
     * one that blinks every time. Anyone whose plots are big enough to feel the difference can turn the GPU
     * back on in the settings.
     */
    private static RenderingMode renderingMode() {
        return PropertyManager.getBoolean(HARDWARE_ACCELERATION_KEY, false) ? HARDWARE_ACCELERATED : OFF_SCREEN;
    }

    private static Engine setupEngine() {
        EngineOptions opts = EngineOptions
                .newBuilder(renderingMode())
                .licenseKey(new String(Base64.getDecoder().decode(System.getProperty("jxbrowser.license.key")), StandardCharsets.UTF_8))
                .disableTouchMenu()
                .enableIncognito() // no storage dir, all in memory, fresh state after every start.
                .disableSandbox() // does not work on all linux systems.
//                .userDataDir(Workspace.jxBrowserDir) do not store user data without sandbox/ just Incognito
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
        final Browser browser = jxBrowserEngine.newBrowser();
        try {
            return new JxBrowserPanel(fullUrlWithParameters, browser, linkInterception);
        } catch (Throwable e) {
            //the panel that would have released this browser when it is removed from its window does
            //not exist, so its browser process would be left running until the engine is closed
            if (!browser.isClosed())
                browser.close();
            throw e;
        }
    }

    @Override
    public void destroy() {
        jxBrowserEngine.close();
    }
}
