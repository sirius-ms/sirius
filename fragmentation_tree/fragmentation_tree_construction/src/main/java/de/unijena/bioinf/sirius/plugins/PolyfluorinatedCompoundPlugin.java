package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.*;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

import java.util.Arrays;
import java.util.List;

public class PolyfluorinatedCompoundPlugin extends SiriusPlugin {

    @Override
    public void initializePlugin(PluginInitializer initializer) {
        initializer.getAnalysis().addScoringWrapper(PFASScoringWrapper::new);
    }

    protected static class PFASScoringWrapper extends FragmentationGraphScoringWrapper {
        /**
         * The PFAS scoring assign F-containing formulas a score that is always equal or larger than the
         * same formula with F->H substitute. To compensate for this advantage (and avoid that we favour F formulas
         * over non-F formulas) we add this small score penalty.
         */
        private final double SCORE_PENALTY = -1f;

        private final Element F = PeriodicTable.getInstance().getByName("F");
        private final Element H = PeriodicTable.getInstance().getByName("H");
        public PFASScoringWrapper(FragmentationGraphScoringWrapper parent) {
            super(parent);
        }


        @Override
        public double sumOflossScores(List<LossScorer> scorers, Loss l, ProcessedInput p, Object[] prepared, Tracker<LossScorer> tracker) {
            if (l.getFormula().numberOf(F)>0) {
                final double score1 = parent.sumOflossScores(scorers, l, p, prepared, null);
                // dangerous, but should be fine here
                MolecularFormula oldFormula = l.getFormula();
                MolecularFormula newFormula = oldFormula.substitute(F, H);
                l.setFormula(newFormula);
                final double score2 = parent.sumOflossScores(scorers, l, p, prepared, null) + SCORE_PENALTY;
                if (tracker!=null) {
                    // ouch
                    if (score2>score1) {
                        l.setFormula(newFormula);
                        parent.sumOflossScores(scorers, l, p, prepared, (n,sc)->tracker.scored(n,sc+SCORE_PENALTY/scorers.size()));
                    }else{
                        l.setFormula(oldFormula);
                        parent.sumOflossScores(scorers, l, p, prepared, tracker);
                    }
                }
                l.setFormula(oldFormula);
                return Math.max(score1,score2);
            } else return parent.sumOflossScores(scorers,l,p,prepared, tracker);
        }

        @Override
        public double sumOfFragmentScores(List<FragmentScorer> scorers, Fragment f, ProcessedPeak p, boolean isRoot, Object[] prepared, Tracker<FragmentScorer> tracker) {
            if (f.getFormula().numberOf(F)>0) {
                final double score1 = parent.sumOfFragmentScores(scorers, f, p, isRoot, prepared,null);
                // dangerous, but should be fine here
                MolecularFormula oldFormula = f.getFormula();
                Ionization oldIonization = f.getIonization();
                MolecularFormula newFormula = oldFormula.substitute(F, H);
                f.setFormula(newFormula, oldIonization);
                final double score2 = parent.sumOfFragmentScores(scorers, f, p, isRoot, prepared,null) + SCORE_PENALTY;
                if (tracker!=null) {
                    // ouch
                    if (score2>score1) {
                        f.setFormula(newFormula,oldIonization);
                        parent.sumOfFragmentScores(scorers, f, p, isRoot, prepared, (n,sc)->tracker.scored(n,sc+SCORE_PENALTY/scorers.size()));
                    }else{
                        f.setFormula(oldFormula,oldIonization);
                        parent.sumOfFragmentScores(scorers, f, p, isRoot, prepared,tracker);
                    }
                }
                f.setFormula(oldFormula, oldIonization);
                return Math.max(score1,score2);
            } else return parent.sumOfFragmentScores(scorers, f, p, isRoot, prepared,tracker);
        }

        @Override
        public double sumOfDecompositionScores(List<DecompositionScorer> scorers, MolecularFormula formula, Ionization ionization, ProcessedPeak peak, ProcessedInput input, Object[] prepared, Tracker<DecompositionScorer> tracker) {
            if (formula.numberOf(F)>0) {
                final double score1 = parent.sumOfDecompositionScores(scorers, formula, ionization, peak, input, prepared,null);
                MassDeviationVertexScorer.ENABLE_WARNING=false; // ouch. Übler Hack
                final double score2 = parent.sumOfDecompositionScores(scorers, formula.substitute(F,H), ionization, peak, input, prepared,null) + SCORE_PENALTY;
                if (tracker!=null) {
                    // ouch
                    if (score2>score1) {
                        parent.sumOfDecompositionScores(scorers, formula.substitute(F,H), ionization, peak, input, prepared, (n,sc)->tracker.scored(n,sc+SCORE_PENALTY/scorers.size()));
                    }else{
                        parent.sumOfDecompositionScores(scorers, formula, ionization, peak, input, prepared,tracker);
                    }
                }
                MassDeviationVertexScorer.ENABLE_WARNING=true;
                return Math.max(score1,score2);
            } else return parent.sumOfDecompositionScores(scorers, formula, ionization, peak, input, prepared,tracker);
        }

    }

}
