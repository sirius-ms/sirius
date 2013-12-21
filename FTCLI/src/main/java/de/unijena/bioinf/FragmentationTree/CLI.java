package de.unijena.bioinf.FragmentationTree;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class CLI {

    enum Algorithm {HIRSCHBERG, LOCAL, GLOBAL};

    interface Options {
        @Option(shortName = "a", description = "algorithm to choose from. Either HIRSCHBERG, LOCAL or GLOBAL",
                defaultValue = "LOCAL")
        public Algorithm getAlgorithm();

        @Option(shortName = "s", description = "file containing the scoring matrix. " +
                "If not given, unit costs are used", defaultToNull = true)
        public File getScoring();

        @Unparsed(description = "two fasta files to align", exactly = 2)
        public List<File> getInputFiles();
    }

/*
    enum Algorithm {NAIVE, KMP, BM}

    interface Options {
        @Option(shortName = "a", description = "algorithm to choose from. Either NAIVE, KMP or BM",
                defaultValue = "NAIVE")
        public Algorithm getAlgorithm();

        @Option(shortName = "p", description = "pattern to search")
        public String getPattern();

        @Option(shortName = "t", description = "translate sequence before searching")
        public boolean isTranslate();

        @Option(shortName = "h", helpRequest = true)
        public boolean isHelp();

    }

    public static void main(String[] args) {

        Options options;
        try {
            options = CliFactory.parseArguments(Options.class, args);
        } catch (ArgumentValidationException e) {
            System.err.println(e.getMessage());
            System.exit(1); return;
        }

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        StringBuilder buffer = new StringBuilder();
        String line;
        try {
            while ((line = input.readLine()) != null) buffer.append(line).append('\n');
        } catch (IOException e) {
            System.err.println("Error while reading input file: " + e.getMessage());
            System.exit(1);
        }
        String searchText = buffer.toString();
        if (options.isTranslate()) searchText = translate(searchText);
        String pattern = options.getPattern();
        switch (options.getAlgorithm()) {
            case NAIVE: searchNaive(searchText, pattern); break;
            case KMP: searchKMP(searchText, pattern); break;
            case BM: searchBM(searchText, pattern); break;
        }
    }

    private static void searchBM(String searchText, String pattern) {

    }

    private static void searchKMP(String searchText, String pattern) {

    }

    private static void searchNaive(String searchText, String pattern) {

    }

    private static String translate(String pattern) {
        return null;
    }
    */

}
