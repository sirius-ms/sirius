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

public class SubstructurePanel extends JCefBrowserPanel implements ListSelectionListener {
    //todo make loadable by using swing based spinner

    public SubstructurePanel(SiriusGui siriusGui, @Nullable FingerprintCandidateBean structureCandidate) {
        super(makeURL(siriusGui, structureCandidate) , siriusGui);
    }

    public SubstructurePanel(SiriusGui siriusGui) {
        this(siriusGui, null);
    }

    private static String makeURL(@NotNull SiriusGui gui, @Nullable FingerprintCandidateBean structureCandidate) {
        String fid = null;
        String formulaId = null;
        String inchi = null;

        if (structureCandidate != null) {
            fid = structureCandidate.getParentFeatureId();
            formulaId = structureCandidate.getCandidate().getFormulaId();
            inchi = structureCandidate.getInChiKey();
        }
        return URI.create(gui.getSiriusClient().getApiClient().getBasePath()).resolve("/epi")
                + makeParameters(gui.getProjectManager().getProjectId(), fid, formulaId, inchi, null,null);
    }


    public void resultsChanged(@Nullable FingerprintCandidateBean fingerprintCandidateBean) {
        String alignedFeatureId = fingerprintCandidateBean != null ? fingerprintCandidateBean.getParentFeatureId() : null;
        String formulaId = fingerprintCandidateBean != null ? fingerprintCandidateBean.getCandidate().getFormulaId() : null;
        String inchiKey = fingerprintCandidateBean != null ? fingerprintCandidateBean.getCandidate().getInchiKey() : null;
        updateSelectedStructureCandidate(alignedFeatureId, formulaId, inchiKey);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        DefaultEventSelectionModel<FingerprintCandidateBean> selections = (DefaultEventSelectionModel<FingerprintCandidateBean>) e.getSource();
        FingerprintCandidateBean sre = selections.getSelected().stream().findFirst().orElse(null);
        resultsChanged(sre);
    }
}
