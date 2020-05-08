package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;

import java.io.IOException;
import java.util.*;

public class InMemoryChemicalDatabase extends AbstractChemicalDatabase {

    protected List<FingerprintCandidate> fingerprintCandidates;
    protected HashMap<MolecularFormula, List<FingerprintCandidate>> candidatesPerFormula;
    protected HashMap<String, FingerprintCandidate> candidatePerKey;
    protected MolecularFormula[] formulas;

    public InMemoryChemicalDatabase(List<FingerprintCandidate> candidates) {
        setCandidates(candidates);
    }

    public void addCandidates(Iterable<FingerprintCandidate> candidates) {
        for (FingerprintCandidate fc : candidates)
            addCandidate(fc);
    }

    public void addCandidate(FingerprintCandidate fc) {
        if (candidatePerKey.put(fc.getInchiKey2D(), fc)==null) {
            final MolecularFormula formula = fc.getInchi().extractFormulaOrThrow();
            if (!candidatesPerFormula.containsKey(formula)) {
                candidatesPerFormula.put(formula, new ArrayList<FingerprintCandidate>());
                this.formulas = Arrays.copyOf(formulas, formulas.length+1);
                this.formulas[formulas.length-1] = formula;
                Arrays.sort(formulas);
            }
            candidatesPerFormula.get(formula).add(fc);
            fingerprintCandidates.add(fc);
        }
    }

    public void setCandidates(List<FingerprintCandidate> candidates) {
        this.candidatePerKey = new HashMap<>();
        for (FingerprintCandidate fc : candidates) {
            candidatePerKey.put(fc.getInchiKey2D(), fc);
        }
        this.fingerprintCandidates = new ArrayList<>(candidatePerKey.values());
        this.candidatesPerFormula = new HashMap<>();
        for (FingerprintCandidate fc : this.fingerprintCandidates) {
            final MolecularFormula formula = fc.getInchi().extractFormulaOrThrow();
            if (!candidatesPerFormula.containsKey(formula)) {
                candidatesPerFormula.put(formula, new ArrayList<FingerprintCandidate>());
            }
            candidatesPerFormula.get(formula).add(fc);
        }
        this.formulas = new MolecularFormula[candidatesPerFormula.keySet().size()];
        int k=0; for (MolecularFormula f : candidatesPerFormula.keySet()) formulas[k++] = f;
        Arrays.sort(formulas);
    }


    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
        final double exactMass = ionType.precursorMassToNeutralMass(mass);
        int index = Arrays.binarySearch(formulas, exactMass, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                double a1, a2;
                if (o1 instanceof MolecularFormula) a1 = ((MolecularFormula)o1).getMass();
                else a1 = (double)o1;
                if (o2 instanceof MolecularFormula) a2 = ((MolecularFormula)o2).getMass();
                else a2 = (double)o2;
                return Double.compare(a1,a2);
            }
        });
        if (index < 0) {
            index = (-index + 1);
        }

        final double minMass = exactMass - deviation.absoluteFor(mass);
        final double maxMass = exactMass + deviation.absoluteFor(mass);
        while (index >= 0 && formulas[index].getMass() >= minMass) --index;
        ++index;
        int endIndex = index;
        while (endIndex < formulas.length && formulas[endIndex].getMass() <= maxMass) {
            ++endIndex;
        }
        final ArrayList<FormulaCandidate> candidates = new ArrayList<>();
        for (int i=index; i < endIndex; ++i) {
            candidates.add(new FormulaCandidate(formulas[i], ionType, 0));
        }
        return candidates;
    }

    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        final List<FingerprintCandidate> fps = candidatesPerFormula.get(formula);
        if (fps==null) return Collections.emptyList();
        return new AbstractList<CompoundCandidate>() {
            @Override
            public CompoundCandidate get(int index) {
                return fps.get(index);
            }
            @Override
            public int size() {
                return fps.size();
            }
        };
    }

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        if (!candidatesPerFormula.containsKey(formula)) return fingerprintCandidates;
        for (FingerprintCandidate fc : candidatesPerFormula.get(formula))
            fingerprintCandidates.add(fc);
        return fingerprintCandidates;
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        final ArrayList<FingerprintCandidate> candidates = new ArrayList<>();
        for (String key : inchi_keys)
            if (candidatePerKey.containsKey(key))
                candidates.add(candidatePerKey.get(key));
        return candidates;
    }

    @Override
    public List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        final ArrayList<InChI> candidates = new ArrayList<>();
        for (String key : inchi_keys)
            if (candidatePerKey.containsKey(key))
                candidates.add(candidatePerKey.get(key).getInchi());
        return candidates;
    }

    @Override
    public List<FingerprintCandidate> lookupManyFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        return lookupManyFingerprintsByInchis(inchi_keys);


    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> compounds) throws ChemicalDatabaseException {
        final ArrayList<FingerprintCandidate> candidates = new ArrayList<>();
        for (CompoundCandidate key : compounds)
            if (candidatePerKey.containsKey(key))
                candidates.add(candidatePerKey.get(key.getInchiKey2D()));
        return candidates;
    }

    @Override
    public void annotateCompounds(List<? extends CompoundCandidate> sublist) throws ChemicalDatabaseException {

    }

    @Override
    public List<InChI> findInchiByNames(List<String> names) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {

    }
}
