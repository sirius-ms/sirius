package de.unijena.bioinf.sirius.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 28.01.17.
 */

import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusActionManager extends ActionMap {
    public static final SiriusActionManager ROOT_MANAGER = new SiriusActionManager(true);

    public SiriusActionManager() {
        this(true);
    }

    private SiriusActionManager(final boolean connectToRoot) {
        super();
        if (connectToRoot) {
            setParent(ROOT_MANAGER);
        }
    }

    public static void initRootManager() {
        for (SiriusActions actions : SiriusActions.values()) {
            try {
                if (ROOT_MANAGER.get(actions.name()) == null) {
                    Action actionInstance = actions.actionClass.newInstance();
                    ROOT_MANAGER.put(actions.name(), actionInstance);
                }
            } catch (InstantiationException | IllegalAccessException e) {
                LoggerFactory.getLogger(SiriusActionManager.class).error("Could not load following Sirius Action: " + actions.name(), e);
            }
        }
    }


    /*private void initActions() {
        put("compute_csi", new AbstractAction() {

        });
        put("compute", new AbstractAction());

        put("cancel_compute", new AbstractAction() {

        });

        put("compute_all", new AbstractAction() {

        });


        put("new_exp", new AbstractAction() {

        });

        put("edit_exp", new AbstractAction() {

        });


        put("save-ws", new AbstractAction() {

        });

        put("load_ws", new AbstractAction() {

        });

        put("batch_import", new AbstractAction() {

        });

        put("delete", new AbstractAction() {

        });

        put("show_settings", new AbstractAction() {

        });

        put("show_bug", new AbstractAction() {

        });

        put("show_about", new AbstractAction() {

        });

        put("show_db", new AbstractAction() {

        });
    }*/
}
