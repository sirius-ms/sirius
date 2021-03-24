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


package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;


import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MS1MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.ms_viewer.InSilicoSelectionBox;
import de.unijena.bioinf.ms.gui.ms_viewer.InsilicoFragmenter;
import de.unijena.bioinf.ms.gui.ms_viewer.WebViewSpectraViewer;
import de.unijena.bioinf.ms.gui.ms_viewer.data.SiriusIsotopePattern;
import de.unijena.bioinf.ms.gui.ms_viewer.data.SpectraJSONWriter;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import javafx.embed.swing.JFXPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class SpectraVisualizationPanel
	extends JPanel implements ActionListener, ItemListener, PanelDescription,
				   ActiveElementChangedListener<FormulaResultBean, InstanceBean>{
	@Override
	public String getDescription() {
		return "Spectra visualisation. Peaks that are explained by the Fragmentation tree of the selected molecular formula are highlighted in red";
	}

	public static final String MS1_DISPLAY = "MS1", MS1_MIRROR_DISPLAY = "MS1 mirror-plot", MS2_DISPLAY = "MS2",
			MS2_MERGED_DISPLAY = "merged";

	InstanceBean experiment = null;
	FormulaResultBean sre = null;
	JComboBox<String> modesBox;
	JComboBox<String> ceBox;
	InSilicoSelectionBox anoBox;
	String preferredMode = MS1_DISPLAY;
	String preferredCE = MS2_MERGED_DISPLAY;

	private InsilicoFragmenter fragmenter;

	private WebViewSpectraViewer browser;

	public SpectraVisualizationPanel() {
		this.setLayout(new BorderLayout());

		this.fragmenter = new InsilicoFragmenter(this);

		JToolBar northPanel = new JToolBar();
		northPanel.setFloatable(false);

		JLabel l = new JLabel("Mode");
		l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
		modesBox = new JComboBox<>();
		modesBox.addItemListener(this);
		ceBox = new JComboBox<>();
		northPanel.add(l);
		northPanel.add(modesBox);
		northPanel.add(ceBox);

		anoBox = new InSilicoSelectionBox(new Dimension(200,100), 5);
		northPanel.add(anoBox);

		anoBox.setAction(new InsilicoFrament());

		northPanel.addSeparator(new Dimension(10, 10));
		this.add(northPanel, BorderLayout.NORTH);


		/////////////
		// Browser //
		/////////////
		this.browser = new WebViewSpectraViewer();
		this.add((JFXPanel) this.browser, BorderLayout.CENTER);
		this.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	}

	@Deprecated
	private void debugWriteSpectra(String jsonstring, String filename){
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
			bw.write(jsonstring);
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("[DEBUG] dumped JSON to " + filename);

	}

	private void drawSpectra(InstanceBean experiment, FormulaResultBean sre, String mode,
							 int ce_index){
		if (mode == null)
			return;
		String jsonSpectra = null;
		SpectraJSONWriter spectraWriter = new SpectraJSONWriter();

		if (mode.contains(MS1_DISPLAY)) {
			// Deviation standardMassDeviation = experiment.getExperiment()
			// 		.getAnnotationOrNull(MS1MassDeviation.class).standardMassDeviation;
			List<SimpleSpectrum> spectra1 = experiment.getMs1Spectra();
			SimpleSpectrum spectrum = experiment.getMergedMs1Spectrum()==null ? spectra1.get(0) : experiment.getMergedMs1Spectrum(); // TODO: can there be more?
			if (mode.equals(MS1_DISPLAY)) {
				jsonSpectra = spectraWriter.ms1JSON(spectrum);
			} else if (mode.equals(MS1_MIRROR_DISPLAY)) {
				FTree ftree = sre.getFragTree().orElse(null);
				SiriusIsotopePattern siriusIsotopePattern = new SiriusIsotopePattern(ftree ,experiment.getExperiment(), spectrum);
				jsonSpectra = spectraWriter.ms1MirrorJSON(spectrum, siriusIsotopePattern);
			} else {
				System.err.println("Cannot draw spectra: Mode " + mode + " not (yet) supported!");
				return;
			}
		} else if (mode.equals(MS2_DISPLAY)) {
			// Deviation standardMassDeviation = experiment.getExperiment()
			// 		.getAnnotationOrNull(MS1MassDeviation.class).standardMassDeviation;
			if (ce_index == -1){
				jsonSpectra = spectraWriter.ms2JSON(experiment.getExperiment(), Optional.ofNullable(sre).flatMap(FormulaResultBean::getFragTree).orElse(null));
			} else {
				MutableMs2Spectrum spectrum = experiment.getMs2Spectra().get(ce_index);
				// if (spectrum == null){
				// 	System.err.printf("MS2 spectrum with selected collision energy (%s) not available!%n",
				// 		ce);
				//    return;
				// }
				FTree ftree = Optional.ofNullable(sre).flatMap(FormulaResultBean::getFragTree).orElse(null);
				if (ftree != null)
					jsonSpectra = spectraWriter.ms2JSON(spectrum, ftree);
				else
					jsonSpectra = spectraWriter.ms2JSON(spectrum);
			}
			// for (int i = 0; i < spectra2.size(); i++) {
			// 	System.out.printf("MS2 spectra %d%n", i);
			// 	jsonSpectra = spectraWriter.ms2JSON(spectra2.get(i), sre.getFragTree().orElse(null));
			// 	debugWriteSpectra(jsonSpectra, "/tmp/test_spectra_MS2_" + i + ".json"); // FIXME: DEBUG
			// }
			// jsonSpectra = spectraWriter.ms2JSON(
			// 	Spectrums.mergeSpectra(new Deviation(10, 0.1), true, false, spectra2),
			// 	sre.getFragTree().orElse(null));
		} else {
			System.err.println("Cannot draw spectra: Mode " + mode + " not (yet) supported!");
			return;
		}
		if (jsonSpectra != null){
			String json=null, svg=null;
			if (insilicoResult!=null) {
				json = insilicoResult.getJson();
				svg = insilicoResult.getSvg();
				debugWriteSpectra(json, "/tmp/test_highlight.json");
				debugWriteSpectra(svg, "/tmp/test_svg.xml");
			}
			browser.loadData(jsonSpectra, json, svg);
		}
	}

	@Override
	public void resultsChanged(InstanceBean experiment, FormulaResultBean sre, List<FormulaResultBean> resultElements, ListSelectionModel selections) {
		if ((this.experiment == null ^ experiment == null) || (this.sre == null ^ sre == null)) {
			// update modeBox elements, don't listen to these events
			modesBox.removeItemListener(this);
			modesBox.removeAllItems();
			if (experiment != null) {
				if (experiment.getMs1Spectra().size() > 0 || experiment.getMergedMs1Spectrum()!=null)
					modesBox.addItem(MS1_DISPLAY);
				if (sre != null) {
					if (experiment.getMs1Spectra().size() > 0 || experiment.getMergedMs1Spectrum()!=null)
						modesBox.addItem(MS1_MIRROR_DISPLAY);
				}
				if (experiment.getMs2Spectra().size() > 0)
					modesBox.addItem(MS2_DISPLAY);
				updateCEBox(experiment);
			}
			modesBox.addItemListener(this);
		}
		if (experiment != null){
			boolean preferredPossible = false; // no `contains` for combobox
			for (int i=0; i < modesBox.getItemCount(); i++)
				preferredPossible |= preferredMode.equals((String) modesBox.getItemAt(i));
			// change to preferred mode if possible, else (potentially automatic) selection
			if (preferredPossible){
				modesBox.removeItemListener(this);
				modesBox.setSelectedItem(preferredMode);
				ceBox.setVisible(modesBox.getSelectedItem().equals(MS2_DISPLAY));
				modesBox.addItemListener(this);
			}
			updateCEBox(experiment);
			drawSpectra(experiment, sre, (String) modesBox.getSelectedItem(), getCEIndex());
		}
		 else
			browser.clear();
		// store data to switch between modes without having to switch to other results
		this.experiment = experiment;
		this.sre = sre;
		clearInsilicoResult();
		anoBox.resultsChanged(experiment,sre,resultElements,selections);
		if (getCurrentMode().msLevel>1) {
			anoBox.activate();
		} else {
			anoBox.deactivate();
		}
	}

	private void updateCEBox(InstanceBean experiment){
		ceBox.removeItemListener(this);
		ceBox.removeAllItems();
		for (int i = 0; i < experiment.getMs2Spectra().size(); ++i){
			MutableMs2Spectrum spectrum = experiment.getMs2Spectra().get(i);
			CollisionEnergy collisionEnergy = spectrum.getCollisionEnergy();
			ceBox.addItem(collisionEnergy.equals(CollisionEnergy.none()) ?
						  "mode " + (i + 1) : collisionEnergy.toString());
		}
		ceBox.addItem(MS2_MERGED_DISPLAY);
		ceBox.setSelectedItem(MS2_MERGED_DISPLAY);
		ceBox.addItemListener(this);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object sel = modesBox.getSelectedItem();
		ceBox.setVisible(sel.equals(MS2_DISPLAY));
		if (sel.equals(MS2_DISPLAY) || sel.equals(MS2_MERGED_DISPLAY)) {
			anoBox.activate();
		} else {
			anoBox.deactivate();
		}
		preferredMode = (String) sel;
		if (sel != null && !(experiment == null && sre == null)){
			drawSpectra(experiment, sre, (String) sel, getCEIndex());
		}
	}

	private int getCEIndex(){
		return ceBox.getSelectedItem().equals(MS2_MERGED_DISPLAY) ? -1 : ceBox.getSelectedIndex();
	}

	public enum SpectrumMode {
		MS1(MS1_DISPLAY, 1),
		MS1_MIRROR(MS1_MIRROR_DISPLAY, 1),
		MS2_MERGED(MS2_MERGED_DISPLAY, 2),
		MS2(MS2_DISPLAY, 2);

		private final String label;
		private final int msLevel;

		SpectrumMode(String label, int msLevel) {
			this.label = label;
			this.msLevel = msLevel;
		}
	};

	public SpectrumMode getCurrentMode() {
		// we should use a variable for this!
		// then, connect everything with listeners
		final Object s = modesBox.getSelectedItem();
		if (s.equals(MS1_DISPLAY)) return SpectrumMode.MS1;
		if (s.equals(MS1_MIRROR_DISPLAY)) return SpectrumMode.MS1_MIRROR;
		if (s.equals(MS2_MERGED_DISPLAY)) return SpectrumMode.MS2_MERGED;
		if (s.equals(MS2_DISPLAY)) return SpectrumMode.MS2;
		return SpectrumMode.MS1; // ?
	}

	public class InsilicoFrament extends AbstractAction {

		@Override
		public void actionPerformed(ActionEvent e) {
			final Object selectedItem = anoBox.getSelectedItem();
			if (selectedItem!=null && selectedItem instanceof InSilicoSelectionBox.Item) {
				InSilicoSelectionBox.Item item = (InSilicoSelectionBox.Item)selectedItem;
				if (item.getCandidate()!=null) {
					fragmenter.fragment(sre,item.getCandidate());
					System.out.println("WORKER STARTED");
				}
			}
		}
	}

	////////////////
	// TODO: temporary workaround
	////////////////
	private InsilicoFragmenter.Result insilicoResult;
	public void setInsilicoResult(InsilicoFragmenter.Result result) {
		this.insilicoResult = result;
		System.out.println("INSILICO DONE\n" + result.getJson());
		if (getCurrentMode().msLevel >= 2) {
			drawSpectra(experiment, sre, getCurrentMode().label, getCEIndex());
		}
	}
	public void clearInsilicoResult() {
		this.insilicoResult = null;
	}
}
