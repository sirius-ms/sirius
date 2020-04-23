package de.unijena.bioinf.canopus;

import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;

import java.util.*;

class CompoundClass {
    protected final short index;
    protected final ClassyfireProperty ontology;
    protected final List<LabeledCompound> compounds;

    CompoundClass(short index, ClassyfireProperty ontology) {
        this.index = index;
        this.ontology = ontology;
        this.compounds = new ArrayList<>(500);
    }

    public List<LabeledCompound> drawExamples(int number, Random r) {
        if (compounds.size() > number*20) {
            final HashMap<String, LabeledCompound> compoundHashMap = new HashMap<>();
            while (compoundHashMap.size() < number) {
                LabeledCompound c = compounds.get(r.nextInt(compounds.size()));
                compoundHashMap.put(c.inchiKey, c);
            }
            return new ArrayList<>(compoundHashMap.values());
        } else {
            final ArrayList<LabeledCompound> examples = new ArrayList<>(compounds);
            Collections.shuffle(examples);
            return examples.subList(0, number);
        }
    }
}
