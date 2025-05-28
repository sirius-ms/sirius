package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
import de.unijena.bioinf.ms.gui.webView.JCefBrowserPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.net.URI;

public class SketcherPanel extends JCefBrowserPanel implements ListSelectionListener {

    public SketcherPanel(SiriusGui siriusGui, @Nullable FingerprintCandidateBean structureCandidate) {
        super(makeURL(siriusGui, structureCandidate) , siriusGui);

    }

    public SketcherPanel(SiriusGui siriusGui) {
        this(siriusGui, null);

    }


    public void resultsChanged(@Nullable FingerprintCandidateBean fingerprintCandidateBean) {
        System.out.println("results changed");
        String alignedFeatureId = fingerprintCandidateBean != null ? fingerprintCandidateBean.getParentFeatureId() : null;
        String smiles = fingerprintCandidateBean != null ? fingerprintCandidateBean.getCandidate().getSmiles() : null;
        updateSelectedFeatureSketcher(alignedFeatureId,smiles);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        System.out.println("value changed");
        DefaultEventSelectionModel<FingerprintCandidateBean> selections = (DefaultEventSelectionModel<FingerprintCandidateBean>) e.getSource();
        FingerprintCandidateBean sre = selections.getSelected().stream().findFirst().orElse(null);
        resultsChanged(sre);
    }

    private static String makeURL(@NotNull SiriusGui gui, @Nullable FingerprintCandidateBean structureCandidate) {
        String fid = null;
        String smiles=null;

        if (structureCandidate != null) {
            fid = structureCandidate.getParentFeatureId();
            smiles =structureCandidate.getSmiles();
        }

        System.out.println(URI.create(gui.getSiriusClient().getApiClient().getBasePath()).resolve("/structEdit")
                + makeParameters(gui.getProjectManager().getProjectId(), fid, null, null, null,smiles));
        return URI.create(gui.getSiriusClient().getApiClient().getBasePath()).resolve("/structEdit")
                + makeParameters(gui.getProjectManager().getProjectId(), fid, null, null, null,smiles);
    }
}
