package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

public class SimpleSpectraTree extends BasicSpectraTree<Spectrum<Peak>, Peak> {

    SimpleSpectraTree(Node<Spectrum<Peak>, Peak> root) {
        super(root);
    }
}
