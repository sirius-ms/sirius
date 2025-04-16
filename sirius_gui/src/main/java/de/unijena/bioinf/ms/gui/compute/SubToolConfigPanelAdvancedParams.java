package de.unijena.bioinf.ms.gui.compute;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A subclass of {@link SubToolConfigPanel} that keeps track of "advanced" UI components that can be hidden or shown.
 */
public abstract class SubToolConfigPanelAdvancedParams<C> extends SubToolConfigPanel<C> {
    protected boolean displayAdvancedParameters;
    protected final List<Component> advancedParametersComponents;

    public SubToolConfigPanelAdvancedParams(Class<C> annotatedObject, boolean displayAdvancedParameters) {
        super(annotatedObject);
        this.displayAdvancedParameters = displayAdvancedParameters;
        advancedParametersComponents = new ArrayList<>();
    }

    public SubToolConfigPanelAdvancedParams(Class<C> annotatedObject, boolean displayAdvancedParameters, @NotNull Layout layout) {
        super(annotatedObject, layout);
        this.displayAdvancedParameters = displayAdvancedParameters;
        advancedParametersComponents = new ArrayList<>();
    }

    protected void addAdvancedComponent(Component c) {
        advancedParametersComponents.add(c);
        c.setVisible(displayAdvancedParameters);
    }

    public void setDisplayAdvancedParameters(boolean display) {
        displayAdvancedParameters = display;
        advancedParametersComponents.forEach(c -> c.setVisible(displayAdvancedParameters));
    }
}
