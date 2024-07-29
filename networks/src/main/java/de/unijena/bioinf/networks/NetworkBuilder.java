package de.unijena.bioinf.networks;

public interface NetworkBuilder {

    public MolecularNetwork addEdgesFromSimilarityMatrix(MolecularNetwork.NetworkBuilder network, float[][] similarityMatrix);

}
