package de.unijena.bioinf.ms.gui.webView.jxbrowser;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.browser.event.ConsoleMessageReceived;
import com.teamdev.jxbrowser.engine.Engine;
import com.teamdev.jxbrowser.engine.EngineOptions;
import com.teamdev.jxbrowser.engine.Theme;
import com.teamdev.jxbrowser.js.ConsoleMessage;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.webView.BrowserPanelProvider;
import de.unijena.bioinf.ms.gui.webView.LinkInterception;
import de.unijena.bioinf.ms.properties.PropertyManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

import static com.teamdev.jxbrowser.engine.RenderingMode.HARDWARE_ACCELERATED;

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

    private static Engine setupEngine(){
        EngineOptions opts = EngineOptions
                .newBuilder(HARDWARE_ACCELERATED)
                .licenseKey(PropertyManager.getProperty("jxbrowser.license.key"))
                .disableTouchMenu()
                .enableIncognito() // no storage dir, all in memory, fresh state after every start.
                .build();

        Engine engine = Engine.newInstance(opts);
        engine.setTheme(Colors.isDarkTheme() ? Theme.DARK : Theme.LIGHT);
        return engine;
    }

    @Override
    public JxBrowserPanel newBrowserPanel(@NotNull String fullUrlWithParameters, @NotNull LinkInterception linkInterception) {
        Browser browser = jxBrowserEngine.newBrowser();
        browser.on(ConsoleMessageReceived.class, event -> {
            ConsoleMessage consoleMessage = event.consoleMessage();
            log.debug("JS Console [{}]: {}", consoleMessage.level(), consoleMessage.message());
        });

        return new JxBrowserPanel(fullUrlWithParameters, browser, linkInterception);
    }

    @Override
    public void destroy() {
        jxBrowserEngine.close();
    }
}
