package de.unijena.bioinf.ms.gui.subtools.summaries;

import de.unijena.bioinf.ms.frontend.io.FileChooserPanel;
import de.unijena.bioinf.ms.frontend.subtools.summaries.SummaryOptions;
import de.unijena.bioinf.ms.gui.compute.SubToolConfigPanel;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;


public class SummaryConfigPanel extends SubToolConfigPanel<SummaryOptions> {

    @AllArgsConstructor
    public enum Hits {
        TOP("Top Hits (recommended)", "no-top-hit-summary"),
        ADDUCT("Top Hits with Adducts", "top-hit-adduct-summary"),
        K("Top k Hits", "top-k-summary"),
        ALL("All Hits", "full-summary");

        private final String displayName;

        @Getter
        private final String optionName;

        @Override
        public String toString() {
            return displayName;
        }
    }

    public SummaryConfigPanel(@NotNull String outputLocation) {
        super(SummaryOptions.class);

        final TwoColumnPanel paras = new TwoColumnPanel();

        JComboBox<Hits> hitsComboBox = GuiUtils.makeComboBoxWithTooltips(Hits.values(), null, option -> getOptionTooltip(option.getOptionName()).orElseThrow());
        paras.addNamed("Hits", hitsComboBox);

        paras.addNamed("", makeGenericOptionCheckBox("Feature quality summary", "feature-quality-summary"));
        paras.addNamed("", makeGenericOptionCheckBox("ChemVista summary", "chemvista"));

        parameterBindings.put(Hits.TOP.optionName, () -> String.valueOf(Hits.TOP.equals(hitsComboBox.getSelectedItem())));
        parameterBindings.put(Hits.ADDUCT.optionName, () -> "~" + Hits.ADDUCT.equals(hitsComboBox.getSelectedItem()));
        parameterBindings.put(Hits.ALL.optionName, () -> "~" + Hits.ALL.equals(hitsComboBox.getSelectedItem()));

        JLabel kLabel = new JLabel("k");
        JSpinner kSpinner = makeGenericOptionSpinner(Hits.K.getOptionName(), 10d, 0d, 100d, 1d, (v) -> Hits.K.equals(hitsComboBox.getSelectedItem()) ? String.valueOf(v.getNumber().intValue()) : "~false");

        JPanel kPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        kPanel.add(kLabel);
        kPanel.add(kSpinner);
        paras.add(Box.createRigidArea(new Dimension(0, kPanel.getPreferredSize().height)), kPanel);

        kLabel.setVisible(false);
        kSpinner.setVisible(false);

        hitsComboBox.addItemListener(e -> {
            if (Hits.K.equals(e.getItem())) {
                boolean visible = e.getStateChange() == ItemEvent.SELECTED;
                kLabel.setVisible(visible);
                kSpinner.setVisible(visible);
            }
        });

        //todo add more summary options if ready

//        paras.add(new JXTitledSeparator("Include prediction table"));
//        paras.add(makeGenericOptionCheckBox("CANOPUS ClassyFire predictions", "classyfire"));
//        paras.add(makeGenericOptionCheckBox("CANOPUS NPC predictions", "npc"));
//        paras.add(makeGenericOptionCheckBox("CSI:FingerID PubChem Fingerprints", "pubchem"));
//        paras.add(makeGenericOptionCheckBox("CSI:FingerID MACCS Fingerprints", "maccs"));
//        JSpinner digitSpinner = makeGenericOptionSpinner("digits",
//                getOptionDefaultByName("digits").map(Integer::parseInt).orElse(-1),
//                -1, 20, 1,
//                (v) -> String.valueOf(v.getNumber().intValue()));
//        paras.addNamed("Precision", digitSpinner);


        FileChooserPanel summaryLocation = new FileChooserPanel(
                outputLocation, "",
                JFileChooser.FILES_AND_DIRECTORIES,JFileChooser.SAVE_DIALOG);
        summaryLocation.field.setPlaceholder("Leave empty or pick a directory");
        parameterBindings.put("output", summaryLocation::getFilePath);
        getOptionTooltip("output").ifPresent(summaryLocation::setToolTipText);
        summaryLocation.field.setPreferredSize(new Dimension(300, summaryLocation.field.getPreferredSize().height));
        paras.addNamed("Destination", summaryLocation);

        JComboBox<SummaryOptions.Format> formatComboBox = makeGenericOptionComboBox("format", SummaryOptions.Format.class);
        JCheckBox quoteCheckbox = makeGenericOptionCheckBox("Quote strings", "quote-strings");

        paras.addNamed("Format", formatComboBox);
        paras.addNamed("", quoteCheckbox);

        formatComboBox.addItemListener(e -> {
            if (SummaryOptions.Format.XLSX.equals(e.getItem())) {
                quoteCheckbox.setVisible(e.getStateChange() == ItemEvent.DESELECTED);
            }
        });

        add(paras);
    }
}
