package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.jjobs.BasicJJob;

public class LoadMoleculeJob extends BasicJJob<Boolean> {

    protected Compound[] compounds;

    public LoadMoleculeJob(Compound[] compounds) {
        this(compounds, JobPriority.HIGH);
    }

    public LoadMoleculeJob(Compound[] compounds, JobPriority prio) {
        super(JobType.CPU);
        setPriority(prio);
        this.compounds = compounds;
    }

    @Override
    protected Boolean compute() throws Exception {
        for (Compound c : compounds) {
            c.getMolecule();
        }
        return true;
    }
}
