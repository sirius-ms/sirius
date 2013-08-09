package de.unijena.bioinf.siriuscli;


import de.unijena.bioinf.fteval.FTEval;

public class EvalCommand implements Command {
    @Override
    public String getDescription() {
        return "Evaluates the performance of SIRIUS on a test dataset";
    }

    @Override
    public String getName() {
        return "fteval";
    }

    @Override
    public void run(String[] args) {
        FTEval.main(args);
    }

    @Override
    public String getVersion() {
        return FTEval.VERSION_STRING;
    }
}