package de.unijena.bioinf.ms.gui.fingerid;

import com.teamdev.jxbrowser.js.JsPromise;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.dialogs.LoadablePanelDialog;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs.SketcherPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

public class SketcherDialog extends LoadablePanelDialog {

    private SketcherPanel sketcherPanel;
    private final JButton addButton;
    private final JButton doneButton;

    public SketcherDialog(Window owner, SiriusGui siriusGui, @Nullable FingerprintCandidateBean structureCandidate) {
        super(owner, "Structure Sketcher");

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
        runInBackgroundAndLoad(() -> sketcherPanel.updateSelectedFeatureSketcher(c.getParentFeatureId(), c.getSmiles()));
    }

    private void addCurrentStructure(boolean closeOnSuccess) {
        addButton.setEnabled(false);
        doneButton.setEnabled(false);
        JsPromise promise = sketcherPanel.tryUploadCurrentStructure();
        promise.then(result -> {
            addButton.setEnabled(true);
            doneButton.setEnabled(true);
            if (closeOnSuccess && Boolean.TRUE.equals(result[0])) {
                dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
            }
            return promise;
        });
    }
}
