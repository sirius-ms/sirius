package de.unijena.bioinf.ms.frontend.workfow;

import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainJob;
import de.unijena.bioinf.ms.frontend.workflow.InstanceBufferFactory;
import de.unijena.bioinf.ms.frontend.workflow.SimpleInstanceBuffer;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

public class GuiInstanceBufferFactory implements InstanceBufferFactory<SimpleInstanceBuffer> {
    /**
     * Here we can Control which jobs should be visible in the Job Panel
     * @param bufferSize
     * @param instances
     * @param tasks
     * @param dependJob
     * @return
     */


    @Override
    public SimpleInstanceBuffer create(int bufferSize, @NotNull Iterator<? extends Instance> instances, @NotNull List<InstanceJob.Factory<?>> tasks, @Nullable DataSetJob dependJob) {
        return new SimpleInstanceBuffer(bufferSize, instances, tasks, dependJob, new JobSubmitter() {
            @Override
            public <Job extends JJob<Result>, Result> Job submitJob(Job j) { //todo what do we want to show here?
                if (j instanceof ToolChainJob) {
                    final String jobType = ((ToolChainJob<?>) j).getToolName();
                    Jobs.submit((ProgressJJob<?>)j, j.identifier(), jobType);
                    return j;
                } else {
                    return Jobs.MANAGER.submitJob(j);
                }
            }
        });
    }
}
