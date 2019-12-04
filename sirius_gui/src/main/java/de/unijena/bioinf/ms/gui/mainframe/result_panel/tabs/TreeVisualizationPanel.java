package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.frontend.io.projectspace.FormulaResultBean;
import de.unijena.bioinf.ms.frontend.io.projectspace.InstanceBean;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.ms.gui.dialogs.FilePresentDialog;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.tree_viewer.*;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import de.unijena.bioinf.ms.properties.PropertyManager;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.jetbrains.annotations.NotNull;
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
import java.util.HashMap;
import java.util.List;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;


public class TreeVisualizationPanel extends JPanel
        implements ActionListener, ChangeListener, ComponentListener,
        ActiveElementChangedListener<FormulaResultBean, InstanceBean>,
        PanelDescription {
    public enum FileFormat {
        dot, json, jpg, png, gif, svg, pdf, none
    }

    public String getDescription() {
        return "Visualization of the Fragmentation tree " +
                "for the selected molecular formula (JS)";
    }

    //    FormulaResultBean sre;
    FTree ftree;
    TreeViewerBrowser browser;
    TreeViewerBridge jsBridge;
    JToolBar toolBar;
    public JComboBox<String> presetBox; // accessible from TreeViewerSettings
    JLabel colorLegendText;
    JSlider scaleSlider;
    JButton saveTreeBtn;
    JButton advancedSettingsBtn;
    JButton resetBtn;
    TreeViewerSettings settings;
    TreeConfig localConfig;

    public TreeVisualizationPanel() {
        this.setLayout(new BorderLayout());

        localConfig = new TreeConfig();
        localConfig.setFromString("presets", PropertyManager
                .getProperty(
                        "de.unijena.bioinf.tree_viewer.presets"));

        ////////////////
        //// Toolbar ///
        ////////////////
        toolBar = new JToolBar();
        toolBar.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        toolBar.setFloatable(false);
        presetBox = new JComboBox<>((String[]) localConfig.get("presets"));
        presetBox.addActionListener(this);
        presetBox.setSelectedItem(PropertyManager.getProperty(
                "de.unijena.bioinf.tree_viewer.preset"));
        JLabel presetLabel = new JLabel("Preset");
        presetLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
        toolBar.add(presetLabel);
        toolBar.add(presetBox);
        toolBar.addSeparator(new Dimension(10, 10));
        saveTreeBtn = Buttons.getExportButton24("Export tree");
        saveTreeBtn.addActionListener(this);
        saveTreeBtn.setEnabled(false);
        saveTreeBtn.setToolTipText("Export the tree view (or zoomed-in region) "
                + "to various formats");
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

        browser.addJS("d3.min.js");
        browser.addJS("d3-colorbar.js");
        browser.addJS("tree_viewer/treeViewer.js");
        browser.addJS("tree_viewer/treeViewerSettings.js");
        browser.addJS("tree_viewer/treeViewerConnector.js");
        this.add((JFXPanel) this.browser, BorderLayout.CENTER);
        this.setVisible(true);
        HashMap<String, Object> bridges = new HashMap<String, Object>() {{
            put("config", localConfig);
            put("connector", new TreeViewerConnector());
        }};
        browser.load(bridges);
        for (Component comp : toolBar.getComponents())
            comp.setEnabled(false);
        this.addComponentListener(this);
        applyPreset((String) presetBox.getSelectedItem());
    }

    public void showTree(@NotNull FTree tree) {
        this.ftree = tree;
        String jsonTree = new FTJsonWriter().treeToJsonString(tree);
        browser.loadTree(jsonTree);
        for (Component comp : toolBar.getComponents())
            comp.setEnabled(true);
        Platform.runLater(() -> {
            // adapt scale slider to tree scales
            scaleSlider.setMaximum((int) (1 / jsBridge.getTreeScaleMin()
                    * 100));
            scaleSlider.setValue((int) (1 / jsBridge.getTreeScale() * 100));
            scaleSlider.setMinimum(TreeViewerBridge.TREE_SCALE_MIN);
        });
        if (settings == null)
            settings = new TreeViewerSettings(this);

    }

    @Override
    public void resultsChanged(InstanceBean experiment,
                               FormulaResultBean sre,
                               List<FormulaResultBean> resultElements,
                               ListSelectionModel selections) {
        if (sre != null && sre.getFragTree().isPresent())
            showTree(sre.getFragTree().get());
        else {
            browser.loadTree(null);
            for (Component comp : toolBar.getComponents())
                comp.setEnabled(false);
        }
    }

    public void applyPreset(String preset) {
        String propertyPrefix;
        if (localConfig == null)
            localConfig = new TreeConfig();
        propertyPrefix = "de.unijena.bioinf.tree_viewer."
                + preset.toString() + ".";

        for (String setting : TreeConfig.SETTINGS)
            localConfig.setFromString(setting, PropertyManager
                    .getProperty(propertyPrefix + setting));

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
        Runnable r = null;
        if (e.getSource() == presetBox) {
            applyPreset((String) presetBox.getSelectedItem());
        } else if (e.getSource() == advancedSettingsBtn) {
            r = () -> {
                settings.toggleShow();
            };
        } else if (e.getSource() == resetBtn) {
            r = () -> {
                resetTreeView();
            };
        } else if (e.getSource() == saveTreeBtn) {
            r = () -> {
                saveTree();
            };
        }
        if (r != null)
            Platform.runLater(r);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == scaleSlider)
            Platform.runLater(() -> {
                jsBridge.scaleTree(1 / (((float) scaleSlider.getValue()) / 100));
            });
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

        FileFormat defaultFF = FileFormat.valueOf(PropertyManager.getProperty(SiriusProperties.DEFAULT_TREE_FILE_FORMAT, null, FileFormat.png.name()));

        /*if (defaultFF == FileFormat.dot) {
            jfc.setFileFilter(dotFilter);
        } else if (defaultFF == FileFormat.gif) {
            jfc.setFileFilter(gifFilter);
        } else if (defaultFF == FileFormat.jpg) {
            jfc.setFileFilter(jpgFilter);
        } else if (defaultFF == FileFormat.png) {
            jfc.setFileFilter(pngFilter);
        } else if (defaultFF == FileFormat.json) {
            jfc.setFileFilter(jsonFilter);
        }
*/
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
                    FilePresentDialog fpd = new FilePresentDialog(MF, selFile.getName());
                    ReturnValue rv = fpd.getReturnValue();
                    if (rv == ReturnValue.Success) {
                        selectedFile = selFile;
                    }
//						int rt = JOptionPane.showConfirmDialog(this, "The file \""+selFile.getName()+"\" is already present. Override it?");
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
            try {
                if (ff == FileFormat.dot) {
                    new FTDotWriter().writeTreeToFile(selectedFile, ftree);
                } else if (ff == FileFormat.svg) {
                    TreeViewerIO.writeSVG(selectedFile, jsBridge.getSVG());
                } else if (ff == FileFormat.pdf) {
                    TreeViewerIO.writePDF(selectedFile, jsBridge.getSVG());
                } else if (ff == FileFormat.json) {
                    new FTJsonWriter().writeTreeToFile(selectedFile, ftree);
                }
            } catch (Exception e2) {
                ErrorReportDialog fed = new ErrorReportDialog(MF, e2.getMessage());
                LoggerFactory.getLogger(this.getClass()).error(e2.getMessage(), e2);
            }
        }
    }

    @Override
    public void componentResized(ComponentEvent componentEvent) {
        int height = ((JFXPanel) this.browser).getHeight();
        int width = ((JFXPanel) this.browser).getWidth();
        Platform.runLater(() -> {
            browser.executeJS("window.outerHeight = " + String.valueOf(height));
            browser.executeJS("window.outerWidth = " + String.valueOf(width));
            if (ftree != null) {
                browser.executeJS("update()");
                Platform.runLater(() -> {
                    // adapt scale slider to tree scales
                    scaleSlider.setMaximum((int) (1 / jsBridge.getTreeScaleMin()
                            * 100));
                    scaleSlider.setValue((int) (1 / jsBridge.getTreeScale() * 100));
                    scaleSlider.setMinimum(TreeViewerBridge.TREE_SCALE_MIN);
                });
            }
        });
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
}
