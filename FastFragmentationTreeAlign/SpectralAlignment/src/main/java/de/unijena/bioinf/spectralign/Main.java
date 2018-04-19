/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.spectralign;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.ms.JenaMsParser;

import java.io.*;

public class Main {

    public static void main(String[] argv) {
        if (argv.length > 2) {
            processTwoDatasets(new SpectralAligner(), new File(argv[0]), new File(argv[1]), new File(argv[2]));
        } else {
            process(new SpectralAligner(), new File(argv[0]), new File(argv[1]));
        }
    }

    public static void processTwoDatasets(SpectralAligner aligner, File dir1, File dir2, File output) {
        try {
            final BufferedWriter out = new BufferedWriter(new FileWriter(output));
            final File[] input1 = dir1.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".ms");
                }
            });
            final File[] input2 = dir2.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".ms");
                }
            });
            final Ms2Experiment[] inputs1 = new Ms2Experiment[input1.length];
            final Ms2Experiment[] inputs2 = new Ms2Experiment[input2.length];
            final String[] rowNames = new String[input1.length];
            out.write("\"scores\"");
            for (int i = 0; i < input1.length; ++i) {
                final Ms2Experiment exp = new GenericParser<Ms2Experiment>(new JenaMsParser()).parseFile(input1[i]);
                inputs1[i] = exp;
                rowNames[i] = removeExt(input1[i].getName());
            }
            for (int i = 0; i < input2.length; ++i) {
                final Ms2Experiment exp = new GenericParser<Ms2Experiment>(new JenaMsParser()).parseFile(input2[i]);
                inputs2[i] = exp;
                out.write(",\"");
                out.write(removeExt(input2[i].getName()));
                out.write('"');
            }
            out.newLine();
            final double[][] scores = new double[inputs1.length][inputs2.length];
            final int N = Math.max(1, Runtime.getRuntime().availableProcessors());
            if (N == 1) {
                singleThreadedAlignment(inputs1, inputs2, scores, aligner);
            } else {
                multiThreadedAlignment(inputs1, inputs2, scores, aligner, N);
            }
            for (int i = 0; i < inputs1.length; ++i) {
                out.write(q(rowNames[i]));
                for (int j = 0; j < input2.length; ++j) {
                    out.write(",");
                    out.write(String.valueOf(scores[i][j]));
                }
                out.newLine();
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public static void process(SpectralAligner aligner, File dir, File output) {
        try {
            final BufferedWriter out = new BufferedWriter(new FileWriter(output));
            final File[] input = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".ms");
                }
            });
            final Ms2Experiment[] inputs = new Ms2Experiment[input.length];
            out.write("\"scores\"");
            for (int i = 0; i < input.length; ++i) {
                final Ms2Experiment exp = new GenericParser<Ms2Experiment>(new JenaMsParser()).parseFile(input[i]);
                inputs[i] = exp;
                out.write(",\"");
                out.write(removeExt(input[i].getName()));
                out.write('"');
            }
            out.newLine();
            final double[][] scores = new double[inputs.length][inputs.length];
            final int N = Math.max(1, Runtime.getRuntime().availableProcessors());
            if (N == 1) {
                singleThreadedAlignment(inputs, inputs, scores, aligner);
            } else {
                multiThreadedAlignment(inputs, inputs, scores, aligner, N);
            }
            for (int i = 0; i < inputs.length; ++i) {
                out.write(q(input[i]));
                for (int j = 0; j < inputs.length; ++j) {
                    out.write(",");
                    out.write(String.valueOf(scores[i][j]));
                }
                out.newLine();
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private static void multiThreadedAlignment(Ms2Experiment[] inputsLeft, Ms2Experiment[] inputsRight, double[][] scores, SpectralAligner aligner, int N) {
        final Worker[] workers = new Worker[N];
        for (int i = 0; i < N; ++i) {
            workers[i] = new Worker(aligner, inputsLeft, inputsRight, scores, i, N);
        }
        for (int i = 0; i < N; ++i) {
            workers[i].shutdown();
        }
        if (inputsLeft == inputsRight) {
            for (int i = 0; i < scores.length; ++i) {
                for (int j = 0; j < i; ++j) {
                    scores[i][j] = scores[j][i];
                }
            }
        }
    }

    private static final class Worker implements Runnable {
        private double[] row;
        private transient double[][] scores;
        private Thread thread;
        private SpectralAligner aligner;
        private final int k, N;
        private final boolean symetric;
        private final Ms2Experiment[] inputsLeft, inputsRight;
        private final Spectrum<Peak>[] specLeft, specRight;

        private Worker(SpectralAligner aligner, Ms2Experiment[] inputsLeft, Ms2Experiment[] inputsRight, double[][] scores, int k, int N) {
            this.aligner = aligner;
            row = new double[inputsRight.length];
            this.inputsLeft = inputsLeft;
            this.inputsRight = inputsRight;
            this.symetric = (inputsLeft == inputsRight);
            this.specLeft = preprocess(aligner, inputsLeft);
            if (symetric) this.specRight = this.specLeft;
            else this.specRight = preprocess(aligner, inputsRight);
            this.scores = scores;
            this.k = k;
            this.N = N;
            thread = new Thread(this);
            thread.start();
        }

        @Override
        public void run() {
            if (symetric) {
                for (int i = k; i < inputsLeft.length; i += N) {
                    row[i] = 1d;
                    for (int j = i + 1; j < inputsLeft.length; ++j) {
                        row[j] = aligner.align(specLeft[i], specRight[j]);
                    }
                    System.arraycopy(row, i, scores[i], i, row.length - i);
                }
            } else {
                for (int i = k; i < inputsLeft.length; i += N) {
                    for (int j = 0; j < inputsRight.length; ++j) {
                        row[j] = aligner.align(specLeft[i], specRight[j]);
                    }
                    System.arraycopy(row, i, scores[i], i, row.length - i);
                }
            }
        }

        private void shutdown() {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void singleThreadedAlignment(Ms2Experiment[] inputsLeft, Ms2Experiment[] inputsRight, double[][] scores, SpectralAligner aligner) {
        if (inputsLeft == inputsRight) {
            final Spectrum<Peak>[] leftSpec = preprocess(aligner, inputsLeft);
            for (int i = 0; i < inputsLeft.length; ++i) {
                scores[i][i] = 1d;
                for (int j = i + 1; j < inputsLeft.length; ++j) {
                    scores[i][j] = aligner.align(leftSpec[i], leftSpec[j]);
                    scores[j][i] = scores[i][j];
                }
            }
        } else {
            final Spectrum<Peak>[] leftSpec = preprocess(aligner, inputsLeft);
            final Spectrum<Peak>[] rightSpec = preprocess(aligner, inputsRight);
            for (int i = 0; i < inputsLeft.length; ++i) {
                for (int j = 0; j < inputsRight.length; ++j) {
                    scores[i][j] = aligner.align(leftSpec[i], rightSpec[j]);
                }
            }
        }
    }

    private static Spectrum<Peak>[] preprocess(SpectralAligner aligner, Ms2Experiment[] inputs) {
        final Spectrum<Peak>[] spectra = new Spectrum[inputs.length];
        for (int k = 0; k < inputs.length; ++k) {
            spectra[k] = aligner.preprocessExperiment(new MutableMs2Experiment(inputs[k]));
        }
        return spectra;
    }

    private static String q(String name) {
        return "\"" + name + "\"";
    }

    private static String q(File f) {
        return q(removeExt(f.getName()));
    }

    private static String removeExt(String s) {
        return s.substring(0, s.lastIndexOf('.'));
    }

}
