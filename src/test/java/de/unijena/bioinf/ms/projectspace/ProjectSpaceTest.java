package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.canopus.Canopus;
import de.unijena.bioinf.fingerid.webapi.WebAPI;
import de.unijena.bioinf.ms.io.projectspace.CanopusResultSerializer;
import de.unijena.bioinf.ms.io.projectspace.FingerIdResultSerializer;
import de.unijena.bioinf.ms.io.projectspace.IdentificationResultSerializer;
import de.unijena.bioinf.ms.io.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.ms.properties.PropertyManager;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class ProjectSpaceTest {

    public static void main(String[] args) throws IOException {
        PropertyManager.setProperty("de.unijena.bioinf.fingerid.web.host","https://www.csi-fingerid.uni-jena.de");
        PropertyManager.setProperty("de.unijena.bioinf.fingerid.db.date","2017-08-28");
        PropertyManager.setProperty("de.unijena.bioinf.sirius.version","4.1.0-SNAPSHOT");
        PropertyManager.setProperty("de.unijena.bioinf.fingerid.version","1.2.0-SNAPSHOT");
        PropertyManager.setProperty("de.unijena.bioinf.sirius.fingerID.cache", "/home/fleisch/.sirius/csi_fingerid_cache");
//        File root = new File("/home/fleisch/work/sirius_testing/ws (copy)");
        File root = new File("/home/fleisch/work/sirius_testing/CSIfingerID_output_BA_QE_2iso5 (copy)");
        if (args.length > 0)
            root = new File(args[0]);

        final WebAPI api = new WebAPI();
        final Canopus canopus =  Canopus.loadFromFile(new File("/home/fleisch/work/sirius_testing/canopus/canopus_fp.data"));
        SiriusProjectSpace space = SiriusProjectSpace.create(null, root,
                new IdentificationResultSerializer(), new FingerIdResultSerializer(api), new CanopusResultSerializer(canopus));
        space.writeSummaries(space.parseExperiments()); //this can be cached
        space.close();
        System.out.println("done!");
    }
}
