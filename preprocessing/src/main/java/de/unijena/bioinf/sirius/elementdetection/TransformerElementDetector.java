package de.unijena.bioinf.sirius.elementdetection;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.ms.DetectedElements;
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms1IsotopePattern;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.elementdetection.transformer.TransformerBasedPredictor;
import de.unijena.bioinf.sirius.elementdetection.transformer.TransformerPrediction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Optional;
import java.util.Set;

public class TransformerElementDetector implements ElementDetection{

    private final TransformerBasedPredictor predictor;
    private final Set<Element> predictableElements;

    public TransformerElementDetector() {
        this(DEFAULT_INSTANCE);
    }

    private static final TransformerBasedPredictor DEFAULT_INSTANCE = readFromClassPath();
    private static TransformerBasedPredictor readFromClassPath() {
        try (final InputStream stream = TransformerElementDetector.class.getResourceAsStream("/transformer.bin");
             ReadableByteChannel channel = Channels.newChannel(stream)) {
            ByteBuffer buffer = ByteBuffer.allocate(1500*1000);
            channel.read(buffer);
            buffer.flip();
            return TransformerBasedPredictor.read(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TransformerElementDetector(TransformerBasedPredictor predictor) {
        this.predictor = predictor;
        this.predictableElements = Set.of(predictor.getPredictableElements());
    }

    public TransformerBasedPredictor getPredictor() {
        return predictor;
    }

    @Override
    public DetectedElements detect(ProcessedInput processedInput) {
        SimpleSpectrum ms1 = processedInput.getAnnotationOrThrow(Ms1IsotopePattern.class).getSpectrum();

        // at this stage we are only interested in predictions of the most-left peak
        Optional<TransformerPrediction> maybePrediction = ms1.size() > 0 ? predictor.predict(ms1, 0) : Optional.empty();

        if (maybePrediction.isEmpty()) return DetectedElements.singleton(DetectedElements.Source.ISOTOPE_PATTERN_DETECTION);

        TransformerPrediction prediction = maybePrediction.get();
        return DetectedElements.singleton(DetectedElements.Source.ISOTOPE_PATTERN_DETECTION, prediction.getPredictions());
    }

    @Override
    public Set<Element> getPredictableElements() {
        return Set.of(predictor.getPredictableElements());
    }
}
