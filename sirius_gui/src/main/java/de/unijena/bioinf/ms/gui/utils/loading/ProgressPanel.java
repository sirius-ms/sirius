package de.unijena.bioinf.ms.gui.utils.loading;

import de.unijena.bioinf.ms.gui.configs.Colors;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public abstract class ProgressPanel<P extends JProgressBar> extends JPanel {
    protected static final String DEFAULT_PROGRESS_STRING = "Loading...";
    @Getter
    @NotNull
    protected final P progressBar;

    protected final JLabel messageLabel;

    public ProgressPanel(@NotNull P progressBar) {
        super(new BorderLayout());
        setOpaque(false);

        this.progressBar = progressBar;
        this.progressBar.setOpaque(false);

        this.messageLabel = new JLabel("");
        this.messageLabel.setOpaque(false);
    }

    public String getMessage() {
        return messageLabel.getText();
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }
}
