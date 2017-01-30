package de.unijena.bioinf.sirius.gui.mainframe;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 27.01.17.
 */

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import de.unijena.bioinf.sirius.gui.actions.SiriusActionManager;
import de.unijena.bioinf.sirius.gui.actions.SiriusActions;
import de.unijena.bioinf.sirius.gui.compute.JobDialog;
import de.unijena.bioinf.sirius.gui.compute.JobLog;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.utils.Colors;
import de.unijena.bioinf.sirius.gui.utils.Icons;
import de.unijena.bioinf.sirius.gui.utils.ToolbarButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;
import static de.unijena.bioinf.sirius.gui.mainframe.Workspace.COMPOUNT_LIST;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusToolbar extends JToolBar {
    private ToolbarButton newB, loadB, saveB, batchB, computeAllB, exportResultsB, configFingerID, jobs, db, settings, bug, about;
    private static AtomicBoolean isActive = new AtomicBoolean(true);

    public SiriusToolbar() {
        // ########## Toolbar ############

        setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Colors.ICON_BLUE));
        initActions();

        newB = new ToolbarButton(SiriusActions.IMPORT_EXP.getInstance());
        add(newB);

        batchB = new ToolbarButton(SiriusActions.IMPORT_EXP_BATCH.getInstance());
        add(batchB);
        addSeparator(new Dimension(20, 20));

        loadB = new ToolbarButton(SiriusActions.LOAD_WS.getInstance());
        add(loadB);
        saveB = new ToolbarButton(SiriusActions.SAVE_WS.getInstance());
        add(saveB);
        addSeparator(new Dimension(20, 20));
//
        computeAllB = new ToolbarButton(SiriusActions.COMPUTE_ALL.getInstance());
        add(computeAllB);

        exportResultsB = new ToolbarButton(SiriusActions.EXPORT_RESULTS.getInstance());
        add(exportResultsB);
        addSeparator(new Dimension(20, 20));
//        add(Box.createGlue());

        configFingerID = new ToolbarButton(SiriusActions.COMPUTE_CSI.getInstance());
        add(configFingerID);
        addSeparator(new Dimension(20, 20));
//        add(Box.createGlue());

        //todo implement database menu
        /*db = new ToolbarButton(SiriusActions.SHOW_DB.getInstance());
        add(db);
        addSeparator(new Dimension(20,20));*/


        jobs = new ToolbarButton(SiriusActions.SHOW_JOBS.getInstance());
        add(jobs);
        addSeparator(new Dimension(20, 20));
        add(Box.createGlue());
        addSeparator(new Dimension(20, 20));


        settings = new ToolbarButton(SiriusActions.SHOW_SETTINGS.getInstance());
        add(settings);

        bug = new ToolbarButton(SiriusActions.SHOW_BUGS.getInstance());
        add(bug);

        about = new ToolbarButton(SiriusActions.SHOW_ABOUT.getInstance());
        add(about);

        setRollover(true);
        setFloatable(false);
        //Toolbar end
    }

    private void initActions(){
        final ActionMap am = getActionMap();
        am .setParent(SiriusActionManager.ROOT_MANAGER);
    }


}
