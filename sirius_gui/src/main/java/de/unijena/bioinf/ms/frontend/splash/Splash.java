package de.unijena.bioinf.ms.frontend.splash;

import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

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

    public Splash() {
        UIDefaults overrides = new UIDefaults();
        overrides.put("ProgressBar[Enabled].foregroundPainter", new MyPainter(new Color(155, 166, 219)));

        progressBar = new JProgressBar(min, max);
        progressBar.putClientProperty("Nimbus.Overrides", overrides);
        progressBar.putClientProperty("Nimbus.Overrides.InheritDefaults", false);
        progressBar.setStringPainted(true);
        progressBar.setString("Starting SIRIUS...");

        JPanel contentPane = new JPanel();
        this.setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout());
        ImageIcon icon = Icons.SPLASH;
        this.setSize(icon.getIconWidth(), icon.getIconHeight());
        contentPane.add(new JLabel(icon, JLabel.CENTER), BorderLayout.CENTER);
        contentPane.add(progressBar, BorderLayout.SOUTH);

        this.setBackground(Color.WHITE);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    @Override
    public void progressChanged(JobProgressEvent progressEvent) {
        if (progressEvent.getSource() != source) {
            source = progressEvent.getSource();
            progressBar.setMaximum(progressBar.getMaximum() + progressEvent.getMaxValue());
            progressBar.setMinimum(progressBar.getMinimum() + progressEvent.getMinValue());
        } else {
            progressBar.setMaximum(progressBar.getMaximum() + (progressEvent.getMaxValue() - max));
            progressBar.setMinimum(progressBar.getMinimum() + (progressEvent.getMinValue() - min));
        }

        max = progressEvent.getMaxValue();
        min = progressEvent.getMinValue();

        if (progressEvent.isDetermined()) {
            progressBar.setIndeterminate(false);
            progressBar.setValue(progressBar.getValue() + (progressEvent.getNewValue() - Optional.ofNullable(progressEvent.getOldValue()).orElse(0)));
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