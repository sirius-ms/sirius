

/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.fingerid;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.DefaultEventListModel;
import de.unijena.bioinf.chemdb.DataSources;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.fingerid.candidate_filters.MolecularPropertyMatcherEditor;
import de.unijena.bioinf.ms.gui.fingerid.candidate_filters.SmartFilterMatcherEditor;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CandidateListDetailView extends CandidateListView implements ActiveElementChangedListener<FingerprintCandidateBean, InstanceBean>, MouseListener, ActionListener {
    public static final Color INVERT_HIGHLIGHTED_COLOR = new Color(255, 30, 0, 192);
    public static final Color INVERT_HIGHLIGHTED_COLOR2 = new Color(255, 197, 0, 192);
    public static final Color PRIMARY_HIGHLIGHTED_COLOR = new Color(0, 100, 255, 128);
    public static final Color SECONDARY_HIGHLIGHTED_COLOR = new Color(100, 100, 255, 64).brighter();
    protected JList<FingerprintCandidateBean> candidateList;
    protected StructureSearcher structureSearcher;
    protected Thread structureSearcherThread;


    protected JMenuItem CopyInchiKey, CopyInchi, OpenInBrowser1, OpenInBrowser2, highlight;
    protected JPopupMenu popupMenu;

    protected int highlightAgree = -1;
    protected int highlightedCandidate = -1;
    protected int selectedCompoundId;

    private ToolbarToggleButton filterByMolecularPropertyButton;
    private JTextField smartFilterTextField;
    private MolecularPropertyMatcherEditor molecularPropertyMatcherEditor;

    public CandidateListDetailView(StructureList sourceList) {
        super(sourceList);
        candidateList = new CandidateInnerList(new DefaultEventListModel<>(filteredSource));

        ToolTipManager.sharedInstance().registerComponent(candidateList);
        candidateList.setCellRenderer(new CandidateCellRenderer(sourceList.csiScoreStats, this));
        candidateList.setFixedCellHeight(-1);
        candidateList.setPrototypeCellValue(FingerprintCandidateBean.PROTOTYPE);
        final JScrollPane scrollPane = new JScrollPane(candidateList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        candidateList.addMouseListener(this);
        this.structureSearcher = new StructureSearcher(sourceList.getElementList().size()); //todo does this work
        this.structureSearcherThread = new Thread(structureSearcher);
        structureSearcherThread.start();
        this.structureSearcher.reloadList(sourceList);
        this.molecularPropertyMatcherEditor.setStructureSearcher(structureSearcher);

        ///// add popup menu
        popupMenu = new JPopupMenu();
        CopyInchiKey = new JMenuItem("Copy 2D InChIKey");
        CopyInchi = new JMenuItem("Copy 2D InChI");
        OpenInBrowser1 = new JMenuItem("Open in PubChem");
        OpenInBrowser2 = new JMenuItem("Open in all databases");
        highlight = new JMenuItem("Highlight matching substructures");
        CopyInchi.addActionListener(this);
        CopyInchiKey.addActionListener(this);
        OpenInBrowser1.addActionListener(this);
        OpenInBrowser2.addActionListener(this);
        highlight.addActionListener(this);
        popupMenu.add(CopyInchiKey);
        popupMenu.add(CopyInchi);
        popupMenu.add(OpenInBrowser1);
        popupMenu.add(OpenInBrowser2);
        popupMenu.add(highlight);
        setVisible(true);
    }


    @Override
    protected JToolBar getToolBar() {
        JToolBar tb = super.getToolBar();

        filterByMolecularPropertyButton = new ToolbarToggleButton(null, Icons.MolecularProperty_24, "filter by highlighted molecular property");

        smartFilterTextField = new JTextField();
        TwoColumnPanel panel2 = new TwoColumnPanel();
        panel2.setBorder(new EmptyBorder(0, 0, 0, 0));
        smartFilterTextField.setPreferredSize(new Dimension(100, smartFilterTextField.getPreferredSize().height));
        panel2.add(new JLabel("SMARTS Filter"), smartFilterTextField);

        tb.add(filterByMolecularPropertyButton);
        tb.add(panel2);

        return tb;
    }

    @Override
    protected EventList<MatcherEditor<FingerprintCandidateBean>> getSearchFieldMatchers() {
        EventList<MatcherEditor<FingerprintCandidateBean>> list = super.getSearchFieldMatchers();
        list.add(new SmartFilterMatcherEditor(smartFilterTextField));

        molecularPropertyMatcherEditor = new MolecularPropertyMatcherEditor(filterByMolecularPropertyButton);
        list.add(molecularPropertyMatcherEditor);
        return list;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (selectedCompoundId < 0) return;
        final FingerprintCandidateBean c = candidateList.getModel().getElementAt(selectedCompoundId);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (e.getSource() == CopyInchiKey) {
            clipboard.setContents(new StringSelection(c.candidate.getInchi().key2D()), null);
        } else if (e.getSource() == CopyInchi) {
            clipboard.setContents(new StringSelection(c.candidate.getInchi().in2D), null);
        } else if (e.getSource() == OpenInBrowser1) {
            try {
                Desktop.getDesktop().browse(new URI("https://www.ncbi.nlm.nih.gov/pccompound?term=%22" + c.candidate.getInchi().key2D() + "%22[InChIKey]"));
            } catch (IOException | URISyntaxException e1) {
                LoggerFactory.getLogger(this.getClass()).error(e1.getMessage(), e1);
            }
        } else if (e.getSource() == OpenInBrowser2) {
            for (Map.Entry<String, String> entry : c.candidate.getLinkedDatabases().entries()) {
                DataSources.getSourceFromName(entry.getKey()).ifPresent(s -> {
                    if (entry.getValue() == null || s.URI == null)
                        return;
                    try {
                        if (s.URI.contains("%s")) {
                            Desktop.getDesktop().browse(new URI(String.format(Locale.US, s.URI, URLEncoder.encode(entry.getValue(), "UTF-8"))));
                        } else {
                            Desktop.getDesktop().browse(new URI(String.format(Locale.US, s.URI, Integer.parseInt(entry.getValue()))));
                        }
                    } catch (IOException | URISyntaxException e1) {
                        LoggerFactory.getLogger(this.getClass()).error(e1.getMessage(), e1);
                    }
                });
            }
        } else if (c!=null && e.getSource() == this.highlight) {
            SwingWorker w = new SwingWorker<Object, Object>() {

                @Override
                protected Object doInBackground() throws Exception {
                    try {
                        c.highlightInBackground();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return c;
                }

                @Override
                protected void done() {
                    super.done();
                    if (getState() == StateValue.DONE) {
                        final CompoundMatchHighlighter h;
                        synchronized (c) {
                            h = c.highlighter;
                        }
                        h.hightlight(c);
                        source.getElementList().elementChanged(c);
                    }
                }
            };
            w.execute();
        }
    }

    public void dispose() {
        structureSearcher.stop();
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
        final FingerprintCandidateBean candidate = candidateList.getModel().getElementAt(index);
        highlightedCandidate = candidate.index();
        final Rectangle relativeRect = candidateList.getCellBounds(index, index);

        final FingerprintAgreement ag = candidate.substructures;
        int[] rowcol = null;
        if (ag != null)
            rowcol = calculateAgreementIndex(ag, relativeRect, point);

        if (rowcol != null) {
            highlightAgree = candidate.substructures.indexAt(rowcol[0], rowcol[1]);
            structureSearcher.reloadList(source, highlightAgree, highlightedCandidate);
            molecularPropertyMatcherEditor.highlightChanged(filterByMolecularPropertyButton.isSelected());
        } else {
            if (highlightAgree >= 0) {
                highlightAgree = -1;
                structureSearcher.reloadList(source, highlightAgree, highlightedCandidate);
                molecularPropertyMatcherEditor.highlightChanged(filterByMolecularPropertyButton.isSelected());
            }

            for (DatabaseLabel l : candidate.labels) {
                if (l.rect.contains(point.x, point.y)) {
                    clickOnDBLabel(l);
                    break;
                }
            }
        }
    }

    private void clickOnDBLabel(DatabaseLabel label) {
        DataSources.getSourceFromName(label.name).ifPresent(s -> {
            if (label.values == null || label.values.length == 0 || s.URI == null)
                return;
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
        });
    }


    private void popup(MouseEvent e) {
        popupMenu.setVisible(false);
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
        final FingerprintCandidateBean candidate = candidateList.getModel().getElementAt(index);
        highlightedCandidate = candidate.index();

        if (e.isPopupTrigger()) popup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) popup(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void resultsChanged(InstanceBean experiment, FingerprintCandidateBean sre, List<FingerprintCandidateBean> resultElements, ListSelectionModel selections) {
        if (sre != null)
            this.structureSearcher.reloadList(source);
        filterByMolecularPropertyButton.setSelected(false);
    }

    private int[] calculateAgreementIndex(FingerprintAgreement ag, Rectangle relativeRect, Point clickPoint) {
        final Rectangle box = ag.getBounds();
        final int absX = box.x + relativeRect.x;
        final int absY = box.y + relativeRect.y;
        final int absX2 = box.width + absX;
        final int absY2 = box.height + absY;
        final boolean in = clickPoint.x >= absX && clickPoint.y >= absY && clickPoint.x < absX2 && clickPoint.y < absY2;

        if (in) {
            int rx = clickPoint.x - absX;
            int ry = clickPoint.y - absY;

            return new int[]{
                    ry / CandidateCellRenderer.CELL_SIZE, //row
                    rx / CandidateCellRenderer.CELL_SIZE //col
            };
        }
        return null;
    }


    public class CandidateInnerList extends JList<FingerprintCandidateBean> {
        private final NumberFormat prob = new DecimalFormat("%");

        public CandidateInnerList(ListModel<FingerprintCandidateBean> dataModel) {
            super(dataModel);
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            final Point point = e.getPoint();
            final int index = locationToIndex(point);
            if (index < 0) return null;
            final FingerprintCandidateBean candidate = getModel().getElementAt(index);
            final Rectangle relativeRect = getCellBounds(index, index);


            final FingerprintAgreement ag = candidate.substructures;
            if (ag != null) {
                int[] rowcol = calculateAgreementIndex(ag, relativeRect, point);
                if (rowcol != null) {
                    int fpindex = candidate.substructures.indexAt(rowcol[0], rowcol[1]);
                    return candidate.candidate.getFingerprint().getFingerprintVersion().getMolecularProperty(fpindex).getDescription() + "  (" + prob.format(candidate.getPlatts().getProbability(fpindex)) + " %)";
                }
            }
            return null;
        }
    }
}
