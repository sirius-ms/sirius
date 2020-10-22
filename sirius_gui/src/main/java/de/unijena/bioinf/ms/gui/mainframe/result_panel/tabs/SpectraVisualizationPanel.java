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
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;

import javax.swing.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.HashMap;

public class SpectraVisualizationPanel extends JPanel implements ActionListener, PanelDescription, ActiveElementChangedListener<FormulaResultBean, InstanceBean> {
	@Override
	public String getDescription() {
		return "Spectra visualisation. Peaks that are explained by the Fragmentation tree of the selected molecular formula are highlighted in red";
	}

	InstanceBean experiment = null;
	FormulaResultBean sre = null;
	JComboBox<String> modesBox;

	private WebViewSpectraViewer browser;

	public SpectraVisualizationPanel() {
		this.setLayout(new BorderLayout());


		JToolBar northPanel = new JToolBar();
		northPanel.setFloatable(false);

		JLabel l = new JLabel("Mode");
		l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
		String modes[] = {"MS1", "MS1 mirror-plot", "MS2"};
		modesBox = new JComboBox<>(modes);
		modesBox.addActionListener(this);
		modesBox.setSelectedItem("MS1 mirror-plot");
		northPanel.add(l);
		northPanel.add(modesBox);

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
		if (e.getSource() == modesBox){
			if (sre != null && experiment != null)
				drawSpectra(experiment, sre, (String) modesBox.getSelectedItem());
		}
	}

	private void debugWriteSpectra(String jsonstring){
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter("/tmp/test_spectra.json"));
			bw.write(jsonstring);
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void drawSpectra(InstanceBean experiment, FormulaResultBean sre, String mode){
		String jsonSpectra;
		SpectraJSONWriter spectraWriter = new SpectraJSONWriter();
		FTree ftree = sre.getFragTree().orElse(null);
		if (ftree == null){
			System.err.println("Cannot draw spectra: FragTree cannot be retrieved!");
			return;
		}
		String jsonTree = new FTJsonWriter().treeToJsonString(ftree, experiment.getID().getIonMass().orElse(null)); // FIXME:
																												// for
																												// debugging?
		switch (mode){
		case "MS1 mirror-plot":
			List<SimpleSpectrum> spectra1 = experiment.getMs1Spectra();
			if (spectra1.size() == 0){
				System.err.println("Cannot draw MS1 mirror-plot: Spectra cannot be retrieved!");
				return;
			}
			IsotopePattern ip = ftree.getAnnotationOrNull(IsotopePattern.class);
			jsonSpectra = spectraWriter.spectraJSONString(
				spectra1.get(0 // TODO: are there cases with more than one?
					), ip.getPattern(), ftree);
			break;
		case "MS2":
			List<MutableMs2Spectrum> spectra2 = experiment.getMs2Spectra();
			if (spectra2.size() == 0){
				System.err.println("Cannot draw MS1 mirror-plot: Spectra cannot be retrieved!");
				return;
			}
			jsonSpectra = spectraWriter.spectrumJSONString(spectra2.get(
							spectra2.size() - 1 // should me merged MS/MS, TODO: verify
					), ftree);
			break;
		case "MS1": // TODO: implement: same JSON or remove second spectrum?
		default:
			System.err.println("Cannot draw spectra: Mode " + mode + " not (yet) supported!");
			return;
		}
		debugWriteSpectra(jsonSpectra); // FIXME: DEBUG
		browser.loadData(jsonSpectra, jsonTree);
	}	

	@Override
	public void resultsChanged(InstanceBean experiment, FormulaResultBean sre, List<FormulaResultBean> resultElements, ListSelectionModel selections) {
		if (sre != null) {
			// store data to switch between modes without having to switch to other results
			this.experiment = experiment;
			this.sre = sre;
			drawSpectra(experiment, sre, (String) modesBox.getSelectedItem());
		} else {
			this.experiment = null;
			this.sre = null;
			browser.clear();
		}
	}


	private void printAllSpectra(List<SimpleSpectrum> spectra1, List<MutableMs2Spectrum> spectra2, FTree ftree){
		// MS1
		IsotopePattern ip = ftree.getAnnotationOrNull(IsotopePattern.class);
		SiriusIsotopePattern ip_annotated = new SiriusIsotopePattern(ftree, spectra1.get(0));
		if (ip != null){
			MolecularFormula form = ip.getCandidate();
			SpectraJSONWriter spectraWriter = new SpectraJSONWriter();
			System.out.println(spectraWriter.spectraJSONString(spectra1.get(0), ip.getPattern(), ftree));
		}
		else
			System.out.println("isotope pattern is null");
		// MS2
		if (spectra2.size() > 0){
			SpectraJSONWriter spectraWriter = new SpectraJSONWriter();
			for (int i = 0; i < spectra2.size(); ++i){
				System.out.println("=== SpectraWriter: MS2 Spectrum " + spectra2.get(i).getCollisionEnergy()
								   + ", " + spectra2.get(i).getIonization() + ", " + spectra2.get(i).getScanNumber());
				String jsonstring = spectraWriter.spectrumJSONString(spectra2.get(i), ftree);
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
}
