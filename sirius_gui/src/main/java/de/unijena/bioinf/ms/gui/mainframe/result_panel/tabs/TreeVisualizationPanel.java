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

import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.dialogs.FilePresentDialog;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.tree_viewer.*;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import de.unijena.bioinf.ms.gui.webView.WebViewIO;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import javafx.embed.swing.JFXPanel;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//todo post-nightsky: switch to nightsky api data structures

public class TreeVisualizationPanel extends JPanel
        implements ActionListener, ChangeListener, ComponentListener,
        ActiveElementChangedListener<FormulaResultBean, InstanceBean>,
        PanelDescription {
    public enum FileFormat {
        dot, json, jpg, png, gif, svg, pdf, none
    }

    @Override
    public String getDescription() {
        return "<html>"
                +"<b>Fragmentation Tree Viewer</b>"
                +"<br>"
                + "Interactive visualization of the Fragmentation tree for the selected molecular formula."
                + "</html>";
    }

    String ftreeJson;
    public TreeViewerBrowser browser;
    TreeViewerBridge jsBridge;
    TreeViewerConnector jsConnector;
    JToolBar toolBar;
    public JComboBox<String> presetBox; // accessible from TreeViewerSettings
    JSlider scaleSlider;
    JButton saveTreeBtn;
    JButton advancedSettingsBtn;
    JButton resetBtn;
    TreeViewerSettings settings;
    TreeConfig localConfig;

    JFrame popupOwner;

    public TreeVisualizationPanel() {
        this.setLayout(new BorderLayout());
        this.popupOwner = (JFrame) SwingUtilities.getWindowAncestor(this);

        localConfig = new TreeConfig();
        localConfig.setFromString(
            "presets", PropertyManager.getProperty(
                PropertyManager.PROPERTY_BASE + ".tree_viewer.presets"));

        ////////////////
        //// Toolbar ///
        ////////////////
        toolBar = new JToolBar();
        toolBar.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        toolBar.setFloatable(false);
        toolBar.setPreferredSize(new Dimension(toolBar.getPreferredSize().width,32));
        presetBox = new JComboBox<>((String[]) localConfig.get("presets"));
        presetBox.addActionListener(this);
        presetBox.setSelectedItem(
            PropertyManager.getProperty(PropertyManager.PROPERTY_BASE
                                        + ".tree_viewer.preset"));
        JLabel presetLabel = new JLabel("Preset");
        presetLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
        toolBar.add(presetLabel);
        toolBar.add(presetBox);
        toolBar.addSeparator(new Dimension(10, 10));
        saveTreeBtn = Buttons.getExportButton24("Export tree");
        saveTreeBtn.addActionListener(this);
        saveTreeBtn.setEnabled(false);
        saveTreeBtn.setToolTipText("Export current tree view (or zoomed-in region) to various formats");
        toolBar.add(saveTreeBtn);
        toolBar.addSeparator(new Dimension(10, 10));
        scaleSlider = new JSlider(JSlider.HORIZONTAL,
                TreeViewerBridge.TREE_SCALE_MIN,
                TreeViewerBridge.TREE_SCALE_MAX,
                TreeViewerBridge.TREE_SCALE_INIT);
        scaleSlider.addChangeListener(this);
        scaleSlider.setToolTipText("Increase/Decrease the space between nodes");
        JLabel scaleSliderLabel = new JLabel("Scale");
        scaleSliderLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
        scaleSliderLabel.setToolTipText("Increase/Decrease the space between "
                + "nodes");
        toolBar.add(scaleSliderLabel);
        toolBar.add(scaleSlider);
        toolBar.addSeparator(new Dimension(10, 10));
        advancedSettingsBtn = new JButton("Customize");
        advancedSettingsBtn.setEnabled(true);
        advancedSettingsBtn.addActionListener(this);
        advancedSettingsBtn.setToolTipText("Customize various settings for "
                + "the visualization");
        settings = null;
        toolBar.add(advancedSettingsBtn);
        toolBar.addSeparator(new Dimension(10, 10));
        resetBtn = new JButton("Reset");
        resetBtn.addActionListener(this);
        resetBtn.setToolTipText("Revert any changed made to the visualization "
                + "and the tree itself");
        toolBar.add(resetBtn);
        this.add(toolBar, BorderLayout.NORTH);

        /////////////
        // Browser //
        /////////////
        // using JavaFX WebView for now
        this.browser = new WebViewTreeViewer();

        this.jsBridge = new TreeViewerBridge(browser);
        this.jsConnector = new TreeViewerConnector();

        browser.addJS("d3.min.js");
        browser.addJS("d3-colorbar.js");
        browser.addJS("tree_viewer/treeViewer.js");
        browser.addJS("tree_viewer/treeViewerSettings.js");
        browser.addJS("tree_viewer/treeViewerConnector.js");
		if (SiriusProperties.getProperty("de.unijena.bioinf.tree_viewer.special", null, "").equals("xmas"))
			browser.addJS("snow.js");
        this.add((JFXPanel) this.browser, BorderLayout.CENTER);
        this.setVisible(true);
        HashMap<String, Object> bridges = new HashMap<String, Object>() {{
            put("config", localConfig);
            put("connector", jsConnector);
        }};
        browser.load(bridges);
        setToolbarEnabled(false);
        this.addComponentListener(this);
        applyPreset((String) presetBox.getSelectedItem());
    }

    private JJob<Boolean> backgroundLoader = null;
    private final Lock backgroundLoaderLock = new ReentrantLock();

    @Override
    public void resultsChanged(InstanceBean elementsParent,
                               FormulaResultBean selectedElement,
                               List<FormulaResultBean> resultElements,
                               ListSelectionModel selections) {

            try {
                backgroundLoaderLock.lock();
                final JJob<Boolean> old = backgroundLoader;
                backgroundLoader = Jobs.runInBackground(new TinyBackgroundJJob<>() {

                    @Override
                    protected Boolean compute() throws Exception {
                        //cancel running job if not finished to not waist resources for fetching data that is not longer needed.
                        if (old != null && !old.isFinished()) {
                            old.cancel(false);
                            old.getResult(); //await cancellation so that nothing strange can happen.
                        }else if (selectedElement != null && Objects.equals(selectedElement.getFTreeJson().orElse(null), ftreeJson)) {
                            return false;
                        }
                        browser.clear();
                        checkForInterruption();
                        if (selectedElement != null) {
                            // At som stage we can think about directly load the json representation vom the project space
                            TreeVisualizationPanel.this.ftreeJson = selectedElement.getFTreeJson().orElse(null);
                            checkForInterruption();
                            if (ftreeJson != null) {
                                checkForInterruption();
                                if (!ftreeJson.isBlank()) {
                                    browser.loadTree(ftreeJson);
                                    checkForInterruption();
                                    Jobs.runEDTAndWait(() -> setToolbarEnabled(true));
                                    checkForInterruption();

                                    final AtomicInteger tScale = new AtomicInteger();
                                    final AtomicInteger tScaleMin = new AtomicInteger();
                                    //waiting ok because from generic background thread
                                    Jobs.runJFXAndWait(() -> {
                                        tScaleMin.set(Float.floatToIntBits(jsBridge.getTreeScaleMin()));
                                        tScale.set(Float.floatToIntBits(jsBridge.getTreeScale()));
                                    });
                                    //waiting ok because from generic background thread
                                    Jobs.runEDTAndWait(() -> {
                                        // adapt scale slider to tree scales
                                        scaleSlider.setMaximum((int) (1 / Float.intBitsToFloat(tScaleMin.get()) * 100));
                                        scaleSlider.setValue((int) (1 / Float.intBitsToFloat(tScale.get()) * 100));
                                        scaleSlider.setMinimum(TreeViewerBridge.TREE_SCALE_MIN);
                                    });

                                    checkForInterruption();
                                    if (settings == null)
                                        Jobs.runEDTAndWait(() -> settings = new TreeViewerSettings(TreeVisualizationPanel.this));
                                    return true;
                                }else {
                                    Jobs.runEDTAndWait(() -> setToolbarEnabled(false));
                                }
                            }else {
                                Jobs.runEDTAndWait(() -> setToolbarEnabled(false));
                            }
                        }
                        ftreeJson = null;
                        browser.clear(); //todo maybe not needed
                        Jobs.runEDTAndWait(() -> setToolbarEnabled(false));
                        return false;
                    }

                    @Override
                    public void cancel(boolean mayInterruptIfRunning) {
                        super.cancel(mayInterruptIfRunning);
                        browser.cancelTasks();
                    }
                });
            } finally {
                backgroundLoaderLock.unlock();
            }
    }


    public void showTree(String jsonTree) throws InvocationTargetException, InterruptedException {
        if (jsonTree != null && !jsonTree.isBlank()) {
            browser.loadTree(jsonTree);
            setToolbarEnabled(true);

            final AtomicInteger tScale = new AtomicInteger();
            final AtomicInteger tScaleMin = new AtomicInteger();
            Jobs.runJFXLater(() -> {
                tScaleMin.set(Float.floatToIntBits(jsBridge.getTreeScaleMin()));
                tScale.set(Float.floatToIntBits(jsBridge.getTreeScale()));
                Jobs.runEDTLater(() -> {
                    // adapt scale slider to tree scales
                    scaleSlider.setMaximum((int) (1 / Float.intBitsToFloat(tScaleMin.get()) * 100));
                    scaleSlider.setValue((int) (1 / Float.intBitsToFloat(tScale.get()) * 100));
                    scaleSlider.setMinimum(TreeViewerBridge.TREE_SCALE_MIN);
                });
            });

            if (settings == null)
                settings = new TreeViewerSettings(TreeVisualizationPanel.this);
        } else {
            browser.clear();
            setToolbarEnabled(false);
        }
    }

    protected void setToolbarEnabled(boolean enabled) {
        for (Component comp : toolBar.getComponents())
            comp.setEnabled(enabled);
    }

    public void applyPreset(String preset) {
        if (localConfig == null)
            localConfig = new TreeConfig();
        String propertyPrefix = PropertyManager.PROPERTY_BASE + ".tree_viewer.";
        String presetPropertyPrefix = propertyPrefix + preset.toString() + ".";

        for (String setting : TreeConfig.SETTINGS)
            localConfig.setFromString(
                    setting, PropertyManager.getProperty(
                            // preferably use preset value
                            presetPropertyPrefix + setting,
                            propertyPrefix + setting, null));

        updateConfig();
        if (settings != null)
            settings.updateConfig();
    }

    public void updateConfig() {
        // Nothing to change for now
    }

    public void resetTreeView() {
        jsBridge.resetTree();
        jsBridge.resetZoom();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == presetBox) {
            applyPreset((String) presetBox.getSelectedItem());
        } else if (e.getSource() == advancedSettingsBtn) {
            Jobs.runEDTLater(settings::toggleShow); //swing code
        } else if (e.getSource() == resetBtn) {
            Jobs.runJFXLater(this::resetTreeView); //browser code
        } else if (e.getSource() == saveTreeBtn) {
            saveTree(); //todo which thread do we need here? Swing EDT but with loader for the IO conversion!
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == scaleSlider)
            jsBridge.scaleTree(1 / (((float) scaleSlider.getValue()) / 100));
    }

    public void saveTree() {
        // carried over from
        // de.unijena.bioinf.sirius.gui.mainframe.TreeVisualizationPanel
        abstract class FTreeFilter extends FileFilter {

            private String fileSuffix, description;

            public FTreeFilter(String fileSuffix, String description) {
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

        class FTreeDotFilter extends FTreeFilter {

            public FTreeDotFilter() {
                super(".dot", "Dot");
            }

        }

        class FTreeSVGFilter extends FTreeFilter {

            public FTreeSVGFilter() {
                super(".svg", "SVG");
            }

        }

        class FTreePDFFilter extends FTreeFilter {

            public FTreePDFFilter() {
                super(".pdf", "PDF");
            }

        }

        class FTreeJSONFilter extends FTreeFilter {

            public FTreeJSONFilter() {
                super(".json", "JSON");
            }
        }

        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(PropertyManager.getFile(SiriusProperties.DEFAULT_TREE_EXPORT_PATH));
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setAcceptAllFileFilterUsed(false);

        FileFilter svgFilter = new FTreeSVGFilter();
        FileFilter pdfFilter = new FTreePDFFilter();
        FileFilter dotFilter = new FTreeDotFilter();
        FileFilter jsonFilter = new FTreeJSONFilter();


        jfc.addChoosableFileFilter(dotFilter);
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

                if (jfc.getFileFilter() == dotFilter) {
                    ff = FileFormat.dot;
                    if (!selFile.getAbsolutePath().endsWith(".dot")) {
                        selFile = new File(selFile.getAbsolutePath() + ".dot");
                    }
                } else if (jfc.getFileFilter() == svgFilter) {
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
            Jobs.runInBackgroundAndLoad(popupOwner, "Exporting Tree...", () -> {
                try {
                    if (fff == FileFormat.dot) {
                        new FTDotWriter().writeTreeToFile(fSelectedFile,
                                new FTJsonReader().treeFromJsonString(ftreeJson, null));
                    } else if (fff == FileFormat.svg) {
                        final StringBuilder svg = new StringBuilder();
                        Jobs.runJFXAndWait(() -> svg.append(jsBridge.getSVG()));
                        WebViewIO.writeSVG(fSelectedFile, svg.toString());
                    } else if (fff == FileFormat.pdf) {
                        final StringBuilder svg = new StringBuilder();
                        Jobs.runJFXAndWait(() -> svg.append(jsBridge.getSVG()));
                        WebViewIO.writePDF(fSelectedFile, svg.toString());
                    } else if (fff == FileFormat.json) {
                        Files.writeString(fSelectedFile.toPath(), ftreeJson);
                    }
                } catch (Exception e2) {
                    new StacktraceDialog(popupOwner, e2.getMessage(), e2);
                    LoggerFactory.getLogger(this.getClass()).error(e2.getMessage(), e2);
                }
            });
        }
    }

    @Override
    public void componentResized(ComponentEvent componentEvent) {
        int height = ((JFXPanel) this.browser).getHeight();
        int width = ((JFXPanel) this.browser).getWidth();
        browser.executeJS("window.outerHeight = " + height);
        browser.executeJS("window.outerWidth = " + width);
        if (ftreeJson != null) {
            browser.executeJS("update()");
            //this enusre the correct order without blocking
            Jobs.runJFXLater(() -> {
                final AtomicInteger tScale = new AtomicInteger();
                final AtomicInteger tScaleMin = new AtomicInteger();
                tScaleMin.set(Float.floatToIntBits(jsBridge.getTreeScaleMin()));
                tScale.set(Float.floatToIntBits(jsBridge.getTreeScale()));
                Jobs.runEDTLater(() -> {
                    // adapt scale slider to tree scales
                    scaleSlider.setMaximum((int) (1 / Float.intBitsToFloat(tScaleMin.get()) * 100));
                    scaleSlider.setValue((int) (1 / Float.intBitsToFloat(tScale.get()) * 100));
                    scaleSlider.setMinimum(TreeViewerBridge.TREE_SCALE_MIN);
                });
            });
        }
    }


    @Override
    public void componentMoved(ComponentEvent componentEvent) {

    }

    @Override
    public void componentShown(ComponentEvent componentEvent) {

    }

    @Override
    public void componentHidden(ComponentEvent componentEvent) {

    }

    public TreeViewerBridge getJsBridge() {
        return jsBridge;
    }

    public TreeConfig getLocalConfig() {
        return localConfig;
    }

    public TreeViewerConnector getConnector(){
        return jsConnector;
    }
}
