package de.unijena.bioinf;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.ms.rest.model.JobId;
import de.unijena.bioinf.ms.rest.model.JobTable;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import de.unijena.bioinf.utils.NetUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

final class WebJobWatcher {
    private final Map<JobId, WebJJob<?, ?, ?>> waitingJobs = new ConcurrentHashMap<>();
    private final WebAPI api;
    private WebJobWatcherJJob job = null;

    //this is for efficient job update even with a large number of jobs on large multi core machines
    public WebJobWatcher(WebAPI api) {
        this.api = api;
    }

    public <J extends WebJJob<?, ?, ?>> J watchJob(@NotNull final J jobToWatch) {
        waitingJobs.put(jobToWatch.jobId, jobToWatch);

        checkWatcherJob();

        synchronized (waitingJobs) {
            waitingJobs.notifyAll();
        }

        return jobToWatch;
    }

    private synchronized void checkWatcherJob() {
        if (job == null || job.isFinished())
            job = SiriusJobs.getGlobalJobManager().submitJob(new WebJobWatcherJJob());
    }

    final class WebJobWatcherJJob extends BasicJJob<Boolean> {

        public WebJobWatcherJJob() {
            super(JobType.WEBSERVICE);
        }

        @Override
        protected Boolean compute() throws Exception {
            long waitTime = 1000;
            while (true) {
                checkForInterruption();

                synchronized (waitingJobs) {
                    if (waitingJobs.isEmpty())
                        waitingJobs.wait();
                }

                final Set<JobId> orphanJobs = new HashSet<>(waitingJobs.keySet());
                EnumMap<JobTable, List<JobUpdate<?>>> updates = NetUtils.tryAndWait(() -> {
                    checkForInterruption();
                    return api.updateJobStates(waitingJobs.keySet().stream().map(id -> id.jobTable).collect(Collectors.toSet()));
                });
                List<JobUpdate<?>> toRemove = null;


                if (updates != null && !updates.isEmpty()) {
                    //update, find orphans and notify finished jobs
                    toRemove = updates.values().stream().flatMap(Collection::stream).filter(up -> {
                        try {
                            checkForInterruption();

                            final WebJJob<?, ?, ?> job;
                            orphanJobs.remove(up.getGlobalId());
                            job = waitingJobs.get(up.getGlobalId());

                            if (job == null) {
                                LOG().warn("Job \"" + up.getJobId() + "\" was found on the server but is unknown locally. Trying to match it again later!");
                                return false;
                            }

                            return job.update(up).isFinished();
                        } catch (Exception e) {
                            LOG().warn("Could not update Job", e);
                            return false;
                        }
                    }).collect(Collectors.toList());

                    checkForInterruption();

                    // crash and notify orphan jobs
                    orphanJobs.stream().map(waitingJobs::get).forEach(j -> {
                        if (!j.isFinished()) {
                            j.crash(new Exception("Job not found on Server. It might have been deleted due to an Error."));
                        } else
                            LOG().warn("Already finished job is missing on Server. This indicates some Synchronization problem");
                    });

                    checkForInterruption();

                    orphanJobs.addAll(toRemove.stream().map(JobUpdate::getGlobalId).collect(Collectors.toSet()));

                    checkForInterruption();

                    if (!orphanJobs.isEmpty()) {
                        orphanJobs.forEach(waitingJobs::remove);
                        // not it sync because it may take some time and is not needed since jobwatcher is single threaded
                        NetUtils.tryAndWait(() -> {
                            checkForInterruption();
                            api.deleteJobs(orphanJobs);
                            return true;
                        });
                    }
                } else {
                    LOG().warn("Cannot fetch jobUpdates from CSI:FingerID Server. Trying again in " + waitTime + "ms.");
                }

                checkForInterruption();
                // if nothing was finished increase waiting time
                // else set back to normal for fast reaction times
                if (toRemove == null || toRemove.isEmpty()) {
                    waitTime = Math.min(waitTime * 2, 30000);
                    LOG().info("No CSI:FingerID prediction jobs finished. Try again in " + waitTime / 1000d + "s");
                } else {
                    waitTime = 1000;
                }

                Thread.sleep(waitTime);
            }
        }
    }
}
