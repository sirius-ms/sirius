package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
import de.unijena.bioinf.ms.gui.webView.JCefBrowserPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.net.URI;

public class SubstructurePanel extends JCefBrowserPanel implements ListSelectionListener {
    //todo make loadable by using swing based spinner
    public SubstructurePanel(SiriusGui siriusGui) {
        super(URI.create(siriusGui.getSiriusClient().getApiClient().getBasePath()).resolve("/epi")
                + THEME_REST_PARA + "&pid=" + siriusGui.getProjectManager().getProjectId(), siriusGui);
    }


    public void resultsChanged(@Nullable FingerprintCandidateBean fingerprintCandidateBean) {
        String alignedFeatureId = fingerprintCandidateBean != null ? fingerprintCandidateBean.getParentFeatureId() : null;
        String formulaId = fingerprintCandidateBean != null ? fingerprintCandidateBean.getCandidate().getFormulaId() : null;
        String inchiKey = fingerprintCandidateBean != null ? fingerprintCandidateBean.getCandidate().getInchiKey() : null;
        System.out.println("Loading afid: " + alignedFeatureId + ", fid: " + formulaId + ", inchikey: " + inchiKey);
        updateSelectedStructureCandidate(alignedFeatureId, formulaId, inchiKey);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        DefaultEventSelectionModel<FingerprintCandidateBean> selections = (DefaultEventSelectionModel<FingerprintCandidateBean>) e.getSource();
        FingerprintCandidateBean sre = selections.getSelected().stream().findFirst().orElse(null);
        resultsChanged(sre);
    }
}
