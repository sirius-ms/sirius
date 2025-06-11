package de.unijena.bioinf.ms.gui.webView;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public abstract class BrowserPanel extends JPanel {
    protected LinkInterception linkInterception = LinkInterception.NONE;

    public abstract void submitDataUpdate(String javascript);

    public void updateSelectedFeature(@Nullable String alignedFeatureId) {
        submitDataUpdate(String.format("window.urlUtils.updateSelectedEntity(alignedFeatureID=%s)", parseNullable(alignedFeatureId)));
    }

    public void updateSelectedFormulaCandidate(@Nullable String alignedFeatureId, @Nullable String formulaId) {
        submitDataUpdate(String.format("window.urlUtils.updateSelectedEntity(alignedFeatureID=%s, formulaID=%s)", parseNullable(alignedFeatureId), parseNullable(formulaId)));
    }

    public void updateSelectedStructureCandidate(@Nullable String alignedFeatureId, @Nullable String formulaId, @Nullable String inchiKey) {
        submitDataUpdate(String.format("window.urlUtils.updateSelectedEntity(alignedFeatureID=%s, formulaID=%s, inchikey=%s)", parseNullable(alignedFeatureId), parseNullable(formulaId), parseNullable(inchiKey)));
    }

    public void updateSelectedSpectralMatch(@Nullable String alignedFeatureId, @Nullable String matchId) {
        submitDataUpdate(String.format("window.urlUtils.updateSelectedEntity(alignedFeatureID=%s, undefined, undefined, matchid=%s)", parseNullable(alignedFeatureId), parseNullable(matchId)));
    }

    protected static String parseNullable(@Nullable String s) {
        return s == null || s.isBlank() ? "null" : ("'" + s + "'");
    }

    public abstract void cleanupResources();

    /**
     * Called automatically when the component is being removed from the parent container.
     * This is the proper Swing way to clean up resources when a component is no longer displayed.
     */
    @Override
    public void removeNotify() {
        // Clean up resources before the component is removed
        cleanupResources();
        // Call the superclass implementation to complete normal component removal
        super.removeNotify();
    }

    // Some arbitrary size numbers to fix JSplitPane divider not moving
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(500,300);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(50,20);
    }
}
