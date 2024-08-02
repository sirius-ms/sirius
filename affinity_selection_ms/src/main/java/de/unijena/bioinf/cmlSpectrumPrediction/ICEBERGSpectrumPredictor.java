package de.unijena.bioinf.cmlSpectrumPrediction;

import com.fasterxml.jackson.databind.JsonNode;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.data.JSONDocumentType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.SimplePeak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.fragmenter.CombinatorialFragment;
import de.unijena.bioinf.fragmenter.MolecularGraph;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.BitSet;

public class ICEBERGSpectrumPredictor extends AbstractMs2SpectrumPredictor<Peak>{

    private final MolecularGraph molecule;
    private final String smiles;
    private final int numFragments;
    private final String fileName;

    private final static String DEVICE = "cpu";
    private final static double THRESHOLD = 0d;
    private final static boolean BINNED_OUT = false;

    private static File PYTHON_ENV_PATH;
    private static File ICEBERG_SCRIPT;
    private static File ICEBERG_MODELS_DIR;
    private static File OUTPUT_DIR;

    public ICEBERGSpectrumPredictor(String smiles, PrecursorIonType precursorIonType, int numFragments, String fileName) throws InvalidSmilesException {
        super(null, precursorIonType);
        this.smiles = smiles;
        this.molecule = new MolecularGraph(new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(smiles));
        this.numFragments = numFragments;
        this.fileName = fileName;
    }

    public ICEBERGSpectrumPredictor(String smiles, PrecursorIonType precursorIonType, int numFragments) throws InvalidSmilesException{
        this(smiles, precursorIonType, numFragments, "iceberg_prediction_output.json");
    }

    @Override
    public Ms2Spectrum<Peak> predictSpectrum() {
        double moleculeMass = this.molecule.getFormula().getMass();
        double precursorMz = this.precursorIonType.neutralMassToPrecursorMass(moleculeMass);
        try {
            // Run ICEBERG through the python script 'iceberg_predictMol.py':
            if(!this.startPrediction()) throw new RuntimeException("Unexpected termination during python call.");

            // This produced a .json file containing the predicted fragments together with the predicted intensities:
            File outputFile = new File(OUTPUT_DIR, this.fileName);
            JsonNode frags = JSONDocumentType.readFromFile(outputFile);
            SimpleMutableSpectrum spec = new SimpleMutableSpectrum();

            for(JsonNode frag : frags){
                BitSet bitset = this.atomIndicesJsonNode2BitSet(frag.get("atom_indices"));
                int[][] sssr = this.molecule.getSSSR();
                BitSet disc = new BitSet(sssr.length);
                for(int ringId = 0; ringId < sssr.length; ringId++){
                    for(int atomIdx : sssr[ringId]){
                        if(!bitset.get(atomIdx)){
                            disc.set(ringId);
                            break;
                        }
                    }
                }
                CombinatorialFragment combFrag = new CombinatorialFragment(this.molecule, bitset, disc);

                JsonNode fragMzVals = frag.get("mz_charge");
                JsonNode fragIntens = frag.get("intens");
                for(int i = 0; i < fragMzVals.size(); i++){
                    SimplePeak peak = new SimplePeak(fragMzVals.get(i).asDouble(), fragIntens.get(i).asDouble());
                    spec.addPeak(peak);
                    this.peak2fragment.put(peak, combFrag);
                }
            }
            Spectrums.sortSpectrumByMass(spec);
            spec.removePeakAt(spec.size()-1); // remove precursor peak
            return new MutableMs2Spectrum(spec, precursorMz, null, 2);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            System.err.println(e.getMessage());
            return new MutableMs2Spectrum(new SimpleMutableSpectrum(), precursorMz, null, 2);
        }
    }

    private boolean startPrediction() throws IOException, InterruptedException, URISyntaxException {
        ProcessBuilder pb = new ProcessBuilder(PYTHON_ENV_PATH.getPath(), ICEBERG_SCRIPT.getPath(), this.smiles, this.precursorIonType.toString(),
                DEVICE, Integer.toString(this.numFragments), Double.toString(THRESHOLD),
                Boolean.toString(BINNED_OUT), ICEBERG_MODELS_DIR.getPath(), OUTPUT_DIR.getPath(), this.fileName);
        pb.redirectErrorStream(true);
        pb.inheritIO();

        Process process = pb.start();
        int exitCode = process.waitFor();
        return exitCode == 0;
    }

    private BitSet atomIndicesJsonNode2BitSet(JsonNode node){
        BitSet bitset = new BitSet(this.molecule.getMolecule().getAtomCount());
        for(JsonNode indexNode : node) bitset.set(indexNode.asInt());
        return bitset;
    }

    public static void initializeClass(File pythonEnvPath, File icebergScriptPath, File modelsDir, File outputDir){
        PYTHON_ENV_PATH = pythonEnvPath;
        ICEBERG_SCRIPT = icebergScriptPath;
        ICEBERG_MODELS_DIR = modelsDir;
        OUTPUT_DIR = outputDir;
    }

}
