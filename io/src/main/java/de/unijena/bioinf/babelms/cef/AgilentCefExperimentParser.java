package de.unijena.bioinf.babelms.cef;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.Parser;
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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AgilentCefExperimentParser implements Parser<Ms2Experiment> {
    private final static QName qName = new QName("Compound");
    private Unmarshaller unmarshaller;
    private XMLEventReader xmlEventReader;

    private InputStream currentStream = null;
    private URL currentUrl = null;

    @Override
    public <S extends Ms2Experiment> S parse(@Nullable BufferedReader ignored, @NotNull URL source) throws IOException {
        //XML parsing on readers works bad, so we create our own stream from url
        if (!Objects.equals(currentUrl, source) || xmlEventReader == null || unmarshaller == null) {
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
                    CEF.CompoundList.Compound compound = unmarshaller.unmarshal(xmlEventReader, CEF.CompoundList.Compound.class).getValue();
                    return experimentFromCompound(compound);
                } else {
                    xmlEventReader.next();
                }
            }
        } catch (XMLStreamException | JAXBException e) {
            throw new IOException("Error when parsing Compound!", e);
        }

        return null;

    }

    private <S extends Ms2Experiment> S experimentFromCompound(CEF.CompoundList.Compound compound) {
        MutableMs2Experiment exp = new MutableMs2Experiment();
        List<SimpleSpectrum> ms1Spectra = new ArrayList<>();
        List<MutableMs2Spectrum> ms2Spectra = new ArrayList<>();

        exp.setName("rt=" + compound.location.rt + ",y=" + compound.location.y);

        for (CEF.CompoundList.Compound.Spectrum spec : compound.getSpectrum()) {
            if (spec.type.contains("MS1")) {
                ms1Spectra.add(makeMs1Spectrum(spec));
            } else if (spec.type.contains("MS2")) {
                ms2Spectra.add(makeMs2Spectrum(spec));
                exp.setIonMass(spec.mzOfInterest.getMz());
                exp.setPrecursorIonType(spec.getMSDetails().p.equals("+") ? PrecursorIonType.unknownPositive() : PrecursorIonType.unknownNegative());
            } else {
                LoggerFactory.getLogger(getClass()).warn("Cannot handle spectrum of type '" + spec.getType() + "'. Skipping this spectrum");
            }
        }

        exp.setMs1Spectra(ms1Spectra);
        exp.setMs2Spectra(ms2Spectra);
        return (S) exp;
    }

    private MutableMs2Spectrum makeMs2Spectrum(CEF.CompoundList.Compound.Spectrum spec) {
        return new MutableMs2Spectrum(
                makeMs1Spectrum(spec),
                spec.getMzOfInterest().getMz(),
                CollisionEnergy.fromString(spec.msDetails.fv.replace("V", "ev")),
                2
        );
    }

    SimpleSpectrum makeMs1Spectrum(CEF.CompoundList.Compound.Spectrum spec) {
        return new SimpleSpectrum(
                spec.msPeaks.p.stream().mapToDouble(CEF.CompoundList.Compound.Spectrum.MSPeaks.P::getX).toArray(),
                spec.msPeaks.p.stream().mapToDouble(CEF.CompoundList.Compound.Spectrum.MSPeaks.P::getY).toArray()
        );
    }

}

