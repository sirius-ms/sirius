package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.jjobs.BasicJJob;

import java.util.Arrays;

public class LoadMoleculeJob extends BasicJJob<Boolean> {

    protected Iterable<FingerprintCandidateBean> compounds;

    public LoadMoleculeJob(Iterable<FingerprintCandidateBean> compounds) {
        this(JobPriority.NOW, compounds);
    }

    public LoadMoleculeJob(FingerprintCandidateBean... compounds) {
        this(JobPriority.NOW, compounds);
    }

    public LoadMoleculeJob(JobPriority prio, FingerprintCandidateBean... compounds) {
        this(prio, Arrays.asList(compounds));
    }

    public LoadMoleculeJob(JobPriority prio, Iterable<FingerprintCandidateBean> compounds) {
        super(JobType.CPU);
        setPriority(prio);
        this.compounds = compounds;
    }

    @Override
    protected Boolean compute() throws Exception {
        for (FingerprintCandidateBean c : compounds) {
            c.getMolecule();
        }
        return true;
    }
}
