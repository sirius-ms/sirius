package de.unijena.bioinf.ms.gui.utils;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Input verifier that colors the component pink and overrides its tooltip with an error message.
 * An instance of this verifier should be added to only one JComponent.
 */
public abstract class ErrorReportingInputVerifier extends InputVerifier {

    private String originalTooltip;
    private Color originalBackground;

    private void initComponent(JComponent component) {
        originalTooltip = component.getToolTipText();
        originalBackground = component.getBackground();
    }

    /**
     * Verify the input and produce a suitable error message
     * @return error message or null if there are no errors
     */
    public abstract String getErrorMessage(JComponent input);

    @Override
    public boolean verify(JComponent input) {
        return getErrorMessage(input) == null;
    }

    @Override
    public boolean shouldYieldFocus(JComponent source, JComponent target) {
        if (originalBackground == null) {
            initComponent(source);
        }
        String error = getErrorMessage(source);
        if (error != null) {
            source.setBackground(Color.PINK);
            source.setToolTipText(error);
        } else if (!Objects.equals(originalTooltip, source.getToolTipText())) {
            source.setBackground(originalBackground);
            source.setToolTipText(originalTooltip);
        }

        return true;
    }
}
