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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.BitSet;
import java.util.Objects;

public class ICEBERGSpectrumPredictor extends AbstractMs2SpectrumPredictor<Peak>{

    private final MolecularGraph molecule;
    private final String smiles;
    private final int numFragments;
    private final static String DEVICE = "cpu";
    private final static double THRESHOLD = 0d;
    private final static boolean BINNED_OUT = false;

    public ICEBERGSpectrumPredictor(String smiles, PrecursorIonType precursorIonType, int numFragments) throws InvalidSmilesException {
        super(null, precursorIonType);
        this.smiles = smiles;
        this.molecule = new MolecularGraph(new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(smiles));
        this.numFragments = numFragments;
    }

    @Override
    public Ms2Spectrum<Peak> predictSpectrum() {
        double moleculeMass = this.molecule.getFormula().getMass();
        double precursorMz = this.precursorIonType.neutralMassToPrecursorMass(moleculeMass);
        try {
            // Retrieve the "ms-gen" environment path:
            File envPath = this.getCondaEnvPath();
            File pythonEnvPath = new File(envPath, "python");

            // Run ICEBERG through the python script 'iceberg_predictMol.py':
            if(!this.startPrediction(pythonEnvPath)) throw new RuntimeException("Unexpected termination during python call.");

            // This produced a .json file containing the predicted fragments together with the predicted intensities:
            File outputFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("iceberg_prediction_output.json")).toURI());
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

    private File getCondaEnvPath() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("python", "-m", "conda", "env", "list");
        Process process = pb.start();
        if(process.waitFor() != 0) throw new RuntimeException("Retrieving conda environment path resulted in abnormal termination.");

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
            String line = reader.readLine();
            while(line != null){
                String[] envData = line.split("\\s+");
                if(envData[0].equals("ms-gen")) return new File(envData[1]);
                line = reader.readLine();
            }
        }
        return null;
    }

    private boolean startPrediction(File pythonEnvPath) throws IOException, InterruptedException, URISyntaxException {
        File icebergScript = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("iceberg_predictMol.py")).toURI());
        ProcessBuilder pb = new ProcessBuilder(pythonEnvPath.getPath(), icebergScript.getPath(), this.smiles, this.precursorIonType.toString(),
                DEVICE, Integer.toString(this.numFragments), Double.toString(THRESHOLD),
                Boolean.toString(BINNED_OUT), icebergScript.getParent());
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

}
