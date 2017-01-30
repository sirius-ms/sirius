package de.unijena.bioinf.sirius.gui.settings;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 07.10.16.
 */

import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree.TreeBuilderFactory;
import de.unijena.bioinf.sirius.gui.io.FileChooserPanel;
import org.jdesktop.swingx.JXTitledSeparator;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class GerneralSettingsPanel extends TwoCloumnPanel implements SettingsPanel {
    private Properties props;
    final FileChooserPanel db;
    final JComboBox<String> solver;

    public GerneralSettingsPanel(Properties properties) {
        super();
        this.props = properties;


        add(new JXTitledSeparator("ILP solver"));
        solver = new JComboBox<>(new String[]{"gurobi,glpk", "glpk,gurobi", "gurobi", "glpk"}); //todo change setting without restart
        solver.setSelectedItem(props.getProperty("de.unijena.bioinf.sirius.treebuilder"));
        solver.setToolTipText("Choose the allowed solvers and in which order they should be checked. Note that glpk is part of Sirius whereas the others not");
        add(new JLabel("Allowed solvers:"), solver);

        add(new JXTitledSeparator("CSI:fingerID"));
        String p = props.getProperty("de.unijena.bioinf.sirius.fingerID.cache");
        db = new FileChooserPanel(p, JFileChooser.DIRECTORIES_ONLY);
        db.setToolTipText("Specify the directory where CSI:FingerId should store the compound candidates.");
        add(new JLabel("Database cache:"), db);
    }

    @Override
    public void refreshValues() {
    }

    @Override
    public void saveProperties() {

        props.setProperty("de.unijena.bioinf.sirius.treebuilder", (String) solver.getSelectedItem());
        TreeBuilderFactory.setBuilderPriorities(((String)solver.getSelectedItem()).replaceAll("\\s", "").split(","));
        final Path dir = Paths.get(db.getFilePath());
        if (Files.isDirectory(dir)) {
            props.setProperty("de.unijena.bioinf.sirius.fingerID.cache", dir.toAbsolutePath().toString());
            new SwingWorker<Integer, String>() {
                @Override
                protected Integer doInBackground() throws Exception {
                    MF.getCsiFingerId().setDirectory(dir.toFile());
                    return 1;
                }
            }.execute();
        }else {
            LoggerFactory.getLogger(this.getClass()).warn("Specified path is not a directory (" + dir.toString() + "). Directory not Changed!");
        }
    }

    @Override
    public String name() {
        return "General";
    }

}
