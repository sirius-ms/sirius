package de.unijena.bioinf.ms.gui.subtools.summaries;

import de.unijena.bioinf.ms.frontend.io.FileChooserPanel;
import de.unijena.bioinf.ms.frontend.subtools.summaries.SummaryOptions;
import de.unijena.bioinf.ms.gui.compute.SubToolConfigPanel;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import org.jdesktop.swingx.JXTitledSeparator;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Optional;


public class SummaryConfigPanel extends SubToolConfigPanel<SummaryOptions> {

    public SummaryConfigPanel() {
        super(SummaryOptions.class);

        final TwoColumnPanel paras = new TwoColumnPanel();
        paras.add(new JXTitledSeparator("Include prediction table"));
        paras.add(makeGenericOptionCheckBox("CANOPUS ClassyFire predictions", "classyfire"));
        paras.add(makeGenericOptionCheckBox("CANOPUS NPC predictions", "npc"));
        paras.add(makeGenericOptionCheckBox("CSI:FingerID PubChem Fingerprints", "pubchem"));
        paras.add(makeGenericOptionCheckBox("CSI:FingerID MACCS Fingerprints", "maccs"));
        JSpinner digitSpinner = makeGenericOptionSpinner("digits",
                getOptionDefaultByName("digits").map(Integer::parseInt).orElse(-1),
                -1, 20, 1,
                (v) -> String.valueOf(v.getNumber().intValue()));
        paras.addNamed("Precision", digitSpinner);


        paras.add(new JXTitledSeparator("Summary Output Location"));
        paras.add(makeGenericOptionCheckBox("Zip Compression", "compress"));

        paras.add(new JXTitledSeparator("Summary Output Location"));
        FileChooserPanel summaryLocation = new FileChooserPanel(
                Optional.ofNullable(MainFrame.MF.ps().projectSpace().getLocation().getParent()).map(Path::toString).orElse(""), "",
                JFileChooser.FILES_AND_DIRECTORIES,JFileChooser.SAVE_DIALOG);
        summaryLocation.field.setPlaceholder("Writes to project-space if empty");
        parameterBindings.put("output", summaryLocation::getFilePath);
        getOptionDescriptionByName("output").ifPresent(it -> summaryLocation.setToolTipText(GuiUtils.formatToolTip(it)));
        summaryLocation.field.setPreferredSize(new Dimension(300, summaryLocation.field.getPreferredSize().height));
        paras.add(summaryLocation);

        add(paras);
    }
}
