package de.unijena.bioinf.ms.frontend.workfow;

import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.SubToolJob;
import de.unijena.bioinf.ms.frontend.workflow.InstanceBufferFactory;
import de.unijena.bioinf.ms.frontend.workflow.SimpleInstanceBuffer;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

public class GuiInstanceBufferFactory implements InstanceBufferFactory<SimpleInstanceBuffer> {
    @Override
    public SimpleInstanceBuffer create(int bufferSize, @NotNull Iterator<? extends Instance> instances, @NotNull List<InstanceJob.Factory<?>> tasks, @Nullable DataSetJob dependJob) {
        return new SimpleInstanceBuffer(bufferSize, instances, tasks, dependJob, (j) -> {
            if (j instanceof ProgressJJob) {
                final String jobType = (j instanceof SubToolJob) ? ((SubToolJob<?>) j).getToolName() : j.getClass().getSimpleName();
                Jobs.submit((ProgressJJob<?>) j, j.identifier(), jobType);
            } else {
                Jobs.MANAGER.submitJob(j);
            }
        });
    }
}
