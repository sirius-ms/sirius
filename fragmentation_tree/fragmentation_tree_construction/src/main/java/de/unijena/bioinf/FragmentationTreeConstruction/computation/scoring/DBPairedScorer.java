package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaPacker;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaSet;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.plugins.BottomUpSearch;
import gnu.trove.procedure.TObjectDoubleProcedure;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class DBPairedScorer implements FragmentScorer<MolecularFormula>, Parameterized {

    public static void main(String[] args) {
        try {
            final List<MolecularFormula> formulas = new ArrayList<>();
            FormulaConstraints ALL = FormulaConstraints.fromString("HCNOSBrBFClSiIAsSeCuFeMgNaCa");
            //final MolecularFormulaSet set = new MolecularFormulaSet(constraints.getChemicalAlphabet());
            Ionization hplus = PrecursorIonType.getPrecursorIonType("[M + H]+").getIonization();
            for (String line : FileUtils.readLines(new File("/home/kaidu/temp/bioformulas.csv"))) {
                MolecularFormula f = MolecularFormula.parse(line);
                if (ALL.isSatisfied(f, hplus))
                    formulas.add(f);
            }

            for (String[] tab : FileUtils.readTable(new File("/home/kaidu/temp/fragments.csv"), true)) {
                int count = Integer.parseInt(tab[3]);
                if (count >= 20) {
                    MolecularFormula f = MolecularFormula.parse(tab[0]);
                    formulas.add(f);
                }
            }
            for (String[] tab : FileUtils.readTable(new File("/home/kaidu/temp/rootlosses.csv"), true)) {
                int count = Integer.parseInt(tab[3]);
                if (count >= 20) {
                    MolecularFormula f = MolecularFormula.parse(tab[0]);
                    formulas.add(f);
                }
            }


            MolecularFormulaMap MAP = new MolecularFormulaMap(formulas);
            try (final OutputStream out = FileUtils.getOut(new File("/home/kaidu/software/sirius/sirius-libs/chemistry_base/src/main/resources//bioformulas.bin.gz"))) {
                ObjectOutputStream oout = new ObjectOutputStream(out);
                oout.writeObject(MAP);
                oout.close();
            }

        } catch (IOException | UnknownElementException e) {
            throw new RuntimeException(e);
        }
    }

    private double score = 1d;

    public DBPairedScorer() {

    }

    @Override
    public MolecularFormula prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
        Optional<Fragment> root = FragmentScorer.getDecompositionRootNode(graph);
        if (root.isEmpty()) {
            LoggerFactory.getLogger(CommonRootLossScorer.class).warn("Cannot score root losses for graph with multiple roots. Rootloss score is set to 0 for this instance.");
            return null;
        }
        return root.get().getFormula();
    }

    @Override
    public double score(Fragment graphFragment, ProcessedPeak correspondingPeak, boolean isRoot, MolecularFormula prepared) {
        if (prepared!=null && BottomUpSearch.MOLECULAR_FORMULA_MAP.contains(graphFragment.getFormula()) && BottomUpSearch.MOLECULAR_FORMULA_MAP.contains(prepared)) {
            return score;
        } else return 0d;
    }


    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        score = document.getDoubleFromDictionary(dictionary, "score");
    }

    @Override
    public <G, D, L> void exportParameters(final ParameterHelper helper, final DataDocument<G, D, L> document, final D dictionary) {
        document.addToDictionary(dictionary,"score", score);
    }
}
