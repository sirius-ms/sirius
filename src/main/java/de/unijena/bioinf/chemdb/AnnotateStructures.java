package de.unijena.bioinf.chemdb;

import java.util.List;

public interface AnnotateStructures {

    /**
     * lookup database sources. Add links to the compound candidate
     */
    public void annotateCompounds(List<? extends CompoundCandidate> sublist) throws DatabaseException;

}
