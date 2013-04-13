package de.unijena.bioinf.FragmentationTreeConstruction.io;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class FragmentReader {

    public static class FragmentList {
        public final ArrayList<MolecularFormula> formulas = new ArrayList<MolecularFormula>();
        public final ArrayList<Double> mzs = new ArrayList<Double>();
    }

    private final SAXParserFactory factory;
    public FragmentReader() {
        this.factory = SAXParserFactory.newInstance();
    }

    public FragmentList parse(File file, MolecularFormula standardIon) throws IOException {
        try {
            final MSXHandler handler = new MSXHandler(file.getName(), standardIon);
            factory.newSAXParser().parse(file, handler);
            return handler.input;
        } catch (SAXException e) {
            throw new IOException(e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    private final static class MSXHandler extends DefaultHandler {

        private final FragmentList input;
        private String formula;
        private double mz;
        private String ion;
        private final StringBuilder buffer;
        private boolean bufferOpen;
        private boolean closed = false;
        private final MolecularFormula standardIon;

        private MSXHandler(String name, MolecularFormula standardIon) {
            this.input = new FragmentList();
            this.buffer = new StringBuilder(512);
            this.bufferOpen = false;
            this.standardIon = standardIon;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (closed) return;
            bufferOpen = qName.equalsIgnoreCase("Mz") || qName.equalsIgnoreCase("Formula") || qName.equalsIgnoreCase("Ion");
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (closed) return;
            if (bufferOpen) {
                buffer.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (closed) return;
            if (qName.equalsIgnoreCase("Mz")) {
                this.mz = Double.parseDouble(buffer.toString().trim());
            } else if (qName.equalsIgnoreCase("Formula")) {
                this.formula = buffer.toString().trim();
            } else if (qName.equalsIgnoreCase("Ion")) {
                this.ion = buffer.toString().trim();
            } else if (qName.equalsIgnoreCase("Fragment")) {
                if (formula != null && !formula.equals("?")) {
                    final MolecularFormula form = MolecularFormula.parse(formula).add(FragmentReader.getIon(ion)).subtract(standardIon);
                    this.input.formulas.add(form);
                } else {
                    this.input.mzs.add(new Double(mz));
                }
                formula = null;
                mz = 0d;
                ion = null;
            } else if (qName.equalsIgnoreCase("Fragments")) {
                closed = true;
            }
            if (bufferOpen) {
                buffer.delete(0, buffer.length());
            }   
        }
    }

    public static MolecularFormula getIon(String ion) {
             if (ion.equals("[M+H]+") ) {
                return MolecularFormula.parse("H");
             } else if (ion.equals("[M-H]-")) {
                return MolecularFormula.parse("H").negate();
             } else if (ion.equals("[M-H+]-")) {
            	return MolecularFormula.parse("H").negate();
             } else if (ion.equals("[M-2H]-")) {
            	 return MolecularFormula.parse("2H").negate();
             } else if (ion.equals("[M-H+2H]+")) {
            	 return MolecularFormula.parse("H");
             } else if (ion.equals("[M+H+]+")) {
            	 return MolecularFormula.parse("H");
             } else if (ion.equals("[M+Cl]-")) {
                return MolecularFormula.parse("Cl");
             } else if (ion.equals("[M+Na]+")) {
                return MolecularFormula.parse("Na");
             } else if (ion.equals("[M+H-H20]+")) {
                return MolecularFormula.parse("H").subtract(MolecularFormula.parse("H2O"));
             } else if (ion.equals("[M+H-Cl]+")) {
                return MolecularFormula.parse("H").subtract(MolecularFormula.parse("Cl"));
             } else if (ion.equals("[M]+") || ion.equals("[M]-")) return MolecularFormula.parse("");
             else throw new RuntimeException("Unknown ion " + ion);
        }


}
