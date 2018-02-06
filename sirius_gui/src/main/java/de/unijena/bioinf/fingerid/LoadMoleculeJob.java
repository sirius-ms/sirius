package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.jjobs.BasicJJob;

public class LoadMoleculeJob extends BasicJJob<Boolean> {

    protected Compound[] compounds;

    public LoadMoleculeJob(Compound[] compounds) {
        super(JobType.CPU);
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
