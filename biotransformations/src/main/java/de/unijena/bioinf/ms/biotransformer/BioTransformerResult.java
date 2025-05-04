package de.unijena.bioinf.ms.biotransformer;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public record BioTransformerResult(List<BioTransformation> biotranformations) {
    /**
     * Sammelt alle Produkt-IAtomContainer aus allen Biotransformationen in diesem Ergebnis.
     * Kapselt den direkten Zugriff auf die externe Biotransformation-Bibliothek.
     *
     * @return Eine Liste aller Produkt-IAtomContainer.
     */
    public List<IAtomContainer> getAllProductContainers() {
        return biotranformations.stream()
                .flatMap(transformation -> {
                    // Hier erfolgt der Zugriff auf die externe Bibliothek, aber gekapselt in dieser Methode.
                    IAtomContainerSet products = transformation.getProducts();
                    // Konvertiere das Iterable<IAtomContainer> in einen Stream
                    return StreamSupport.stream(products.atomContainers().spliterator(), false);
                })
                .collect(Collectors.toList());
    }
}