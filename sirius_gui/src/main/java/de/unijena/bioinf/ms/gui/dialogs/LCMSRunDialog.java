

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
import de.unijena.bioinf.ms.gui.blank_subtraction.BlankSubtraction;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import io.sirius.ms.sdk.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;


public class LCMSRunDialog extends JDialog implements ActionListener {

    private final JButton discard, save;

    private final SiriusGui gui;

    private final Map<String, String> sampleTypes;

    public LCMSRunDialog(Frame owner, SiriusGui gui) {
        super(owner, true);
        this.gui = gui;

        setTitle("Samples");
        setLayout(new BorderLayout());

        //region north

        JPanel header = new DialogHeader(Icons.SAMPLE.derive(64,64));
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
            public Run setColumnValue(Run run, Object o, int i) {
                if (o instanceof String str) {
                    if (str.equalsIgnoreCase(BlankSubtraction.SAMPLE))
                        sampleTypes.put(run.getRunId(), BlankSubtraction.SAMPLE);
                    else if (str.equalsIgnoreCase(BlankSubtraction.BLANK))
                        sampleTypes.put(run.getRunId(), BlankSubtraction.BLANK);
                    else if (str.equalsIgnoreCase(BlankSubtraction.CTRL))
                        sampleTypes.put(run.getRunId(), BlankSubtraction.CTRL);
                }
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

        JComboBox<String> sampleBox = new JComboBox<>(BlankSubtraction.CATEGORY_VALUES.toArray(String[]::new));
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(sampleBox));
        TableComparatorChooser.install(table, sortedRuns, AbstractTableComparatorChooser.SINGLE_COLUMN);
        add(new JScrollPane(table), BorderLayout.CENTER);

        Jobs.runEDTLater(() -> gui.acceptSiriusClient((client, pid) -> {
            List<Run> runs = client.runs().getRunsPaged(pid, 0, Integer.MAX_VALUE, null, List.of(RunOptField.TAGS)).getContent();
            if (runs != null) {
                for (Run run : runs) {
                    if (run.getTags() != null && run.getTags().containsKey(BlankSubtraction.CATEGORY_NAME)) {
                        sampleTypes.put(run.getRunId(), run.getTags().get(BlankSubtraction.CATEGORY_NAME).getText());
                    } else {
                        sampleTypes.put(run.getRunId(), BlankSubtraction.SAMPLE);
                    }
                }
                runList.addAll(runs);
            }
        }));

        //endregion

        //region SOUTH

        discard = new JButton("Discard");
        discard.addActionListener(this);
        save = new JButton("Save");
        save.addActionListener(this);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(save);
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
        this.dispose();
        if (e.getSource() == save) {
            Set<String> samples = new HashSet<>();
            Set<String> blanks = new HashSet<>();
            Set<String> controls = new HashSet<>();

            for (Map.Entry<String, String> entry : sampleTypes.entrySet()) {
                switch (entry.getValue()) {
                    case BlankSubtraction.SAMPLE: samples.add(entry.getKey()); break;
                    case BlankSubtraction.BLANK: blanks.add(entry.getKey()); break;
                    case BlankSubtraction.CTRL: controls.add(entry.getKey()); break;
                    default: break;
                }
            }
            gui.acceptSiriusClient((client, pid) -> {
                SampleTypeFoldChangeRequest request = new SampleTypeFoldChangeRequest();
                request.setSampleRunIds(new ArrayList<>(samples));
                request.setBlankRunIds(new ArrayList<>(blanks));
                request.setControlRunIds(new ArrayList<>(controls));

                Job job = client.runs().computeFoldChangeForBlankSubtraction(pid, request, List.of(JobOptField.PROGRESS));
            });
            // TODO show progress for workflow
        }
    }

}
