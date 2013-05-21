package de.unijena.bioinf.IsotopePatternAnalysis;


import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.ChargedPeak;
import de.unijena.bioinf.ChemistryBase.ms.utils.ChargedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.*;
import de.unijena.bioinf.MassDecomposer.Chemistry.ChemicalAlphabetWrapper;
import de.unijena.bioinf.MassDecomposer.MassDecomposer;
import de.unijena.bioinf.MassDecomposer.ValenceValidator;
import org.apache.commons.collections.primitives.ArrayIntList;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

public class Main {

    private final static boolean APRIORI = true;
    private final static boolean NO_ODD_RDBE = true;
    private final static boolean ONLY_CHNOPS = false;
    private final static double MISSING_PEAKS_LAMBDA = 50;

    private final static double OFFSET = 0.002;

    private final static double LAMBDA0 = 0.01;
    private final static double LAMBDA1 = 0.9;

    private final static int MAX_PEAK_SIZE = 3;

    private final static int MODE = 2;
    private final static int METHOD = 0; // 0=sirius, 1=pluscal, 2=diff

    public static void main(String... args) {
        /*
        try {
            IsotopicDistribution.setInstance(IsotopicDistribution.loadChemcalc2012());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        */
        final File dir =
                new File("/home/kai/data/custom/Pluskal et al supporting information/ms/");
                //new File("/home/kai/data/ms/MM48_orbitrap_MS2/flo");
        final PatternGenerator gen = new PatternGenerator(PeriodicTable.getInstance().ionByName("[M+H+]+"), Normalization.Max(1));
        final PatternScoreList<ChargedPeak, ChargedSpectrum>scorer = new PatternScoreList<ChargedPeak, ChargedSpectrum>();
        final ChemicalAlphabet alphabet = new ChemicalAlphabet(MolecularFormula.parse(ONLY_CHNOPS ? "CHNOPS" : "CHNOPSFe").elementArray());
        final MassDecomposer<Element> decomposer = new MassDecomposer<Element>(new ChemicalAlphabetWrapper(alphabet));
        if (METHOD == 0 || METHOD==2)   {
            scorer.addScorer(new MassDeviationScorer<ChargedPeak, ChargedSpectrum>(3, 5, 6.5));
        } else if (METHOD == 3) {
            scorer.addScorer(new MassDifferenceDeviationScorer<ChargedPeak, ChargedSpectrum>(3, 5, 6.5));
        }
        if (MISSING_PEAKS_LAMBDA > 0 && METHOD != 1) {
            scorer.addScorer(new MissingPeakScorer<ChargedPeak, ChargedSpectrum>(MISSING_PEAKS_LAMBDA));
        }

        if (METHOD==0) scorer.addScorer(new LogNormDistributedIntensityScorer<ChargedPeak, ChargedSpectrum>(3, LAMBDA0, LAMBDA1));
        if (METHOD==1 || METHOD==3) scorer.addScorer(new PluscalScorer<ChargedPeak, ChargedSpectrum>(Normalization.Sum(1)));
        if (METHOD==2) scorer.addScorer(new IntensityDiffScorer<ChargedPeak, ChargedSpectrum>(
                new LogNormDistributedIntensityScorer<Peak, Spectrum<Peak>>(3, LAMBDA0, LAMBDA1)));


        final NormalDistribution rdbe = new NormalDistribution(6.151312, 4.541604);
        final NormalDistribution het2carb = new NormalDistribution(0.5886335, 0.5550574);
        final NormalDistribution hy2carb = new NormalDistribution(1.435877, 0.4960778);
        /*
        final DecompositionScorer<ArrayList<Object>> AprioriScorer = new VertexScoreList(
                new RDBEVertexScorer(rdbe),
                new Hetero2CarbonVertexScorer(het2carb),
                new Hydrogen2CarbonVertexScorer(hy2carb)
        );
        */
        ArrayList<Object> DUMMY = new ArrayList<Object>();
        DUMMY.add(null); DUMMY.add(null); DUMMY.add(null);

        int rank1 = 0;
        int ranktop3 = 0;
        ArrayIntList averageRanks = new ArrayIntList();
        int num=0;

        for (File f : dir.listFiles()) {
            if (f.getName().endsWith(".ms")) {
                final MsFile file = new MsFile(f);
                final ArrayList<Result> results = new ArrayList<Result>();
                if (ONLY_CHNOPS && (Pattern.compile("[^CHNOPS0-9 ]").matcher(file.compound.formatByHill()).find() || file.compound.getIntMass() > 500)) {
                    continue;
                }
                if (MODE==1) {
                    System.out.println("####### " + f.getName() + " (" + file.compound + ") #######");
                    final SimpleMutableSpectrum q = new SimpleMutableSpectrum(file.spectrum.getNeutralMassSpectrum());
                    Spectrums.normalize(q, Normalization.Sum(100));
                    for (int i=0; i < file.spectrum.size(); ++i) {
                        System.out.println(q.getMzAt(i) + "\t" + q.getIntensityAt(i));
                    }
                    System.out.println("");
                }
                for (int[] compomer : decomposer.decompose(file.spectrum.getPeakAt(0).getNeutralMass(), new Deviation(5, 1e-3)) ) {
                    MolecularFormula form = alphabet.decompositionToFormula(compomer);
                    if (NO_ODD_RDBE && form.doubledRDBE() % 2 != 0) continue;

                    {

                        double apriori = 0d;//APRIORI ? AprioriScorer.score(form, null, null, DUMMY) : 0d;

                        ChargedSpectrum ip;
                        {
                            if (METHOD==1) {
                                SimpleMutableSpectrum t = new SimpleMutableSpectrum(gen.generatePattern(form, 10));
                                Spectrums.normalize(t, Normalization.Max(1));
                                int q=t.size()-1;
                                while (t.getIntensityAt(q) < 0.001) {
                                    t.removePeakAt(q--);
                                }
                                ip = new ChargedSpectrum(t, PeriodicTable.getInstance().ionByName("[M+H+]+"));
                            } else {
                                ChargedSpectrum t =
                                        gen.generatePattern(form, Math.min(MAX_PEAK_SIZE, file.spectrum.size()+1));//
                                //gen.generatePatternWithTreshold(form, 1e-3);
                                ip = t;
                            }
                        }

                        final SimpleMutableSpectrum s = new SimpleMutableSpectrum(file.spectrum);
                        if (METHOD == 0 || METHOD == 3) {
                            while (s.size() > MAX_PEAK_SIZE) s.removePeakAt(s.size()-1);
                            Spectrums.normalize(s, Normalization.Sum(1));
                            Spectrums.addOffset(s, 0, OFFSET);
                        }
                        if (METHOD != 2) Spectrums.normalize(s, Normalization.Sum(1));

                        if (METHOD == 1) {
                            Spectrums.normalize(s, Normalization.Max(1));
                            while (ip.size() > s.size()) {
                                s.addPeak(new Peak(ip.getMzAt(s.size()), 0));
                            }
                            while (s.size() > ip.size()) {
                                s.removePeakAt(s.size()-1);
                            }
                        }

                        final ChargedSpectrum rs = new ChargedSpectrum(s, PeriodicTable.getInstance().ionByName("[M+H+]+"));

                        if (form.equals(file.compound)) {
                            form.toString();
                        }
                        final double[] scores = new double[scorer.getScorers().size()+1];
                        double gesamt = 0d;
                        int k=0;
                        for (IsotopePatternScorer<ChargedPeak, ChargedSpectrum> c : scorer.getScorers()) {
                            scores[k++] = c.score(rs, ip, Normalization.Sum(1));
                            gesamt += scores[k-1];
                        }
                        scores[k] = apriori/4;
                        gesamt += scores[k];
                        results.add(new Result(form, gesamt, scores));
                    }
                }
                Collections.sort(results, Collections.reverseOrder());
                if (MODE==1) {
                    System.out.print("formula\tscore <- [");
                    for (IsotopePatternScorer<ChargedPeak, ChargedSpectrum> c : scorer.getScorers()) {
                        final String[] n = c.getClass().getSimpleName().split("\\.");
                        System.out.print(n[n.length-1] + ", ");
                    }
                    System.out.println("A priori]");
                }
                int k=0;
                boolean found = false;
                for (Result r : results) {
                    ++k;
                    if (MODE==1){
                        System.out.print(r.formula + "\t" + r.score + " <- " + Arrays.toString(r.scores));
                    }
                    if (r.formula.equals(file.compound)) {
                        if(MODE==1) {
                            System.out.println("\t\t********");
                            System.out.println((results.size() - k) + " further decompositions");
                        } else if (MODE == 2) {
                            System.out.println(f.getName() + "\t\t" + k + " / " + results.size() + " (score: " + r.score + " / " + results.get(0).score + ")");
                        }
                        found=true;
                        break;
                    }
                    if (MODE==1)System.out.println("");
                }
                if (found) {
                    if (k == 1) ++rank1;
                    if (k <= 3) ++ranktop3;
                    averageRanks.add(k);
                    ++num;
                }
                if (MODE == 2 && !found) {
                    System.out.print(f.getName());
                    System.out.flush();
                    System.out.println("\t\t" + "nowhere / " + results.size() + " ( optscore: " + results.get(0).score + ")");
                }
            }
        }
        final int[] ranks = averageRanks.toArray();
        Arrays.sort(ranks);

        System.out.println("\n###########\nFound " + num + " solutions\nRank 1: " + rank1 + "\nTop 3: " + ranktop3 + "\nAverage rank: " + ranks[ranks.length/2]);
    }

    public static void main2(String... args) {
        final File dir =
                new File("/home/kai/data/ms/MM48_orbitrap_MS2/flo");
        final PatternGenerator gen = new PatternGenerator(PeriodicTable.getInstance().ionByName("[M+H+]+"), Normalization.Sum(1d));
        for (File f : dir.listFiles()) {
            if (f.getName().endsWith(".ms")) {
                final MsFile file = new MsFile(f);
                final Spectrum<?> q = gen.generatePattern(file.compound, file.spectrum.size()+1);
                final SimpleMutableSpectrum s = new SimpleMutableSpectrum(file.spectrum);
                Spectrums.normalize(s, Normalization.Sum(1));
                System.out.println(s.getIntensityAt(s.size()-1) + "," + q.getIntensityAt(s.size()));
            }
        }
    }

    private static class Result implements Comparable<Result> {
        private MolecularFormula formula;
        private double score;
        private double[] scores;

        private Result(MolecularFormula formula, double score, double[] scores) {
            this.formula = formula;
            this.score = score;
            this.scores = scores;
        }

        @Override
        public int compareTo(Result o) {
            return Double.compare(score, o.score);
        }
    }

    public static class MsFile {
        ChargedSpectrum spectrum;
        MolecularFormula compound;

        private MsFile(File in) {
            try {
                final BufferedReader r = new BufferedReader(new FileReader(in));
                final SimpleMutableSpectrum s = new SimpleMutableSpectrum();
                boolean read = false;
                final Pattern num = Pattern.compile("^\\d");
                while (r.ready()) {
                    final String line = r.readLine();
                    if (read && num.matcher(line).find()) {
                        String[] xs = line.split("\\s+");
                        s.addPeak(new Peak(Double.parseDouble(xs[0]), Double.parseDouble(xs[1])));
                    } else if (line.startsWith(">ms1peaks")) {
                        read = true;
                    } else if (line.startsWith(">col")) {
                        read=false;
                    } else if (line.startsWith(">formula")) {
                        String[] xs = line.split("\\s+");
                        compound = MolecularFormula.parse(xs[1]);
                    }
                }
                Spectrums.sortSpectrumByMass(s);
                spectrum = new ChargedSpectrum(s, PeriodicTable.getInstance().ionByName("[M+H+]+"));
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

    }

}
