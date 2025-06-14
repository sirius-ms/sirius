package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
import de.unijena.bioinf.ms.gui.webView.BrowserPanel;
import de.unijena.bioinf.ms.gui.webView.BrowserPanelProvider;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

import static de.unijena.bioinf.ms.gui.webView.BrowserPanelProvider.makeParameters;


public class SketcherPanel extends JPanel {

    public SketcherPanel(SiriusGui siriusGui, @Nullable FingerprintCandidateBean structureCandidate) {
        super(new BorderLayout());
        BrowserPanel browserPanel = makeBrowserPanel(siriusGui, structureCandidate);
        add(browserPanel, BorderLayout.CENTER);
    }

    private static BrowserPanel makeBrowserPanel(SiriusGui siriusGui, @Nullable FingerprintCandidateBean structureCandidate){
        String fid = null;
        String smiles=null;

        if (structureCandidate != null) {
            fid = structureCandidate.getParentFeatureId();
            smiles =structureCandidate.getSmiles();
        }

        //this smiles parameter should not be part of the main browser panel since it's not a project space id.
        //We could use the inchikey of the candidate instead and the sketcher loads the candidate from the api.
        //this would fit the general principle of just providing ids to the js views. However, it is likely slower.
        BrowserPanelProvider<?> provider = siriusGui.getBrowserPanelProvider();
        String url = provider.resolveBaseUrl("/structEdit")
                + makeParameters(siriusGui.getProjectManager().getProjectId(), fid, null, null, null)
                + "&smiles=" + smiles;
        return provider.newBrowserPanel(url);
    }
}
