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

import ca.odell.glazedlists.swing.DefaultEventListModel;
import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.table.ActiveElementChangedListener;
import org.slf4j.LoggerFactory;

import javax.swing.*;
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

public class CandidateListDetailView extends CandidateListView implements ActiveElementChangedListener<CompoundCandidate, ExperimentContainer>, MouseListener, ActionListener {


    protected JList<CompoundCandidate> candidateList;
    protected StructureSearcher structureSearcher;
    protected Thread structureSearcherThread;


    protected JMenuItem CopyInchiKey, CopyInchi, OpenInBrowser1, OpenInBrowser2;
    protected JPopupMenu popupMenu;

    protected int highlightAgree = -1;
    protected int highlightedCandidate = -1;
    protected int selectedCompoundId;


    public CandidateListDetailView(final CSIFingerIdComputation computation, CandidateList sourceList) {
        super(sourceList);
        candidateList = new CandidateInnerList(new DefaultEventListModel<CompoundCandidate>(filteredSource));

        ToolTipManager.sharedInstance().registerComponent(candidateList);
        candidateList.setCellRenderer(new CandidateCellRenderer(computation, sourceList.scoreStats, this));
        candidateList.setFixedCellHeight(-1);
        candidateList.setPrototypeCellValue(CompoundCandidate.PROTOTYPE);
        final JScrollPane scrollPane = new JScrollPane(candidateList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        candidateList.addMouseListener(this);
        this.structureSearcher = new StructureSearcher(sourceList.getElementList().size()); //todo does this work
        this.structureSearcherThread = new Thread(structureSearcher);
        structureSearcherThread.start();
        this.structureSearcher.reloadList(sourceList);

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
        final CompoundCandidate candidate = candidateList.getModel().getElementAt(index);
        highlightedCandidate = candidate.index;

        final Rectangle relativeRect = candidateList.getCellBounds(index, index);
        final FingerprintAgreement ag = candidate.substructures;
        int[] rowcol = null;
        if (ag != null)
            rowcol = calculateAgreementIndex(ag, relativeRect, point);

        if (rowcol != null) {
            highlightAgree = candidate.substructures.indexAt(rowcol[0], rowcol[1]);
            structureSearcher.reloadList(source, highlightAgree, highlightedCandidate);
        } else {
            if (highlightAgree >= 0) {
                highlightAgree = -1;
                structureSearcher.reloadList(source, highlightAgree, highlightedCandidate);
            }

            double rpx = point.x - relativeRect.getX(), rpy = point.y - relativeRect.getY();
            for (de.unijena.bioinf.sirius.gui.fingerid.DatabaseLabel l : candidate.labels) {
                if (l.rect.contains(rpx, rpy)) {
                    clickOnDBLabel(l); //todo I think here is still a bugsss
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
        final CompoundCandidate candidate = candidateList.getModel().getElementAt(index);
        highlightedCandidate = candidate.index;

        if (e.isPopupTrigger()) popup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    public static final Color PRIMARY_HIGHLIGHTED_COLOR = new Color(0, 100, 255, 128);
    public static final Color SECONDARY_HIGHLIGHTED_COLOR = new Color(100, 100, 255, 64).brighter();

    @Override
    public void resultsChanged(ExperimentContainer experiment, CompoundCandidate sre, List<CompoundCandidate> resultElements, ListSelectionModel selections) {
        if (sre != null)
            this.structureSearcher.reloadList(source);
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


    public class CandidateInnerList extends JList<CompoundCandidate> {
        private final NumberFormat prob = new DecimalFormat("%");

        public CandidateInnerList(ListModel<CompoundCandidate> dataModel) {
            super(dataModel);
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            final Point point = e.getPoint();
            final int index = locationToIndex(point);
            if (index < 0) return null;
            final CompoundCandidate candidate = getModel().getElementAt(index);
            final Rectangle relativeRect = getCellBounds(index, index);


            final FingerprintAgreement ag = candidate.substructures;
            if (ag != null) {
                int[] rowcol = calculateAgreementIndex(ag, relativeRect, point);
                if (rowcol != null) {
                    int fpindex = candidate.substructures.indexAt(rowcol[0], rowcol[1]);
                    return candidate.compound.fingerprint.getFingerprintVersion().getMolecularProperty(fpindex).getDescription() + "  (" + prob.format(candidate.getPlatts().getProbability(fpindex)) + " %)";
                }
            }
            return null;
        }
    }
}
