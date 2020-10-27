/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms.cef;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;

public class AgilentCefExperimentParser implements Parser<Ms2Experiment> {
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#0.000");
    private final static QName qName = new QName("Compound");
    private Unmarshaller unmarshaller;
    private XMLEventReader xmlEventReader;

    private InputStream currentStream = null;
    private URL currentUrl = null;



    private Iterator<Ms2Experiment> iterator = null;
    @Override
    public <S extends Ms2Experiment> S parse(@Nullable BufferedReader ignored, @NotNull URL source) throws IOException {
        //XML parsing on readers works bad, so we create our own stream from url
        if (iterator != null && iterator.hasNext()) {
            return (S) iterator.next();
        } else if (!Objects.equals(currentUrl, source) || xmlEventReader == null || unmarshaller == null) {
            try {
                currentUrl = source;
                currentStream = source.openStream();
                // create xml event reader for input stream
                XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
                xmlEventReader = xmlInputFactory.createXMLEventReader(currentStream);

                // initialize jaxb
                JAXBContext context = JAXBContext.newInstance(CEF.class);
                unmarshaller = context.createUnmarshaller();
            } catch (JAXBException | XMLStreamException e) {
                throw new IOException("Error When initializing JAXB parser context", e);
            }
        }


        try {
            XMLEvent e = null;
            // loop though the xml stream
            while ((e = xmlEventReader.peek()) != null) {
                // check the event is a Document start element
                if (e.isStartElement() && ((StartElement) e).getName().equals(qName)) {
                    // unmarshall the compound
                    Compound compound = unmarshaller.unmarshal(xmlEventReader, Compound.class).getValue();
                    iterator = experimentFromCompound(compound).iterator();
                    return iterator.hasNext() ? (S) iterator.next() : null;
                } else {
                    xmlEventReader.next();
                }
            }
        } catch (XMLStreamException | JAXBException e) {
            throw new IOException("Error when parsing Compound!", e);
        }

        return null;

    }

    private <S extends Ms2Experiment> List<S> experimentFromCompound(Compound compound) {
        if (compound.getSpectrum().stream().anyMatch(s -> s.getType().equals("MFE"))) {
            return experimentFromMFECompound(compound);
        } else {
            return experimentFromRawCompound(compound);
        }
    }

    private static final Pattern PEAK_MATCHER = Pattern.compile("^\\d+M.*|\\+\\d+$");
    private <S extends Ms2Experiment> List<S> experimentFromMFECompound(Compound compound) {
        final Spectrum mfe = compound.getSpectrum().stream().filter(s -> s.getType().equals("MFE"))
                .findAny().orElseThrow(() -> new IllegalArgumentException("Compound must contain a MFE spectrum to be parsed as MFE spectrum!"));

        List<S> siriusCompounds = new ArrayList<>();

        mfe.msPeaks.getP().stream().filter(p -> {
            if (PEAK_MATCHER.matcher(p.getS()).find()) {
                LoggerFactory.getLogger(getClass()).warn("Skipping potential precursor at `" + p.getX() + "Da` (and corresponding MS/MS) due to an unsupported ion type '" + p.getS() + "'.)");
                return false;
            }else {
                return true;
            }
        }).forEach(p -> {
            MutableMs2Experiment exp = new MutableMs2Experiment();
            exp.setIonMass(p.getX().doubleValue());
            exp.setPrecursorIonType(PrecursorIonType.fromString("[" + p.getS() + "]" + mfe.getMSDetails().p));
            siriusCompounds.add(experimentFromCompound(compound, exp));
        });

        return siriusCompounds;
    }

    private <S extends Ms2Experiment> List<S> experimentFromRawCompound(Compound compound) {
        MutableMs2Experiment exp = new MutableMs2Experiment();

        Spectrum s = compound.getSpectrum().stream().filter(spec -> spec.getMSDetails().getScanType().equals("ProductIon")).findFirst().orElseThrow(() -> new IllegalArgumentException("No MS/MS spectrum found for compound at rt " + compound.location.rt));
        exp.setPrecursorIonType(s.getMSDetails().p.equals("-") ? PrecursorIonType.unknownNegative() : PrecursorIonType.unknownPositive());
        exp.setIonMass(s.mzOfInterest.getMz().doubleValue());

        return List.of(experimentFromCompound(compound, exp));
    }

    private <S extends Ms2Experiment> S experimentFromCompound(Compound compound, MutableMs2Experiment exp) {
        //todo how do we get the real dev? maybe load profile/ from outside
        MS2MassDeviation dev = PropertyManager.DEFAULTS.createInstanceWithDefaults(MS2MassDeviation.class);
        exp.setName("rt=" + compound.location.rt + "-p=" + NUMBER_FORMAT.format(exp.getIonMass()));
        exp.setSource(new SpectrumFileSource(currentUrl));

        List<SimpleSpectrum> ms1Spectra = new ArrayList<>();
        List<MutableMs2Spectrum> ms2Spectra = new ArrayList<>();
        for (Spectrum spec : compound.getSpectrum()) {
            if (spec.type.equals("MFE")) {
                // ignore was already handled beforehand.
            } else if (spec.getMSDetails().getScanType().equals("Scan")) {
                ms1Spectra.add(makeMs1Spectrum(spec));
                if (!exp.hasAnnotation(RetentionTime.class))
                    parseRT(compound, spec).ifPresent(rt -> exp.addAnnotation(RetentionTime.class, rt));
            } else if (spec.getMSDetails().getScanType().equals("ProductIon") && dev.standardMassDeviation.inErrorWindow(spec.mzOfInterest.mz.doubleValue(), exp.getIonMass())) {
                ms2Spectra.add(makeMs2Spectrum(spec));
            } else {
                Optional<Spectrum> s = Optional.of(spec);
                LoggerFactory.getLogger(getClass()).warn("Spectrum of type '" + s.map(Spectrum::getType).orElse("N/A")
                        + "' with precursor mass '" + s.map(Spectrum::getMzOfInterest).map(MzOfInterest::getMz).map(BigDecimal::doubleValue).map(String::valueOf).orElse("N/A")
                        + "' is either not supported or it does not correspond to a supported precursor. Skipping this spectrum.");
            }
        }

        if (!exp.hasAnnotation(RetentionTime.class))
            parseRT(compound).ifPresent(rt -> exp.addAnnotation(RetentionTime.class, rt));
        exp.setMs1Spectra(ms1Spectra);
        exp.setMs2Spectra(ms2Spectra);
        return (S) exp;
    }

    private CollisionEnergy parseCE(Spectrum spec) {
        try {
            return CollisionEnergy.fromString(spec.msDetails.getCe().replace("V", "ev"));
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).warn("Could not parse collision energy! Cause: " + e.getMessage());
            LoggerFactory.getLogger(getClass()).debug("Could not parse collision energy!" , e);
            return CollisionEnergy.none();
        }
    }

    private Optional<RetentionTime> parseRT(@NotNull Compound c) {
        return parseRT(c, null);
    }

    private Optional<RetentionTime> parseRT(@NotNull Compound c, @Nullable Spectrum ms1) {
        try {
            double middle = c.getLocation().getRt().doubleValue();
            return Optional.of(Optional.ofNullable(ms1).map(Spectrum::getRTRanges).map(RTRanges::getRTRange)
                    .map(range -> new RetentionTime(range.min.doubleValue() * 60, range.max.doubleValue() * 60, middle * 60))
                    .orElse(new RetentionTime(middle * 60)));
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).warn("Could not parse Retention time!", e);
            return Optional.empty();
        }
    }

    private MutableMs2Spectrum makeMs2Spectrum(Spectrum spec) {
        return new MutableMs2Spectrum(
                makeMs1Spectrum(spec),
                spec.getMzOfInterest().getMz().doubleValue(),
                parseCE(spec),
                2
        );
    }


    SimpleSpectrum makeMs1Spectrum(Spectrum spec) {
        return new SimpleSpectrum(
                spec.msPeaks.p.stream().map(P::getX).mapToDouble(BigDecimal::doubleValue).toArray(),
                spec.msPeaks.p.stream().map(P::getY).mapToDouble(BigDecimal::doubleValue).toArray()
        );
    }

}

