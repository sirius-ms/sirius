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


package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;


import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.MS1MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.ms.gui.dialogs.FilePresentDialog;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.ms_viewer.InSilicoSelectionBox;
import de.unijena.bioinf.ms.gui.ms_viewer.InsilicoFragmenter;
import de.unijena.bioinf.ms.gui.ms_viewer.SpectraViewerConnector;
import de.unijena.bioinf.ms.gui.ms_viewer.WebViewSpectraViewer;
import de.unijena.bioinf.ms.gui.ms_viewer.data.SiriusIsotopePattern;
import de.unijena.bioinf.ms.gui.ms_viewer.data.SpectraJSONWriter;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import de.unijena.bioinf.ms.gui.webView.WebViewIO;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

public class SpectraVisualizationPanel
	extends JPanel implements ActionListener, ItemListener, PanelDescription,
				   ActiveElementChangedListener<FormulaResultBean, InstanceBean>{
	@Override
	public String getDescription() {
		return "Spectra visualisation. Peaks that are explained by the Fragmentation tree of the selected molecular formula are highlighted in red";
	}

	public static final String MS1_DISPLAY = "MS1", MS1_MIRROR_DISPLAY = "MS1 mirror-plot", MS2_DISPLAY = "MS2",
			MS2_MERGED_DISPLAY = "merged";

    public enum FileFormat {
        svg, pdf, json, none
    }

	InstanceBean experiment = null;
	FormulaResultBean sre = null;
	CompoundCandidate annotation = null;
    String jsonSpectra = null;

	JComboBox<String> modesBox;
	JComboBox<String> ceBox;
	final Optional<InSilicoSelectionBox> optAnoBox;
	String preferredMode;

    JButton saveButton;

	private InsilicoFragmenter fragmenter;

    public WebViewSpectraViewer browser;

	public SpectraVisualizationPanel(boolean annotationBox) {
		this(MS1_DISPLAY, annotationBox);
	}

	public SpectraVisualizationPanel(String preferredMode, boolean annotationBox) {
		this.setLayout(new BorderLayout());
		this.fragmenter = new InsilicoFragmenter();
		this.preferredMode = preferredMode;

		JToolBar northPanel = new JToolBar();
		northPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
		northPanel.setPreferredSize(new Dimension(northPanel.getPreferredSize().width,32));
		northPanel.setFloatable(false);

		JLabel l = new JLabel("Mode");
		l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
		modesBox = new JComboBox<>();
		modesBox.addItemListener(this);
		ceBox = new JComboBox<>();
		northPanel.add(l);
		northPanel.add(modesBox);
		northPanel.add(ceBox);

		optAnoBox = annotationBox ? Optional.of(new InSilicoSelectionBox(new Dimension(200, 100), 5)) : Optional.empty();

		optAnoBox.ifPresent(anoBox -> {
			northPanel.add(anoBox);
			anoBox.setAction(new InsilicoFrament());
		});

        northPanel.addSeparator(new Dimension(10, 10));
        saveButton = Buttons.getExportButton24("Export spectra");
        saveButton.addActionListener(this);
        saveButton.setToolTipText("Export the current view to various formats");
        northPanel.add(saveButton);

		northPanel.addSeparator(new Dimension(10, 10));
		this.add(northPanel, BorderLayout.NORTH);


		/////////////
		// Browser //
		/////////////
		this.browser = new WebViewSpectraViewer();
		this.add(this.browser, BorderLayout.CENTER);
		this.setVisible(true);
	}

    public SpectraViewerConnector getConnector(){
        return browser.getConnector();
    }

	@Override
	public void actionPerformed(ActionEvent e) {
        if (e.getSource() == saveButton) {
            saveSpectra(); //todo which thread do we need here? Swing EDT but with loader for the IO conversion!
        }
	}

	private void drawSpectra(InstanceBean experiment, FormulaResultBean sre, String mode, int ce_index) {
		if (mode == null)
			return;
        jsonSpectra = null;
		SpectraJSONWriter spectraWriter = new SpectraJSONWriter();

		if (mode.contains(MS1_DISPLAY)) {
			List<SimpleSpectrum> spectra1 = experiment.getMs1Spectra();
			SimpleSpectrum spectrum = experiment.getMergedMs1Spectrum() == null ? spectra1.get(0)
					: experiment.getMergedMs1Spectrum();

			SiriusIsotopePattern siriusIsotopePattern = Optional.ofNullable(sre).flatMap(FormulaResultBean::getFragTree)
					.map(ftree -> new SiriusIsotopePattern(ftree, experiment.getExperiment(), spectrum)).orElse(null);


			if (mode.equals(MS1_DISPLAY)) {
				SimpleSpectrum isoPattern = siriusIsotopePattern != null ? siriusIsotopePattern.getIsotopePattern()
						: Spectrums.extractIsotopePattern(spectrum, experiment.getExperiment());
				jsonSpectra = spectraWriter.ms1JSON(spectrum, isoPattern,
						experiment.getExperiment().getAnnotationOrDefault(MS1MassDeviation.class).massDifferenceDeviation);
			} else if (mode.equals(MS1_MIRROR_DISPLAY)) {
				if (siriusIsotopePattern != null) {
					jsonSpectra = spectraWriter.ms1MirrorJSON(siriusIsotopePattern,
							experiment.getExperiment().getAnnotationOrDefault(MS1MassDeviation.class).massDifferenceDeviation);
				} else {
					LoggerFactory.getLogger(getClass()).warn(MS1_MIRROR_DISPLAY + "was selected but no simulated pattern was available. Can not show mirror plot!");
				}

			} else {
				return;
			}
		} else if (mode.equals(MS2_DISPLAY)) {
			if (ce_index == -1){
				jsonSpectra = spectraWriter.ms2JSON(experiment.getExperiment(), Optional.ofNullable(sre).flatMap(FormulaResultBean::getFragTree).orElse(null));
			} else {
				MutableMs2Spectrum spectrum = experiment.getMs2Spectra().get(ce_index);
				FTree ftree = Optional.ofNullable(sre).flatMap(FormulaResultBean::getFragTree).orElse(null);
				if (ftree != null)
					jsonSpectra = spectraWriter.ms2JSON(spectrum, ftree);
				else
					jsonSpectra = spectraWriter.ms2JSON(spectrum);
			}
		} else {
			System.err.println("Cannot draw spectra: Mode " + mode + " not (yet) supported!");
			return;
		}
		if (jsonSpectra != null) {
			String json = null, svg = null;
			if (mode.equals(MS2_DISPLAY) && insilicoResult != null) {
				// only set these when in MS2 mode
				json = insilicoResult.getJson();
				svg = insilicoResult.getSvg();
			}
			browser.loadData(jsonSpectra, json, svg);
		}
	}

	@Override
	public void resultsChanged(InstanceBean experiment, FormulaResultBean sre, List<FormulaResultBean> resultElements, ListSelectionModel selections) {
		resultsChanged(experiment, sre, null);
	}

	private JJob<Boolean> backgroundLoader = null;
	private final Lock backgroundLoaderLock = new ReentrantLock();

	public void resultsChanged(InstanceBean experiment, FormulaResultBean sre, @Nullable CompoundCandidate spectrumAno) {
		try {
			backgroundLoaderLock.lock();
			final JJob<Boolean> old = backgroundLoader;
			backgroundLoader = Jobs.runInBackground(new BasicMasterJJob<>(JJob.JobType.TINY_BACKGROUND) {
				@Override
				protected Boolean compute() throws Exception {
					//cancel running job if not finished to not waist resources for fetching data that is not longer needed.
					if (old != null && !old.isFinished()) {
						old.cancel(true);
						old.getResult(); //await cancellation so that nothing strange can happen.
					}
					checkForInterruption();

					if ((SpectraVisualizationPanel.this.experiment != experiment)  || (SpectraVisualizationPanel.this.sre  != sre) || (SpectraVisualizationPanel.this.annotation != spectrumAno)) {
						Jobs.runEDTAndWait(() -> {
							// update modeBox elements, don't listen to these events
							modesBox.removeItemListener(SpectraVisualizationPanel.this);
							try {
								modesBox.removeAllItems();
								if (experiment != null) {
									if (experiment.getMs1Spectra().size() > 0 || experiment.getMergedMs1Spectrum() != null)
										modesBox.addItem(MS1_DISPLAY);
									if (sre != null) {
										if (experiment.getMs1Spectra().size() > 0 || experiment.getMergedMs1Spectrum() != null)
											modesBox.addItem(MS1_MIRROR_DISPLAY);
									}
									if (experiment.getMs2Spectra().size() > 0)
										modesBox.addItem(MS2_DISPLAY);
									updateCEBox(experiment);
								}
							} finally {
								modesBox.addItemListener(SpectraVisualizationPanel.this);
							}
						});
					}

					checkForInterruption();

					if (sre != SpectraVisualizationPanel.this.sre || spectrumAno != null) {
						if (sre != null && spectrumAno != null) {
							InsilicoFragmenter.Result r = submitSubJob(fragmenter.fragmentJob(sre, spectrumAno)).awaitResult();
							checkForInterruption();
							setInsilicoResult(r);
						} else {
							clearInsilicoResult();
						}
					}


					if (experiment != null) {
						Jobs.runEDTAndWait(() -> {
							boolean preferredPossible = false; // no `contains` for combobox
							for (int i = 0; i < modesBox.getItemCount(); i++)
								preferredPossible |= preferredMode.equals(modesBox.getItemAt(i));
							// change to preferred mode if possible, else (potentially automatic) selection
							if (preferredPossible) {
								modesBox.removeItemListener(SpectraVisualizationPanel.this);
								modesBox.setSelectedItem(preferredMode);
								ceBox.setVisible(modesBox.getSelectedItem() != null && modesBox.getSelectedItem().equals(MS2_DISPLAY));
								modesBox.addItemListener(SpectraVisualizationPanel.this);
							}
							updateCEBox(experiment);
							drawSpectra(experiment, sre, (String) modesBox.getSelectedItem(), getCEIndex());
                            // highlight last selected peak, even when experiments were changed
                            float peak_selection = getConnector().getCurrentSelection();
                            if (peak_selection > -1)
                                browser.executeJS("SpectrumPlot.setSelection(main.spectrum, " + peak_selection + ")");

							optAnoBox.ifPresent(anoBox -> getCurrentMode().ifPresent(mode -> {
								if (mode.msLevel > 1) {
									anoBox.activate();
								} else {
									anoBox.deactivate();
								}
							}));
						});
					} else {
						browser.clear();
						Jobs.runEDTAndWait(() -> optAnoBox.ifPresent(InSilicoSelectionBox::deactivate));
					}
					// store data to switch between modes without having to switch to other results
					SpectraVisualizationPanel.this.experiment = experiment;
					SpectraVisualizationPanel.this.sre = sre;
					SpectraVisualizationPanel.this.annotation = spectrumAno;

					checkForInterruption();
					if (spectrumAno == null) //todo hacky
						Jobs.runEDTAndWait(() -> optAnoBox.ifPresent(anoBox -> anoBox.resultsChanged(sre)));
					return true;
				}
			});
		} finally {
			backgroundLoaderLock.unlock();
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
		final Object sel = modesBox.getSelectedItem();
		ceBox.setVisible(sel != null && sel.equals(MS2_DISPLAY));

		optAnoBox.ifPresent(anoBox -> {
			if (sel != null && (sel.equals(MS2_DISPLAY) || sel.equals(MS2_MERGED_DISPLAY))) {
				anoBox.activate();
			} else {
				anoBox.deactivate();
			}
		});

		preferredMode = (String) sel;
		if (sel != null && !(experiment == null && sre == null)) {
			drawSpectra(experiment, sre, (String) sel, getCEIndex());
		}
	}

	private int getCEIndex() {
		return ceBox.getSelectedItem() == null || ceBox.getSelectedItem().equals(MS2_MERGED_DISPLAY) ? -1 : ceBox.getSelectedIndex();
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
	}

	public Optional<SpectrumMode> getCurrentMode() {
		// we should use a variable for this!
		// then, connect everything with listeners
		final Object s = modesBox.getSelectedItem();
		if (s == null)
			return Optional.empty();
		if (s.equals(MS1_DISPLAY)) return Optional.of(SpectrumMode.MS1);
		if (s.equals(MS1_MIRROR_DISPLAY)) return Optional.of(SpectrumMode.MS1_MIRROR);
		if (s.equals(MS2_MERGED_DISPLAY)) return Optional.of(SpectrumMode.MS2_MERGED);
		if (s.equals(MS2_DISPLAY)) return Optional.of(SpectrumMode.MS2);
		return Optional.of(SpectrumMode.MS1); // ?
	}

	public class InsilicoFrament extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			final Object selectedItem = ((InSilicoSelectionBox) e.getSource()).getSelectedItem();
			if (selectedItem != null && selectedItem instanceof InSilicoSelectionBox.Item) {
				InSilicoSelectionBox.Item item = (InSilicoSelectionBox.Item) selectedItem;
				if (item.getCandidate() != null) {
					resultsChanged(experiment, sre, item.getCandidate());
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
		getCurrentMode().ifPresent(m -> {
			if (m.msLevel >= 2) {
				drawSpectra(experiment, sre, m.label, getCEIndex());
			}
		});

	}
	public void clearInsilicoResult() {
		this.insilicoResult = null;
	}

    public void saveSpectra() {
        // adapted from
        // de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs.TreeVisualizationPanel
        abstract class SpectraFilter extends FileFilter {

            private String fileSuffix, description;

            public SpectraFilter(String fileSuffix, String description) {
                this.fileSuffix = fileSuffix;
                this.description = description;
            }

            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName();
                return name.endsWith(fileSuffix);
            }

            @Override
            public String getDescription() {
                return description;
            }

        }

        class SpectraSVGFilter extends SpectraFilter {

            public SpectraSVGFilter() {
                super(".svg", "SVG");
            }

        }

        class SpectraPDFFilter extends SpectraFilter {

            public SpectraPDFFilter() {
                super(".pdf", "PDF");
            }

        }

        class SpectraJSONFilter extends SpectraFilter {

            public SpectraJSONFilter() {
                super(".json", "JSON");
            }
        }

        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(PropertyManager.getFile(SiriusProperties.DEFAULT_TREE_EXPORT_PATH));
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setAcceptAllFileFilterUsed(false);

        FileFilter svgFilter = new SpectraSVGFilter();
        FileFilter pdfFilter = new SpectraPDFFilter();
        FileFilter jsonFilter = new SpectraJSONFilter();


        jfc.addChoosableFileFilter(svgFilter);
        jfc.addChoosableFileFilter(pdfFilter);
        jfc.addChoosableFileFilter(jsonFilter);

        jfc.setFileFilter(svgFilter);

        File selectedFile = null;
        FileFormat ff = FileFormat.none;

        while (selectedFile == null) {
            int returnval = jfc.showSaveDialog(this);
            if (returnval == JFileChooser.APPROVE_OPTION) {
                File selFile = jfc.getSelectedFile();

                {
                    final String path = selFile.getParentFile().getAbsolutePath();
                    Jobs.runInBackground(() ->
                            SiriusProperties.SIRIUS_PROPERTIES_FILE().
                                    setAndStoreProperty(SiriusProperties.DEFAULT_TREE_EXPORT_PATH, path)
                        );
                }


                if (jfc.getFileFilter() == svgFilter) {
                    ff = FileFormat.svg;
                    if (!selFile.getAbsolutePath().endsWith(".svg")) {
                        selFile = new File(selFile.getAbsolutePath() + ".svg");
                    }
                } else if (jfc.getFileFilter() == pdfFilter) {
                    ff = FileFormat.pdf;
                    if (!selFile.getAbsolutePath().endsWith(".pdf")) {
                        selFile = new File(selFile.getAbsolutePath() + ".pdf");
                    }
                } else if (jfc.getFileFilter() == jsonFilter) {
                    ff = FileFormat.json;
                    if (!selFile.getAbsolutePath().endsWith(".json")) {
                        selFile = new File(selFile.getAbsolutePath() + ".json");
                    }
                } else {
                    throw new RuntimeException(jfc.getFileFilter().getClass().getName());
                }

                if (selFile.exists()) {
                    FilePresentDialog fpd = new FilePresentDialog(MF, selFile.getName());
                    ReturnValue rv = fpd.getReturnValue();
                    if (rv == ReturnValue.Success) {
                        selectedFile = selFile;
                    }
                } else {
                    selectedFile = selFile;
                }
            } else {
                break;
            }
        }

        if (ff != FileFormat.none) {
            final String name = ff.name();
            Jobs.runInBackground(() ->
                    SiriusProperties.SIRIUS_PROPERTIES_FILE().
                            setAndStoreProperty(SiriusProperties.DEFAULT_TREE_FILE_FORMAT, name)
                );
        }


        if (selectedFile != null && ff != FileFormat.none) {
            final FileFormat fff = ff;
            final File fSelectedFile = selectedFile;
            Jobs.runInBackgroundAndLoad(MF, "Exporting Spectra...", () -> {
                try {
                    // for SVG/PDF ask whether to export structure
                    boolean exportStructure = false;
                    if ((fff == FileFormat.svg || fff == FileFormat.pdf) && insilicoResult != null){
                        QuestionDialog exportStructureDialog = new QuestionDialog(MF,
                                "Do you want to export the corresponding compound structure as well?");
                        ReturnValue rv = exportStructureDialog.getReturnValue();
                        exportStructure = rv == ReturnValue.Success;
                    }

                    if (fff == FileFormat.svg) {
                        final StringBuilder svgSpectra = new StringBuilder();
                        Jobs.runJFXAndWait(() -> svgSpectra.append((String) browser.getJSObject("svgExport.getSvgString(document.getElementById('spectrumView'))")));
                        WebViewIO.writeSVG(fSelectedFile, svgSpectra.toString());
                        if (exportStructure){
                            // second file for structure SVG
                            final StringBuilder svgStructure = new StringBuilder();
                            Jobs.runJFXAndWait(() -> svgStructure.append((String) browser.getJSObject("svgExport.getSvgString(document.getElementById('structureView').getElementsByTagName('svg')[0])")));
                            Path structurePath = Path.of(fSelectedFile.getParent(), fSelectedFile.getName().replaceFirst("(.[Ss][Vv][Gg])?$", "_structure.svg"));
                            WebViewIO.writeSVG(structurePath.toFile(), svgStructure.toString());
                        }
                    } else if (fff == FileFormat.pdf) {
                        final StringBuilder svg = new StringBuilder();
                        Jobs.runJFXAndWait(() -> svg.append((String) browser.getJSObject("svgExport.getSvgString(document.getElementById('spectrumView'))")));
                        // remove selection etc. rectangles as <rect>s without width attribute break Rasterizer
                        WebViewIO.writePDF(fSelectedFile, svg.toString().replaceAll("<rect [^>]*class=\"(selection|handle)[^>]+>", ""));
                        if (exportStructure){
                            // second file for structure PDF
                            final StringBuilder svgStructure = new StringBuilder();
                            Jobs.runJFXAndWait(() -> svgStructure.append((String) browser.getJSObject("svgExport.getSvgString(document.getElementById('structureView').getElementsByTagName('svg')[0])")));
                            Path structurePath = Path.of(fSelectedFile.getParent(), fSelectedFile.getName().replaceFirst("(.[Pp][Dd][Ff])?$", "_structure.pdf"));
                            WebViewIO.writePDF(structurePath.toFile(), svgStructure.toString());
                        }
                    } else if (fff == FileFormat.json) {
                        try (BufferedWriter bw = Files.newBufferedWriter(fSelectedFile.toPath(), Charset.defaultCharset())) {
                            bw.write(jsonSpectra);
                            bw.close();
                        }
                    }
                } catch (Exception e2) {
                    new ErrorReportDialog(MF, e2.getMessage());
                    LoggerFactory.getLogger(this.getClass()).error(e2.getMessage(), e2);
                }
            });
        }
    }
}
