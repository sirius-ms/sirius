package de.unijena.bioinf.babelms.projectspace;

import de.unijena.bioinf.ms.properties.PropertyManager;

import java.io.IOException;

public class ProjectSpaceTest {

    public static void main(String[] args) throws IOException {
        PropertyManager.setProperty("de.unijena.bioinf.fingerid.web.host", "https://www.csi-fingerid.uni-jena.de");
        PropertyManager.setProperty("de.unijena.bioinf.fingerid.db.date", "2017-08-28");
        PropertyManager.setProperty("de.unijena.bioinf.sirius.version", "4.1.0-SNAPSHOT");
        PropertyManager.setProperty("de.unijena.bioinf.sirius.build", "666");
        PropertyManager.setProperty("de.unijena.bioinf.fingerid.version", "1.2.0-SNAPSHOT");
        PropertyManager.setProperty("de.unijena.bioinf.sirius.fingerID.cache", "/home/fleisch/.sirius/csi_fingerid_cache");
/*
        File root = new File("/home/fleisch/work/sirius_testing/ws (copy)");
        if (args.length > 0)
            root = new File(args[0]);

        SiriusProjectSpace space = ProjectSpaceIO.(null, root, (cur, max, mess) -> {
                    System.out.println((((((double) cur) / (double) max)) * 100d) + "%");
                },
                new IdentificationResultSerializer());

        space.writeSummaries(space.parseExperiments(), (cur, max, mess) -> System.out.println((((((double) cur) / (double) max)) * 100d) + "% " + mess));
        space.close();
        System.out.println("done!");
    */
    }
}
