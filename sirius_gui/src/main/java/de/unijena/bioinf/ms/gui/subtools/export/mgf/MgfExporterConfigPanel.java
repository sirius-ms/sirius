package de.unijena.bioinf.ms.gui.subtools.export.mgf;

import de.unijena.bioinf.ms.frontend.io.FileChooserPanel;
import de.unijena.bioinf.ms.frontend.subtools.export.mgf.MgfExporterOptions;
import de.unijena.bioinf.ms.gui.compute.SubToolConfigPanel;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;


public class MgfExporterConfigPanel extends SubToolConfigPanel<MgfExporterOptions> {

    public MgfExporterConfigPanel(@Nullable String outputDir, @Nullable String outputPrefix) {
        super(MgfExporterOptions.class);

        if (outputPrefix == null)
            outputPrefix = "";

        if (outputDir == null || outputDir.isBlank())
            outputDir = System.getProperty("java.io.tmpdir");


        final TwoColumnPanel paras = new TwoColumnPanel();
        JCheckBox addMs1 = new JCheckBox("Include MS1?", false);
        parameterBindings.put("write-ms1", () -> "~" + addMs1.isSelected());
        getOptionDescriptionByName("write-ms1").ifPresent(it -> addMs1.setToolTipText(GuiUtils.formatToolTip(it)));
        paras.add(addMs1);

        JCheckBox ignoreMs1Only = new JCheckBox("Ignore MS1 only features?", true);
        parameterBindings.put("ignore-ms1-only", () -> "~" + ignoreMs1Only.isSelected());
        getOptionDescriptionByName("ignore-ms1-only").ifPresent(it -> ignoreMs1Only.setToolTipText(GuiUtils.formatToolTip(it)));
        paras.add(ignoreMs1Only);

        JCheckBox mergeMs2 = new JCheckBox("Merge MS/MS?", true);
        parameterBindings.put("merge-ms2", () -> "~" + mergeMs2.isSelected());
        getOptionDescriptionByName("merge-ms2").ifPresent(it -> mergeMs2.setToolTipText(GuiUtils.formatToolTip(it)));
        paras.add(mergeMs2);

        JCheckBox featureId = new JCheckBox("External Feature ID?", true);
        parameterBindings.put("feature-id", () -> "~" + featureId.isSelected());
        getOptionDescriptionByName("feature-id").ifPresent(it -> featureId.setToolTipText(GuiUtils.formatToolTip(it)));
        paras.add(featureId);

        //merge-ppm
        final String buf = "merge-ppm";
        JSpinner mergePpm = makeGenericOptionSpinner(buf,
                getOptionDefaultByName(buf).map(Double::parseDouble).orElse(10d),
                0.25, 20, 0.25, m -> String.valueOf(m.getNumber().doubleValue()));
        paras.addNamed("Merge ppm:", mergePpm);


        paras.add(new JXTitledSeparator("Quant Table file (csv)"));
        FileChooserPanel quantFile = new FileChooserPanel(
                Path.of(outputDir).resolve(outputPrefix.isBlank() ? "quantTable.csv" : outputPrefix + "_quantTable.csv").toAbsolutePath().toString(),
                JFileChooser.FILES_ONLY, JFileChooser.SAVE_DIALOG);
        parameterBindings.put("quant-table", quantFile::getFilePath);
        getOptionDescriptionByName("quant-table").ifPresent(it -> quantFile.setToolTipText(GuiUtils.formatToolTip(it)));
        quantFile.field.setPreferredSize(new Dimension(300, quantFile.field.getPreferredSize().height));
        paras.add(quantFile);


        paras.add(new JXTitledSeparator("MGF file"));
        FileChooserPanel mgfFile = new FileChooserPanel(
                Path.of(outputDir).resolve(outputPrefix.isBlank() ? "fbmn.mgf" : outputPrefix + ".mgf").toAbsolutePath().toString(),
                JFileChooser.FILES_ONLY, JFileChooser.SAVE_DIALOG);
        parameterBindings.put("output", mgfFile::getFilePath);
        getOptionDescriptionByName("output").ifPresent(it -> mgfFile.setToolTipText(GuiUtils.formatToolTip(it)));
        mgfFile.field.setPreferredSize(new Dimension(300, mgfFile.field.getPreferredSize().height));
        paras.add(mgfFile);

        add(paras);
    }
}
