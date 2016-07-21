package de.unijena.bioinf.sirius.gui.compute;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;

public class JobDialog extends JDialog implements JobLog.JobListener {

    protected JFrame owner;

    protected JobList running, terminated;
    protected DefaultListModel<JobLog.Job> r, t;

    public JobDialog(JFrame owner) {
        super(owner,"Jobs", false);
        this.owner = owner;
        final JPanel innerPanel = new JPanel(); innerPanel.setLayout(new BoxLayout(innerPanel,BoxLayout.Y_AXIS));
        final JPanel running = new JPanel(new BorderLayout());
        final JPanel done = new JPanel(new BorderLayout());
        innerPanel.add(running);
        innerPanel.add(done);
        running.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Running Jobs"));
        done.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Terminated Jobs"));
        add(innerPanel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.r = new DefaultListModel<>();
        this.t = new DefaultListModel<>();
        this.running = new JobList(r);
        this.terminated = new JobList(t);
        final JobCell cell = new JobCell(null,0);
        this.running.setCellRenderer(cell);
        this.terminated.setCellRenderer(cell);
        final JScrollPane scrollPane1 = new JScrollPane(this.running, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        final JScrollPane scrollPane2 = new JScrollPane(this.terminated, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        running.add(scrollPane1);
        done.add(scrollPane2);
        setMinimumSize(new Dimension(640, 480));
    }

    protected static class JobList extends JList<JobLog.Job> {
        public JobList(ListModel<JobLog.Job> dataModel) {
            super(dataModel);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            final int i = locationToIndex(event.getPoint());
            if (i < getModel().getSize()) {
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
        if (i>=0) r.setElementAt(job, i);
    }

    @Override
    public void jobIsDone(final JobLog.Job job) {
        r.removeElement(job);
        t.add(0,job);
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
            setPreferredSize(new Dimension(500,14));
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
            final Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(font);
            g2.setColor(Color.BLACK);
            {
                if (job.isError()) {
                    g2.setColor(new Color(203,91,76));
                    g2.fillRect(0,0,getWidth(),getHeight());
                    g2.setColor(Color.BLACK);
                } else if (job.isRunning()) {
                    g2.setColor(new Color(51,116,149));
                    g2.fillRect(0,0,getWidth(),getHeight());
                    g2.setColor(Color.BLACK);
                } else {
                    if (index%2==0) {
                        g2.setColor(Color.WHITE);
                    } else {
                        g2.setColor(new Color(213, 227, 238));
                    }
                    g2.fillRect(0,0,getWidth(),getHeight());
                    g2.setColor(Color.BLACK);
                }
            }
            if (job==null) return;

            {
                final int namewidth = g2.getFontMetrics().stringWidth(job.name());
                final String name;
                if (namewidth > 160) {
                    final int width = g2.getFontMetrics().charWidth('m');
                    final int maxlength = (160/width);
                    name = job.name().substring(0, maxlength) + "...";
                } else name = job.name();
                g2.drawString(name, 0, 12);
            }
            {
                final int descwidth = g2.getFontMetrics().stringWidth(job.description());
                final String desc;
                if (descwidth > 380) {
                    final int width = g2.getFontMetrics().charWidth('W');
                    final int maxlength = (380/width);
                    desc = job.description().substring(0, maxlength) + "...";
                } else desc= job.description();
                g2.drawString(desc, 170, 12);
            }
            {
                final int width = g2.getFontMetrics().stringWidth("enqueued");
                if (job.isDone()) {
                    g2.drawString("done", getWidth()-width, 12);
                } else if (job.isRunning()) {
                    g2.drawString("running", getWidth()-width, 12);
                } else if (job.isError()) {
                    g2.drawString("failed", getWidth()-width, 12);
                } else {
                    g2.drawString("enqueued", getWidth()-width, 12);
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

/*
    protected class JobRenderer implements ListCellRenderer<JobLog.Job> {

        @Override
        public Component getListCellRendererComponent(JList<? extends JobLog.Job> list, JobLog.Job value, int index, boolean isSelected, boolean cellHasFocus) {

        }
    }
    */
}
