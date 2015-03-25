package de.unijena.bioinf.sirius.cli;

import de.unijena.bioinf.babelms.dot.FTDotWriter;

import java.io.File;

public abstract class OutputHandler {

    public static OutputHandler create(Options options) {
        return new ComputeTreeHandler(options.getFormat(), options.getOutput(), options.getNumberOfTrees()<=1);
    }

    private OutputHandler() {

    }

    public abstract void handle(IdentificationResult result);

    protected static class ComputeTreeHandler extends OutputHandler {

        private static enum Format {dot, json};

        private Format format;
        private File target;
        private boolean single;

        protected ComputeTreeHandler(String format, File target, boolean singleOutput) {
            this.format = format==null ? (singleOutput ? estimateFromFileExtension(target) : Format.dot) : Format.valueOf(format.toLowerCase());
            this.target = target;
            this.single = singleOutput;
        }

        private static Format estimateFromFileExtension(File target) {
            final String name = target.getName();
            final int i = name.lastIndexOf('.');
            if (i >= 0) {
                final String ext = name.substring(i);
                if (ext.equalsIgnoreCase(".dot")) return Format.dot;
                if (ext.equalsIgnoreCase(".json")) return Format.json;
            }
            return Format.dot;
        }

        public void handle(IdentificationResult result) {
            if (single) {
                FTDotWriter writer = new FTDotWriter();
            } else {

            }
        }

    }

}
