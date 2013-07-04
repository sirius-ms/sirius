package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.babelms.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;

public class JenaGCMSParser implements Parser<JenaGCMSExperiment> {
    private static final double DEFAULT_COLLISIONENERGY = 70.0;
    private static final Ionization DEFAULT_IONIZATION = new ElectronIonization();
    private final boolean molecularIonKnown;

    public JenaGCMSParser(boolean molecularIonKnown){
        this.molecularIonKnown = molecularIonKnown;
    }

    @Override
    public JenaGCMSExperiment parse(BufferedReader reader) throws IOException {
        return new ParserInstance(reader).parse(molecularIonKnown);

    }

    private static class ParserInstance{
        private ParserInstance(BufferedReader reader) {
            this.reader = reader;
            this.simpleSpectrum = new SimpleMutableSpectrum();
            this.compoundName = "";
            this.collisionEnergy = DEFAULT_COLLISIONENERGY;
            this.ionization = DEFAULT_IONIZATION;
            this.tic = 0;
            this.retentionTime = 0;
        }

        private final BufferedReader reader;
        private String compoundName;
        private MolecularFormula compoundFormula;
        private double ionMass;
        private Ionization ionization;
        private SimpleMutableSpectrum simpleSpectrum;
        private JenaMsSpectrum msSpectrum;
        private double collisionEnergy;
        private double tic;
        private double retentionTime;

        public JenaGCMSExperiment parse(boolean parentMoleculeKnown)  throws IOException {
            int i=1; //linecounter

            for (String line = reader.readLine(); line!=null; line=reader.readLine()){
                line=line.trim();
                if (line.length()>0){
                    if (!line.contains(">")) {
                        String[] values = line.split("\t");
                        if (values.length<2){
                            values = line.split(" ");
                            if(values.length<2) throw new IOException("in line " + i+ ". Line seems to contain a peak, but does not.");
                        }
                        try{
                            if(Double.parseDouble(values[1])>0){
                                simpleSpectrum.addPeak(new Peak(Double.parseDouble(values[0]), Double.parseDouble(values[1])));
                            }
                        }catch (NumberFormatException e){
                            throw new IOException("in line " + i+ ". String \"" + values[0]+ "\" or \"" + values[1]+ "\" could not be parsed to double");
                        }
                    }else{
                        if (parentMoleculeKnown){
                            if (line.contains("formula")){
                                String formula = line.split(":")[1].trim();
                                compoundFormula = MolecularFormula.parse(formula);
                            }else if (line.contains("mass")){
                                ionMass = Double.parseDouble(line.split(":")[1].trim());
                            }
                        }

                    }
                }

                i++;
            }

            msSpectrum  = new JenaMsSpectrum(simpleSpectrum, tic, retentionTime);

            return new JenaGCMSExperiment(compoundName, compoundFormula, ionMass, ionization, Collections.singletonList(msSpectrum), collisionEnergy);
        }

    }




}
