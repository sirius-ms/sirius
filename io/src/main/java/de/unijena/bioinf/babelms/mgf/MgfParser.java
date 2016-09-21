/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.babelms.mgf;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.SpectralParser;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MgfParser extends SpectralParser implements Parser<Ms2Experiment> {

    private static class MgfSpec {
        private MutableMs2Spectrum spectrum;
        private PrecursorIonType ionType;
        private HashMap<String, String> fields;
        private String inchi, smiles, name;

        public MgfSpec(MgfSpec s) {
            this.spectrum=new MutableMs2Spectrum(s.spectrum);
            this.ionType=s.ionType;
            this.fields=new HashMap<String, String>(s.fields);
            this.inchi = s.inchi;
            this.smiles=s.smiles;
            this.name = s.name;
        }

        public MgfSpec() {
            this.spectrum = new MutableMs2Spectrum();
            this.fields = new HashMap<String, String>();
        }
    }

    private static class MgfParserInstance {
        private final MgfSpec prototype;
        private final ArrayDeque<MgfSpec> buffer;
        private final BufferedReader reader;

        public MgfParserInstance(BufferedReader reader) {
            this.reader = reader;
            this.prototype = new MgfSpec(); this.prototype.spectrum=new MutableMs2Spectrum();
            this.buffer = new ArrayDeque<MgfSpec>();
        }

        public boolean hasNext() throws IOException {
            addNextEntry();
            return !buffer.isEmpty();
        }

        public MgfSpec peekNext() throws IOException {
            addNextEntry();
            return buffer.peekFirst();
        }

        public MgfSpec pollNext() throws IOException {
            addNextEntry();
            return buffer.pollFirst();
        }

        private void addNextEntry() throws IOException {
            if (!buffer.isEmpty()) return;
            MgfSpec s = readNext();
            if (s!=null)
                buffer.addLast(s);
        }

        private static Pattern CHARGE_PATTERN = Pattern.compile("(\\d+)([+-])?");
        private static Pattern NOT_AVAILABLE = Pattern.compile("\\s*N/A\\s*");

        private void handleKeyword(MgfSpec spec, String keyword, String value) throws IOException {
            keyword = keyword.toUpperCase();
            value = value.trim();
            if (value.isEmpty()) return;
            if (value.charAt(0)=='"' && value.charAt(value.length()-1)=='"') value = value.substring(1,value.length()-1);
            if (keyword.equals("PEPMASS")) {
                spec.spectrum.setPrecursorMz(Double.parseDouble(value.split("\\s+")[0]));
            } else if (keyword.equals("CHARGE")) {
                final Matcher m = CHARGE_PATTERN.matcher(value);
                m.find();
                int charge = ("-".equals(m.group(2))) ? -Integer.parseInt(m.group(1)) : Integer.parseInt(m.group(1));
                if (charge==0) charge=1;
                if (spec.spectrum.getIonization()==null || spec.spectrum.getIonization().getCharge() != charge)
                    spec.spectrum.setIonization(new Charge(charge));
                if (spec.ionType==null) spec.ionType = PrecursorIonType.unknown(charge);
            } else if (keyword.startsWith("ION")) {
                final PrecursorIonType ion;
                final Matcher cm = CHARGE_PATTERN.matcher(value);
                if (value.toLowerCase().startsWith("pos")) {
                    ion = PrecursorIonType.unknown(1);
                } else if (value.toLowerCase().startsWith("neg")) {
                    ion = PrecursorIonType.unknown(-1);
                } else if (cm.matches()) {
                    int v = Integer.parseInt(cm.group(1));
                    if ("-".equals(cm.group(2))) ion = PrecursorIonType.unknown(-1);
                    else ion = PrecursorIonType.unknown(1);
                } else {
                    ion = PeriodicTable.getInstance().ionByName(value);
                    if (ion==null) throw new IOException("Unknown ion '" + value +"'");
                    else {

                    }
                }
                spec.spectrum.setIonization(ion.getIonization());
                spec.ionType = ion;
            } else if (keyword.contains("LEVEL")) {
                spec.spectrum.setMsLevel(Integer.parseInt(value));
            } else {
                if (NOT_AVAILABLE.matcher(value).matches()) return;
                if (keyword.equalsIgnoreCase("INCHI")) {
                    spec.inchi = value;
                } else if (keyword.equalsIgnoreCase("SMILES")) {
                    spec.smiles = value;
                } else if (keyword.equalsIgnoreCase("NAME") || keyword.equalsIgnoreCase("TITLE")) {
                    spec.name = value;
                } else {
                    spec.fields.put(keyword, value);
                }
            }
        }

        private MgfSpec readNext() throws IOException {
            String line;
            boolean reading=false;
            MgfSpec spec = null;
            while ((line=reader.readLine())!=null) {
                try {
                    if (line.isEmpty()) continue;
                    if (!reading && line.startsWith("BEGIN IONS")) {
                        spec =  new MgfSpec(prototype);
                        reading=true;
                    } else if (reading && line.startsWith("END IONS")) {
                        return spec;
                    } else if (reading) {
                        if (Character.isDigit(line.charAt(0))) {
                            final String[] parts = line.split("\\s+");
                            spec.spectrum.addPeak(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
                        } else {
                            final int i = line.indexOf('=');
                            if (i>=0) handleKeyword(spec, line.substring(0, i), line.substring(i+1));
                        }
                    } else {
                        final int i = line.indexOf('=');
                        if (i>=0) handleKeyword(prototype, line.substring(0, i), line.substring(i+1));
                    }
                } catch (RuntimeException e) {
                    LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
                    if (reading) {
                        while ((line=reader.readLine())!=null) {
                            if (line.startsWith("END IONS")) {
                                reading = false;
                                break;
                            } else if (line.startsWith("BEGIN IONS")) {
                                reading = true;
                                spec =  new MgfSpec(prototype);
                                break;
                            }
                        }
                    }
                }
            }
            return null;
        }
    }

    @Override
    public Iterator<Ms2Spectrum<Peak>> parseSpectra(final BufferedReader reader) throws IOException {
        return new Iterator<Ms2Spectrum<Peak>>() {

            private final MgfParserInstance inst = new MgfParserInstance(reader);

            @Override
            public boolean hasNext() {
                try {
                    return inst.hasNext();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Ms2Spectrum<Peak> next() {
                try {
                    if (!inst.hasNext()) throw new NoSuchElementException();
                    return inst.pollNext().spectrum;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private MgfParserInstance inst;

    @Override
    public synchronized Ms2Experiment parse(BufferedReader reader, URL source) throws IOException {
        if (inst==null || inst.reader!=reader) inst = new MgfParserInstance(reader);
        if (!inst.hasNext()) return null;
        final MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.setMs2Spectra(new ArrayList<MutableMs2Spectrum>());
        exp.setMs1Spectra(new ArrayList<SimpleSpectrum>());
        exp.setIonMass(inst.peekNext().spectrum.getPrecursorMz());
        exp.setName(inst.peekNext().name);
        final HashMap<String,String> additionalFields = new HashMap<String, String>();
        while (inst.hasNext() && Math.abs(inst.peekNext().spectrum.getPrecursorMz()-exp.getIonMass()) < 0.002 && (inst.peekNext().name == exp.getName() || (inst.peekNext().name != null && inst.peekNext().name.equals(exp.getName())))) {
            final MgfSpec spec = inst.pollNext();
            if (spec.spectrum.getMsLevel()==1) exp.getMs1Spectra().add(new SimpleSpectrum(spec.spectrum));
            else exp.getMs2Spectra().add(new MutableMs2Spectrum(spec.spectrum));
            if (exp.getPrecursorIonType()==null) {
                exp.setPrecursorIonType(spec.ionType);
            }
            if (spec.inchi!=null && spec.inchi.startsWith("InChI=")) {
                exp.setAnnotation(InChI.class, new InChI(null, spec.inchi));
            }
            if (spec.smiles!=null) {
                exp.setAnnotation(Smiles.class, new Smiles(spec.smiles));
            }
            additionalFields.putAll(spec.fields);
        }
        if (!additionalFields.isEmpty()) {
            exp.setAnnotation(Map.class, additionalFields);
        }
        exp.setSource(source);
        return exp;
    }
}
