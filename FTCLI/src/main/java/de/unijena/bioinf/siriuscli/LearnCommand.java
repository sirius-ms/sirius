package de.unijena.bioinf.siriuscli;

import de.unijena.bioinf.FTAnalysis.FTLearn;

/**
 * Created with IntelliJ IDEA.
 * User: kaidu
 * Date: 03.07.13
 * Time: 19:22
 * To change this template use File | Settings | File Templates.
 */
public class LearnCommand implements Command {
    @Override
    public String getDescription() {
        return "Learn a profile (and all scoring parameters) from reference measurements";
    }

    @Override
    public String getName() {
        return "learn";
    }

    @Override
    public void run(String[] args) {
        FTLearn.main(args);
    }

    @Override
    public String getVersion() {
        return FTLearn.VERSION_STRING;
    }
}
