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


import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.ms_viewer.WebViewSpectraViewer;
import de.unijena.bioinf.ms.gui.ms_viewer.data.ExperimentContainerDataModel;
import de.unijena.bioinf.ms.gui.ms_viewer.data.MSViewerDataModel;
import de.unijena.bioinf.ms.gui.ms_viewer.data.PeakInformation;
import de.unijena.bioinf.ms.gui.ms_viewer.data.SiriusIsotopePattern;
import de.unijena.bioinf.ms.gui.ms_viewer.data.SpectraJSONWriter;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.sirius.Ms2Preprocessor;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;

import javax.swing.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.HashMap;

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
	String preferredMode = MS1_DISPLAY;
	String preferredCE = MS2_MERGED_DISPLAY;
	FormulaResultBean currentResult;

	private WebViewSpectraViewer browser;

	public SpectraVisualizationPanel() {
		this.setLayout(new BorderLayout());


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

		northPanel.addSeparator(new Dimension(10, 10));

		this.add(northPanel, BorderLayout.NORTH);


		/////////////
		// Browser //
		/////////////
		this.browser = new WebViewSpectraViewer();

		browser.addJS("d3.min.js");
		browser.addJS("spectra_viewer/spectra_viewer.js");
		this.add((JFXPanel) this.browser, BorderLayout.CENTER);
		this.setVisible(true);
		HashMap<String, Object> bridges = new HashMap<String, Object>() {{}};
		browser.load(bridges);
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
							 String ce){
		if (mode == null)
			return;
		String jsonSpectra = null;
		SpectraJSONWriter spectraWriter = new SpectraJSONWriter();

		if (mode.contains(MS1_DISPLAY)) {
			List<SimpleSpectrum> spectra1 = experiment.getMs1Spectra();
			SimpleSpectrum spectrum = spectra1.get(0); // TODO: can there be more?
			if (mode.equals(MS1_DISPLAY)) {
				jsonSpectra = spectraWriter.ms1JSON(spectrum);
			} else if (mode.equals(MS1_MIRROR_DISPLAY)) {
				FTree ftree = sre.getFragTree().orElse(null);
				IsotopePattern ip = ftree.getAnnotationOrNull(IsotopePattern.class);
				jsonSpectra = spectraWriter.ms1MirrorJSON(spectrum, ip.getPattern(), ftree);
			} else {
				System.err.println("Cannot draw spectra: Mode " + mode + " not (yet) supported!");
				return;
			}
		} else if (mode.equals(MS2_DISPLAY)) {

			if (ce.equals(MS2_MERGED_DISPLAY)){
				jsonSpectra = spectraWriter.ms2JSON(experiment.getExperiment(), sre.getFragTree().orElse(null));
				debugWriteSpectra(jsonSpectra, "/tmp/test_spectra_MS2_merged.json"); // FIXME: DEBUG
			} else {
				MutableMs2Spectrum spectrum = experiment.getMs2Spectra().stream()
						.filter(s -> s.getCollisionEnergy().toString().equals(ce)).findFirst().orElse(null);
				if (spectrum == null){
					System.err.printf("MS2 spectrum with selected collision energy (%s) not available!%n",
						ce);
				   return;
				}
				jsonSpectra = spectraWriter.ms2JSON(spectrum, sre.getFragTree().orElse(null));
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
			// debugWriteSpectra(jsonSpectra, "/tmp/test_spectra.json"); // FIXME: DEBUG
			browser.loadData(jsonSpectra);
		}
	}

	@Override
	public void resultsChanged(InstanceBean experiment, FormulaResultBean sre, List<FormulaResultBean> resultElements, ListSelectionModel selections) {
		if ((this.experiment == null ^ experiment == null) || (this.sre == null ^ sre == null)){
			// update modeBox elements, don't listen to these events
			modesBox.removeItemListener(this);
			modesBox.removeAllItems();
			ceBox.removeItemListener(this);
			ceBox.removeAllItems();
			if (experiment != null) {
				if (experiment.getMs1Spectra().size() > 0)
					modesBox.addItem(MS1_DISPLAY);
				if (sre != null) {
					if (experiment.getMs1Spectra().size() > 0)
						modesBox.addItem(MS1_MIRROR_DISPLAY);
					if (experiment.getMs2Spectra().size() > 0)
						modesBox.addItem(MS2_DISPLAY);
					for (MutableMs2Spectrum spectrum : experiment.getMs2Spectra()){
						ceBox.addItem(spectrum.getCollisionEnergy().toString());
					}
					ceBox.addItem(MS2_MERGED_DISPLAY);
					ceBox.setSelectedItem(MS2_MERGED_DISPLAY);
				}
			}
			modesBox.addItemListener(this);
			ceBox.addItemListener(this);
		}
		if (experiment != null){
			FTree ftree;
			// if (sre != null && (ftree = sre.getFragTree().orElse(null)) != null) { // debug
			// 	JsonArray jPeaks = new SpectraJSONWriter().ms2JsonPeaks(experiment.getExperiment(), ftree);
			// 	final Gson gson = new GsonBuilder().setPrettyPrinting().create();
			// 	System.out.println(gson.toJson(jPeaks));
			// }
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
			drawSpectra(experiment, sre, (String) modesBox.getSelectedItem(),
						(String) ceBox.getSelectedItem());
		}
		 else
			browser.clear();
		// store data to switch between modes without having to switch to other results
		this.experiment = experiment;
		this.sre = sre;
	}


	private void printAllSpectra(List<SimpleSpectrum> spectra1, List<MutableMs2Spectrum> spectra2, FTree ftree){
		// MS1
		IsotopePattern ip = ftree.getAnnotationOrNull(IsotopePattern.class);
		SiriusIsotopePattern ip_annotated = new SiriusIsotopePattern(ftree, spectra1.get(0));
		if (ip != null){
			MolecularFormula form = ip.getCandidate();
			SpectraJSONWriter spectraWriter = new SpectraJSONWriter();
			System.out.println(spectraWriter.ms1MirrorJSON(spectra1.get(0), ip.getPattern(), ftree));
		}
		else
			System.out.println("isotope pattern is null");
		// MS2
		if (spectra2.size() > 0){
			SpectraJSONWriter spectraWriter = new SpectraJSONWriter();
			for (int i = 0; i < spectra2.size(); ++i){
				System.out.println("=== SpectraWriter: MS2 Spectrum " + spectra2.get(i).getCollisionEnergy()
								   + ", " + spectra2.get(i).getIonization() + ", " + spectra2.get(i).getScanNumber());
				String jsonstring = spectraWriter.ms2JSON(spectra2.get(i), ftree);
				try{
					BufferedWriter bw = new BufferedWriter(
						new FileWriter(
							"ms2spectrum" + i + ".json"));
					bw.write(jsonstring);
					bw.close();
				} catch (IOException e){
					e.printStackTrace();
				}
			}
		}
	}


	public void printSpectrum(SimpleSpectrum s) {
		 double scale = Spectrums.getMaximalIntensity(s);
		System.out.println("Spectrum: ");
		for (int i = 0; i < s.size(); ++i){
			System.out.println(i + " intensity: " + s.getIntensityAt(i) / scale
							   + ", mz: " + s.getMzAt(i));
		}
	}

	public void printSpectrum(MSViewerDataModel s) {
		System.out.println("Spectrum " + s.toString() + ":");
		for (int i = 0; i < s.getSize(); ++i){
			System.out.println(i + " intensity: " + s.getRelativeIntensity(i)
							   + ", mz: " + s.getMass(i) + " isIsotope? -> " + s.isIsotope(i)
							   + ", formula: " + s.getMolecularFormula(i));
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
			Object sel = modesBox.getSelectedItem();
			ceBox.setVisible(sel.equals(MS2_DISPLAY));
			preferredMode = (String) sel;
			// System.out.println("Changed preferred mode to " + preferredMode);
			if (sel != null && !(experiment == null && sre == null)){
				drawSpectra(experiment, sre, (String) sel, (String) ceBox.getSelectedItem()
					);
			}
	}
}
