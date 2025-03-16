

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

package de.unijena.bioinf.ms.gui.dialogs;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser;
import ca.odell.glazedlists.gui.WritableTableFormat;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.compute.jjobs.LoadingBackroundTask;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.persistence.model.core.tags.Groups;
import io.sirius.ms.sdk.model.*;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

import static de.unijena.bioinf.ms.gui.dialogs.import5.CombinedImportDialog.POSSIBLE_SAMPLE_TYPES;
import static de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinitions.*;


public class LCMSRunDialog extends JDialog implements ActionListener {

    private final JButton discard, save;

    private final SiriusGui gui;

    private final Map<String, String> sampleTypes;

    public LCMSRunDialog(Frame owner, SiriusGui gui, @Nullable List<Run> runs, boolean discardable) {
        super(owner, true);
        this.gui = gui;

        setTitle("Samples");
        setLayout(new BorderLayout());

        //region north

        JPanel header = new DialogHeader(Icons.SAMPLE.derive(64, 64));
        add(header, BorderLayout.NORTH);

        //endregion

        //region center

        sampleTypes = new HashMap<>();
        EventList<Run> runList = new BasicEventList<>();
        SortedList<Run> sortedRuns = new SortedList<>(runList, (o1, o2) -> {
            if (o1.getName() != null && o2.getName() != null) {
                return o1.getName().compareTo(o2.getName());
            } else {
                return 0;
            }
        });

        JTable table = new JTable();
        table.setModel(new DefaultEventTableModel<>(sortedRuns, new WritableTableFormat<>() {
            @Override
            public boolean isEditable(Run run, int i) {
                return i == 2;
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
            public String getColumnName(int i) {
                return switch (i) {
                    case 0 -> "Run";
                    case 1 -> "Location";
                    case 2 -> "Sample type";
                    default -> throw new IllegalStateException("Unexpected value: " + i);
                };
            }

            @Override
            public Object getColumnValue(Run run, int i) {
                return switch (i) {
                    case 0 -> run.getName();
                    case 1 -> run.getSource();
                    case 2 -> sampleTypes.get(run.getRunId());
                    default -> throw new IllegalStateException("Unexpected value: " + i);
                };
            }
        }));

        JComboBox<String> sampleBox = new JComboBox<>(POSSIBLE_SAMPLE_TYPES.toArray(String[]::new));
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(sampleBox));
        TableComparatorChooser.install(table, sortedRuns, AbstractTableComparatorChooser.SINGLE_COLUMN);
        add(new JScrollPane(table), BorderLayout.CENTER);

        Jobs.runEDTLater(() -> {
            List<Run> runs1;
            if (runs == null) {
                runs1 = gui.applySiriusClient((c, pid) -> c.runs().getRunsPageExperimental(pid, null, 0, Integer.MAX_VALUE, null, List.of(RunOptField.TAGS)).getContent());
            } else {
                runs1 = runs;
            }
            if (runs1 != null) {
                for (Run run : runs1) {
                    if (run.getTags() != null && run.getTags().containsKey(SAMPLE_TYPE.getTagName())) {
                        sampleTypes.put(run.getRunId(), (String) run.getTags().get(SAMPLE_TYPE.getTagName()).getValue());
                    } else {
                        sampleTypes.put(run.getRunId(), SAMPLE_TYPE_SAMPLE);
                    }
                }
                runList.addAll(runs1);
            }
        });
        //endregion

        //region SOUTH

        discard = new JButton("Discard");
        discard.addActionListener(this);
        save = new JButton("Save");
        save.addActionListener(this);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(save);
        if (discardable)
            buttons.add(discard);

        add(buttons, BorderLayout.SOUTH);

        //endregion

        setMinimumSize(new Dimension(640, 480));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // compute fold-changes and add tags to runs
        this.dispose();
        try {
            if (e.getSource() == save) {
                List<TagSubmission> tags = new ArrayList<>(sampleTypes.size());

                sampleTypes.forEach((objectId, sampleType) ->
                        tags.add(new TagSubmission()
                                .taggedObjectId(objectId)
                                .value(sampleType)
                                .tagName(SAMPLE_TYPE.getTagName())));

                LoadingBackroundTask<Job> task = gui.applySiriusClient((client, pid) -> {
                    client.runs().addTagsToRunsExperimental(pid, tags);

                    FoldChangeJobSubmission request = new FoldChangeJobSubmission()
                            .aggregationTypes(List.of(/*AggregationType.MIN,*/ AggregationType.AVG, AggregationType.MAX))
                            .quantificationMeasures(List.of(QuantMeasure.APEX_INTENSITY, QuantMeasure.AREA_UNDER_CURVE))
                            .rightRunGroup(Groups.SAMPLE_RUNS.getGroupName())
                            .leftRunGroup(Groups.BLANK_RUNS.getGroupName());

                    Job job = client.featureStatistics().computeAlignedFeatureFoldChangesExperimental(pid, request, List.of(JobOptField.PROGRESS));
                    return Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Computing blank removal fold changes...", new io.sirius.ms.sdk.jjobs.SseProgressJJob(gui.getSiriusClient(), pid, job));
                });
                task.awaitResult();
            }
        } catch (Exception exc) {
            Jobs.runEDTLater(() -> new StacktraceDialog(gui.getMainFrame(), exc.getMessage(), exc.getCause() != null ? exc.getCause(): exc));
        }
    }

}
