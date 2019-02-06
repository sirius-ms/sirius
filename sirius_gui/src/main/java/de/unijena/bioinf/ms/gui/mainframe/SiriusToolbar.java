package de.unijena.bioinf.ms.gui.mainframe;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 27.01.17.
 */

import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.utils.ToolbarButton;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
class SiriusToolbar extends JToolBar {
    private ToolbarButton newB, loadB, saveB, batchB, computeAllB, exportResultsB, configFingerID, jobs, db, connect, settings, bug, about;

    SiriusToolbar() {

        setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Colors.ICON_BLUE));

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

//        configFingerID = new ToolbarButton(SiriusActions.COMPUTE_CSI.getInstance());
//        add(configFingerID);
//        addSeparator(new Dimension(20, 20));
//        add(Box.createGlue());

        //todo implement database menu
        db = new ToolbarButton(SiriusActions.SHOW_DB.getInstance());
        add(db);
        addSeparator(new Dimension(20,20));


        jobs = new ToolbarButton(SiriusActions.SHOW_JOBS.getInstance());
        add(jobs);
        addSeparator(new Dimension(20, 20));
        add(Box.createGlue());
        addSeparator(new Dimension(20, 20));

        settings = new ToolbarButton(SiriusActions.SHOW_SETTINGS.getInstance());
        add(settings);

        connect = new ToolbarButton(SiriusActions.CHECK_CONNECTION.getInstance());
        add(connect);

        bug = new ToolbarButton(SiriusActions.SHOW_BUGS.getInstance());
        add(bug);

        about = new ToolbarButton(SiriusActions.SHOW_ABOUT.getInstance());
        add(about);

        setRollover(true);
        setFloatable(false);
    }
}
