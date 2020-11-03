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

package de.unijena.bioinf.ms.gui.settings;

import de.unijena.bioinf.ms.frontend.io.FileChooserPanel;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import org.jdesktop.swingx.JXTitledSeparator;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class GerneralSettingsPanel extends TwoColumnPanel implements SettingsPanel {
    private Properties props;
    final FileChooserPanel db;
    final JComboBox<String> solver;

    public GerneralSettingsPanel(Properties properties) {
        super();
        this.props = properties;

        add(new JXTitledSeparator("ILP solver"));
        Vector<String> items = new Vector<>(Arrays.asList("clp,cplex,gurobi,glpk", "cplex,gurobi,clp,glpk", "cplex,clp,glpk", "gurobi,clp,glpk", "clp,glpk","glpk,clp", "gurobi", "cplex", "clp", "glpk"));
        String selected = props.getProperty("de.unijena.bioinf.sirius.treebuilder.solvers");
        if (!items.contains(selected))
            items.add(selected);
        solver = new JComboBox<>(items);
        solver.setSelectedItem(selected);
        solver.setToolTipText("Choose the allowed solvers and in which order they should be checked. Note that glpk is part of Sirius whereas the others not");
        add(new JLabel("Allowed solvers:"), solver);

        add(new JXTitledSeparator("CSI:FingerID"));
        String p = props.getProperty("de.unijena.bioinf.sirius.fingerID.cache");
        db = new FileChooserPanel(p, JFileChooser.DIRECTORIES_ONLY);
        db.setToolTipText("Specify the directory where CSI:FingerID should store the compound candidates.");
        add(new JLabel("Database cache:"), db);
    }

    @Override
    public void saveProperties() {
        props.setProperty("de.unijena.bioinf.sirius.treebuilder.solvers", (String) solver.getSelectedItem());
//        props.setProperty("de.unijena.bioinf.sirius.treebuilder.timeout", treeTimeout.getNumber().toString());
        final Path dir = Paths.get(db.getFilePath());
        if (Files.isDirectory(dir)) {
            props.setProperty("de.unijena.bioinf.sirius.fingerID.cache", dir.toAbsolutePath().toString());
            Jobs.runInBackgroundAndLoad(MF, () -> {
                //todo do we need to invalidate chache somehow
                System.out.println("WaRN Check if we have to do something???");
            });
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
