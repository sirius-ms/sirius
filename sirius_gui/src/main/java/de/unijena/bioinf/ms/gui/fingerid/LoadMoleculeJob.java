package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.jjobs.BasicJJob;

import java.util.Arrays;

public class LoadMoleculeJob extends BasicJJob<Boolean> {

    protected Iterable<FingerprintCandidatePropertyChangeSupport> compounds;

    public LoadMoleculeJob(Iterable<FingerprintCandidatePropertyChangeSupport> compounds) {
        this(JobPriority.NOW, compounds);
    }

    public LoadMoleculeJob(FingerprintCandidatePropertyChangeSupport... compounds) {
        this(JobPriority.NOW, compounds);
    }

    public LoadMoleculeJob(JobPriority prio, FingerprintCandidatePropertyChangeSupport... compounds) {
        this(prio, Arrays.asList(compounds));
    }

    public LoadMoleculeJob(JobPriority prio, Iterable<FingerprintCandidatePropertyChangeSupport> compounds) {
        super(JobType.CPU);
        setPriority(prio);
        this.compounds = compounds;
    }

    @Override
    protected Boolean compute() throws Exception {
        for (FingerprintCandidatePropertyChangeSupport c : compounds) {
            c.getMolecule();
        }
        return true;
    }
}
