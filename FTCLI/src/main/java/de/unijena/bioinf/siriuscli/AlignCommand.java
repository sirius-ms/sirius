package de.unijena.bioinf.siriuscli;

public class AlignCommand implements Command {

    public AlignCommand() {
    }

    @Override
    public String getDescription() {
        return "compute alignment scores between fragmentation trees"; // TODO
    }

    @Override
    public String getName() {
        return "align";
    }

    @Override
    public void run(String[] args) {
        de.unijena.bioinf.ftalign.Main.main(args);
    }

    @Override
    public String getVersion() {
        return de.unijena.bioinf.ftalign.Main.VERSION;
    }
}
