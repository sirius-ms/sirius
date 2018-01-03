package de.unijena.bioinf.sirius.gui.settings;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 07.10.16.
 */

import de.unijena.bioinf.sirius.gui.io.FileChooserPanel;
import de.unijena.bioinf.sirius.gui.utils.TwoCloumnPanel;
import org.jdesktop.swingx.JXTitledSeparator;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class GerneralSettingsPanel extends TwoCloumnPanel implements SettingsPanel {
    private Properties props;
    final FileChooserPanel db;
    final JComboBox<String> solver;
    final SpinnerNumberModel treeTimeout;

    public GerneralSettingsPanel(Properties properties) {
        super();
        this.props = properties;

        add(new JXTitledSeparator("ILP solver"));
        Vector<String> items = new Vector<>(Arrays.asList("gurobi,cplex,glpk", "gurobi,glpk", "glpk,gurobi", "gurobi", "cplex", "glpk"));
        String selected = props.getProperty("de.unijena.bioinf.sirius.treebuilder");
        if (!items.contains(selected))
            items.add(selected);
        solver = new JComboBox<>(items);
        solver.setSelectedItem(selected);
        solver.setToolTipText("Choose the allowed solvers and in which order they should be checked. Note that glpk is part of Sirius whereas the others not");
        add(new JLabel("Allowed solvers:"), solver);
        treeTimeout = createTimeoutModel();
        add(new JLabel("Tree timeout (seconds):"), new JSpinner(treeTimeout));

        add(new JXTitledSeparator("CSI:FingerID"));
        String p = props.getProperty("de.unijena.bioinf.sirius.fingerID.cache");
        db = new FileChooserPanel(p, JFileChooser.DIRECTORIES_ONLY);
        db.setToolTipText("Specify the directory where CSI:FingerId should store the compound candidates.");
        add(new JLabel("Database cache:"), db);
    }

    @Override
    public void saveProperties() {
        props.setProperty("de.unijena.bioinf.sirius.treebuilder", (String) solver.getSelectedItem());
        props.setProperty("de.unijena.bioinf.sirius.treebuilder.timeout", treeTimeout.getNumber().toString());
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
        } else {
            LoggerFactory.getLogger(this.getClass()).warn("Specified path is not a directory (" + dir.toString() + "). Directory not Changed!");
        }
    }

    private SpinnerNumberModel createTimeoutModel() {
        String seconds = props.getProperty("de.unijena.bioinf.sirius.treebuilder.timeout", "1800");

        SpinnerNumberModel model = new SpinnerNumberModel();
        model.setValue(Integer.valueOf(seconds));


        return model;
    }

    @Override
    public String name() {
        return "General";
    }

}
