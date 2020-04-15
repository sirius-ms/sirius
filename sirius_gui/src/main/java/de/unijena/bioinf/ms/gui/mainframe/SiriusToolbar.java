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
    private ToolbarButton imCompB, openB, saveB,exportB, imB, computeAllB, configFingerID, jobs, db, connect, settings, bug, about;

    SiriusToolbar() {

        setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Colors.ICON_BLUE));

        //import
        imCompB = new ToolbarButton(SiriusActions.IMPORT_EXP.getInstance());
        add(imCompB);
//        addSeparator(new Dimension(20, 20));

        imB = new ToolbarButton(SiriusActions.IMPORT_EXP_BATCH.getInstance());
        add(imB);
        addSeparator(new Dimension(20, 20));

        //open
        openB = new ToolbarButton(SiriusActions.LOAD_WS.getInstance());
        add(openB);
        //save
        saveB = new ToolbarButton(SiriusActions.SAVE_WS.getInstance());
        add(saveB);
        addSeparator(new Dimension(20, 20));

        exportB = new ToolbarButton(SiriusActions.EXPORT_WS.getInstance());
        add(exportB);

        addSeparator(new Dimension(20, 20));
        add(Box.createGlue());
        addSeparator(new Dimension(20, 20));

        //compute
        computeAllB = new ToolbarButton(SiriusActions.COMPUTE_ALL.getInstance());
        add(computeAllB);
        addSeparator(new Dimension(20, 20));

        //todo implement database menu
        //todo reenable if fixed
//        db = new ToolbarButton(SiriusActions.SHOW_DB.getInstance());
//        add(db);
//        addSeparator(new Dimension(20,20));


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
