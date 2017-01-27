package de.unijena.bioinf.sirius.gui.mainframe;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 27.01.17.
 */

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import de.unijena.bioinf.sirius.cli.SiriusApplication;
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

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;
import static de.unijena.bioinf.sirius.gui.mainframe.Workspace.COMPOUNT_LIST;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusToolbar extends JToolBar {
    protected boolean computeAllActive;
    private ToolbarButton newB, loadB, saveB, batchB, computeAllB, exportResultsB, configFingerID, jobs, db, settings, bug, about;


    public SiriusToolbar(final JobDialog jobDialog) {
        // ########## Toolbar ############

        setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Colors.ICON_BLUE));
        initActions();
        ActionMap am = getActionMap();

        newB = new ToolbarButton("Import", Icons.DOC_32);
        newB.addActionListener(am.get("new_exp"));
        newB.setToolTipText("Import measurements of a single compound");
        add(newB);

        batchB = new ToolbarButton("Batch Import", Icons.DOCS_32);
        batchB.addActionListener(am.get("batch_import"));
        batchB.setToolTipText("Import measurements of several compounds");

        add(batchB);
        addSeparator(new Dimension(20, 20));

        loadB = new ToolbarButton("Load Workspace", Icons.FOLDER_OPEN_32);
        loadB.addActionListener(am.get("load_ws"));
        loadB.setToolTipText("Load all experiments and computed results from a previously saved workspace.");
        add(loadB);
        saveB = new ToolbarButton("Save Workspace", Icons.FOLDER_CLOSE_32);
        saveB.addActionListener(am.get("save_ws"));
        saveB.setEnabled(false);
        add(saveB);
        addSeparator(new Dimension(20, 20));
//
        computeAllB = new ToolbarButton("Compute All", Icons.RUN_32);
        computeAllB.addActionListener(am.get("compute_all"));
        computeAllB.setEnabled(false);
        add(computeAllB);

        exportResultsB = new ToolbarButton("Export Results", Icons.EXPORT_32);
        exportResultsB.addActionListener(am.get("export"));
        exportResultsB.setEnabled(false);
        add(exportResultsB);
        addSeparator(new Dimension(20, 20));
//        add(Box.createGlue());

        configFingerID = new ToolbarButton("CSI:FingerId", Icons.FINGER_32);
        configFingerID.addActionListener(am.get("compute_csi"));
        configFingerID.setEnabled(false);

        add(configFingerID);
        addSeparator(new Dimension(20, 20));
//        add(Box.createGlue());

        //todo implement database menu
//        db = new ToolbarButton("Database", Icons.DB_32);
        /*add(db);
        db.addActionListener(this);
        addSeparator(new Dimension(20,20));*/


        jobs = new ToolbarButton("Jobs", Icons.FB_LOADER_STOP_32);
        jobs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jobDialog.showDialog();
            }
        });
        add(jobs);
        addSeparator(new Dimension(20, 20));
        add(Box.createGlue());
        addSeparator(new Dimension(20, 20));


        settings = new ToolbarButton("Settings", Icons.GEAR_32);
        settings.setToolTipText("Settings");
        settings.addActionListener(am.get("show_settings"));
        add(settings);

        bug = new ToolbarButton("Bug Report", Icons.BUG_32);
        bug.setToolTipText("Report a bug or send a feature request");
        bug.addActionListener(am.get("show_bug"));
        add(bug);

        about = new ToolbarButton("About", Icons.INFO_32);
        about.setToolTipText("About Sirius");
        about.addActionListener(am.get("show_about"));
        add(about);

        setRollover(true);
        setFloatable(false);
        //Toolbar end


        //Workspace Listener
        COMPOUNT_LIST.addListEventListener(new ListEventListener<ExperimentContainer>() {
            @Override
            public void listChanged(ListEvent<ExperimentContainer> listChanges) {
                EventList<ExperimentContainer> sl = listChanges.getSourceList();
                if (!sl.isEmpty()) {
                    saveB.setEnabled(true);
                    for (ExperimentContainer e : sl) {
                        if (e.getComputeState() == ComputingStatus.COMPUTED) {
                            exportResultsB.setEnabled(true);
                            return;
                        }
                    }
                    exportResultsB.setEnabled(false);
                } else {
                    saveB.setEnabled(false);
                }
            }
        });


        //filtered Workspace Listener
        MF.getCompountListPanel().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ExperimentListChangeEvent listChanges) {
                if (listChanges.sourceList.getModel().getSize() > 0) {
                    computeAllB.setEnabled(true);
                } else {
                    computeAllB.setEnabled(false);
                }

            }
        });

        JobLog.getInstance().addListener(new JobLog.JobListener() {
            @Override
            public void jobIsSubmitted(JobLog.Job job) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (JobLog.getInstance().hasActiveJobs()) {
                            jobs.setIcon(Icons.FB_LOADER_RUN_32);
                            computationStarted();
                        } else {
                            jobs.setIcon(Icons.FB_LOADER_STOP_32);
                            computationCanceled();
                        };
                    }
                });
            }

            @Override
            public void jobIsRunning(JobLog.Job job) {
                jobIsSubmitted(job);
            }

            @Override
            public void jobIsDone(final JobLog.Job job) {
                jobIsSubmitted(job);
            }

            @Override
            public void jobIsFailed(JobLog.Job job) {
                jobIsSubmitted(job);
            }

            @Override
            public void jobDescriptionChanged(JobLog.Job job) {}
        });
    }

    private void initActions(){
        final ActionMap am = getActionMap();
        am .setParent(MF.getACTIONS());
    }

    private void computationStarted() {
        this.computeAllActive = true;
        this.computeAllB.setText("    Cancel    "); //todo: ugly hack to prevent button resizing in toolbar, find nice solution
        this.computeAllB.setIcon(Icons.CANCEL_32);
    }




    private void computationCanceled() {
        this.computeAllActive = false;
        this.computeAllB.setText("Compute All");
        this.computeAllB.setIcon(Icons.RUN_32);
    }


    public void setFingerIDEnabled(boolean enabled) {
        configFingerID.setEnabled(enabled);
    }

    public boolean isFingerIDEnabled() {
        return configFingerID.isEnabled();
    }
}
