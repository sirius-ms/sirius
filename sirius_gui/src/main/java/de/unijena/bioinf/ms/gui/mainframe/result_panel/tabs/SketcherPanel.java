package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import com.teamdev.jxbrowser.js.JsPromise;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
import io.sirius.ms.gui.webView.BrowserPanel;
import io.sirius.ms.gui.webView.BrowserPanelProvider;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static io.sirius.ms.gui.webView.BrowserPanel.parseNullable;
import static io.sirius.ms.gui.webView.BrowserPanelProvider.makeParameters;


public class SketcherPanel extends JPanel {

    private final BrowserPanel browserPanel;

    public SketcherPanel(SiriusGui siriusGui, @Nullable FingerprintCandidateBean structureCandidate) {
        super(new BorderLayout());
        browserPanel = makeBrowserPanel(siriusGui, structureCandidate);
        add(browserPanel, BorderLayout.CENTER);
    }

    private static BrowserPanel makeBrowserPanel(SiriusGui siriusGui, @Nullable FingerprintCandidateBean structureCandidate) {
        String fid = null;
        String smiles="";

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
                + "&smiles=" + URLEncoder.encode(smiles, StandardCharsets.UTF_8);
        return provider.newBrowserPanel(url);
    }

    public void updateSelectedFeatureSketcher(@Nullable String alignedFeatureId, @Nullable String smiles){
        browserPanel.submitDataUpdate(String.format("window.urlUtils.updateSelectedEntity(alignedFeatureID=%s,undefined,undefined,undefined, smiles=%s)", parseNullable(alignedFeatureId), parseNullable(smiles)));
    }

    public JsPromise tryUploadCurrentStructure() {
        return browserPanel.executeJavaScript("new Promise((resolve) => { document.dispatchEvent(new CustomEvent('uploadCurrentMoleculeFromEditor', { detail: resolve }))})");
    }
}
