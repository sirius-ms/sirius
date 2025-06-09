package de.unijena.bioinf.ms.gui.webView.jcefbrowser;

import com.jetbrains.cef.JCefAppConfig;
import de.unijena.bioinf.ms.gui.webView.BrowserPanelProvider;
import de.unijena.bioinf.ms.gui.webView.JCEFLinuxFixer;
import de.unijena.bioinf.ms.gui.webView.LinkInterception;
import lombok.SneakyThrows;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

public class JCefBrowserPanelProvider extends BrowserPanelProvider<JCefBrowserPanel> {
    @NotNull
    private final CefApp cefApp;

    public JCefBrowserPanelProvider(@NotNull CefApp cefApp, @NotNull URI baseUrl) {
        super(baseUrl);
        this.cefApp = cefApp;
    }

    public JCefBrowserPanelProvider(@NotNull URI baseUrl) {
        this(makeCefApp(), baseUrl);
    }

    @Override
    public JCefBrowserPanel newBrowserPanel(@NotNull String fullUrlWithParameters, @NotNull LinkInterception linkInterception) {
        return new JCefBrowserPanel(fullUrlWithParameters, cefApp.createClient(), linkInterception);
    }

    public CefBrowser newBrowser(String url) {
        return newBrowser(url, false);
    }

    public CefBrowser newBrowser(String url, boolean transparent) {
        return cefApp.createClient().createBrowser(url, false, transparent);
    }


    @SneakyThrows
    @NotNull
    private static CefApp makeCefApp() {
        final JCefAppConfig jCefAppConfig = JCefAppConfig.getInstance();
        final CefSettings cefSettings = jCefAppConfig.getCefSettings();

        // For remote devtools, open localhost:port in chrome
//        cefSettings.remote_debugging_port = 9222;
//        jCefAppConfig.getAppArgsAsList().add("--remote-allow-origins=*");
//        cefSettings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_VERBOSE;

        CefApp.startup(jCefAppConfig.getAppArgs());
        CefApp instance = CefApp.getInstance(jCefAppConfig.getAppArgs(), cefSettings);

        if (System.getProperty("os.name").toLowerCase().contains("linux"))
            JCEFLinuxFixer.preloadJCef(instance);

        return instance;
    }

    @Override
    public void destroy() {
        cefApp.dispose();
    }
}
