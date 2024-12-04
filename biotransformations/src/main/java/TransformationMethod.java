import biotransformer.transformation.Biotransformation;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.List;
public interface TransformationMethod {
    List <Biotransformation> transform(List<IAtomContainer> inputs);
}
