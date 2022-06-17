import de.unijena.bioinf.canopus.Canopus;
import de.unijena.bioinf.babelms.projectspace.*;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class ProjectSpaceTest {

    public static void main(String[] args) throws IOException {
        PropertyManager.setProperty("de.unijena.bioinf.fingerid.web.host", "https://www.csi-fingerid.uni-jena.de");
        PropertyManager.setProperty("de.unijena.bioinf.sirius.version", "4.1.0-SNAPSHOT");
        PropertyManager.setProperty("de.unijena.bioinf.sirius.build", "666");
        PropertyManager.setProperty("de.unijena.bioinf.fingerid.version", "1.2.0-SNAPSHOT");
        PropertyManager.setProperty("de.unijena.bioinf.sirius.fingerID.cache", "/home/fleisch/.sirius/csi_fingerid_cache");
        File root1 = new File("/home/fleisch/work/sirius_testing/ws (copy)");
        File root3 = new File("/home/fleisch/work/sirius_testing/merged_in");
        File root2 = new File("/home/fleisch/work/sirius_testing/CSIfingerID_output_BA_QE_2iso5 (copy)");
        File root = new File("/home/fleisch/work/sirius_testing/merged_out");
        File rootZip = new File("/home/fleisch/work/sirius_testing/bigTest (copy).zip");
        if (args.length > 0)
            root = new File(args[0]);

       /* final WebAPI api = ApplicationCore.WEB_API;
        final Canopus canopus = Canopus.loadFromFile(new File("/home/fleisch/work/sirius_testing/canopus/canopus_fp.data"));
//        SiriusProjectSpace space = SiriusProjectSpaceIO.create(null, rootZip,
        SiriusProjectSpace space = SiriusProjectSpaceIO.create(rootZip, Arrays.asList(root1,root3,root2), null,
                (cur,max,mess) ->{System.out.println((((((double)cur)/(double)max)) * 100d) + "%");},
                new IdentificationResultSerializer(), new FingerIdResultSerializer(api), new CanopusResultSerializer(canopus));
        space.registerSummaryWriter(new MztabSummaryWriter());
        space.writeSummaries((cur, max, mess) -> System.out.println((((((double) cur) / (double) max)) * 100d) + "% " + mess));
        space.close();*/
        System.out.println("done!");
    }
}
