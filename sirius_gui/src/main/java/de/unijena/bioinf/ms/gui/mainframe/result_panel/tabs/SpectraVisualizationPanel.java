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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MS1MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ChemistryBase.ms.utils.WrapperSpectrum;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.fragmenter.MolecularGraph;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.dialogs.FilePresentDialog;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.ms_viewer.SpectraViewContainer;
import de.unijena.bioinf.ms.gui.ms_viewer.SpectraViewerConnector;
import de.unijena.bioinf.ms.gui.ms_viewer.WebViewSpectraViewer;
import de.unijena.bioinf.ms.gui.spectral_matching.SpectralMatchBean;
import de.unijena.bioinf.ms.gui.spectral_matching.SpectralMatchList;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import de.unijena.bioinf.ms.gui.webView.WebViewIO;
import de.unijena.bioinf.ms.nightsky.sdk.model.*;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.depict.DepictionGenerator;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.renderer.color.UniColor;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.slf4j.LoggerFactory;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class SpectraVisualizationPanel extends JPanel implements ActionListener, ItemListener, PanelDescription {

    protected final CardLayout centerCard = new CardLayout();
    protected final JPanel centerCardPanel = new JPanel(centerCard);
    protected final JLabel errorLabel;

    @Override
    public String getDescription() {
        return "<html>"
                + "<b>Spectra Viewer</b>"
                + "<br>"
                + "Shows MS1, MS1 vs Simulated Isotope Pattern and MS/MS spectra."
                + "<br>"
                + "MS1: Isotope pattern highlighted in blue."
                + "<br>"
                + "MS/MS: Peaks that are explained by the Fragmentation tree of the selected molecular formula are highlighted in green."
                + "</html>";
    }

    public static final String MS1_DISPLAY = "MS1", MS1_MIRROR_DISPLAY = "MS1 mirror-plot", MS2_DISPLAY = "MS2", MS2_MIRROR_DISPLAY = "MS2 mirror-plot",
            MS2_MERGED_DISPLAY = "merged";

    public enum FileFormat {
        svg, pdf, json, none
    }

    final Set<String> possibleModes;
    MsData msData;
    IsotopePatternAnnotation isotopePatternAnnotation;
    AnnotatedMsMsData annotatedMsMsData;
    SpectraViewContainer jsonSpectra;
    private String smiles;

    JComboBox<String> modesBox;
    JComboBox<String> ceBox;
    String preferredMode;
    JButton saveButton;
    JFrame popupOwner;

    public WebViewSpectraViewer browser;
    final JToolBar toolBar;

    private final boolean ms2MirrorEnabled;
    private SpectralSimilarity[] similarities;
    private IntList queryIndices;
    private SpectralMatchBean selectedMatchBean;

    public SpectraVisualizationPanel() {
        this(MS1_DISPLAY);
    }

    public SpectraVisualizationPanel(String preferredMode) {
        this(preferredMode, false);
    }

    public SpectraVisualizationPanel(String preferredMode, boolean ms2MirrorEnabled) {
        this(preferredMode, ms2MirrorEnabled
                ? Set.of(MS1_DISPLAY, MS1_MIRROR_DISPLAY, MS2_DISPLAY, MS2_MIRROR_DISPLAY)
                : Set.of(MS1_DISPLAY, MS1_MIRROR_DISPLAY, MS2_DISPLAY));
    }

    public SpectraVisualizationPanel(String preferredMode, String... possibleModes) {
        this(preferredMode, Set.of(possibleModes));
    }

    public SpectraVisualizationPanel(String preferredMode, Set<String> possibleModes) {
        this.setLayout(new BorderLayout());
        this.possibleModes = possibleModes;
        this.preferredMode = preferredMode;
        this.ms2MirrorEnabled = possibleModes.contains(MS2_MIRROR_DISPLAY);
        this.popupOwner = (JFrame) SwingUtilities.getWindowAncestor(this);

        toolBar = new JToolBar();
        toolBar.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        toolBar.setPreferredSize(new Dimension(toolBar.getPreferredSize().width, 32));
        toolBar.setFloatable(false);

        JLabel l = new JLabel("Mode");
        l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
        modesBox = new JComboBox<>();
        modesBox.addItemListener(this);
        ceBox = new JComboBox<>();
        toolBar.add(l);
        toolBar.add(modesBox);
        toolBar.add(ceBox);

        toolBar.addSeparator(new Dimension(10, 10));
        saveButton = Buttons.getExportButton24("Export spectra");
        saveButton.addActionListener(this);
        saveButton.setToolTipText("Export the current view to various formats");
        toolBar.add(saveButton);

        toolBar.addSeparator(new Dimension(10, 10));
        this.add(toolBar, BorderLayout.NORTH);
        setToolbarEnabled(false);

        /////////////
        // Browser //
        /////////////
        this.browser = new WebViewSpectraViewer();

        Pair<JPanel, JLabel> error = GuiUtils.newEmptyResultsPanelWithLabel(null);
        this.errorLabel = error.right();

        this.centerCardPanel.add("browser", this.browser);
        this.centerCardPanel.add("error", error.left());
        showBrowser();

        this.add(centerCardPanel, BorderLayout.CENTER);
        this.setVisible(true);
    }

    protected void setToolbarEnabled(boolean enabled) {
        for (Component comp : toolBar.getComponents())
            comp.setEnabled(enabled);
    }

    public SpectraViewerConnector getConnector() {
        return browser.getConnector();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == saveButton) {
            saveSpectra();
        }
    }

    private void showBrowser() {
        this.centerCard.show(centerCardPanel, "browser");
    }

    private void showError(@NotNull String message) {
        this.errorLabel.setText(message);
        this.centerCard.show(centerCardPanel, "error");
    }

    private void drawSpectra() {
        try {
            showBrowser();
            browser.clear();
            String mode = (String) modesBox.getSelectedItem();
            if (mode == null)
                return;
            if (msData == null && isotopePatternAnnotation == null && annotatedMsMsData == null)
                return;

            int ce_index = getCEIndex();
            jsonSpectra = null;
            smiles = null;


            if (mode.contains(MS1_DISPLAY)) {
                BasicSpectrum spectrum = msData.getMergedMs1();
                if (spectrum == null && !msData.getMs1Spectra().isEmpty())
                    spectrum = msData.getMs1Spectra().iterator().next();

                if (spectrum != null) {
                    if (mode.equals(MS1_DISPLAY)) {
                        //match already extracted pattern to highlight peaks, remove patter from result
                        SpectraViewContainer<BasicSpectrum> ob = matchSpectra(spectrum, isotopePatternAnnotation != null ? isotopePatternAnnotation.getIsotopePattern() : null);
                        if (ob.getSpectra().size() > 1 && ob.getPeakMatches().size() > 1) {
                            ob.getSpectra().remove(1);
                            ob.getPeakMatches().remove(1);
                        }
                        jsonSpectra = ob;
                    } else if (mode.equals(MS1_MIRROR_DISPLAY)) {
                        if (isotopePatternAnnotation != null && isotopePatternAnnotation.getSimulatedPattern() != null) {
                            jsonSpectra = matchSpectra(spectrum, isotopePatternAnnotation.getSimulatedPattern());
                        } else {
                            showError("No isotope pattern available!");
                            LoggerFactory.getLogger(getClass()).warn(MS1_MIRROR_DISPLAY + "was selected but no simulated pattern was available. Cannot show mirror plot!");
                        }
                    } else {
                        return;
                    }
                }
            } else if (mode.equals(MS2_DISPLAY)) {
                if (ce_index == -1) { //merged ms/ms
                    if (annotatedMsMsData != null && annotatedMsMsData.getMergedMs2() != null) {
                        jsonSpectra = SpectraViewContainer.of(annotatedMsMsData.getMergedMs2());
                        smiles = annotatedMsMsData.getMergedMs2().getSpectrumAnnotation().getStructureAnnotationSmiles();
                    } else
                        jsonSpectra = SpectraViewContainer.of(msData.getMergedMs2());
                } else {
                    if (annotatedMsMsData != null && !annotatedMsMsData.getMs2Spectra().isEmpty()) {
                        AnnotatedSpectrum spec = annotatedMsMsData.getMs2Spectra().get(ce_index);
                        jsonSpectra = SpectraViewContainer.of(spec);
                        smiles = spec.getSpectrumAnnotation().getStructureAnnotationSmiles();
                    } else
                        jsonSpectra = SpectraViewContainer.of(msData.getMs2Spectra().get(ce_index));
                }
            } else if (mode.equals(MS2_MIRROR_DISPLAY)) {
                if (selectedMatchBean.getReference().isEmpty()) {
                    showError("Reference spectrum not found!");
                    LoggerFactory.getLogger(getClass()).warn("Cannot draw spectra: Spectrum " + selectedMatchBean.getMatch().getDbId() + " not found!");
                    return;
                }
                jsonSpectra = SpectraViewContainer.of(List.of(msData.getMs2Spectra().get(queryIndices.getInt(ce_index)), selectedMatchBean.getReference().get()));
                smiles = selectedMatchBean.getMatch().getSmiles();
            } else {
                showError("Mode not supported!");
                LoggerFactory.getLogger(getClass()).warn("Cannot draw spectra: Mode " + mode + " not (yet) supported!");
                return;
            }

            if (jsonSpectra != null) {
                String svg = null;
                String diff = null;
                int showMzTopK = 5;
                if (mode.startsWith(MS2_DISPLAY) && smiles != null) {
                    svg = makeSVG(smiles);
                }
                if (mode.equals(MS1_MIRROR_DISPLAY))
                    diff = "difference";
                if (mode.equals(MS2_MIRROR_DISPLAY))
                    diff = "normal";
                browser.loadData(jsonSpectra, svg, diff, showMzTopK);
            }
        } catch (JsonProcessingException e) {
            showError("Error when creating data json!");
            LoggerFactory.getLogger(getClass()).error("Error when creating data Json!", e);
        }
    }

    private SpectraViewContainer<BasicSpectrum> matchSpectra(@NotNull BasicSpectrum spectrum, @Nullable BasicSpectrum pattern) {
        if (pattern == null)
            return SpectraViewContainer.of(spectrum);
        return matchSpectra(spectrum, pattern,
                PropertyManager.DEFAULTS.createInstanceWithDefaults(MS1MassDeviation.class).massDifferenceDeviation);
    }

    private SpectraViewContainer<BasicSpectrum> matchSpectra(@NotNull BasicSpectrum spectrum,
                                                             @NotNull BasicSpectrum pattern,
                                                             @NotNull Deviation massDiffDev
    ) {
        SpectraViewContainer.PeakMatch[] peakMatchesSpectrum = new SpectraViewContainer.PeakMatch[spectrum.getPeaks().size()];
        SpectraViewContainer.PeakMatch[] peakMatchesPattern = new SpectraViewContainer.PeakMatch[pattern.getPeaks().size()];

        WrapperSpectrum<de.unijena.bioinf.ms.nightsky.sdk.model.SimplePeak> pat = WrapperSpectrum.
                of(pattern.getPeaks(), p -> p.getMz(), p -> p.getIntensity());

        int i = 0;
        for (SimplePeak p : spectrum.getPeaks()) {
            int j = Spectrums.mostIntensivePeakWithin(pat, p.getMz(), massDiffDev);
            if (j >= 0) {
                peakMatchesSpectrum[i] = new SpectraViewContainer.PeakMatch(j);
                peakMatchesPattern[j] = new SpectraViewContainer.PeakMatch(i);
            }
            i++;
        }

        return new SpectraViewContainer<>(
                Stream.of(spectrum, pattern).collect(Collectors.toCollection(ArrayList::new)),
                Stream.of(Arrays.asList(peakMatchesSpectrum), Arrays.asList(peakMatchesPattern))
                        .collect(Collectors.toCollection(ArrayList::new)));
    }

    public static String makeSVG(String smiles) {
        try {
            final MolecularGraph graph = new MolecularGraph(
                    new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(smiles)
            );
            return new DepictionGenerator()
                    .withAromaticDisplay()
                    .withAtomColors(new UniColor(Colors.FOREGROUND))
                    .withBackgroundColor(Colors.BACKGROUND)
                    .depict(graph.getMolecule()).toSvgStr();
        } catch (CDKException e) {
            LoggerFactory.getLogger(SpectraVisualizationPanel.class).error("Error when creating Structure SVG from smiles!", e);
            return null;
        }
    }

    private volatile JJob<Boolean> backgroundLoader = null;
    private final Lock backgroundLoaderLock = new ReentrantLock();


    public void clear() {
        resultsChanged(null, null, null, null, null, true);
    }

    public void resultsChanged(InstanceBean instance, @Nullable String formulaCandidateId, @Nullable String smiles) {
        resultsChanged(instance, formulaCandidateId, smiles, null, null, false);
    }

    public void resultsChanged(InstanceBean instance, @Nullable SpectralMatchList matchList, @Nullable SpectralMatchBean selectedMatchBean) {
        resultsChanged(instance, null, null, matchList, selectedMatchBean, matchList == null || selectedMatchBean == null);
    }

    private void resultsChanged(InstanceBean instance, @Nullable String formulaCandidateId, @Nullable String smiles, @Nullable SpectralMatchList matchList, @Nullable SpectralMatchBean matchBean, boolean clear) {
        try {
            backgroundLoaderLock.lock();
            final JJob<Boolean> old = backgroundLoader;
            backgroundLoader = Jobs.runInBackground(new BasicMasterJJob<>(JJob.JobType.TINY_BACKGROUND) {
                @Override
                protected Boolean compute() throws Exception {
                    //cancel running job if not finished to not waist resources for fetching data that is not longer needed.
                    if (old != null) {
                        old.cancel(true);
                        old.getResult(); //await cancellation so that nothing strange can happen.
                    }
                    showBrowser();
                    clearData();

                    if (clear) {
                        clearData();
                        Jobs.runEDTAndWait(() -> setToolbarEnabled(false));
                        browser.clear();
                        return true;
                    }

                    checkForInterruption();
                    //todo check if data is unchanged and prevent re-rendering
                    if (instance != null) {
                        final MsData msData = instance.getMsData();
                        if (msData != null) {
                            final IsotopePatternAnnotation isotopePatternAnnotation;
                            final AnnotatedMsMsData annotatedMsMsData;
                            checkForInterruption();
                            if (formulaCandidateId != null) {
                                isotopePatternAnnotation = instance.withIds((pid, fid) -> instance.getClient().features()
                                        .getIsotopePatternAnnotationWithResponseSpec(pid, fid, formulaCandidateId)
                                        .bodyToMono(IsotopePatternAnnotation.class).onErrorComplete().block());
                                checkForInterruption();
//

                                String ftreeJson = instance.withIds((pid, fid) -> instance.getClient().features()
                                        .getSiriusFragTreeWithResponseSpec(pid, fid, formulaCandidateId)
                                        .bodyToMono(String.class).onErrorComplete().block());

                                checkForInterruption();
                                annotatedMsMsData = ftreeJson == null ? null :
                                        submitSubJob(new SpectrumAnnotationJJob(new FTJsonReader().treeFromJsonString(ftreeJson, null), msData, smiles))
                                                .awaitResult();
                            } else {
                                isotopePatternAnnotation = null;
                                annotatedMsMsData = null;
                            }
                            if (matchList != null && matchBean != null) {
                                selectedMatchBean = matchBean;
                                similarities = new SpectralSimilarity[msData.getMs2Spectra().size()];
                                queryIndices = new IntArrayList();
                                matchList.getMatchBeanGroup(matchBean.getMatch().getUuid()).forEach(match -> {
                                    similarities[match.getMatch().getQuerySpectrumIndex()] = new SpectralSimilarity(
                                            match.getMatch().getSimilarity(),
                                            match.getMatch().getSharedPeaks() != null ? match.getMatch().getSharedPeaks() : 0
                                    );
                                    queryIndices.add((int) match.getMatch().getQuerySpectrumIndex());
                                });
                                queryIndices.sort(IntComparators.NATURAL_COMPARATOR);
                            }

                            checkForInterruption();
                            {
                                final List<String> items = new ArrayList<>(5);

                            Jobs.runEDTAndWait(() -> setToolbarEnabled(true));
                            if (!msData.getMs1Spectra().isEmpty() || msData.getMergedMs1() != null)
                                items.add(MS1_DISPLAY);
                            if (isotopePatternAnnotation != null) {
                                if (isotopePatternAnnotation.getSimulatedPattern() != null)
                                    items.add(MS1_MIRROR_DISPLAY);
                            }
                            if (!msData.getMs2Spectra().isEmpty())
                                items.add(MS2_DISPLAY);
                            if (ms2MirrorEnabled && !msData.getMs2Spectra().isEmpty())
                                items.add(MS2_MIRROR_DISPLAY);

                                checkForInterruption();

                                Jobs.runEDTAndWait(() -> {
                                    // update modeBox elements, don't listen to these events
                                    modesBox.removeItemListener(SpectraVisualizationPanel.this);
                                    try {
                                        modesBox.removeAllItems();
                                        if (!items.isEmpty()) {
                                            items.stream().filter(possibleModes::contains).forEach(modesBox::addItem);
                                            updateCEBox(msData);
                                        }
                                    } finally {
                                        modesBox.addItemListener(SpectraVisualizationPanel.this);
                                    }
                                });
                            }

                            SpectraVisualizationPanel.this.msData = msData;
                            SpectraVisualizationPanel.this.isotopePatternAnnotation = isotopePatternAnnotation;
                            SpectraVisualizationPanel.this.annotatedMsMsData = annotatedMsMsData;

                            checkForInterruption();

                            // todo nightsky: why are these two jobs?
                            Jobs.runEDTAndWait(() -> {
                                boolean preferredPossible = false; // no `contains` for combobox
                                for (int i = 0; i < modesBox.getItemCount(); i++)
                                    preferredPossible |= preferredMode.equals(modesBox.getItemAt(i));
                                // change to preferred mode if possible, else (potentially automatic) selection
                                if (preferredPossible) {
                                    modesBox.removeItemListener(SpectraVisualizationPanel.this);
                                    modesBox.setSelectedItem(preferredMode);
                                    ceBox.setVisible(modesBox.getSelectedItem() != null && ((String) modesBox.getSelectedItem()).startsWith(MS2_DISPLAY));
                                    modesBox.addItemListener(SpectraVisualizationPanel.this);
                                }
                                updateCEBox(msData);
                                drawSpectra();
                                // highlight last selected peak, even when experiments were changed
                                float peak_selection = getConnector().getCurrentSelection();
                                if (peak_selection > -1)
                                    browser.executeJS("SpectrumPlot.setSelection(main.spectrum, " + peak_selection + ")");

                            });
                        } else {
                            clearData();
                            Jobs.runEDTAndWait(() -> setToolbarEnabled(false));
                            browser.clear();
                        }
                    }
                    return true;
                }

                @Override
                public void cancel(boolean mayInterruptIfRunning) {
                    super.cancel(mayInterruptIfRunning);
                }
            });
        } finally {
            backgroundLoaderLock.unlock();
        }
    }

    private void clearData() {
        msData = null;
        smiles = null;
        isotopePatternAnnotation = null;
        annotatedMsMsData = null;
        selectedMatchBean = null;
        similarities = null;
    }

    private void updateCEBox(MsData msData) {
        ceBox.removeItemListener(this);
        ceBox.removeAllItems();
        if (ms2MirrorEnabled) {
            SpectralSimilarity maxSimilarity = new SpectralSimilarity(0, 0);
            int maxIndex = 0;
            for (int i = 0; i < msData.getMs2Spectra().size(); ++i) {
                if (similarities != null && similarities[i] != null) {
                    BasicSpectrum spectrum = msData.getMs2Spectra().get(i);
                    String collisionEnergy = spectrum.getCollisionEnergy();
                    ceBox.addItem(collisionEnergy == null ? "mode " + (i + 1) : collisionEnergy +
                            String.format(" (%.1f %% similarity, %d shared peaks)", 100 * similarities[i].similarity, similarities[i].sharedPeaks));
                    if (similarities[i].similarity > maxSimilarity.similarity || (Math.abs(similarities[i].similarity - maxSimilarity.similarity) < 1E-3 && similarities[i].sharedPeaks > maxSimilarity.sharedPeaks)) {
                        maxSimilarity = similarities[i];
                        maxIndex = i;
                    }
                }
            }
            ceBox.setSelectedIndex(maxIndex);
        } else {
            for (int i = 0; i < msData.getMs2Spectra().size(); ++i) {
                BasicSpectrum spectrum = msData.getMs2Spectra().get(i);
                String collisionEnergy = spectrum.getCollisionEnergy();
                ceBox.addItem(collisionEnergy == null ? "mode " + (i + 1) : collisionEnergy);
            }
            ceBox.addItem(MS2_MERGED_DISPLAY);
            ceBox.setSelectedItem(MS2_MERGED_DISPLAY);
        }
        ceBox.addItemListener(this);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        final String sel = (String) modesBox.getSelectedItem();
        ceBox.setVisible(sel != null && sel.startsWith(MS2_DISPLAY));

        preferredMode = sel;
        if (sel != null)
            drawSpectra();
    }

    private int getCEIndex() {
        return ceBox.getSelectedItem() == null || ceBox.getSelectedItem().equals(MS2_MERGED_DISPLAY) ? -1 : ceBox.getSelectedIndex();
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
                    FilePresentDialog fpd = new FilePresentDialog(popupOwner, selFile.getName());
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
            Jobs.runInBackgroundAndLoad(popupOwner, "Exporting Spectra...", () -> {
                try {
                    // for SVG/PDF ask whether to export structure
                    boolean exportStructure = false;
                    if ((fff == FileFormat.svg || fff == FileFormat.pdf) &&
                            modesBox.getSelectedItem() != null &&
                            ((String) modesBox.getSelectedItem()).startsWith(MS2_DISPLAY) &&
                            smiles != null
                    ) {
                        QuestionDialog exportStructureDialog = new QuestionDialog(popupOwner,
                                "Do you want to export the corresponding compound structure as well?");
                        ReturnValue rv = exportStructureDialog.getReturnValue();
                        exportStructure = rv == ReturnValue.Success;
                    }

                    if (fff == FileFormat.svg) {
                        final StringBuilder svgSpectra = new StringBuilder();
                        Jobs.runJFXAndWait(() -> svgSpectra.append((String) browser.getJSObject("svgExport.getSvgString(document.getElementById('spectrumView'))")));
                        WebViewIO.writeSVG(fSelectedFile, svgSpectra.toString());
                        if (exportStructure) {
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
                        if (exportStructure) {
                            // second file for structure PDF
                            final StringBuilder svgStructure = new StringBuilder();
                            Jobs.runJFXAndWait(() -> svgStructure.append((String) browser.getJSObject("svgExport.getSvgString(document.getElementById('structureView').getElementsByTagName('svg')[0])")));
                            Path structurePath = Path.of(fSelectedFile.getParent(), fSelectedFile.getName().replaceFirst("(.[Pp][Dd][Ff])?$", "_structure.pdf"));
                            WebViewIO.writePDF(structurePath.toFile(), svgStructure.toString());
                        }
                    } else if (fff == FileFormat.json) {
                        try (BufferedWriter bw = Files.newBufferedWriter(fSelectedFile.toPath(), Charset.defaultCharset())) {
                            bw.write(new ObjectMapper().writeValueAsString(jsonSpectra));
                        }
                    }
                } catch (Exception e2) {
                    new StacktraceDialog(popupOwner, e2.getMessage(), e2);
                    LoggerFactory.getLogger(this.getClass()).error(e2.getMessage(), e2);
                }
            });
        }
    }
}
