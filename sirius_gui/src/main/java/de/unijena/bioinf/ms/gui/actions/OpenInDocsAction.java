package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static de.unijena.bioinf.ChemistryBase.utils.Utils.notNullOrBlank;

public abstract class OpenInDocsAction extends AbstractGuiAction {
    protected final String path;

    public OpenInDocsAction(@NotNull String name, SiriusGui gui) {
        this(name, null, gui);
    }

    public OpenInDocsAction(@NotNull String name, @Nullable String path, SiriusGui gui) {
        super(name, gui);
        this.path = path;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String url = PropertyManager.getProperty("de.unijena.bioinf.sirius.docu.url");
        try {
            URI uri = new URI(url);
            if (notNullOrBlank(path))
                uri = uri.resolve(path);
            try {
                GuiUtils.openURL(mainFrame, uri, "Open Online Documentation", gui, true);
            } catch (IOException er) {
                LoggerFactory.getLogger(getClass()).error("Could neither open 'Online Documentation' in system browser nor in internal Browser.{}Please copy the url to your browser: {}", System.lineSeparator(), uri, er);
                new ExceptionDialog(mainFrame, "Could neither open 'Online Documentation' in system browser nor in SIRIUS' internal browser: " + er.getMessage() + System.lineSeparator() + "Please copy the url to your browser: " + uri);
            }
        } catch (URISyntaxException ex) {
            new ExceptionDialog(mainFrame,"Malformed URL '" + url + "'. Cause: " + ex.getMessage());
        }
    }
}
