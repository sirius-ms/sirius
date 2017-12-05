package de.unijena.bioinf.ms.cli;

public class SiriusApplication {
    public static void main(String[] args) {
        final ZodiacCLI<ZodiacOptions> cli = new ZodiacCLI<>();
        cli.parseArgsAndInit(args, ZodiacOptions.class);
        cli.compute();
    }
}
