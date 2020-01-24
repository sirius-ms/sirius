package de.unijena.bioinf.ms.frontend.workfow;

import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.ms.frontend.subtools.middleware.MiddlewareAppOptions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class MiddlewareWorkflowBuilder<R extends RootOptions<PreprocessingJob<? extends ProjectSpaceManager>>> extends WorkflowBuilder<R> {
    public MiddlewareWorkflowBuilder(@NotNull R rootOptions, @NotNull DefaultParameterConfigLoader configOptionLoader) throws IOException {
        super(rootOptions, configOptionLoader);
    }

    @Override
    protected Object[] singletonTools() {
        ArrayList<Object> it = new ArrayList<>(Arrays.asList(super.singletonTools()));
        it.add(new MiddlewareAppOptions());
        return it.toArray(Object[]::new);
    }
}
