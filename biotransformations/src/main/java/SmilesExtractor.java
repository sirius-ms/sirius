import biotransformer.btransformers.Biotransformer;
import biotransformer.transformation.Biotransformation;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;

import java.util.ArrayList;
import java.util.List;

public class SmilesExtractor {
    public List <String> extractSmilesFromTransformation(ArrayList<Biotransformation> transformations, Biotransformer transformer){
        List<String> smiles = new ArrayList<>();
        try {
            IAtomContainerSet products = transformer.extractProductsFromBiotransformations(transformations);
            for (IAtomContainer product : products.atomContainers()){
                smiles.add(transformer.smiGen.create(product));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return smiles;
    }

    public List<String> extractSmilesFromTransformation(ArrayList<Biotransformation> transformations, InChIGeneratorFactory inChIGenFactory) {
        List<String> smiles = new ArrayList<>();
        try {
            for (Biotransformation transformation : transformations) {
                IAtomContainerSet products = transformation.getProducts();
                for (IAtomContainer product : products.atomContainers()) {
                    // Verwende die InChIGeneratorFactory, um SMILES zu generieren
                    String smilesString = inChIGenFactory.getInChIGenerator(product).getInchiKey();
                    smiles.add(smilesString);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return smiles;
    }

}
