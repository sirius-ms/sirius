package de.unijena.bioinf.ms.cli;

public class FingeridApplication {
    public static void main(String[] args) {
        final FingeridCLI<FingerIdOptions> cli = new FingeridCLI<>();
        cli.parseArgsAndInit(args, FingerIdOptions.class);
        cli.compute();
    }
}
