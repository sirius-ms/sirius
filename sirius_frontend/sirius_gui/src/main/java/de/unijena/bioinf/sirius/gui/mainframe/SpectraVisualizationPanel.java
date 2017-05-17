package de.unijena.bioinf.sirius.gui.mainframe;


import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.gui.msviewer.MSViewerPanel;
import de.unijena.bioinf.sirius.gui.msviewer.MSViewerPanelListener;
import de.unijena.bioinf.sirius.gui.msviewer.data.ExperimentContainerDataModel;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.configs.Buttons;
import de.unijena.bioinf.sirius.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.sirius.gui.configs.Icons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;

public class SpectraVisualizationPanel extends JPanel implements ActionListener, MSViewerPanelListener, MouseListener, ActiveElementChangedListener<SiriusResultElement,ExperimentContainer> {

	private JComboBox<String> spectraSelection;
	private MSViewerPanel msviewer;

	private ExperimentContainer ec;
	private DefaultComboBoxModel<String> cbModel;

	private DecimalFormat cEFormat;

	private ExperimentContainerDataModel model;

	private JButton zoomIn, zoomOut;

	private boolean zoomed;

	private SiriusResultElement sre;

	JPopupMenu zoomPopMenu;
	JMenuItem zoomInMI, zoomOutMI;

	public SpectraVisualizationPanel() {
		this.ec = null;
		this.sre = null;

		zoomIn = Buttons.getZoomInButton24();
		zoomOut = Buttons.getZoomOutButton24();
		zoomIn.addActionListener(this);
		zoomOut.addActionListener(this);
		zoomIn.setEnabled(false);
		zoomOut.setEnabled(false);
		zoomed = false;

		constructZoomPopupMenu();

		this.cEFormat = new DecimalFormat("#0.0");
		this.setLayout(new BorderLayout());


		JToolBar northPanel = new JToolBar();
		northPanel.setFloatable(false);
		cbModel = new DefaultComboBoxModel<>();

		spectraSelection = new JComboBox<String>(cbModel);
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

		model = new ExperimentContainerDataModel(ec);

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

	private final String MS1_DISPLAY = "MS 1", MSMS_DISPLAY = "MSMS merged";

	private void updateLogic() {
		HashSet<String> nameStorage = new HashSet<>();
		boolean hasMs1 = false, hasMs2 = false, hasMerged = false;
		spectraSelection.removeActionListener(this);

		cbModel.removeAllElements();
		if (ec == null) {
			this.zoomIn.setEnabled(false);
			this.zoomOut.setEnabled(false);
			this.zoomInMI.setEnabled(false);
			this.zoomOutMI.setEnabled(false);
			spectraSelection.setEnabled(false);
		} else {
			this.zoomIn.setEnabled(false);
			this.zoomOut.setEnabled(false);
			this.zoomInMI.setEnabled(false);
			this.zoomOutMI.setEnabled(false);
			java.util.List<CompactSpectrum> ms1 = ec.getMs1Spectra();
			java.util.List<CompactSpectrum> ms2 = ec.getMs2Spectra();
			if (!(ms1 == null || ms1.isEmpty())) {
				cbModel.addElement(MS1_DISPLAY);
				hasMs1 = true;
			}
			if (ms2 != null) {
				if (ms2.size() > 1) {
					cbModel.addElement(MSMS_DISPLAY);
					hasMerged = true;
				}
				hasMs2 = true;
				for (CompactSpectrum sp : ms2) {

					String value = null;

					if (sp.getCollisionEnergy() != null) {
						double minEn = sp.getCollisionEnergy().getMinEnergy();
						double maxEn = sp.getCollisionEnergy().getMaxEnergy();

						if (minEn == maxEn) {
							value = cEFormat.format(minEn) + " eV";
						} else {
							value = cEFormat.format(minEn) + "-" + cEFormat.format(maxEn) + " eV";
						}
						int counter = 2;
						while (nameStorage.contains(value)) {
							if (minEn == maxEn) {
								value = cEFormat.format(minEn) + " eV (" + counter + ")";
							} else {
								value = cEFormat.format(minEn) + "-" + cEFormat.format(maxEn) + " eV (" + counter + ")";
							}
							counter++;
						}
					} else {
						value = "MSMS";
						int counter = 2;
						while (nameStorage.contains(value)) {
							value = "MSMS (" + counter + ")";
							counter++;
						}
					}


					nameStorage.add(value);
					cbModel.addElement(value);
				}
			}

			final ExperimentContainerDataModel.DisplayMode currentMode = model.getMode();
			final int currentIndex = model.getSelectedMs2Spectrum();
			if (cbModel.getSize() > 0) {
				if (currentMode == ExperimentContainerDataModel.DisplayMode.MS) {
					if (hasMs1) cbModel.setSelectedItem(MS1_DISPLAY);
					else spectraSelection.setSelectedIndex(0);

				} else if (currentMode == ExperimentContainerDataModel.DisplayMode.MSMS) {
					int i = currentIndex;
					if (hasMs1) ++i;
					if (hasMerged) ++i;
					i = Math.max(0, Math.min(i, spectraSelection.getItemCount() - 1));
					spectraSelection.setSelectedIndex(i);
				} else if (currentMode == ExperimentContainerDataModel.DisplayMode.MERGED) {
					if (hasMerged) cbModel.setSelectedItem(MSMS_DISPLAY);
					else spectraSelection.setSelectedIndex(hasMs1 && hasMs2 ? 1 : 0);
				} else {
					if (cbModel.getSize() > 0) spectraSelection.setSelectedIndex(0);
					else spectraSelection.setSelectedIndex(-1);
				}
			} else spectraSelection.setSelectedIndex(-1);

			spectraSelection.setEnabled(true);
			spectraSelection.addActionListener(this);
		}
	}

	public void resultsChanged(ExperimentContainer ec, SiriusResultElement sre, List<SiriusResultElement> sres, ListSelectionModel selection) {
		if (this.ec != ec || this.sre != sre) {
			this.ec = ec;
			this.sre = sre;

			updateLogic();

			if (ec != null) {
				this.model.changeData(ec, this.sre, getDisplayMode(), getSelectedMs2Spectrum());
			} else {
				this.model = new ExperimentContainerDataModel(null);
			}

			msviewer.setData(model);
			msviewer.repaint();
		}
	}

	public ExperimentContainer getExperiment() {
		return ec;
	}

	private int getSelectedMs2Spectrum() {
		int index = this.spectraSelection.getSelectedIndex();
		if (ec.getMs1Spectra().size() > 0) --index;
		if (ec.getMs2Spectra().size() > 1) --index;
		return index;
	}

	private ExperimentContainerDataModel.DisplayMode getDisplayMode() {
		String s = (String) this.spectraSelection.getSelectedItem();
		if (s == null) return ExperimentContainerDataModel.DisplayMode.DUMMY;
		if (s.equals(MS1_DISPLAY)) return ExperimentContainerDataModel.DisplayMode.MS;
		else if (s.equals(MSMS_DISPLAY)) return ExperimentContainerDataModel.DisplayMode.MERGED;
		else return ExperimentContainerDataModel.DisplayMode.MSMS;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == spectraSelection) {
			java.util.List<CompactSpectrum> ms1 = ec.getMs1Spectra();
			java.util.List<CompactSpectrum> ms2 = ec.getMs2Spectra();
			int index = this.spectraSelection.getSelectedIndex();
			if (index < 0) return;
			final ExperimentContainerDataModel.DisplayMode mode = getDisplayMode();
			if (mode == ExperimentContainerDataModel.DisplayMode.MS) {
				model.selectMS1Spectrum();
			} else if (mode == ExperimentContainerDataModel.DisplayMode.MSMS) {
				model.selectMS2Spectrum(getSelectedMs2Spectrum());
			} else if (mode == ExperimentContainerDataModel.DisplayMode.MERGED) {
				model.selectMergedSpectrum();
			} else {
				model.showDummySpectrum();
			}
			msviewer.setData(model);
			this.showMolecularFormulaMarkings();
//			msviewer.repaint();
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
	public void markingsRemoved() {
		this.model.removeMarkings();
		this.zoomIn.setEnabled(false);
		this.zoomInMI.setEnabled(false);
//		this.msviewer.repaint();
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
