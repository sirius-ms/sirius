package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static de.unijena.bioinf.ChemistryBase.utils.Utils.notNullOrBlank;

public abstract class OpenInDocsAction extends AbstractAction {
    protected final Frame popupOwner;
    protected final String path;

    public OpenInDocsAction(@NotNull String name, @NotNull Frame popupOwner) {
        this(name, null, popupOwner);
    }

    public OpenInDocsAction(@NotNull String name, @Nullable String path, @NotNull Frame popupOwner) {
        super(name);
        this.popupOwner = popupOwner;
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
                GuiUtils.openURL(popupOwner, uri, "Open Online Documentation", true);
            } catch (IOException er) {
                LoggerFactory.getLogger(getClass()).error("Could not open 'Online Documentation' in system browser, Try internal browser as fallback.", er);
                try {
                    GuiUtils.openURL(popupOwner, uri, "Open Online Documentation (internal)", false);
                } catch (IOException ex2) {
                    LoggerFactory.getLogger(getClass()).error("Could neither open 'Online Documentation' in system browser nor in internal Browser." +   System.lineSeparator() + "Please copy the url to your browser: " + uri, ex2);
                    new ExceptionDialog(popupOwner, "Could neither open 'Online Documentation' in system browser nor in SIRIUS' internal browser: " + ex2.getMessage() + System.lineSeparator() + "Please copy the url to your browser: " + uri);
                }
                LoggerFactory.getLogger(this.getClass()).error(er.getMessage(), er);
            }
        } catch (URISyntaxException ex) {
            new ExceptionDialog(popupOwner,"Malformed URL '" + url + "'. Cause: " + ex.getMessage());
        }
    }
}
