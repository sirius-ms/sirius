package de.unijena.bioinf.cmlFragmentation;

import org.openscience.cdk.interfaces.IBond;

import java.util.Arrays;
import java.util.List;

public class SimpleFragmentationRule implements FragmentationRules {

    private final List<String> allowedElements;

    public SimpleFragmentationRule(String[] allowedElements){
        this.allowedElements = Arrays.asList(allowedElements);
    }

    @Override
    public boolean match(IBond bond) {
        String atom1Symbol = bond.getAtom(0).getSymbol();
        String atom2Symbol = bond.getAtom(1).getSymbol();
        return (this.allowedElements.contains(atom1Symbol) && !this.allowedElements.contains(atom2Symbol)) ||
                (!this.allowedElements.contains(atom1Symbol) && this.allowedElements.contains(atom2Symbol));
    }
}