package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.frontend.subtools.canopus.CanopusOptions;
import de.unijena.bioinf.ms.frontend.subtools.fingerprint.FingerprintOptions;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import picocli.CommandLine;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FingerprintAndCanopusConfigPanel extends ConfigPanel{
    private final CommandLine.Command fingerprintCommand = FingerprintOptions.class.getAnnotation(CommandLine.Command.class);
    private final CommandLine.Command canopusCommand = CanopusOptions.class.getAnnotation(CommandLine.Command.class);
    public final static String description = "Predict molecular fingerprints using CSI:FingerID and compound categories using CANOPUS for each compound individually. " +
            "Fingerprints are predicted from MS/MS and fragmentation trees using CSI:FingerID fingerprint prediction. " +
            "Compound categories are predicted based on each predicted molecular fingerprint (CSI:FingerID) individually using CANOPUS.";

    private final JCheckBox threshold;

    public FingerprintAndCanopusConfigPanel() {
        final TwoColumnPanel additionalOptions = new TwoColumnPanel();
        threshold = makeParameterCheckBox("FormulaResultThreshold");
        additionalOptions.addNamed("Score threshold", threshold);
        add(new TextHeaderBoxPanel("General", additionalOptions));
    }

    public List<String> toolCommands() {
        return Arrays.asList(fingerprintCommand.name(), canopusCommand.name());
    }

    @Override
    public void applyValuesFromPreset(Map<String, String> preset) {
        threshold.setSelected(Boolean.parseBoolean(preset.get("FormulaResultThreshold")));
    }
}
