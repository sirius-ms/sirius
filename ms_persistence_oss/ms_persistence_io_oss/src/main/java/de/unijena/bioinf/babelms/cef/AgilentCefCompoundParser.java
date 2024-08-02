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
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.IsotopePattern;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.apache.commons.io.input.ReaderInputStream;
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
import java.net.URI;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static de.unijena.bioinf.babelms.cef.CEFUtils.*;

public class AgilentCefCompoundParser implements Parser<de.unijena.bioinf.ms.persistence.model.core.Compound> {
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#0.000");
    private final static QName qName = new QName("Compound");
    private Unmarshaller unmarshaller;
    private XMLEventReader xmlEventReader;

    private InputStream currentStream = null;
    private URI currentUrl = null;

    @Override
    public de.unijena.bioinf.ms.persistence.model.core.Compound parse(InputStream inputStream, URI source) throws IOException {
        if (!Objects.equals(currentUrl, source) || !Objects.equals(inputStream, currentStream) || xmlEventReader == null || unmarshaller == null) {
            if (inputStream == null && source == null)
                throw new IllegalArgumentException("Neither Reader nor File is given, No Input to parse!");
            currentUrl = source;
            if (inputStream != null) {
                currentStream = inputStream;
            } else {
                currentStream = currentUrl.toURL().openStream();
            }
            initXmlParser();
        }
        return parse();
    }

    @Override
    public de.unijena.bioinf.ms.persistence.model.core.Compound parse(BufferedReader secondChoice, @Nullable URI source) throws IOException {
        if (!Objects.equals(currentUrl, source) || xmlEventReader == null || unmarshaller == null) {

            if (secondChoice == null && source == null)
                throw new IllegalArgumentException("Neither Reader nor File is given, No Input to parse!");

            currentUrl = source;
            if (source != null) {
                currentStream = currentUrl.toURL().openStream();
                if (secondChoice != null)
                    secondChoice.close();
            } else {
                currentStream = ReaderInputStream.builder().setReader(secondChoice).get();
            }
            initXmlParser();
        }
        return parse();
    }

    private void initXmlParser() throws IOException {
        try {
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

    public de.unijena.bioinf.ms.persistence.model.core.Compound parse() throws IOException {
        //XML parsing on readers works bad, so we create our own stream from url
        try {
            XMLEvent e;
            // loop though the xml stream
            while ((e = xmlEventReader.peek()) != null) {
                // check the event is a Document start element
                if (e.isStartElement() && ((StartElement) e).getName().equals(qName)) {
                    // unmarshall the compound
                    Compound compound = unmarshaller.unmarshal(xmlEventReader, Compound.class).getValue();
                    return parseCompound(compound);
                } else {
                    xmlEventReader.next();
                }
            }
        } catch (XMLStreamException | JAXBException e) {
            throw new IOException("Error when parsing Compound!", e);
        }

        return null;

    }

    private de.unijena.bioinf.ms.persistence.model.core.Compound parseCompound(Compound compound) {
        final List<AlignedFeatures> siriusFeatures;
        if (compound.getSpectrum().stream().anyMatch(s -> s.getType().equalsIgnoreCase("MFE") || s.getType().equalsIgnoreCase("FBF"))) { //MFE/FBF data
            siriusFeatures = fromMFECompound(compound);
        } else if (compound.getSpectrum().stream().noneMatch(s -> s.getMSDetails().getScanType().equals("ProductIon"))) { //ms1 only data from raw format
            siriusFeatures = fromMS1OnlyCompound(compound);
        } else {
            siriusFeatures = fromRawCompound(compound);
        }

        de.unijena.bioinf.ms.persistence.model.core.Compound.CompoundBuilder b =
                de.unijena.bioinf.ms.persistence.model.core.Compound.builder()
                        .adductFeatures(siriusFeatures);
        Optional.ofNullable(compound.location)
                .map(Location::getM)
                .map(BigDecimal::doubleValue)
                .ifPresent(b::neutralMass);
        parseRT(compound).ifPresent(b::rt);

        return b.build();


    }

    private static final Pattern UNSUPPORTED_IONTYPE_MATCHER = Pattern.compile("^\\d+M.*");
    private static final Pattern ISOTOPE_PEAK_MATCHER = Pattern.compile("\\+\\d+$");

    private List<AlignedFeatures> fromMFECompound(Compound compound) {
        final Spectrum mfe = compound.getSpectrum().stream()
                .filter(s ->
                        s.getType().equalsIgnoreCase("MFE") || s.getType().equalsIgnoreCase("FBF")
                ).findAny().orElseThrow(() -> new IllegalArgumentException("Compound must contain a MFE/FBF spectrum to be parsed as MFE/FBF spectrum!"));


        List<AlignedFeatures> siriusFeatures = new ArrayList<>();

        mfe.msPeaks.getP().stream().filter(p -> {
            if (UNSUPPORTED_IONTYPE_MATCHER.matcher(p.getS()).find()) {
                LoggerFactory.getLogger(getClass()).warn("Skipping potential precursor at '" + p.getX() + "Da' (and corresponding MS/MS) due to an unsupported ion type '" + p.getS() + "'.");
                return false;
            } else if (ISOTOPE_PEAK_MATCHER.matcher(p.getS()).find()) {
                LoggerFactory.getLogger(getClass()).debug("Skipping isotope peak during precursor search '" + p.getX() + "Da' ('" + p.getS() + "')."); //todo to debug
                return false;
            } else {
                return true;
            }
        }).forEach(p -> {
            PrecursorIonType ionType = PrecursorIonType.fromString("[" + p.getS() + "]" + mfe.getMSDetails().p);
            Feature f = Feature.builder()
                    .averageMass(p.getX().doubleValue())
                    .apexMass(p.getX().doubleValue())
                    .apexIntensity(p.getY().doubleValue())
                    .charge((byte) ionType.getCharge())
                    .snr(p.getY().doubleValue() > 0 ? p.getX().doubleValue() / p.getY().doubleValue() : 0)
                    .build();
            parseRT(mfe).ifPresent(f::setRetentionTime);

            IsotopePattern isotopePattern = featureFromCompound(compound, f);

            AlignedFeatures al = AlignedFeatures.singleton(f, isotopePattern);
            al.setDetectedAdducts(DetectedAdducts.singleton(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.INPUT_FILE, ionType));
            siriusFeatures.add(al);
        });

        // We are not sure what adduct it was, unlike when multiple adducts were detected
        if (siriusFeatures.size() == 1)
            siriusFeatures.iterator().next().setDetectedAdducts(null);

        return siriusFeatures;
    }

    private List<AlignedFeatures> fromMS1OnlyCompound(Compound compound) {
        Spectrum ms = compound.getSpectrum().stream()
                .filter(s -> s.getMSDetails().getScanType().equals("Scan"))
                .filter(s -> s.mzOfInterest != null)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No spectrum (neither MS1 nor MS/MS) with precursor information (MzOfInterest) found for compound at rt " + compound.location.rt));

        double apexMass = 0, apexInt = 0;
        for (P p : ms.msPeaks.p) {
            if (p.getY().doubleValue() > apexInt) {
                apexMass = p.getX().doubleValue();
                apexInt = p.getY().doubleValue();
            }
        }

        Feature f = Feature.builder()
                .averageMass(ms.mzOfInterest.getMz().doubleValue())
                .apexMass(apexMass)
                .apexIntensity(apexInt)
                .snr(apexInt > 0 ? apexMass / apexInt : 0)
                .charge((byte) (ms.getMSDetails().p.equals("-") ? -1 : +1))
                .build();
        parseRT(ms).ifPresent(f::setRetentionTime);

        featureFromCompound(compound, f);
        return List.of(AlignedFeatures.singleton(f));
    }

    private List<AlignedFeatures> fromRawCompound(Compound compound) {
        Spectrum s = compound.getSpectrum().stream().filter(spec -> spec.getMSDetails().getScanType().equals("ProductIon")).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No MS/MS spectrum found for compound at rt " + compound.location.rt));

        double apexMass = 0, apexInt = 0;
        for (P p : s.msPeaks.p) {
            if (p.getY().doubleValue() > apexInt) {
                apexMass = p.getX().doubleValue();
                apexInt = p.getY().doubleValue();
            }
        }

        Feature f = Feature.builder()
                .averageMass(s.mzOfInterest.getMz().doubleValue())
                .apexMass(apexMass)
                .apexIntensity(apexInt)
                .snr(apexInt > 0 ? apexMass / apexInt : 0)
                .charge((byte) (s.getMSDetails().p.equals("-") ? -1 : 1))
                .build();
        parseRT(s).ifPresent(f::setRetentionTime);

        featureFromCompound(compound, f);

        return List.of(AlignedFeatures.singleton(f));
    }

    private IsotopePattern featureFromCompound(Compound compound, Feature feature) {
        //todo how do we get the real dev? maybe load profile/ from outside
        MS2MassDeviation dev = PropertyManager.DEFAULTS.createInstanceWithDefaults(MS2MassDeviation.class);
//        exp.setName("rt=" + compound.location.rt + "-p=" + NUMBER_FORMAT.format(exp.getIonMass()));

        SimpleSpectrum isotopePattern = null; // is MFE spec
        List<Scan> ms1Spectra = new ArrayList<>();
        List<MSMSScan> ms2Spectra = new ArrayList<>();


        for (Spectrum spec : compound.getSpectrum()) {
            if (spec.type.equalsIgnoreCase("MFE") || spec.type.equalsIgnoreCase("FBF")) {
                isotopePattern = makeMs1Spectrum(spec); // todo do we have to remove the adduct peaks?
            } else if (spec.getMSDetails().getScanType().equals("Scan")) {
                ms1Spectra.add(makeMs1Scan(spec));
            } else if (spec.getMSDetails().getScanType().equals("ProductIon") && dev.standardMassDeviation.inErrorWindow(spec.mzOfInterest.mz.doubleValue(), feature.getAverageMass())) {
                ms2Spectra.add(makeMs2Scan(spec));
            } else {
                Optional<Spectrum> s = Optional.of(spec);
                LoggerFactory.getLogger(getClass()).warn("Spectrum of type '" + s.map(Spectrum::getType).orElse("N/A")
                        + "' with precursor mass '" + s.map(Spectrum::getMzOfInterest).map(MzOfInterest::getMz).map(BigDecimal::doubleValue).map(String::valueOf).orElse("N/A")
                        + "' is either not supported or it does not correspond to a supported precursor. Skipping this spectrum.");
            }
        }

        if (ms1Spectra.isEmpty()) { //if empty use MFE and FBF spectra
            for (Spectrum spec : compound.getSpectrum()) {
                if (spec.getMSDetails().getScanType().equals("Scan")) {
                    ms1Spectra.add(makeMs1Scan(spec));
                }
            }
        }

        // TODO handle MS2 spectra
//        feature.setMsms(ms2Spectra);

        return (isotopePattern != null) ? new IsotopePattern(isotopePattern, IsotopePattern.Type.REPRESENTATIVE) : null;
    }

    private MSMSScan makeMs2Scan(Spectrum spec) {
        MSMSScan.MSMSScanBuilder b = MSMSScan.builder();
        //MFE scans are not real scans. so there is rt range instead of a scan time?
        parseRT(spec).map(RetentionTime::getMiddleTime).ifPresent(b::scanTime);
        return b.peaks(makeMs1Spectrum(spec))
                .mzOfInterest(spec.getMzOfInterest().getMz().doubleValue())
                .collisionEnergy(parseCE(spec))
                .build();
    }


    private Scan makeMs1Scan(Spectrum spec) {
        Scan.ScanBuilder b = Scan.builder();
        b.peaks(makeMs1Spectrum(spec));
        parseRT(spec).map(RetentionTime::getMiddleTime).ifPresent(b::scanTime);
        return b.build();
    }
}

