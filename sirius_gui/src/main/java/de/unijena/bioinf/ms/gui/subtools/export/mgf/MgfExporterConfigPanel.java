package de.unijena.bioinf.ms.gui.subtools.export.mgf;

import de.unijena.bioinf.ms.frontend.io.FileChooserPanel;
import de.unijena.bioinf.ms.frontend.subtools.export.mgf.MgfExporterOptions;
import de.unijena.bioinf.ms.gui.compute.SubToolConfigPanel;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import org.jdesktop.swingx.JXTitledSeparator;

import javax.swing.*;
import java.awt.*;


public class MgfExporterConfigPanel extends SubToolConfigPanel<MgfExporterOptions> {

    public MgfExporterConfigPanel() {
        super(MgfExporterOptions.class);


        final TwoColumnPanel paras = new TwoColumnPanel();
        JCheckBox addMs1 = new JCheckBox("Include MS1?", false);
        parameterBindings.put("write-ms1", () -> "~" + addMs1.isSelected());
        getOptionDescriptionByName("write-ms1").ifPresent(it -> addMs1.setToolTipText(GuiUtils.formatToolTip(it)));
        paras.add(addMs1);

        JCheckBox mergeMs2 = new JCheckBox("Merge MS/MS?", true);
        parameterBindings.put("merge-ms2", () -> "~" + mergeMs2.isSelected());
        getOptionDescriptionByName("merge-ms2").ifPresent(it -> mergeMs2.setToolTipText(GuiUtils.formatToolTip(it)));
        paras.add(mergeMs2);

        //merge-ppm
        final String buf = "merge-ppm";
        JSpinner mergePpm = makeGenericOptionSpinner(buf,
                getOptionDefaultByName(buf).map(Double::parseDouble).orElse(10d),
                0.25, 20, 0.25, m -> String.valueOf(m.getNumber().doubleValue()));
        paras.addNamed("Merge ppm:", mergePpm);


        paras.add(new JXTitledSeparator("Quant Table file (csv)"));
        FileChooserPanel quantFile = new FileChooserPanel(JFileChooser.FILES_ONLY, JFileChooser.SAVE_DIALOG);
        parameterBindings.put("quant-table", quantFile::getFilePath);
        getOptionDescriptionByName("quant-table").ifPresent(it -> quantFile.setToolTipText(GuiUtils.formatToolTip(it)));
        quantFile.field.setPreferredSize(new Dimension(300, quantFile.field.getPreferredSize().height));
        paras.add(quantFile);


        paras.add(new JXTitledSeparator("MGF file"));
        FileChooserPanel mgfFile = new FileChooserPanel(JFileChooser.FILES_ONLY, JFileChooser.SAVE_DIALOG);
        parameterBindings.put("output", mgfFile::getFilePath);
        getOptionDescriptionByName("output").ifPresent(it -> mgfFile.setToolTipText(GuiUtils.formatToolTip(it)));
        mgfFile.field.setPreferredSize(new Dimension(300, mgfFile.field.getPreferredSize().height));
        paras.add(mgfFile);

        add(paras);
    }
}
