package de.unijena.bioinf.sirius.elementdetection;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.FormulaSettings;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.elementdetection.transformer.TransformerBasedPredictor;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Set;

public class TransformerElementDetector implements ElementDetection{

    private final TransformerBasedPredictor predictor;
    private final Set<Element> predictableElements;

    public TransformerElementDetector() {
        this(readFromClassPath());
    }

    private static TransformerBasedPredictor readFromClassPath() {
        try (final InputStream stream = TransformerElementDetector.class.getResourceAsStream("/transformer.bin");
             ReadableByteChannel channel = Channels.newChannel(stream)) {
            ByteBuffer buffer = ByteBuffer.allocate(1000*300);
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

    @Override
    public DetectedFormulaConstraints detect(ProcessedInput processedInput) {
        final FormulaSettings settings = processedInput.getAnnotationOrDefault(FormulaSettings.class);
        return null;
    }

    @Override
    public Set<Element> getPredictableElements() {
        return Set.of();
    }

    private void checkDetectableElements(FormulaSettings settings){
        //todo this check is performed for each compound. Rather do it once.
        final ChemicalAlphabet detectable = settings.getAutoDetectionAlphabet();
        for (Element element : detectable) {
            if (!predictableElements.contains(element)) {
                LoggerFactory.getLogger(DeepNeuralNetworkElementDetector.class).warn(element.getSymbol()+" was specified but is not detectable.");
            }
        }
    }
}
