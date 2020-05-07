package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.babelms.CloseableIterator;

import javax.json.*;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Chemical compounds are stored as compressed json files, one file per formula
 *
 * TODO: implement cache? Or better do this as an extra database implementation
 * TODO: May be replacable by with {@link FilebasedDatabase}
 */
@Deprecated
public class FileDatabase extends AbstractChemicalDatabase {

    private final HashMap<MolecularFormula, File> formulas;
    private final MolecularFormula[] formulasOrderedByMass;
    protected FingerprintVersion version;

    private static final Pattern ALL_LOWERCASES = Pattern.compile("^[a-z0-9]+$");

    public FileDatabase(File pathToDatabase) throws IOException {
        this.formulas = new HashMap<>();
        for (File f : pathToDatabase.listFiles()) {
            final String name = f.getName();
            if (name.equalsIgnoreCase("settings.json")) {
                initSettings(f);
            }
            if (!name.endsWith(".json.gz")) {
                continue;
            }
            final String realName = name.substring(0, name.length()-".json.gz".length());
            if (ALL_LOWERCASES.matcher(name).matches()) {
                continue;
            }
            // remember file
            MolecularFormula.parseAndExecute(realName, formula -> formulas.put(formula, f));
        }
        this.formulasOrderedByMass = formulas.keySet().toArray(new MolecularFormula[formulas.size()]);
        Arrays.sort(formulasOrderedByMass);
    }

    private void initSettings(File f) throws IOException {
        JsonReader reader = Json.createReader(new FileReader(f));
        JsonObject o = reader.readObject();
        javax.json.JsonArray fpAry = o.getJsonArray("fingerprintVersion");
        if (fpAry == null) {
            this.version = CdkFingerprintVersion.getDefault();
        } else {
            final List<CdkFingerprintVersion.USED_FINGERPRINTS> usedFingerprints = new ArrayList<>();
            for (JsonValue v : fpAry) {
                if (v instanceof JsonString) {
                    try {
                        usedFingerprints.add(CdkFingerprintVersion.USED_FINGERPRINTS.valueOf(((JsonString) v).getString().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Unknown fingerprint type '" + ((JsonString) v).getString() + "'");
                    }
                }
            }
            this.version = new CdkFingerprintVersion(usedFingerprints.toArray(new CdkFingerprintVersion.USED_FINGERPRINTS[usedFingerprints.size()]));
        }
    }

    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {

        final double exactmass = ionType.precursorMassToNeutralMass(mass);
        final double lowermass = exactmass - deviation.absoluteFor(mass);
        final double uppermass = exactmass + deviation.absoluteFor(mass);

        final int searchKey = Arrays.binarySearch(formulasOrderedByMass, lowermass, new Comparator<Comparable<? extends Comparable<?>>>() {
            @Override
            public int compare(Comparable<? extends Comparable<?>> o1, Comparable<? extends Comparable<?>> o2) {
                final double mass1, mass2;
                if (o1 instanceof MolecularFormula) mass1 = ((MolecularFormula) o1).getMass();
                else mass1 = (Double)o1;
                if (o2 instanceof MolecularFormula) mass2 = ((MolecularFormula) o2).getMass();
                else mass2 = (Double)o2;
                return Double.compare(mass1, mass2);
            }
        });
        final int lowerKey;
        if (searchKey >= 0) {
            lowerKey = searchKey;
        } else lowerKey = (-searchKey - 1);
        final List<FormulaCandidate> list = new ArrayList<>();
        int i=lowerKey;
        while (formulasOrderedByMass[i].getMass() >= lowermass && formulasOrderedByMass[i].getMass() <= uppermass) {
            list.add(new FormulaCandidate(formulasOrderedByMass[i++], ionType, 0));
        }
        return list;
    }

    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        final List<CompoundCandidate> cc = new ArrayList<>();
        for (FingerprintCandidate fc : lookupStructuresAndFingerprintsByFormula(formula)) {
            cc.add(fc);
        }
        return cc;
    }

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        if (!formulas.containsKey(formula)) {
            return fingerprintCandidates;
        }
        try (final GZIPInputStream zin = new GZIPInputStream(new BufferedInputStream(new FileInputStream(formulas.get(formula))))) {
            try (final CloseableIterator<FingerprintCandidate> fciter = new JSONReader().readFingerprints(version, new InputStreamReader(zin))) {
                while (fciter.hasNext()) fingerprintCandidates.add(fciter.next());
            }
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
        return fingerprintCandidates;
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FingerprintCandidate> lookupManyFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> compounds) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void annotateCompounds(List<? extends CompoundCandidate> sublist) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<InChI> findInchiByNames(List<String> names) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {

    }
}
