package de.unijena.bioinf.sirius.cli;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.FTJsonWriter;

import java.io.File;
import java.io.IOException;

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
            this.format = format==null ? (singleOutput ? estimateFromFileExtension(target) : Format.json) : Format.valueOf(format.toLowerCase());
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
            return Format.json;
        }

        public void handle(IdentificationResult result) {
            if (single) {
                try {
                    if (target.isDirectory()) {
                        final File targetFile = new File(target, result.instance.fileNameWithoutExtension() + "."+format.name());
                        write(target, result.optTree);
                    } else {
                        write(target, result.optTree);
                    }
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                    return;
                }
            } else {
                final File targetDir = target;
                if (!targetDir.isDirectory()) {
                    System.err.println("output path should be a directory name for multiple tree output!");
                    return;
                }
                final File subDir = new File(targetDir, result.instance.fileNameWithoutExtension());
                subDir.mkdir();
                int k=1;
                for (FTree tree : result.optTrees) {
                    final File name = new File(subDir, k + "_" + tree.getRoot().getFormula());
                    try {
                        write(name, tree);
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                    }
                }
            }
        }

        private void write(File target, FTree optTree) throws IOException {
            if (format==Format.dot) new FTDotWriter().writeTreeToFile(target, optTree, null,null,null);
            else if (format==Format.json) new FTJsonWriter().writeTreeToFile(target, optTree);
        }

    }

}
