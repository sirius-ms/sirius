package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.fingerid.webapi.WebAPI;
import de.unijena.bioinf.ms.properties.PropertyManager;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class ProjectSpaceTest {

    public static void main(String[] args) throws IOException {
        Properties props = PropertyManager.PROPERTIES;
        props.setProperty("de.unijena.bioinf.fingerid.web.host","https://www.csi-fingerid.uni-jena.de");
        props.setProperty("de.unijena.bioinf.fingerid.db.date","2017-08-28");
        props.setProperty("de.unijena.bioinf.sirius.version","4.0.4-SNAPSHOT");
        props.setProperty("de.unijena.bioinf.fingerid.version","1.1.4-SNAPSHOT");
        props.setProperty("de.unijena.bioinf.sirius.fingerID.cache", "/home/fleisch/.sirius/csi_fingerid_cache");
        File root = new File("/home/fleisch/work/sirius_testing/ws (copy)");
//        File root = new File("/home/fleisch/work/sirius_testing/CSIfingerID_output_BA_QE_2iso5 (copy)");
        if (args.length > 0)
            root = new File(args[0]);

        final WebAPI api = new WebAPI();
        SiriusProjectSpace space = SiriusProjectSpace.create(null, root,
                new IdentificationResultSerializer(), new FingerIdResultSerializer(api), new CanopusResultSerializer());
        space.close();
        System.out.println("done!");
    }
}
