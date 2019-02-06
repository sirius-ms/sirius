/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ms.io.filefilter.SupportedExportCSVFormatsFilter;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ExporterAccessory extends JPanel implements ActionListener {

    // export CSI:FingerID Instances

    private ButtonGroup exportAs;
    private JFileChooser fileChooser;

    private boolean fingeridWasSelected;
    private boolean siriusWasSelected;

    private AbstractButton singleFile, exportingSirius, exportingFingerId, multiFile;

    public ExporterAccessory(final JFileChooser fileChooser) {
        this.fileChooser= fileChooser;
        setLayout(new BorderLayout());

        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.setAlignmentX(0);
        add(panel, BorderLayout.CENTER);

        // export options
        final JPanel generalPanel = new JPanel();
        generalPanel.setAlignmentX(0);
        generalPanel.setLayout(new BoxLayout(generalPanel, BoxLayout.PAGE_AXIS));
        generalPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "SIRIUS", TitledBorder.LEFT, TitledBorder.TOP ));
        panel.add(generalPanel);

        {
            final JPanel flow = new JPanel();
            flow.setLayout(new FlowLayout());
            generalPanel.add(flow);
            flow.add(new JLabel("Export as"));
            exportAs = new ButtonGroup();
            singleFile= new JRadioButton("single file");
            multiFile=new JRadioButton("multiple files");
            flow.add(singleFile);
            multiFile.addActionListener(this);
            singleFile.addActionListener(this);
            flow.add(multiFile);
            exportAs.add(singleFile);
            exportAs.add(multiFile);
        }

        // sirius
        final JPanel siriusPanel = new JPanel();
        siriusPanel.setLayout(new BoxLayout(siriusPanel, BoxLayout.PAGE_AXIS));
        siriusPanel.setAlignmentX(0);
        siriusPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "SIRIUS", TitledBorder.LEFT, TitledBorder.TOP ));
        panel.add(siriusPanel);
        {
            exportingSirius = new JCheckBox("export tree results");
            exportingSirius.setSelected(true);
            siriusPanel.add(exportingSirius);
        }

        // csi_fingerid
        final JPanel fingeridPanel = new JPanel();
        fingeridPanel.setLayout(new BoxLayout(fingeridPanel, BoxLayout.PAGE_AXIS));
        fingeridPanel.setAlignmentX(0);
        fingeridPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "CSI:FingerID", TitledBorder.LEFT, TitledBorder.TOP ));
        {
            exportingFingerId = new JCheckBox("export CSI:FingerID results");
            exportingFingerId.doClick();
            fingeridPanel.add(exportingFingerId);
        }



        panel.add(fingeridPanel);

        multiFile.doClick();

    }

    public boolean isSingleFile() {
        return singleFile.isSelected();
    }

    public boolean isExportingSirius() {
        return exportingSirius.isSelected();
    }

    public boolean isExportingFingerId() {
        return exportingFingerId.isSelected();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isSingleFile()) {
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new SupportedExportCSVFormatsFilter());
            fingeridWasSelected = isExportingFingerId();
            siriusWasSelected = isExportingSirius();
            exportingFingerId.setEnabled(false);
            exportingFingerId.setSelected(false);
            exportingSirius.setEnabled(false);
            exportingSirius.setSelected(true);
        } else {
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setFileFilter(null);
            exportingFingerId.setEnabled(true);
            exportingFingerId.setSelected(fingeridWasSelected);
            exportingSirius.setEnabled(true);
            exportingSirius.setSelected(siriusWasSelected);
        }
    }
}
