package de.unijena.bioinf.ms.biotransformer;

import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.List;

public record BioTransformerResult(IAtomContainer originSubstrate, List<BioTransformation> biotranformations) {

}