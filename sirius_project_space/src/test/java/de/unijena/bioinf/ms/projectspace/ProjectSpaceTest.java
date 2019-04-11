package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.ms.io.projectspace.IdentificationResultSerializer;
import de.unijena.bioinf.ms.io.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.ms.properties.PropertyManager;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class ProjectSpaceTest {

    public static void main(String[] args) throws IOException {
        PropertyManager.setProperty("de.unijena.bioinf.fingerid.web.host", "https://www.csi-fingerid.uni-jena.de");
        PropertyManager.setProperty("de.unijena.bioinf.fingerid.db.date", "2017-08-28");
        PropertyManager.setProperty("de.unijena.bioinf.sirius.version", "4.0.5-SNAPSHOT");
        PropertyManager.setProperty("de.unijena.bioinf.sirius.build", "666");
        PropertyManager.setProperty("de.unijena.bioinf.fingerid.version", "1.1.5-SNAPSHOT");
        PropertyManager.setProperty("de.unijena.bioinf.sirius.fingerID.cache", "/home/fleisch/.sirius/csi_fingerid_cache");

        File root = new File("/home/fleisch/work/sirius_testing/ws (copy)");
        if (args.length > 0)
            root = new File(args[0]);

        SiriusProjectSpace space = SiriusProjectSpace.create(null, root, (cur, max, mess) -> {
                    System.out.println((((((double) cur) / (double) max)) * 100d) + "%");
                },
                new IdentificationResultSerializer());

        space.writeSummaries(space.parseExperiments(), (cur, max, mess) -> System.out.println((((((double) cur) / (double) max)) * 100d) + "% " + mess));
        space.close();
        System.out.println("done!");

    }
}
