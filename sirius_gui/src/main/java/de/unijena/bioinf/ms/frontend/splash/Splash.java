package de.unijena.bioinf.ms.frontend.splash;

import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;

import javax.swing.*;
import java.awt.*;

public class Splash extends JWindow implements JobProgressEventListener {
    static {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int min = 0, max = 1;
    private final JProgressBar progressBar;
    protected ProgressJJob<?> source;

    public Splash(boolean indeterminate, boolean fake) {
        UIDefaults overrides = new UIDefaults();
//        overrides.put("ProgressBar[Enabled].foregroundPainter", new MyPainter(new Color(155, 166, 219)));
        overrides.put("ProgressBar[Enabled].foregroundPainter", new MyPainter(Colors.SplashScreen.PROGRESS_BAR));

        progressBar = new JProgressBar(min, max);
        progressBar.putClientProperty("Nimbus.Overrides", overrides);
        progressBar.putClientProperty("Nimbus.Overrides.InheritDefaults", false);
        progressBar.setStringPainted(true);
        progressBar.setString("Starting SIRIUS...");
        progressBar.setIndeterminate(indeterminate);

        JPanel contentPane = new JPanel();
        this.setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout());
        ImageIcon icon = new ImageIcon(MainFrame.class.getResource("/icons/sirius_splash_scribble.gif"));
        // fake screener is used to init GUI AWT framwork so that JCEF has a canvas to connect to
        if (fake)
            this.setSize(1, 1);
        else
            this.setSize(icon.getIconWidth(), icon.getIconHeight());
        contentPane.add(new JLabel(icon, JLabel.CENTER), BorderLayout.CENTER);
        contentPane.add(progressBar, BorderLayout.SOUTH);

        this.setBackground(Colors.SplashScreen.BACKGROUND);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    @Override
    public void progressChanged(JobProgressEvent progressEvent) {
        if (progressEvent.getSource() != source) {
            source = (ProgressJJob<?>) progressEvent.getSource();
            progressBar.setMaximum(progressBar.getMaximum() + (int) progressEvent.getMaxValue());
            progressBar.setMinimum(progressBar.getMinimum() + (int) progressEvent.getMinValue());
        } else {
            progressBar.setMaximum(progressBar.getMaximum() + ((int) progressEvent.getMaxValue() - max));
            progressBar.setMinimum(progressBar.getMinimum() + ((int) progressEvent.getMinValue() - min));
        }

        max = (int) progressEvent.getMaxValue();
        min = (int) progressEvent.getMinValue();

        if (progressEvent.isDetermined()) {
            progressBar.setIndeterminate(false);
            progressBar.setValue(progressBar.getValue() + (int) progressEvent.getProgress());
        } else {
            progressBar.setIndeterminate(true);
        }

        if (progressEvent.hasMessage())
            progressBar.setString(progressEvent.getMessage());

    }

    class MyPainter implements Painter<JProgressBar> {

        private final Color color;

        public MyPainter(Color c1) {
            this.color = c1;
        }
        @Override
        public void paint(Graphics2D gd, JProgressBar t, int width, int height) {
            gd.setColor(color);
            gd.fillRect(0, 0, width, height);
        }
    }
}