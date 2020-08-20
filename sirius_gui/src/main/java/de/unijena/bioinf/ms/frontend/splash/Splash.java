package de.unijena.bioinf.ms.frontend.splash;

import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Icons;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public class Splash extends JWindow implements JobProgressEventListener {
    private int min = 0, max = 1;
    final JProgressBar progressBar = new JProgressBar(min, max);
    ProgressJJob<?> source;

    public Splash() {
        progressBar.setString("Starting SIRIUS...");
        progressBar.setForeground(Colors.DB_LINKED);
        progressBar.setStringPainted(true);


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
}