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

import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidCreator;
import de.unijena.bioinf.ChemistryBase.chem.FeatureGroup;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.ms.properties.PropertyManager;
import io.hypersistence.tsid.TSID;
import org.apache.commons.io.input.ReaderInputStream;
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
import java.net.URI;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static de.unijena.bioinf.babelms.cef.CEFUtils.*;

public class AgilentCefExperimentParser implements Parser<Ms2Experiment> {
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#0.000");
    private final static QName qName = new QName("Compound");
    private Unmarshaller unmarshaller;
    private XMLEventReader xmlEventReader;

    private InputStream currentStream = null;
    private URI currentUrl = null;


    private Iterator<Ms2Experiment> iterator = null;


    @Override
    public Ms2Experiment parse(BufferedReader reader, @Nullable URI source) throws IOException {
        return parse(source, () -> new ReaderInputStream(reader, Charset.defaultCharset()));
    }

    @Override
    public Ms2Experiment parse(@NotNull InputStream stream, @Nullable URI source) throws IOException {
        return parse(source, () -> stream);
    }

    private Ms2Experiment parse(@Nullable URI source, Supplier<InputStream> streamSupplier) throws IOException {
        //XML parsing on readers works bad, so we create our own stream from url
        if (iterator != null && iterator.hasNext()) {
            return iterator.next();
        } else if (!Objects.equals(currentUrl, source) || xmlEventReader == null || unmarshaller == null) {
            try {
                currentUrl = source;
                currentStream = streamSupplier.get();


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
            XMLEvent e;
            // loop though the xml stream
            while ((e = xmlEventReader.peek()) != null) {
                // check the event is a Document start element
                if (e.isStartElement() && ((StartElement) e).getName().equals(qName)) {
                    // unmarshall the compound
                    Compound compound = unmarshaller.unmarshal(xmlEventReader, Compound.class).getValue();
                    iterator = experimentFromCompound(compound).iterator();
                    return iterator.hasNext() ? iterator.next() : null;
                } else {
                    xmlEventReader.next();
                }
            }
        } catch (XMLStreamException | JAXBException e) {
            throw new IOException("Error when parsing Compound!", e);
        }

        return null;

    }

    private List<Ms2Experiment> experimentFromCompound(Compound compound) {
        long cuuid = TSID.fast().toLong();
        RetentionTime rt = compound.getSpectrum().stream()
                .filter(s -> s.getMSDetails().scanType.equals("Scan"))
                .map(s -> parseRT(compound, s))
                .filter(Optional::isPresent).flatMap(Optional::stream)
                .reduce(RetentionTime::merge).orElse(parseRT(compound).orElse(null));

        final FeatureGroup fg = FeatureGroup.builder()
                .groupId(cuuid)
                .groupRt(rt)
                .build();

        List<Ms2Experiment> exps;
        if (compound.getSpectrum().stream().anyMatch(s -> s.getType().equalsIgnoreCase("MFE") || s.getType().equalsIgnoreCase("FBF"))) { //MFE/FBF data
            exps = experimentFromMFECompound(compound);
        } else if (compound.getSpectrum().stream().noneMatch(s -> s.getMSDetails().getScanType().equals("ProductIon"))) { //ms1 only data from raw format
            exps = experimentFromMS1OnlyCompound(compound);
        } else {
            exps = experimentFromRawCompound(compound);
        }

        exps.forEach(exp -> {
            exp.setAnnotation(FeatureGroup.class, fg);
            exp.addAnnotationIfAbsend(RetentionTime.class, fg.getGroupRt());
        });

        return exps;
    }

    private static final Pattern UNSUPPORTED_IONTYPE_MATCHER = Pattern.compile("^\\d+M.*");
    private static final Pattern ISOTOPE_PEAK_MATCHER = Pattern.compile("\\+\\d+$");

    private List<Ms2Experiment> experimentFromMFECompound(Compound compound) {
        final Spectrum mfe = compound.getSpectrum().stream()
                .filter(s ->
                        s.getType().equalsIgnoreCase("MFE") || s.getType().equalsIgnoreCase("FBF")
                ).findAny().orElseThrow(() -> new IllegalArgumentException("Compound must contain a MFE/FBF spectrum to be parsed as MFE/FBF spectrum!"));

        List<Ms2Experiment> siriusCompounds = new ArrayList<>();

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
            MutableMs2Experiment exp = new MutableMs2Experiment();
            exp.setIonMass(p.getX().doubleValue());
            exp.setPrecursorIonType(PrecursorIonType.fromString("[" + p.getS() + "]" + mfe.getMSDetails().p));
            siriusCompounds.add(experimentFromCompound(compound, exp));
        });

        // We are not sure what adduct it was, unlike when multiple adducts were detected
        if (siriusCompounds.size() == 1) {
            MutableMs2Experiment exp = (MutableMs2Experiment) siriusCompounds.get(0);
            exp.setPrecursorIonType(PrecursorIonType.unknown(exp.getPrecursorIonType().getCharge()));
        }

        return siriusCompounds;
    }

    private List<Ms2Experiment> experimentFromMS1OnlyCompound(Compound compound) {
        MutableMs2Experiment exp = new MutableMs2Experiment();
        Spectrum ms = compound.getSpectrum().stream()
                .filter(s -> s.getMSDetails().getScanType().equals("Scan"))
                .filter(s -> s.mzOfInterest != null)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No spectrum (neither MS1 nor MS/MS) with precursor information (MzOfInterest) found for compound at rt " + compound.location.rt));

        exp.setPrecursorIonType(ms.getMSDetails().p.equals("-") ? PrecursorIonType.unknownNegative() : PrecursorIonType.unknownPositive());
        exp.setIonMass(ms.mzOfInterest.getMz().doubleValue());
        return List.of(experimentFromCompound(compound, exp));
    }

    private List<Ms2Experiment> experimentFromRawCompound(Compound compound) {
        MutableMs2Experiment exp = new MutableMs2Experiment();

        Spectrum s = compound.getSpectrum().stream().filter(spec -> spec.getMSDetails().getScanType().equals("ProductIon")).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No MS/MS spectrum found for compound at rt " + compound.location.rt));
        exp.setPrecursorIonType(s.getMSDetails().p.equals("-") ? PrecursorIonType.unknownNegative() : PrecursorIonType.unknownPositive());
        exp.setIonMass(s.mzOfInterest.getMz().doubleValue());

        return List.of(experimentFromCompound(compound, exp));
    }

    private Ms2Experiment experimentFromCompound(Compound compound, MutableMs2Experiment exp) {
        MS2MassDeviation dev = PropertyManager.DEFAULTS.createInstanceWithDefaults(MS2MassDeviation.class);
        exp.setName("rt" + NUMBER_FORMAT.format(compound.location.rt) + "-p" + NUMBER_FORMAT.format(exp.getIonMass()));
        if (currentUrl != null)
            exp.setSource(new SpectrumFileSource(currentUrl));

        List<SimpleSpectrum> ms1Spectra = new ArrayList<>();
        List<RetentionTime> ms1Rts = new ArrayList<>();
        List<MutableMs2Spectrum> ms2Spectra = new ArrayList<>();
        List<RetentionTime> ms2Rts = new ArrayList<>();

        for (Spectrum spec : compound.getSpectrum()) {
            if (spec.type.equalsIgnoreCase("MFE") || spec.type.equalsIgnoreCase("FBF")) {
                // ignore was already handled beforehand.
            } else if (spec.getMSDetails().getScanType().equals("Scan")) {
                ms1Spectra.add(makeMs1Spectrum(spec));
                parseRT(spec).ifPresent(ms1Rts::add);
            } else if (spec.getMSDetails().getScanType().equals("ProductIon") && dev.standardMassDeviation.inErrorWindow(spec.mzOfInterest.mz.doubleValue(), exp.getIonMass())) {
                ms2Spectra.add(makeMs2Spectrum(spec));
                parseRT(spec).ifPresent(ms2Rts::add);
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
                    ms1Spectra.add(makeMs1Spectrum(spec));
                    if (!exp.hasAnnotation(RetentionTime.class))
                        parseRT(spec).ifPresent(ms1Rts::add);
                }
            }
        }

        //parse RT
        RetentionTime rt = ms2Rts.stream().reduce(RetentionTime::merge).orElse(
                ms1Rts.stream().reduce(RetentionTime::merge).orElse(null));

        if (rt != null)
            exp.setAnnotation(RetentionTime.class, rt);

        exp.setMs1Spectra(ms1Spectra);
        exp.setMs2Spectra(ms2Spectra);
        return exp;
    }
}

