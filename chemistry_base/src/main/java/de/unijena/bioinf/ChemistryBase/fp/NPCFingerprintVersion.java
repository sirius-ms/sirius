package de.unijena.bioinf.ChemistryBase.fp;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class NPCFingerprintVersion extends FingerprintVersion {

    private static final NPCFingerprintVersion SINGLETON;

    static {
        NPCFingerprintVersion v = null;
        try {
            v = readFromClasspath();
        } catch (IOException e) {
            e.printStackTrace();
            LoggerFactory.getLogger(NPCFingerprintVersion.class).error("Cannot load NPC fingerprint version.");
        }
        SINGLETON = v;
    }

    public static NPCFingerprintVersion get() {
        return SINGLETON;
    }

    private static NPCFingerprintVersion readFromClasspath() throws IOException {
        final ArrayList<NPCProperty> pathway = new ArrayList<>();
        for (String[] cols : FileUtils.readTable(FileUtils.ensureBuffering(new InputStreamReader(NPCFingerprintVersion.class.getResourceAsStream("/fingerprints/npc/pathways.csv"))))) {
            pathway.add(new NPCProperty(
               cols[0],
               NPCLevel.PATHWAY,
               Integer.parseInt(cols[1])
            ));
        }
        final ArrayList<NPCProperty> superclass = new ArrayList<>();
        for (String[] cols : FileUtils.readTable(FileUtils.ensureBuffering(new InputStreamReader(NPCFingerprintVersion.class.getResourceAsStream("/fingerprints/npc/superclasses.csv"))))) {
            superclass.add(new NPCProperty(
                    cols[0],
                    NPCLevel.SUPERCLASS,
                    Integer.parseInt(cols[1])
            ));
        }
        final ArrayList<NPCProperty> klass = new ArrayList<>();
        for (String[] cols : FileUtils.readTable(FileUtils.ensureBuffering(new InputStreamReader(NPCFingerprintVersion.class.getResourceAsStream("/fingerprints/npc/classes.csv"))))) {
            klass.add(new NPCProperty(
                    cols[0],
                    NPCLevel.CLASS,
                    Integer.parseInt(cols[1])
            ));
        }
        return new NPCFingerprintVersion(pathway,superclass,klass);
    }

    NPCProperty[] properties, pathways, superclasses, classes;

    protected NPCFingerprintVersion(List<NPCProperty> pathways, List<NPCProperty> superclasses, List<NPCProperty> classes) {
        this.pathways = pathways.toArray(NPCProperty[]::new);
        this.superclasses = superclasses.toArray(NPCProperty[]::new);
        this.classes = classes.toArray(NPCProperty[]::new);
        ArrayList<NPCProperty> prop = new ArrayList<>(pathways);
        prop.addAll(superclasses);
        prop.addAll(classes);
        this.properties = prop.toArray(NPCProperty[]::new);
    }

    @Override
    public NPCProperty getMolecularProperty(int index) {
        return properties[index];
    }

    @Override
    public int size() {
        return properties.length;
    }

    @Override
    public boolean compatible(FingerprintVersion fingerprintVersion) {
       return identical(fingerprintVersion);
    }

    @Override
    public boolean identical(FingerprintVersion fingerprintVersion) {
        if (fingerprintVersion==this) return true;
        if (fingerprintVersion instanceof NPCFingerprintVersion) {
            return Arrays.equals(properties, ((NPCFingerprintVersion) fingerprintVersion).properties);
        } else return false;
    }

    public static enum NPCLevel {
        PATHWAY("Pathway", 0), SUPERCLASS("Superclass", 1), CLASS("Class", 2);
        public final String name;
        public final int level;

        private NPCLevel(String name, int level) {
            this.name = name;
            this.level = level;
        }
    }

    public static class NPCProperty extends MolecularProperty{
        public final String name;
        public final NPCLevel level;
        public final int npcIndex;

        protected NPCProperty(String name, NPCLevel level, int npcIndex) {
            this.name = name;
            this.level = level;
            this.npcIndex = npcIndex;
        }

        @Override
        public String getDescription() {
            return level.name + ": " + name;
        }

        public String getName() {
            return name;
        }

        public NPCLevel getLevel() {
            return level;
        }

        public int getNpcIndex() {
            return npcIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NPCProperty that = (NPCProperty) o;
            return npcIndex == that.npcIndex &&
                    name.equals(that.name) &&
                    level == that.level;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, level, npcIndex);
        }
    }
}
