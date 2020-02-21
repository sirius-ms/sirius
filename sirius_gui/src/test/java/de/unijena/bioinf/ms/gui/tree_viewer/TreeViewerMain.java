package de.unijena.bioinf.ms.gui.tree_viewer;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.jjobs.SwingJobManager;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs.TreeVisualizationPanel;
import de.unijena.bioinf.ms.gui.utils.TwoCloumnPanel;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.properties.SiriusConfigUtils;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class TreeViewerMain {
    static final Logger LOG = LoggerFactory.getLogger(TreeViewerMain.class);
    static JTextField input = new JTextField();
    static JButton laod = new JButton("parse");
    private static String treeViewer;

    public static void main(String[] args) throws IOException, ConfigurationException {
        SiriusJobs.setGlobalJobManager(new SwingJobManager(PropertyManager.getNumberOfThreads(), 1));
        Path siriusTmpProps = Files.createTempFile("sirius", ".properties");
        Files.deleteIfExists(siriusTmpProps);

        //init application properties
        try (InputStream stream = ApplicationCore.class.getResourceAsStream("/sirius.properties")) {
            //init application properties
            final PropertiesConfiguration defaultProps = SiriusConfigUtils.makeConfigFromStream(stream);
            SiriusProperties.initSiriusPropertyFile(siriusTmpProps.toFile(), defaultProps);
        } catch (IOException e) {
            LOG.error("Could NOT create sirius properties file", e);
        }

        JFrame frame = new JFrame("FragTree Viewer");
        JPanel mainPanel = new JPanel(new BorderLayout());
        TwoCloumnPanel north = new TwoCloumnPanel(laod, input);

        TreeVisualizationPanel tvp = new TreeVisualizationPanel();

        laod.addActionListener(e -> {
            try {
                FTree tree = new FTJsonReader().treeFromJsonString(input.getText(), null);
                tvp.showTree(tree);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });


        mainPanel.add(north, BorderLayout.NORTH);
        mainPanel.add(tvp, BorderLayout.CENTER);


        frame.add(mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(1200, 1400);
        frame.setVisible(true);
    }
}
