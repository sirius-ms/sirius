package de.unijena.bioinf.sirius.gui.compute;

import de.unijena.bioinf.jjobs.JJobManagerPanel;
import de.unijena.bioinf.jjobs.JJobTable;
import de.unijena.bioinf.jjobs.JJobTableFormat;
import de.unijena.bioinf.jjobs.SwingJJobContainer;
import de.unijena.bioinf.sirius.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.sirius.gui.table.SiriusTableCellRenderer;
import de.unijena.bioinf.sirius.logging.JobLogDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;

public class JobDialog extends JDialog implements JobLog.JobListener {

    protected JFrame owner;

    protected JobList running, terminated;
    protected DefaultListModel<JobLog.Job> r, t;

    public JobDialog(JFrame owner) {
        super(owner, "Jobs", false);
        this.owner = owner;
        final JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));

        final JPanel running = new JPanel(new BorderLayout());
        final JPanel done = new JPanel(new BorderLayout());
        innerPanel.add(running);
        innerPanel.add(done);
        running.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Running CSI:FingerID Jobs"));
        done.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Terminated CSI:FingerID Jobs"));

        JJobManagerPanel managerPanel = createJobManagerPanel();

        JPanel main = new JPanel(new BorderLayout());
        main.add(managerPanel, BorderLayout.CENTER);
        main.add(innerPanel, BorderLayout.SOUTH);
        add(main);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.r = new DefaultListModel<>();
        this.t = new DefaultListModel<>();
        this.running = new JobList(r);
        this.terminated = new JobList(t);
        final JobCell cell = new JobCell(null, 0);
        this.running.setCellRenderer(cell);
        this.terminated.setCellRenderer(cell);
        final JScrollPane scrollPane1 = new JScrollPane(this.running, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        final JScrollPane scrollPane2 = new JScrollPane(this.terminated, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        running.add(scrollPane1);
        done.add(scrollPane2);
        setMinimumSize(new Dimension(640, 480));
        setLocationRelativeTo(owner);
    }

    private JJobManagerPanel createJobManagerPanel() {
        //todo button enable disable stuff
        final JJobTable jobTable = new JJobTable(Jobs.MANAGER, new JJobTableFormat(), new SiriusTableCellRenderer());
        jobTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // check if a double click
                    int row = jobTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        SwingJJobContainer c = jobTable.getAdvancedTableModel().getElementAt(row);
                        new JobLogDialog(JobDialog.this, c);
                    }
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelB = new JButton("Cancel");
        cancelB.setToolTipText("Tries to cancel selected jobs. Note that not all jobs will be cancelable immediately. Some jobs may not be cancelable at all.");
        cancelB.addActionListener(e -> {
            for (SwingJJobContainer c : jobTable.getAdvancedListSelectionModel().getSelected()) {
                c.getSourceJob().cancel();
            }
        });

        JButton openLogb = new JButton("Show log");
        openLogb.setToolTipText("Opens the log window for the selected Job.");
        openLogb.addActionListener(e -> {
            int row = jobTable.getSelectedRow();
            if (row >= 0) {
                SwingJJobContainer c = jobTable.getAdvancedTableModel().getElementAt(row);
                new JobLogDialog(JobDialog.this, c);
            }
        });

        jobTable.getSelectionModel().addListSelectionListener(e -> {
            final boolean enabled = e.getFirstIndex() >= 0;
            cancelB.setEnabled(enabled);
            openLogb.setEnabled(enabled);
        });

        final boolean enabled = jobTable.getSelectedRow() >= 0;
        cancelB.setEnabled(enabled);
        openLogb.setEnabled(enabled);

        buttonPanel.add(openLogb);
        buttonPanel.add(cancelB);


        JPanel cleaningButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton clearFailedB = new JButton("Clear failed");
        clearFailedB.setToolTipText("Remove all failed jobs from job list. This will also remove the logs");
        clearFailedB.addActionListener(e -> Jobs.MANAGER.clearFailed()); //todo this could be a global action


        cleaningButtonPanel.add(clearFailedB);


        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPanel, BorderLayout.EAST);
        southPanel.add(cleaningButtonPanel, BorderLayout.WEST);

        return new JJobManagerPanel(jobTable, null, southPanel);
    }

    protected static class JobList extends JList<JobLog.Job> {
        public JobList(ListModel<JobLog.Job> dataModel) {
            super(dataModel);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            final int i = locationToIndex(event.getPoint());
            if (i >= 0 && i < getModel().getSize()) {
                final JobLog.Job job = getModel().getElementAt(i);
                if (job != null) {
                    return job.name() + ": " + job.description();
                }
            }
            return null;
        }
    }

    public void showDialog() {
        JobLog.getInstance().addListener(this);
        r.clear();
        for (JobLog.Job j : JobLog.getInstance().runningJobs) r.add(0, j);
        t.clear();
        for (JobLog.Job j : JobLog.getInstance().doneJobs) t.add(0, j);
        setVisible(true);
    }

    @Override
    public void dispose() {
        super.dispose();
        JobLog.getInstance().removeListener(this);
    }

    @Override
    public void jobIsSubmitted(final JobLog.Job job) {
        r.add(0, job);
    }

    @Override
    public void jobIsRunning(JobLog.Job job) {
        int i = r.indexOf(job);
        if (i >= 0) r.setElementAt(job, i);
    }

    @Override
    public void jobIsDone(final JobLog.Job job) {
        r.removeElement(job);
        t.add(0, job);
    }

    @Override
    public void jobIsFailed(final JobLog.Job job) {
        r.removeElement(job);
        t.add(0, job);
    }

    @Override
    public void jobDescriptionChanged(final JobLog.Job job) {
        if (job.isError() || job.isDone()) {
            t.setElementAt(job, t.indexOf(job));
        } else {
            r.setElementAt(job, r.indexOf(job));
        }
    }

    protected static class JobCell extends JPanel implements ListCellRenderer<JobLog.Job> {
        protected JobLog.Job job;
        protected int index;
        protected Font font;

        public JobCell(JobLog.Job job, int index) {
            super();
            this.job = job;
            this.index = index;
            setPreferredSize(new Dimension(500, 14));
            InputStream fontFile = getClass().getResourceAsStream("/ttf/DejaVuSans-Bold.ttf");
            try {
                font = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(13f);

            } catch (FontFormatException | IOException e) {
                font = Font.getFont(Font.SANS_SERIF).deriveFont(13f);
            }
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            final Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(font);
            g2.setColor(Color.BLACK);
            {
                if (job.isError()) {
                    g2.setColor(new Color(203, 91, 76));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(Color.BLACK);
                } else if (job.isRunning()) {
                    g2.setColor(new Color(51, 116, 149));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(Color.BLACK);
                } else {
                    if (index % 2 == 0) {
                        g2.setColor(Color.WHITE);
                    } else {
                        g2.setColor(new Color(213, 227, 238));
                    }
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(Color.BLACK);
                }
            }
            if (job == null) return;

            {
                final int namewidth = g2.getFontMetrics().stringWidth(job.name());
                final String name;
                if (namewidth > 160) {
                    final int width = g2.getFontMetrics().charWidth('m');
                    final int maxlength = (160 / width);
                    name = job.name().substring(0, maxlength) + "...";
                } else name = job.name();
                g2.drawString(name, 0, 12);
            }
            {
                final int descwidth = g2.getFontMetrics().stringWidth(job.description());
                final String desc;
                if (descwidth > 380) {
                    final int width = g2.getFontMetrics().charWidth('W');
                    final int maxlength = (380 / width);
                    desc = job.description().substring(0, maxlength) + "...";
                } else desc = job.description();
                g2.drawString(desc, 170, 12);
            }
            {
                final int width = g2.getFontMetrics().stringWidth("enqueued");
                if (job.isDone()) {
                    g2.drawString("done", getWidth() - width, 12);
                } else if (job.isRunning()) {
                    g2.drawString("running", getWidth() - width, 12);
                } else if (job.isError()) {
                    g2.drawString("failed", getWidth() - width, 12);
                } else {
                    g2.drawString("enqueued", getWidth() - width, 12);
                }
            }
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends JobLog.Job> list, JobLog.Job value, int index, boolean isSelected, boolean cellHasFocus) {
            this.job = value;
            this.index = index;
            return this;
        }
    }
}
