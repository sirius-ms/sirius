package de.unijena.bioinf.ms.gui.webView.jxbrowser;

import com.teamdev.jxbrowser.engine.Engine;
import com.teamdev.jxbrowser.engine.EngineOptions;
import com.teamdev.jxbrowser.engine.Theme;
import com.teamdev.jxbrowser.permission.PermissionType;
import com.teamdev.jxbrowser.permission.callback.RequestPermissionCallback;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.webView.BrowserPanelProvider;
import de.unijena.bioinf.ms.gui.webView.LinkInterception;
import de.unijena.bioinf.ms.properties.PropertyManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

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

    private static Engine setupEngine(){
        EngineOptions opts = EngineOptions
                .newBuilder(OFF_SCREEN)
                .licenseKey(PropertyManager.getProperty("jxbrowser.license.key"))
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
