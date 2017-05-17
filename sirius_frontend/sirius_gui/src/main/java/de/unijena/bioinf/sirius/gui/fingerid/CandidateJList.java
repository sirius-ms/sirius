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

package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.sirius.gui.configs.Buttons;
import de.unijena.bioinf.sirius.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.sirius.gui.dialogs.FilePresentDialog;
import de.unijena.bioinf.sirius.gui.filefilter.SupportedExportCSVFormatsFilter;
import de.unijena.bioinf.sirius.gui.mainframe.Workspace;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.gui.configs.Icons;
import de.unijena.bioinf.sirius.gui.utils.ToolbarToggleButton;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.*;
import java.util.List;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

public class CandidateJList extends JPanel implements MouseListener, ActionListener {

    private final CandidateList sourceList;
    //    protected FingerIdData data;
    protected JList<CompoundCandidate> candidateList;
    protected StructureSearcher structureSearcher;
    protected Thread structureSearcherThread;
//    protected ExperimentContainer correspondingExperimentContainer;


    protected JMenuItem CopyInchiKey, CopyInchi, OpenInBrowser1, OpenInBrowser2;
    protected JPopupMenu popupMenu;

    protected int highlightAgree = -1;
    protected int highlightedCandidate = -1;
    protected int selectedCompoundId;
    protected HashSet<String> logPCalculated = new HashSet<>();

    protected FilterPanel filterPanel;

    protected LogPSlider logPSlider;



    public CandidateJList(final CSIFingerIdComputation computation, CandidateList sourceList) {
        super();
        this.sourceList = sourceList;
        //        this.correspondingExperimentContainer = correspondingExperimentContainer;
//        updateTopScore();
        setLayout(new BorderLayout());
//        this.data = data;

        JPanel northPanels = new JPanel(new BorderLayout());
        add(northPanels, BorderLayout.NORTH);

        JToolBar northPanel = new JToolBar();
        northPanel.setFloatable(false);
        northPanels.add(northPanel, BorderLayout.NORTH);

        filterPanel = new FilterPanel();
        filterPanel.toggle();
        filterPanel.whenFilterChanges(new Runnable() {
            @Override
            public void run() {
                updateFilter();
            }
        });
        northPanels.add(filterPanel, BorderLayout.SOUTH);

        logPSlider = new LogPSlider();
//        northPanel.add(Box.createHorizontalGlue());
        JLabel l = new JLabel("XLogP filter: ");
        l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
        northPanel.add(l);
        northPanel.add(logPSlider);
        logPSlider.setCallback(new Runnable() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updateFilter();
                    }
                });
            }
        });

        northPanel.addSeparator(new Dimension(10, 10));


        final JToggleButton filter = new ToolbarToggleButton(Icons.FILTER_DOWN_24, "show filter");
        filter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (filterPanel.toggle()) {
                    filter.setIcon(Icons.FILTER_UP_24);
                    filter.setToolTipText("hide filter");
                } else {
                    filter.setIcon(Icons.FILTER_DOWN_24);
                    filter.setToolTipText("show filter");
                }
            }
        });
        northPanel.add(filter);


        final JButton exportToCSV = Buttons.getExportButton24("export candidate list");
        exportToCSV.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doExport();
            }
        });
        northPanel.add(exportToCSV);
        northPanel.add(new SearchKeyWordBox());


        candidateList = new CandidateInnerList(new CandidateListModel(sourceList));
        ToolTipManager.sharedInstance().registerComponent(candidateList);
        candidateList.setCellRenderer(new CandidateCellRenderer(computation, sourceList.scoreStats,this));
        candidateList.setFixedCellHeight(-1);
        candidateList.setPrototypeCellValue(CompoundCandidate.PROTOTYPE);
        final JScrollPane scrollPane = new JScrollPane(candidateList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        candidateList.addMouseListener(this);
        this.structureSearcher = new StructureSearcher(computation, sourceList.getElementList().size()); //todo does this work
        this.structureSearcherThread = new Thread(structureSearcher);
        structureSearcherThread.start();
        this.structureSearcher.reloadList((CandidateListModel) candidateList.getModel());

        ///// add popup menu
        popupMenu = new JPopupMenu();
        CopyInchiKey = new JMenuItem("Copy 2D InChIKey");
        CopyInchi = new JMenuItem("Copy 2D InChI");
        OpenInBrowser1 = new JMenuItem("Open in PubChem");
        OpenInBrowser2 = new JMenuItem("Open in all databases");
        CopyInchi.addActionListener(this);
        CopyInchiKey.addActionListener(this);
        OpenInBrowser1.addActionListener(this);
        OpenInBrowser2.addActionListener(this);
        popupMenu.add(CopyInchiKey);
        popupMenu.add(CopyInchi);
        popupMenu.add(OpenInBrowser1);
        popupMenu.add(OpenInBrowser2);
        setVisible(true);

    }

    public void updateFilter() {
        final CandidateListModel model = (CandidateListModel) candidateList.getModel();
        model.change();
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (selectedCompoundId < 0) return;
        final CompoundCandidate c = candidateList.getModel().getElementAt(selectedCompoundId);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (e.getSource() == CopyInchiKey) {
            clipboard.setContents(new StringSelection(c.compound.inchi.key2D()), null);
        } else if (e.getSource() == CopyInchi) {
            clipboard.setContents(new StringSelection(c.compound.inchi.in2D), null);
        } else if (e.getSource() == OpenInBrowser1) {
            try {
                Desktop.getDesktop().browse(new URI("https://www.ncbi.nlm.nih.gov/pccompound?term=%22" + c.compound.inchi.key2D() + "%22[InChIKey]"));
            } catch (IOException | URISyntaxException e1) {
                LoggerFactory.getLogger(this.getClass()).error(e1.getMessage(), e1);
            }
        } else if (e.getSource() == OpenInBrowser2) {
            if (c.compound.databases == null) return;
            for (Map.Entry<String, String> entry : c.compound.databases.entries()) {
                final DatasourceService.Sources s = DatasourceService.getFromName(entry.getKey());
                if (entry.getValue() == null || s == null || s.URI == null) continue;
                try {
                    if (s.URI.contains("%s")) {
                        Desktop.getDesktop().browse(new URI(String.format(Locale.US, s.URI, URLEncoder.encode(entry.getValue(), "UTF-8"))));
                    } else {
                        Desktop.getDesktop().browse(new URI(String.format(Locale.US, s.URI, Integer.parseInt(entry.getValue()))));
                    }
                } catch (IOException | URISyntaxException e1) {
                    LoggerFactory.getLogger(this.getClass()).error(e1.getMessage(), e1);
                }
            }
        }
    }

    private void doExport() {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(Workspace.CONFIG_STORAGE.getDefaultTreeExportPath());
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setAcceptAllFileFilterUsed(false);
        FileFilter csvFileFilter = new SupportedExportCSVFormatsFilter();
        jfc.addChoosableFileFilter(csvFileFilter);
        File selectedFile = null;
        while (selectedFile == null) {
            int returnval = jfc.showSaveDialog(this);
            if (returnval == JFileChooser.APPROVE_OPTION) {
                File selFile = jfc.getSelectedFile();

                Workspace.CONFIG_STORAGE.setDefaultCompoundsExportPath(selFile.getParentFile());

                if (selFile.exists()) {
                    FilePresentDialog fpd = new FilePresentDialog(MF, selFile.getName());
                    ReturnValue rv = fpd.getReturnValue();
                    if (rv == ReturnValue.Success) {
                        selectedFile = selFile;
                    }
                } else {
                    selectedFile = selFile;
                    if (!selectedFile.getName().endsWith(".csv"))
                        selectedFile = new File(selectedFile.getAbsolutePath() + ".csv");
                }
            } else {
                break;
            }
        }

        if (selectedFile != null) {
            Set<FingerIdData> datas = new HashSet<>();
            for (CompoundCandidate candidate : sourceList.getElementList()) {
                datas.add(candidate.data);
            }

            try {
                new CSVExporter().exportToFile(selectedFile, new ArrayList<>(datas));
            } catch (Exception e2) {
                ErrorReportDialog fed = new ErrorReportDialog(MF, e2.getMessage());
                LoggerFactory.getLogger(this.getClass()).error(e2.getMessage(), e2);
            }
        }
    }

    public void dispose() {
        structureSearcher.stop();
    }

    public void refresh(ExperimentContainer ec, FingerIdData data) {
//        this.data = data;
//        this.correspondingExperimentContainer = ec;
//        updateTopScore();
        this.filterPanel.setActiveExperiment(data);
        ((CandidateListModel) candidateList.getModel()).change();
        this.structureSearcher.reloadList((CandidateListModel) candidateList.getModel());
        this.logPSlider.refresh(data);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger()) return;
        highlightedCandidate = -1;
        highlightAgree = -1;
        final Point point = e.getPoint();
        final int index = candidateList.locationToIndex(point);
        selectedCompoundId = index;
        if (index < 0) return;
        final CompoundCandidate candidate = candidateList.getModel().getElementAt(index);
        highlightedCandidate = candidate.index;
        final Rectangle relativeRect = candidateList.getCellBounds(index, index);
        final boolean in;
        int rx, ry;
        {
            final Rectangle box = candidate.substructures.getBounds();
            final int absX = box.x + relativeRect.x;
            final int absY = box.y + relativeRect.y;
            final int absX2 = box.width + absX;
            final int absY2 = box.height + absY;
            in = point.x >= absX && point.y >= absY && point.x < absX2 && point.y < absY2;
            rx = point.x - absX;
            ry = point.y - absY;
        }
        if (in) {
            final int row = ry / CandidateCellRenderer.CELL_SIZE;
            final int col = rx / CandidateCellRenderer.CELL_SIZE;
            highlightAgree = candidate.substructures.indexAt(row, col);
            structureSearcher.reloadList((CandidateListModel) candidateList.getModel(), highlightAgree, highlightedCandidate);
        } else {
            if (highlightAgree >= 0) {
                highlightAgree = -1;
                structureSearcher.reloadList((CandidateListModel) candidateList.getModel(), highlightAgree, highlightedCandidate);
            }

            double rpx = point.x - relativeRect.getX(), rpy = point.y - relativeRect.getY();
            for (de.unijena.bioinf.sirius.gui.fingerid.DatabaseLabel l : candidate.labels) {
                if (l.rect.contains(rpx, rpy)) {
                    clickOnDBLabel(l);
                    break;
                }
            }
        }
    }

    private void clickOnDBLabel(de.unijena.bioinf.sirius.gui.fingerid.DatabaseLabel label) {
        final DatasourceService.Sources s = DatasourceService.getFromName(label.name);
        if (label.values == null || label.values.length == 0 || s == null || s.URI == null) return;
        try {
            for (String id : label.values) {
                if (id == null) continue;
                if (s.URI.contains("%s")) {
                    Desktop.getDesktop().browse(new URI(String.format(Locale.US, s.URI, URLEncoder.encode(id, "UTF-8"))));
                } else {
                    Desktop.getDesktop().browse(new URI(String.format(Locale.US, s.URI, Integer.parseInt(id))));
                }
            }
        } catch (IOException | URISyntaxException e1) {
            LoggerFactory.getLogger(this.getClass()).error(e1.getMessage(), e1);
        }
    }





    private void popup(MouseEvent e, CompoundCandidate candidate) {
        popupMenu.show(candidateList, e.getX(), e.getY());
    }

    @Override
    public void mousePressed(MouseEvent e) {
        highlightedCandidate = -1;
        highlightAgree = -1;
        final Point point = e.getPoint();
        final int index = candidateList.locationToIndex(point);
        selectedCompoundId = index;
        if (index < 0) return;
        final CompoundCandidate candidate = candidateList.getModel().getElementAt(index);
        highlightedCandidate = candidate.rank;
        if (e.isPopupTrigger()) {
            popup(e, candidate);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        highlightedCandidate = -1;
        highlightAgree = -1;
        final Point point = e.getPoint();
        final int index = candidateList.locationToIndex(point);
        selectedCompoundId = index;
        if (index < 0) return;
        final CompoundCandidate candidate = candidateList.getModel().getElementAt(index);
        highlightedCandidate = candidate.rank;
        if (e.isPopupTrigger()) {
            popup(e, candidate);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    public static final Color PRIMARY_HIGHLIGHTED_COLOR = new Color(0, 100, 255, 128);
    public static final Color SECONDARY_HIGHLIGHTED_COLOR = new Color(100, 100, 255, 64).brighter();



    private class SearchKeyWordBox extends JPanel implements ActionListener {

        protected JTextField textField;

        public SearchKeyWordBox() {
            super();
            System.out.println("SEARCH KEYWORD BOX INITIALIZED!");
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            this.textField = new JTextField(32);
            add(textField);
            textField.addActionListener(this);
        }


        @Override
        public void actionPerformed(ActionEvent e) {
            final String label = textField.getText();
            int I=candidateList.getSelectedIndex();
            if (I<0) I = 0;
            for (int i=I, n=candidateList.getModel().getSize(); i < n; ++i) {
                final CompoundCandidate c = candidateList.getModel().getElementAt(i);
                if (match(label, c)) {
                    candidateList.ensureIndexIsVisible(i);
                    return;
                }
            }
            for (int j = 0; j < I; ++j) {
                final CompoundCandidate c = candidateList.getModel().getElementAt(j);
                if (match(label, c)) {
                    candidateList.ensureIndexIsVisible(j);
                    return;
                }
            }
        }

        private boolean match(String label, CompoundCandidate c) {
            if (c.compound.getName()!=null && matchString(label, c.compound.getName())) return true;
            if (c.compound.getInchi().key.startsWith(label)) return true;
            if (matchString(label, c.compound.getInchi().in3D)) return true;
            if (c.compound.smiles!=null && matchString(label, c.compound.smiles.smiles)) return true;
            return false;
        }

        private boolean matchString(String label, String name) {
            return name.contains(label);
        }
    }
}
