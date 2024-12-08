package de.unijena.bioinf.ms.gui.utils.loading;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import raven.swing.spinner.SpinnerProgress;

import javax.swing.*;
import java.awt.*;

public class SpinnerProgressPanel extends ProgressPanel<SpinnerProgress> {

    public SpinnerProgressPanel() {
        this(DEFAULT_PROGRESS_STRING);
    }

    public SpinnerProgressPanel(@Nullable String progressBarString) {
        this(progressBarString, true);
    }

    public SpinnerProgressPanel(boolean indeterminateProgress) {
        this(DEFAULT_PROGRESS_STRING, indeterminateProgress);
    }

    public SpinnerProgressPanel(@Nullable String progressBarString, boolean indeterminateProgress) {
        this(null, progressBarString, indeterminateProgress);
    }

    public SpinnerProgressPanel(@Nullable FlatSVGIcon filterIcon, @Nullable String progressBarString, boolean indeterminateProgress) {
        this(makeProgressBar(filterIcon, progressBarString, indeterminateProgress));
    }

    public SpinnerProgressPanel(@NotNull SpinnerProgress progressBar) {
        super(progressBar);
//      Create a wrapper panel to hold the fixed-size panel
        JPanel wrapperPanel = new JPanel(new GridBagLayout());
        wrapperPanel.add(progressBar); // Add fixed-size panel to the center of the wrapper
        wrapperPanel.setOpaque(false);

        add(wrapperPanel, BorderLayout.CENTER);
        add(messageLabel, BorderLayout.SOUTH);
    }

    public static SpinnerProgress makeProgressBar(@Nullable FlatSVGIcon filterIcon, @Nullable String progressBarString, boolean indeterminateProgress){
        SpinnerProgress spinner = filterIcon == null ? new SpinnerProgress() : new SpinnerProgress(filterIcon);
        spinner.setStringPainted(true);
        spinner.setString(progressBarString);
        spinner.setIndeterminate(indeterminateProgress);
        spinner.setPreferredSize(new Dimension(128, 128));
        spinner.setOpaque(false);
        return spinner;
    }

}
