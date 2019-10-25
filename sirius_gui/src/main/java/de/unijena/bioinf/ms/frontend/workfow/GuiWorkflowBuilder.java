package de.unijena.bioinf.ms.frontend.workfow;

import de.unijena.bioinf.ms.frontend.subtools.GuiAppOptions;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class GuiWorkflowBuilder<R extends RootOptions> extends WorkflowBuilder<R> {
//    public final GuiAppOptions guiAppOptions = new GuiAppOptions();

    public GuiWorkflowBuilder(@NotNull R rootOptions, @NotNull DefaultParameterConfigLoader configOptionLoader) throws IOException {
        super(rootOptions, configOptionLoader);
    }

    @Override
    protected Object[] singletonTools() {
        ArrayList<Object> it = new ArrayList<>(Arrays.asList(super.singletonTools()));
        it.add(new GuiAppOptions());
        return it.toArray(Object[]::new);
//        return Streams.concat(Arrays.stream(new Object[]{guiAppOptions}), Arrays.stream(super.singletonTools())).toArray();
    }
}
