package de.unijena.bioinf.babelms.cef;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class AgilentCefExperimentParser implements Parser<Ms2Experiment> {
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

    private <S extends Ms2Experiment> List<S> experimentFromMFECompound(Compound compound) {
        final Spectrum mfe = compound.getSpectrum().stream().filter(s -> s.getType().equals("MFE"))
                .findAny().orElseThrow(() -> new IllegalArgumentException("Compound must contain a MFE spectrum to be parsed as MFE spectrum!"));

        List<S> siriusCompounds = new ArrayList<>();

        mfe.msPeaks.getP().stream().filter(p -> !p.getS().matches("^M.*\\+\\d+$")).forEach(p -> {
            MutableMs2Experiment exp = new MutableMs2Experiment();
            exp.setIonMass(p.getX().doubleValue());
            exp.setPrecursorIonType(PrecursorIonType.fromString("[" + p.getS() + "]" + mfe.getMSDetails().p));
            siriusCompounds.add(experimentFromCompound(compound, exp));
        });

        return siriusCompounds;
    }

    private <S extends Ms2Experiment> List<S> experimentFromRawCompound(Compound compound) {
        MutableMs2Experiment exp = new MutableMs2Experiment();

        Spectrum s = compound.getSpectrum().stream().filter(spec -> spec.getMSDetails().getScanType().equals("ProductIon")).findFirst().orElseThrow(() -> new IllegalArgumentException("No MS2 spectrum found for compound at rt " + compound.location.rt));
        exp.setPrecursorIonType(s.getMSDetails().p.equals("-") ? PrecursorIonType.unknownNegative() : PrecursorIonType.unknownPositive());
        exp.setIonMass(s.mzOfInterest.getMz().doubleValue());

        return List.of(experimentFromCompound(compound, exp));
    }

    private <S extends Ms2Experiment> S experimentFromCompound(Compound compound, MutableMs2Experiment exp) {
        //todo how do we get the real dev? maybe load profile/ from outside
        MS2MassDeviation dev = PropertyManager.DEFAULTS.createInstanceWithDefaults(MS2MassDeviation.class);
        exp.setName("rt=" + compound.location.rt + ", adduct=" + exp.getPrecursorIonType().toString());
        exp.setSource(new SpectrumFileSource(currentUrl));

        List<SimpleSpectrum> ms1Spectra = new ArrayList<>();
        List<MutableMs2Spectrum> ms2Spectra = new ArrayList<>();
        for (Spectrum spec : compound.getSpectrum()) {
            if (spec.type.equals("MFE")) {
                // ignore was already handled beforehand.
            } else if (spec.getMSDetails().getScanType().equals("Scan")) {
                ms1Spectra.add(makeMs1Spectrum(spec));
            } else if (spec.getMSDetails().getScanType().equals("ProductIon") && dev.standardMassDeviation.inErrorWindow(spec.mzOfInterest.mz.doubleValue(), exp.getIonMass())) {
                ms2Spectra.add(makeMs2Spectrum(spec));
            } else {
                LoggerFactory.getLogger(getClass()).warn("Cannot handle spectrum of type '" + spec.getType() + "'. Skipping this spectrum");
            }
        }

        exp.setMs1Spectra(ms1Spectra);
        exp.setMs2Spectra(ms2Spectra);
        return (S) exp;
    }

    private MutableMs2Spectrum makeMs2Spectrum(Spectrum spec) {
        return new MutableMs2Spectrum(
                makeMs1Spectrum(spec),
                spec.getMzOfInterest().getMz().doubleValue(),
                CollisionEnergy.fromString(spec.msDetails.getCe().replace("V", "ev")),
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

