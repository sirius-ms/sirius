package de.unijena.bioinf.FragmentationTreeConstruction.io;

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Peak;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2SpectrumImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;

import static de.unijena.bioinf.FragmentationTreeConstruction.io.MSXReader.ParserState.*;

public class MSXReader {
    private final SAXParserFactory factory;
    public MSXReader() {
        this.factory = SAXParserFactory.newInstance();
    }

    public MSInput parse(File file) throws IOException {
        try {
            final MSXHandler handler = new MSXHandler(file.getName(), PeriodicTable.getInstance());
            factory.newSAXParser().parse(file, handler);
            assert handler.input.getStandardIon() != null : file.getName();
            return handler.input;
        } catch (SAXException e) {
            throw new IOException(e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    private final static class MSXHandler extends DefaultHandler {

        private final MSInput input;
        private ParserState state;
        private final PeriodicTable table;
        private Ms2SpectrumImpl currentSpectrum;

        private MSXHandler(String name, PeriodicTable table) {
            this.input = new MSInput(name);
            this.state = START;
            this.table = table;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch (state) {
                case START :
                    if (qName.equalsIgnoreCase("ExperimentInformations")) {
                        parseExperimentInformations(attributes);
                    } else if (qName.equalsIgnoreCase("Spectra")) {
                        state = SPECTRA;
                    }
                    return;
                case EXPERIMENT:
                    if (qName.equalsIgnoreCase("Comment")) {
                        input.getOptionalProperties().put(expectAttribute(attributes, "Id"), expectAttribute(attributes, "Value"));
                    }
                    return;
                case SPECTRA:
                    if (qName.equalsIgnoreCase("Spectrum")) {
                        parseSpectrum(attributes);
                    }
                    return;
                case SPECTRUM:
                    if (qName.equalsIgnoreCase("Peak")) {
                        parsePeak(attributes);
                    }
            }
        }

        private void parsePeak(Attributes attributes) throws SAXException{
            final double mz = Double.parseDouble(expectAttribute(attributes, "Mass"));
            final double intensity = Double.parseDouble(expectAttribute(attributes, "Intensity"));
            currentSpectrum.getPeaks().add(new MS2Peak(currentSpectrum, mz, intensity));
        }

        private void parseExperimentInformations(Attributes attributes) {
            for (int i=0; i < attributes.getLength(); ++i) {
                final String key = attributes.getQName(i);
                final String value = attributes.getValue(i).trim();
                if (key.equalsIgnoreCase("CompoundName")) {
                    input.getOptionalProperties().put("CompoundName", value);
                } else if (key.equalsIgnoreCase("MolecularFormula")) {
                    input.setFormula(MolecularFormula.parse(value));
                } else if (key.equalsIgnoreCase("ExperimentType")) {
                    input.getOptionalProperties().put("ExperimentType", value);
                } else if (key.equalsIgnoreCase("FocusedMass")) {
                    input.getOptionalProperties().put("FocusedMass", value);
                } else if (key.equalsIgnoreCase("ModificationMass")) {
                    final double val = Double.parseDouble(value);
                    input.setModificationMass(val);
                    final Ionization ion = table.ionByMass(val, 1e-3);
                    if (ion != null) input.setStandardIon(ion);
                    else input.setStandardIon(new Charge(1));

                }
            }
            state = EXPERIMENT;
        }

        private String expectAttribute(Attributes attributes, String name) throws SAXException{
            final String value = getAttribute(attributes, name);
            if (value != null) return value;
            else throw new SAXException("Expected attribute '"+ name + "'");
        }
        private String getAttribute(Attributes attributes, String name) {
            for (int i=0; i < attributes.getLength(); ++i) {
                if (attributes.getQName(i).equalsIgnoreCase(name)) return attributes.getValue(i);
            }
            return null;
        }

        private void parseSpectrum(Attributes attributes) throws SAXException {
            final String cE = getAttribute(attributes, "CollisionEnergy");
            final double collisionEnergy = (cE == null || cE.equals("?")) ? 0d : Double.parseDouble(cE);
            final int level =(cE != null && collisionEnergy == 0d && !cE.equals("?")) ? 1 : Integer.parseInt(expectAttribute(attributes, "MSLevel"));
            final String pM = getAttribute(attributes, "ParentMass");
            final double parentMass = (pM == null) ? 0d : Double.parseDouble(pM);
            currentSpectrum = new Ms2SpectrumImpl(new CollisionEnergy(collisionEnergy, collisionEnergy), level, parentMass);
            this.state = SPECTRUM;
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch (state) {
                case EXPERIMENT:
                    if (qName.equalsIgnoreCase("ExperimentInformations")) {
                        if (input.getFormula() != null && input.getStandardIon() != null) {
                            input.setFormulaChargedMass(input.getStandardIon().getMass() + input.getFormula().getMass());
                        }
                        state = START;
                    }
                    return;
                case SPECTRA:
                    if (qName.equalsIgnoreCase("Spectra")) {
                        state = START;
                    }
                    return;
                case SPECTRUM:
                    if (qName.equalsIgnoreCase("Spectrum")) {
                        if (currentSpectrum.getMsLevel() == 1) {
                            input.setMs1Spectrum(new SimpleSpectrum(currentSpectrum));
                        } else {
                            input.getMs2Spectra().add(currentSpectrum);
                        }
                        currentSpectrum = null;
                        state = SPECTRA;
                    }
                    return;
			default:
				break;
            }
        }
    }

    enum ParserState {
        EXPERIMENT, SPECTRA, SPECTRUM, START;
    }


}
