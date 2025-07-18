package de.unijena.bioinf.ms.gui.fingerid;

import ca.odell.glazedlists.EventList;
import com.teamdev.jxbrowser.js.JsPromise;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.dialogs.LoadablePanelDialog;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.ResultPanel;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs.DeNovoStructureListDetailViewPanel;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs.SketcherPanel;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.List;

public class SketcherDialog extends LoadablePanelDialog {

    private SketcherPanel sketcherPanel;
    private final JButton addButton;
    private final JButton doneButton;
    private final SiriusGui siriusGui;
    private FingerprintCandidateBean originalStructure;

    public SketcherDialog(Window owner, SiriusGui siriusGui, @Nullable FingerprintCandidateBean structureCandidate) {
        super(owner, "Structure Sketcher");
        setModalityType(ModalityType.MODELESS);
        this.siriusGui = siriusGui;
        originalStructure = structureCandidate;

        JPanel southPanel = new JPanel();
        add(southPanel, BorderLayout.SOUTH);

        addButton = new JButton("Add");
        addButton.addActionListener(e -> addCurrentStructure(false));

        doneButton = new JButton("Add and close");
        doneButton.addActionListener(e -> addCurrentStructure(true));

        loadPanel(() -> {
            sketcherPanel = new SketcherPanel(siriusGui, structureCandidate);
            southPanel.add(addButton);
            southPanel.add(doneButton);
            return sketcherPanel;
        });
    }

    public void updateMolecule(FingerprintCandidateBean c) {
        originalStructure = c;
        runInBackgroundAndLoad(() -> sketcherPanel.updateSelectedFeatureSketcher(c.getParentFeatureId(), c.getSmiles()));
    }

    private void addCurrentStructure(boolean closeOnSuccess) {
        addButton.setEnabled(false);
        doneButton.setEnabled(false);
        JsPromise promise = sketcherPanel.tryUploadCurrentStructure();
        promise.then(result -> {
            addButton.setEnabled(true);
            doneButton.setEnabled(true);
            String inchiKey = result[0].toString();
            if (!inchiKey.isEmpty()) {
                inchiKey = inchiKey.substring(0, 14);
                scrollToStructure(inchiKey);
            }
            if (closeOnSuccess && !inchiKey.isEmpty()) {
                dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
            }
            return promise;
        });
    }

    private void scrollToStructure(String inchiKey) {
        ResultPanel resultsPanel = siriusGui.getMainFrame().getResultsPanel();
        EventList<InstanceBean> selection = resultsPanel.getCompoundList().getCompoundListSelectionModel().getSelected();
        if (!selection.isEmpty() && originalStructure.getParentFeatureId().equals(selection.getFirst().getFeatureId())) {
            resultsPanel.selectDeNovoTab();
            DeNovoStructureListDetailViewPanel deNovoTab = resultsPanel.getDeNovoStructuresTab();
            CandidateListDetailView listView = deNovoTab.getList();
            StructureList structureList = listView.getSource();

            ActiveElementChangedListener<FingerprintCandidateBean, InstanceBean> listener = new ActiveElementChangedListener<>() {
                @Override
                public void resultsChanged(InstanceBean elementsParent, FingerprintCandidateBean selectedElement, List<FingerprintCandidateBean> resultElements, ListSelectionModel selections) {
                    if (resultElements.stream().anyMatch(c -> inchiKey.equals(c.getInChiKey()))) {
                        listView.scrollToStructure(c -> inchiKey.equals(c.getInChiKey()));
                        structureList.removeActiveResultChangedListener(this);
                    }
                }
            };

            structureList.addActiveResultChangedListener(listener);
        }
    }
}
