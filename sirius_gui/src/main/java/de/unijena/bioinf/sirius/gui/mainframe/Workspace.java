package de.unijena.bioinf.sirius.gui.mainframe;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 27.01.17.
 */

import ca.odell.glazedlists.BasicEventList;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class Workspace {
    public static final BasicEventList<ExperimentContainer> COMPOUNT_LIST = new BasicEventList<>();
    private static final HashSet<String> NAMES = new HashSet<>();


    public static void clearWorkspace() {
        NAMES.clear();
        COMPOUNT_LIST.clear();
    }

    public static List<ExperimentContainer> toExperimentContainer(Ms2Experiment... exp) {
        return toExperimentContainer(Arrays.asList(exp));
    }

    public static List<ExperimentContainer> toExperimentContainer(List<Ms2Experiment> exp) {
        ArrayList<ExperimentContainer> ecs = new ArrayList<>(exp.size());
        for (Ms2Experiment ms2Experiment : exp) {
            ecs.add(new ExperimentContainer(ms2Experiment));
        }
        return ecs;
    }

    public static void importCompounds(List<ExperimentContainer> ecs) {
        if (ecs != null) {
            for (ExperimentContainer ec : ecs) {
                if (ec == null) {
                    continue;
                } else {
                    importCompound(ec);
                }
            }
        }
    }

    public static void importCompound(final ExperimentContainer ec) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                resolveCompundNameConflict(ec);
                COMPOUNT_LIST.add(ec);
                if (ec.getResults().size() > 0) ec.setSiriusComputeState(ComputingStatus.COMPUTED);
            }
        });
    }

    public static void resolveCompundNameConflict(ExperimentContainer ec) {
        while (true) {
            if (ec.getGUIName() != null && !ec.getGUIName().isEmpty()) {
                if (NAMES.contains(ec.getGUIName())) {
                    ec.setSuffix(ec.getSuffix() + 1);
                } else {
                    NAMES.add(ec.getGUIName());
                    break;
                }
            } else {
                ec.setName("Unknown");
                ec.setSuffix(1);
            }
        }
    }

    public static void removeAll(List<ExperimentContainer> containers) {
        for (ExperimentContainer container : containers) {
            NAMES.remove(container.getGUIName());
        }
        COMPOUNT_LIST.removeAll(containers);
    }
}
