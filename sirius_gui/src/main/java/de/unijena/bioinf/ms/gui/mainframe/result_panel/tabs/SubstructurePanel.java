package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
import io.sirius.ms.gui.webView.BrowserPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

public class SubstructurePanel extends JPanel implements ListSelectionListener {

    private final BrowserPanel browserPanel;

    public SubstructurePanel(SiriusGui siriusGui, @Nullable FingerprintCandidateBean structureCandidate) {
        super(new BorderLayout());
        this.browserPanel = makePanel(siriusGui, structureCandidate);
        add(browserPanel, BorderLayout.CENTER);
    }

    private static BrowserPanel makePanel(@NotNull SiriusGui gui, @Nullable FingerprintCandidateBean structureCandidate) {
        String fid = null;
        String formulaId = null;
        String inchi = null;

        if (structureCandidate != null) {
            fid = structureCandidate.getParentFeatureId();
            formulaId = structureCandidate.getCandidate().getFormulaId();
            inchi = structureCandidate.getInChiKey();
        }
        return gui.getBrowserPanelProvider().makeReactPanel("/epi", gui.getProjectManager().getProjectId(), fid, formulaId, inchi, null);
    }


    public void resultsChanged(@Nullable FingerprintCandidateBean fingerprintCandidateBean) {
        String alignedFeatureId = fingerprintCandidateBean != null ? fingerprintCandidateBean.getParentFeatureId() : null;
        String formulaId = fingerprintCandidateBean != null ? fingerprintCandidateBean.getCandidate().getFormulaId() : null;
        String inchiKey = fingerprintCandidateBean != null ? fingerprintCandidateBean.getCandidate().getInchiKey() : null;
        browserPanel.updateSelectedStructureCandidate(alignedFeatureId, formulaId, inchiKey);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        DefaultEventSelectionModel<FingerprintCandidateBean> selections = (DefaultEventSelectionModel<FingerprintCandidateBean>) e.getSource();
        FingerprintCandidateBean sre = selections.getSelected().stream().findFirst().orElse(null);
        resultsChanged(sre);
    }
}
