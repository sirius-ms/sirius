import de.unijena.bioinf.io.lcms.MzXMLParser;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.model.lcms.LCMSRun;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class TestMZXMLParser {



    @Test
    public void testMs2CosineDetector() {
        final LCMSProccessingInstance instance = new LCMSProccessingInstance();
        for (File f : Arrays.stream(new File("/home/kaidu/analysis/canopus/agp_julia/peak/Low_plant").listFiles()).toArray(File[]::new)) {
            try {
                final LCMSRun run = new MzXMLParser().parse(f, instance.getStorage());
                instance.addSample(run);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        instance.detectFeatures();
        instance.alignFeatures();
    }

}
