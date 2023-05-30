package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaPacker;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaSet;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import gnu.trove.procedure.TObjectDoubleProcedure;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class DBPairedScorer implements FragmentScorer<MolecularFormula>, Parameterized {

    public static void main(String[] args) {
        final FormulaConstraints constraints = new FormulaConstraints("CHNOPSClBrBIFSe");
        try {
            final MolecularFormulaPacker packer = MolecularFormulaPacker.newPacker(constraints.getChemicalAlphabet());
            final List<MolecularFormula> formulas = new ArrayList<>();
            final MolecularFormulaSet set = new MolecularFormulaSet(constraints.getChemicalAlphabet());
            for (String line : FileUtils.readLines(new File("/home/kaidu/temp/bioformulas.csv"))) {
                MolecularFormula f = MolecularFormula.parse(line);
                set.add(f);
            }
            System.out.println(set.numberOfCompressedFormulas() + " compressed formulas vs " + set.numberOfUncompressedFormulas() + " uncompressed.");
            try (final OutputStream out = FileUtils.getOut(new File("/home/kaidu/temp/bioformulas.bin.gz"))) {
                set.store(out);
            }

        } catch (IOException | UnknownElementException e) {
            throw new RuntimeException(e);
        }
    }

    private final static MolecularFormulaSet formulaSet;
    private double score = 1d;

    static {
        try {
            formulaSet = MolecularFormulaSet.load(new GZIPInputStream(DBPairedScorer.class.getResourceAsStream("/bioformulas.bin.gz")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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
        if (prepared!=null && formulaSet.contains(graphFragment.getFormula()) && formulaSet.contains(prepared)) {
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
