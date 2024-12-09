import biotransformer.biosystems.BioSystem;
import org.junit.jupiter.api.Test;


import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class BiotransformerTest {

    @Test
    public void testCalculateTransformations() {
        BiotransformerWrapper wrapper = new BiotransformerWrapper( BioSystem.BioSystemName.HUMAN, true, false, Set.of("biotransformer")
        );

        String inputMolecule = "C1=CC=CC=C1"; // Benzol
        List<String> results = wrapper.calculateTransformations(inputMolecule);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        System.out.println("Biotransformer Results: " + results);
    }
}
