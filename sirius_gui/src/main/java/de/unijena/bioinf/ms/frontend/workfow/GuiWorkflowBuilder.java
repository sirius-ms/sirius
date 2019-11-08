package de.unijena.bioinf.ms.frontend.workfow;

import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.gui.GuiAppOptions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class GuiWorkflowBuilder<R extends RootOptions> extends MiddlewareWorkflowBuilder<R> {

    public GuiWorkflowBuilder(@NotNull R rootOptions, @NotNull DefaultParameterConfigLoader configOptionLoader) throws IOException {
        super(rootOptions, configOptionLoader);
    }

    @Override
    protected Object[] singletonTools() {
        ArrayList<Object> it = new ArrayList<>(Arrays.asList(super.singletonTools()));
        it.add(new GuiAppOptions());
        return it.toArray(Object[]::new);
    }
}
