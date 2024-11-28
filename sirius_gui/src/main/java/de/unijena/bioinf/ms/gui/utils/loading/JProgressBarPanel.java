package de.unijena.bioinf.ms.gui.utils.loading;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class JProgressBarPanel extends ProgressPanel<JProgressBar> {

    public JProgressBarPanel() {
        this(DEFAULT_PROGRESS_STRING);
    }

    public JProgressBarPanel(@Nullable String progressBarString) {
        this(progressBarString, true);
    }

    public JProgressBarPanel(boolean indeterminateProgress) {
        this(DEFAULT_PROGRESS_STRING, indeterminateProgress);
    }

    public JProgressBarPanel(@Nullable String progressBarString, boolean indeterminateProgress) {
        this(makeProgressBar(progressBarString, indeterminateProgress));
    }

    public JProgressBarPanel(@NotNull JProgressBar progressBar) {
        super(progressBar);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        add(progressBar, BorderLayout.NORTH);

        JPanel progressLabelPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        progressLabelPanel.setOpaque(false);
        progressLabelPanel.add(messageLabel);
        add(progressLabelPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(300, 70));
    }

    private static JProgressBar makeProgressBar(@Nullable String progressBarString, boolean indeterminateProgress){
        JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);

        progressBar.setIndeterminate(indeterminateProgress);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString(progressBarString);
        progressBar.setOpaque(false);
        return progressBar;
    }
}
