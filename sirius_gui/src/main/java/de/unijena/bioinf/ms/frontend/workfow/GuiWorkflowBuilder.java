package de.unijena.bioinf.ms.frontend.workfow;

import de.unijena.bioinf.ms.frontend.subtools.middleware.MiddlewareAppOptions;
import de.unijena.bioinf.ms.frontend.workflow.InstanceBufferFactory;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.gui.GuiAppOptions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class GuiWorkflowBuilder<R extends RootOptions<?, PreprocessingJob<? extends ProjectSpaceManager>,?>> extends MiddlewareWorkflowBuilder<R> {

    public GuiWorkflowBuilder(@NotNull R rootOptions, @NotNull DefaultParameterConfigLoader configOptionLoader, InstanceBufferFactory<?> bufferFactory) throws IOException {
        super(rootOptions, configOptionLoader,bufferFactory);
    }

    @Override
    protected Object[] standaloneTools() {
        ArrayList<Object> it = new ArrayList<>(Arrays.asList(super.standaloneTools()));
        it.add(new GuiAppOptions());
        //todo readd rest option if boot stuff works
        return it.stream().filter(opt -> !opt.getClass().equals(MiddlewareAppOptions.class)).toArray(Object[]::new);
    }
}
