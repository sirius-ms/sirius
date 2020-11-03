package de.unijena.bioinf.canopus;

import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MolecularProperty;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class NPCFingerprintVersion extends FingerprintVersion {

    public static NPCFingerprintVersion readFromDirectory(File directory) throws IOException {
        final ArrayList<NPCProperty> pathway = new ArrayList<>();
        final File pathways = new File(directory, "pathways.csv");
        for (String[] cols : FileUtils.readTable(pathways)) {
            pathway.add(new NPCProperty(
               cols[0],
               NPCLevel.PATHWAY,
               Integer.parseInt(cols[1])
            ));
        }
        final File superclasses = new File(directory, "superclasses.csv");
        final ArrayList<NPCProperty> superclass = new ArrayList<>();
        for (String[] cols : FileUtils.readTable(superclasses)) {
            superclass.add(new NPCProperty(
                    cols[0],
                    NPCLevel.SUPERCLASS,
                    Integer.parseInt(cols[1])
            ));
        }
        final File classes = new File(directory, "classes.csv");
        final ArrayList<NPCProperty> klass = new ArrayList<>();
        for (String[] cols : FileUtils.readTable(classes)) {
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
    public MolecularProperty getMolecularProperty(int index) {
        return properties[index];
    }

    @Override
    public int size() {
        return properties.length;
    }

    @Override
    public boolean compatible(FingerprintVersion fingerprintVersion) {
        if (fingerprintVersion==this) return true;
        if (fingerprintVersion instanceof NPCFingerprintVersion) {
            return Arrays.equals(properties, ((NPCFingerprintVersion) fingerprintVersion).properties);
        } else return false;
    }

    public static enum NPCLevel {
        PATHWAY("Pathway", 0), SUPERCLASS("Superclass", 1), CLASS("class", 2);
        protected String name;
        protected int level;

        private NPCLevel(String name, int level) {
            this.name = name;
            this.level = level;
        }
    }

    public static class NPCProperty extends MolecularProperty{
        protected final String name;
        protected final NPCLevel level;
        protected final int npcIndex;

        protected NPCProperty(String name, NPCLevel level, int npcIndex) {
            this.name = name;
            this.level = level;
            this.npcIndex = npcIndex;
        }

        @Override
        public String getDescription() {
            return level.name + ": " + name;
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
