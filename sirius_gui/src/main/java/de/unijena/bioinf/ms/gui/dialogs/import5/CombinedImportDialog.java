/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.dialogs.import5;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser;
import ca.odell.glazedlists.gui.WritableTableFormat;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.DialogHeader;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinitions;
import io.sirius.ms.sdk.model.Run;
import lombok.Getter;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

import static de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinitions.SAMPLE_TYPE_SAMPLE;

/**
 * Combined import dialog for MS data that allows setting import parameters
 * and configuring sample types for LC/MS runs in a single interface.
 */
public class CombinedImportDialog extends JDialog implements ActionListener {

    public static final List<String> POSSIBLE_SAMPLE_TYPES = TagDefinitions.SAMPLE_TYPE.getValueDefinition().getPossibleValues()
            .stream().map(s -> (String) s).toList();

    private final JButton cancel, importButton;
    private JCheckBox ignoreFormulas, alignCheckBox, sensitiveMode, autoNoiseDetection;
    private JFormattedTextField noiseLevel;
    /**
     * -- GETTER --
     * Gets the sample types for each run
     */
    private final Map<String, String> sampleTypes;
//    private final LCMSConfigPanel configPanel;

    @Getter
    private final boolean hasLCMS, hasPeakLists, alignAllowed;

    private ReturnValue returnValue;
//    private JTable runsTable;

    @Getter
    private final List<Path> LCMSFiles;

    @NotNull
    public List<String> getLCMSFilesSampleTypes() {
        ArrayList<String> sampleTypes = new ArrayList<>();
        for (Path lcmsFile : LCMSFiles)
            sampleTypes.add(this.sampleTypes.get(lcmsFile.toString()));

        return sampleTypes;
    }

    /**
     * Creates a new combined import dialog.
     *
     * @param owner      The parent window
     * @param inputFiles The input files containing MS data
     */
    public CombinedImportDialog(Window owner, InputFilesOptions inputFiles) {
        super(owner, "Import MS Data", Dialog.ModalityType.APPLICATION_MODAL);
        this.sampleTypes = new HashMap<>();
        this.returnValue = ReturnValue.Cancel;
        this.LCMSFiles = inputFiles.msInput.lcmsFiles.keySet().stream().sorted().toList();

        // Config panel for import options
        hasLCMS = !LCMSFiles.isEmpty();
        hasPeakLists = !inputFiles.msInput.msParserfiles.isEmpty();
        alignAllowed = LCMSFiles.size() > 1;

        setLayout(new BorderLayout());
        // Header
        JPanel header = new DialogHeader(Icons.DOCS.derive(64, 64));
        add(header, BorderLayout.NORTH);

        Box contentPanel = Box.createVerticalBox();
        contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(GuiUtils.SMALL_GAP, GuiUtils.MEDIUM_GAP, GuiUtils.SMALL_GAP, GuiUtils.MEDIUM_GAP));
        // Main content panel

        contentPanel.add(new JXTitledSeparator("Import Options"));
        contentPanel.add(Box.createVerticalStrut(GuiUtils.SMALL_GAP));
        makeImportOptions(contentPanel, hasLCMS, alignAllowed, hasPeakLists);

        // LC/MS runs table (only shown if LC/MS files are present)
        if (hasLCMS) {
            contentPanel.add(Box.createVerticalStrut(GuiUtils.MEDIUM_GAP + GuiUtils.SMALL_GAP));
            contentPanel.add(new JXTitledSeparator("LC/MS Runs Types"));
            contentPanel.add(Box.createVerticalStrut(GuiUtils.SMALL_GAP));

            contentPanel.add(new JScrollPane(createRunsTable()));

            // Add some explanatory text
            contentPanel.add(Box.createVerticalStrut(GuiUtils.SMALL_GAP));
            JTextPane infoText = new JTextPane();
            infoText.setContentType("text/html");
            infoText.setText("<html><body style='width: 400px'>" +
                    "<p>Specify the type for each LC/MS run. Run types are used for blank/background subtraction:</p>" +
                    "<ul>" +
                    "<li><b>Sample:</b> Contains compounds of interest</li>" +
                    "<li><b>Blank:</b> Defines background for background subtraction</li>" +
                    "<li><b>Pooled QC:</b> Used for alignment but not for fold change computation</li>" +
                    "</ul>" +
                    "</body></html>");
            infoText.setEditable(false);
            infoText.setBackground(null);

            contentPanel.add(infoText);
        }

        add(contentPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cancel = new JButton("Cancel");
        cancel.addActionListener(this);
        importButton = new JButton("Import");
        importButton.addActionListener(this);

        buttonPanel.add(importButton);
        buttonPanel.add(cancel);
        add(buttonPanel, BorderLayout.SOUTH);

        // Dialog properties
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(800, hasLCMS ? 500 : 300));
        pack();
        setLocationRelativeTo(owner);
        setVisible(true);
    }

    /**
     * Creates the table for displaying and configuring LC/MS runs
     *
     * @return Runs table (JTable)
     */
    private JTable createRunsTable() {
        EventList<Run> runList = new BasicEventList<>();
        SortedList<Run> sortedRuns = new SortedList<>(runList, (o1, o2) -> {
            if (o1.getName() != null && o2.getName() != null) {
                return o1.getName().compareTo(o2.getName());
            } else {
                return 0;
            }
        });

        // Create virtual Run objects from the input files
        for (Path lcmsFile : LCMSFiles) {
            Run run = new Run();
            run.setRunId(lcmsFile.toString());
            run.setName(lcmsFile.getFileName().toString());
            run.setSource(lcmsFile.toString());
            sampleTypes.put(run.getRunId(), SAMPLE_TYPE_SAMPLE); // Default to sample
            runList.add(run);
        }

        JTable runsTable = new JTable();
        runsTable.setModel(new DefaultEventTableModel<>(sortedRuns, new WritableTableFormat<>() {
            @Override
            public boolean isEditable(Run run, int column) {
                return column == 2;
            }

            @Override
            public Run setColumnValue(Run run, Object value, int column) {
                if (column == 2 && value instanceof String str)
                    sampleTypes.put(run.getRunId(), str);

                return run;
            }

            @Override
            public int getColumnCount() {
                return 3;
            }

            @Override
            public String getColumnName(int column) {
                return switch (column) {
                    case 0 -> "File Name";
                    case 1 -> "Path";
                    case 2 -> "Sample Type";
                    default -> throw new IllegalStateException("Unexpected value: " + column);
                };
            }

            @Override
            public Object getColumnValue(Run run, int column) {
                return switch (column) {
                    case 0 -> run.getName();
                    case 1 -> run.getSource();
                    case 2 -> sampleTypes.get(run.getRunId());
                    default -> throw new IllegalStateException("Unexpected value: " + column);
                };
            }
        }));

        // Customize column widths
        runsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        runsTable.getColumnModel().getColumn(1).setPreferredWidth(300);
        runsTable.getColumnModel().getColumn(2).setPreferredWidth(100);

        // Custom renderer for the path column to show a truncated path if needed
        runsTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    setToolTipText(value.toString());
                }
                return c;
            }
        });

        // Setup sample type dropdown
        JComboBox<String> sampleBox = new JComboBox<>(POSSIBLE_SAMPLE_TYPES.toArray(String[]::new));
        runsTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(sampleBox));

        // Allow column sorting
        TableComparatorChooser.install(runsTable, sortedRuns, AbstractTableComparatorChooser.SINGLE_COLUMN);

        return runsTable;
    }

    /**
     * Returns whether the user confirmed the import
     */
    public boolean isSuccess() {
        return returnValue == ReturnValue.Success;
    }

    /**
     * Gets the binding for the import parameters
     */
    public boolean isAlign() {
        return alignCheckBox.isSelected();
    }

    public boolean isIgnoreFormula(){
        return ignoreFormulas.isSelected();
    }

    public boolean isSensitiveMode(){
        return sensitiveMode.isSelected();
    }

    public boolean isAutoNoiseDetection() {
        return autoNoiseDetection.isSelected();
    }

    public double getNoiseLevel() {
        if (autoNoiseDetection.isSelected())
            return -1d;
        String val = noiseLevel.getValue().toString();
        if (Utils.isNullOrBlank(val))
            return Double.parseDouble(SiriusProperties.getProperty("de.unijena.bioinf.sirius.ui.noiseLevel", null, "1000"));
        return Double.parseDouble(val);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == importButton) {
            if (ignoreFormulas != null) {
                SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty(
                        "de.unijena.bioinf.sirius.ui.ignoreFormulas",
                        String.valueOf(ignoreFormulas.isSelected())
                );
            }
            returnValue = ReturnValue.Success;
            dispose();
        } else if (e.getSource() == cancel) {
            returnValue = ReturnValue.Cancel;
            dispose();
        }
    }


    private void makeImportOptions(JComponent content, boolean showLCMSOptions, boolean alignAllowed, boolean showPeakListOptions) {
        JPanel paras = new JPanel(new FlowLayout(FlowLayout.LEFT, GuiUtils.MEDIUM_GAP, GuiUtils.MEDIUM_GAP));
        ignoreFormulas = new JCheckBox("Ignore formulas");
        ignoreFormulas.setSelected(Boolean.parseBoolean(
                SiriusProperties.getProperty("de.unijena.bioinf.sirius.ui.ignoreFormulas", null, "false")
        ));
        ignoreFormulas.setToolTipText(GuiUtils.formatToolTip(
                "If checked, molecular formula and structure annotations will be ignored during peaklist import when given in the input file."
        ));

        // Only show ignoreFormulas checkbox if there are peaklist files
        if (showPeakListOptions)
            paras.add(ignoreFormulas);


        // Only show align checkbox if LC/MS files are present and alignment is allowed
        alignCheckBox = new JCheckBox("Align and merge LC/MS runs");
        alignCheckBox.setToolTipText(GuiUtils.formatToolTip(
                "If checked, all LC/MS runs will be aligned and combined to one merged LC/MS run."
        ));
        sensitiveMode = new JCheckBox("Sensitive mode");
        sensitiveMode.setToolTipText(GuiUtils.formatToolTip(
                "If checked, min-snr is set to 2 instead of 3. Use this to pick very low intensity features. Features with good MS/MS are always picked, so use this option only if you are interested in low intensive MS-only features."
        ));


        JLabel label = new JLabel("Noise level: ");
        autoNoiseDetection =  new JCheckBox("Auto");
        sensitiveMode.setToolTipText(GuiUtils.formatToolTip("If checked, noise level will be autodetected."));
        autoNoiseDetection.setSelected(Boolean.parseBoolean(SiriusProperties.getProperty("de.unijena.bioinf.sirius.ui.autoNoiseDetection", null, "true")));
        noiseLevel = getNoiseLevelInput();
        noiseLevel.setValue(Double.parseDouble(SiriusProperties.getProperty("de.unijena.bioinf.sirius.ui.noiseLevel", null, "1000")));
        noiseLevel.setEnabled(!autoNoiseDetection.isSelected());
        autoNoiseDetection.addActionListener((a)->noiseLevel.setEnabled(!autoNoiseDetection.isSelected()));

        if (showLCMSOptions && alignAllowed) {
            alignCheckBox.setSelected(true);
            sensitiveMode.setSelected(false);
            paras.add(alignCheckBox);
            paras.add(sensitiveMode);

            Box box = Box.createHorizontalBox();
            box.add(label);
            box.add(autoNoiseDetection);
            box.add(noiseLevel);
            paras.add(box);

            paras.add(Box.createVerticalGlue());
        }
        content.add(paras);
    }

    private static JFormattedTextField getNoiseLevelInput() {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        numberFormat.setGroupingUsed(false);
        NumberFormatter numberFormatter = new NumberFormatter(numberFormat);
        numberFormatter.setValueClass(Double.class);
        numberFormatter.setAllowsInvalid(true);
        numberFormatter.setCommitsOnValidEdit(true);
        numberFormatter.setMinimum(0.0);
        JFormattedTextField textField = new JFormattedTextField(numberFormatter);
        textField.setColumns(10);
        return textField;
    }
}