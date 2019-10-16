package de.unijena.bioinf.ms.frontend.workfow;

import com.google.common.collect.Streams;
import de.unijena.bioinf.ms.frontend.subtools.GuiOptions;
import de.unijena.bioinf.ms.frontend.subtools.RootOptionsCLI;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;

public class GuiWorkflowBuilder<R extends RootOptionsCLI> extends WorkflowBuilder<R> {
    public final GuiOptions guiOptions = new GuiOptions();

    public GuiWorkflowBuilder(@NotNull R rootOptions, @NotNull DefaultParameterConfigLoader configOptionLoader) throws IOException {
        super(rootOptions, configOptionLoader);
    }

    @Override
    protected Object[] singletonTools() {
        return Streams.concat(Arrays.stream(new Object[]{guiOptions}), Arrays.stream(super.singletonTools())).toArray();
    }
}
