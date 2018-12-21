package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.ms.properties.PropertyManager;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class ProjectSpaceTest {

    public static void main(String[] args) throws IOException {
        Properties props = PropertyManager.PROPERTIES;
        File root = new File("/home/fleisch/work/sirius_testing/ws");
        if (args.length > 0)
            root = new File(args[0]);

        SiriusProjectSpace space = SiriusProjectSpace.create(null, root, new IdentificationResultSerializer());
        space.close();
        System.out.println("done!");

    }
}
