package de.unijena.bioinf.sirius.gui.mainframe;


import de.unijena.bioinf.sirius.gui.configs.Buttons;
import de.unijena.bioinf.sirius.gui.configs.Icons;
import de.unijena.bioinf.sirius.gui.msviewer.MSViewerPanel;
import de.unijena.bioinf.sirius.gui.msviewer.MSViewerPanelListener;
import de.unijena.bioinf.sirius.gui.msviewer.data.ExperimentContainerDataModel;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.sirius.gui.utils.PanelDescription;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

public class SpectraVisualizationPanel extends JPanel implements ActionListener, MSViewerPanelListener, MouseListener, PanelDescription, ActiveElementChangedListener<SiriusResultElement, ExperimentContainer> {
    @Override
    public String getDescription() {
        return "Spectra visualisation. Peaks that are explained by the Fragmentation tree of the selected molecular formula are highlighted in red";
    }

    private JComboBox<String> spectraSelection;

    private MSViewerPanel msviewer;
    private ExperimentContainerDataModel model;

    public ExperimentContainerDataModel getModel() {
        return model;
    }

    private JButton zoomIn, zoomOut;

    JPopupMenu zoomPopMenu;
    JMenuItem zoomInMI, zoomOutMI;

    public SpectraVisualizationPanel() {
        model = new ExperimentContainerDataModel();

        zoomIn = Buttons.getZoomInButton24();
        zoomOut = Buttons.getZoomOutButton24();
        zoomIn.addActionListener(this);
        zoomOut.addActionListener(this);
        zoomIn.setEnabled(false);
        zoomOut.setEnabled(false);

        constructZoomPopupMenu();


        this.setLayout(new BorderLayout());


        JToolBar northPanel = new JToolBar();
        northPanel.setFloatable(false);

        spectraSelection = new JComboBox<String>(model.getComboBoxModel());
        spectraSelection.setToolTipText("select spectrum");

        updateLogic();

        JLabel l = new JLabel("Spectrum:");
        l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
        northPanel.add(l);
        northPanel.add(spectraSelection);

        northPanel.addSeparator(new Dimension(10, 10));
        northPanel.add(zoomIn);
        northPanel.add(zoomOut);

        this.add(northPanel, BorderLayout.NORTH);

        msviewer = new MSViewerPanel();
        msviewer.showPeakInfoOnlyForImportantPeaks(true);
        msviewer.setData(model);
        this.add(msviewer);
        msviewer.addMSViewerPanelListener(this);
        msviewer.addMouseListener(this);
    }

    public void constructZoomPopupMenu() {
        zoomPopMenu = new JPopupMenu();
        zoomInMI = new JMenuItem("Zoom in", Icons.Zoom_In_16);
        zoomOutMI = new JMenuItem("Zoom out", Icons.Zoom_Out_16);

        zoomInMI.addActionListener(this);
        zoomOutMI.addActionListener(this);

        zoomInMI.setEnabled(false);
        zoomOutMI.setEnabled(false);

        zoomPopMenu.add(zoomInMI);
        zoomPopMenu.add(zoomOutMI);
    }


    private void updateLogic() {
        this.zoomIn.setEnabled(false);
        this.zoomOut.setEnabled(false);
        this.zoomInMI.setEnabled(false);
        this.zoomOutMI.setEnabled(false);
        spectraSelection.setEnabled(model.getComboBoxModel().getSize() > 0);
    }

    private void spectraSelectionAction() {
        model.selectSpectrum((String) spectraSelection.getSelectedItem());
        msviewer.setData(model);
        showMolecularFormulaMarkings();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == spectraSelection) {
            if (this.spectraSelection.getSelectedIndex() < 0) return;
            spectraSelectionAction();
        } else if (e.getSource() == zoomIn || e.getSource() == zoomInMI) {
            zoomIn.setEnabled(false);
            zoomInMI.setEnabled(false);
            int start = model.getFirstMarkedIndex();
            int end = model.getLastMarkedIndex();
            this.model.removeMarkings();
            if (start < 0 || end < 0 || start == end) {
                return;
            }
            this.msviewer.showZoomedView(start, end);
            zoomOut.setEnabled(true);
            zoomOutMI.setEnabled(true);
        } else if (e.getSource() == zoomOut || e.getSource() == zoomOutMI) {
            zoomOut.setEnabled(false);
            zoomOutMI.setEnabled(false);
            this.model.removeMarkings();
            this.msviewer.showOverview();
            zoomIn.setEnabled(true);
            zoomInMI.setEnabled(true);
        }

    }

    private void showMolecularFormulaMarkings() {
        msviewer.showOverview();
        this.zoomIn.setEnabled(false);
        this.zoomOut.setEnabled(false);
        this.zoomInMI.setEnabled(false);
        this.zoomOutMI.setEnabled(false);
        msviewer.repaint();
    }

    @Override
    public void resultsChanged(ExperimentContainer experiment, SiriusResultElement sre, List<SiriusResultElement> resultElements, ListSelectionModel selections) {
        final String selected = (String) spectraSelection.getSelectedItem();
        if (model.changeData(experiment, sre)) {
            spectraSelection.removeActionListener(this);

            updateLogic();

            spectraSelection.setSelectedItem(selected);
            if (spectraSelection.getSelectedItem() == null) {
                for (int i=0, n = spectraSelection.getModel().getSize(); i<n; ++i) {
                    if (spectraSelection.getModel().getElementAt(i).equals(ExperimentContainerDataModel.MSMS_MERGED_DISPLAY) || i == n-1) {
                        spectraSelection.setSelectedIndex(i);
                    }
                }
            }
            spectraSelectionAction();
            spectraSelection.addActionListener(this);

        }
    }

    @Override
    public void markingsRemoved() {
        this.model.removeMarkings();
        this.zoomIn.setEnabled(false);
        this.zoomInMI.setEnabled(false);
    }

    @Override
    public void peaksMarked(List<Integer> indices) {
        this.model.removeMarkings();
        for (int i : indices) this.model.setMarked(i, true);
        if (indices.size() > 0) {
            this.zoomIn.setEnabled(true);
            this.zoomInMI.setEnabled(true);
        }
    }

    @Override
    public void peaksMarkedPerDrag(List<Integer> indices) {
        this.model.removeMarkings();
        for (int i : indices) this.model.setMarked(i, true);
        if (indices.size() > 0) {
            this.zoomIn.setEnabled(true);
            this.zoomInMI.setEnabled(true);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            this.zoomPopMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            this.zoomPopMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }
}
