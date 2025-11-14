package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.lcms.adducts.assignment.AdductAssignment;
import de.unijena.bioinf.lcms.adducts.assignment.OptimalAssignmentViaBeamSearch;
import de.unijena.bioinf.lcms.isotopes.IsotopePattern;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType.getPrecursorIonType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdductAssignmentTest {


    @BeforeEach
    public void setUp() {

    }

    @Test
    public void testSimpleAdductNetwork() {
        final AdductNode[] simple = simpleExampleNetwork();
        final AdductManager manager = new AdductManager(1);
        AdductAssignment[] result = new OptimalAssignmentViaBeamSearch().resolve(manager, simple, 1);
        assertTrue(result[0].mostLikelyAdduct().ionType.equals(getPrecursorIonType("[M+H]+")));
        assertTrue(result[1].mostLikelyAdduct().ionType.equals(getPrecursorIonType("[M+Na]+")));
        assertTrue(result[2].mostLikelyAdduct().ionType.equals(getPrecursorIonType("[M+K]+")));
        assertTrue(result[3].mostLikelyAdduct().ionType.equals(getPrecursorIonType("[M+NH3+H]+")));
        assertTrue(result[4].likelyUnknown());
        assertTrue(result[5].likelyUnknown());
    }
    @Test
    public void testSimpleAdductNetworkWithLosses() {
        final AdductNode[] simple = simpleExampleNetworkWithLosses();
        final AdductManager manager = new AdductManager(1);
        manager.add(Set.of(getPrecursorIonType("[M-H2O+Na]+")), Set.of());
        AdductAssignment[] result = new OptimalAssignmentViaBeamSearch().resolve(manager, simple, 1);
        assertEquals((getPrecursorIonType("[M+H]+")), result[0].mostLikelyAdduct().ionType);
        assertEquals((getPrecursorIonType("[M+Na]+")), result[1].mostLikelyAdduct().ionType);
        assertEquals( (getPrecursorIonType("[M+K]+")), result[2].mostLikelyAdduct().ionType);
        assertEquals((getPrecursorIonType("[M+NH3+H]+")), result[3].mostLikelyAdduct().ionType);
        assertTrue(result[4].likelyUnknown());
        assertTrue(result[5].likelyUnknown());
        assertTrue(result[6].hasAdducts(getPrecursorIonType("[M+H]+"), getPrecursorIonType("[M-H2O+H]+")));
        assertTrue(result[7].hasAdducts(getPrecursorIonType("[M+H]+"), getPrecursorIonType("[M-H2O+H]+"),getPrecursorIonType("[M-H4O2+H]+")));
        assertTrue(result[8].hasAdducts(getPrecursorIonType("[M+Na]+"), getPrecursorIonType("[M-H2O+Na]+")));

        // every ion type is supposed to exist once
        for (int k=0; k < result.length; ++k) {
            for (int i=0; i < result[k].getIonTypes().length; ++i) {
                IonType ionType = result[k].getIonTypes()[i];
                assertEquals(1L, Arrays.stream(result[k].getIonTypes()).filter(x->x.equals(ionType)).count());
            }
        }

    }

    @Test
    public void testSimpleAdductNetworkWithLossesAndIsotopes() {
        final AdductNode[] simple = simpleExampleNetworkWithLossesAndIsotopes();
        final AdductManager manager = new AdductManager(1);
        manager.add(Set.of(getPrecursorIonType("[M-H2O+Na]+")), Set.of());
        AdductAssignment[] result = new OptimalAssignmentViaBeamSearch().resolve(manager, simple, 1);
        assertEquals((getPrecursorIonType("[M+H]+")), result[0].mostLikelyAdduct().ionType);
        assertEquals((getPrecursorIonType("[M+Na]+")), result[1].mostLikelyAdduct().ionType);
        assertEquals( (getPrecursorIonType("[M+K]+")), result[2].mostLikelyAdduct().ionType);
        assertEquals((getPrecursorIonType("[M+NH3+H]+")), result[3].mostLikelyAdduct().ionType);
        assertTrue(result[4].likelyUnknown());
        assertTrue(result[5].likelyUnknown());
        assertTrue(result[6].hasAdducts(getPrecursorIonType("[M+H]+"), getPrecursorIonType("[M-H2O+H]+")));

        /// ////
        assertEquals((getPrecursorIonType("[M+H+1i]+")), result[7].mostLikelyAdduct().ionType);
        assertEquals((getPrecursorIonType("[M+H+2i]+")), result[8].mostLikelyAdduct().ionType);
        assertEquals((getPrecursorIonType("[M+H+1i]+")), result[9].mostLikelyAdduct().ionType);
    }


    @Test
    public void testSimpleAdductNetworkWithLossesAndIsotopesAndMultimeres() {
        final AdductNode[] simple = simpleExampleNetworkWithLossesAndIsotopesAndMultimeres();
        final AdductManager manager = new AdductManager(1);
        manager.add(Set.of(getPrecursorIonType("[M-H2O+Na]+")), Set.of());

        AdductAssignment[] result = new OptimalAssignmentViaBeamSearch().resolve(manager, simple, 1);
        assertEquals((getPrecursorIonType("[M+H]+")), result[0].mostLikelyAdduct().ionType);
        assertEquals((getPrecursorIonType("[M+Na]+")), result[1].mostLikelyAdduct().ionType);
        assertEquals( (getPrecursorIonType("[M+K]+")), result[2].mostLikelyAdduct().ionType);
        assertEquals((getPrecursorIonType("[M+NH3+H]+")), result[3].mostLikelyAdduct().ionType);
        assertTrue(result[4].likelyUnknown());
        assertTrue(result[5].likelyUnknown());
        assertTrue(result[6].hasAdducts(getPrecursorIonType("[M+H]+"), getPrecursorIonType("[M-H2O+H]+")));

        /// ////
        assertEquals((getPrecursorIonType("[M+H+1i]+")), result[7].mostLikelyAdduct().ionType);
        assertEquals((getPrecursorIonType("[M+H+2i]+")), result[8].mostLikelyAdduct().ionType);
        assertEquals((getPrecursorIonType("[M+H+1i]+")), result[9].mostLikelyAdduct().ionType);

        /// ////
        assertEquals((getPrecursorIonType("[2M+H]+")), result[10].mostLikelyAdduct().ionType);
        assertEquals((getPrecursorIonType("[2M+Na]+")), result[11].mostLikelyAdduct().ionType);
        assertEquals((getPrecursorIonType("[2M+H+i]+")), result[12].mostLikelyAdduct().ionType);
        assertEquals((getPrecursorIonType("[2M+H]+")), result[13].mostLikelyAdduct().ionType);
    }

    /*
     * At the level of adduct assignment, as soon as we have an adduct network, we do only require:
     * - graph topology (nodes with indices, edges)
     * - scores on the edges
     */

    public AdductNode[] simpleExampleNetwork() {
        final double M = 128.0;
        PrecursorIonType HPlus = getPrecursorIonType("[M+H]+");
        PrecursorIonType NaPlus = getPrecursorIonType("[M+Na]+");
        PrecursorIonType Potassium = getPrecursorIonType("[M+K]+");
        PrecursorIonType NH3 = getPrecursorIonType("[M+NH3+H]+");

        //
        AdductNode m_h, m_na, m_k, m_nh3;
        m_h = mockNode(0, "H+", HPlus.addIonAndAdduct(M)); m_na = mockNode(1, "Na+", NaPlus.addIonAndAdduct(M));
        m_k =mockNode(2, "K+", Potassium.addIonAndAdduct(M)); m_nh3 = mockNode(3, "NH4+", NH3.addIonAndAdduct(M));
        // add some contradicting nodes
        AdductNode m_wrong_1 = mockNode(4, "W1", Potassium.addIonAndAdduct(m_h.getMass()-NaPlus.getModificationMass()));
        AdductNode m_wrong_2 = mockNode(5, "W2", HPlus.addIonAndAdduct(m_na.getMass()- Potassium.getModificationMass()));
        // add edges
        mockEdge(m_h, m_na, -9f, new AdductRelationship(HPlus, NaPlus));

        mockEdge(m_na, m_k, -6f, new AdductRelationship(NaPlus, Potassium));
        mockEdge(m_h, m_nh3, -7f, new AdductRelationship(HPlus, NH3));
        mockEdge(m_k, m_h, -7.5f, new AdductRelationship(Potassium, HPlus));
        // add contradicting edges
        mockEdge(m_h, m_wrong_1, -10f, new AdductRelationship(NaPlus, Potassium));
        mockEdge(m_na, m_wrong_2, -7f, new AdductRelationship(Potassium, HPlus));

        return new AdductNode[]{m_h,m_na, m_k, m_nh3, m_wrong_1, m_wrong_2};
    }
    public AdductNode[] simpleExampleNetworkWithLosses() {
        final double M = 128.0;
        PrecursorIonType HPlus = getPrecursorIonType("[M+H]+");
        PrecursorIonType NaPlus = getPrecursorIonType("[M+Na]+");
        PrecursorIonType Potassium = getPrecursorIonType("[M+K]+");
        PrecursorIonType NH3 = getPrecursorIonType("[M+NH3+H]+");

        //
        AdductNode m_h, m_na, m_k, m_nh3;
        m_h = mockNode(0, "H+", HPlus.addIonAndAdduct(M)); m_na = mockNode(1, "Na+", NaPlus.addIonAndAdduct(M));
        m_k =mockNode(2, "K+", Potassium.addIonAndAdduct(M)); m_nh3 = mockNode(3, "NH4+", NH3.addIonAndAdduct(M));
        // add some contradicting nodes
        AdductNode m_wrong_1 = mockNode(4, "W1", Potassium.addIonAndAdduct(m_h.getMass()-NaPlus.getModificationMass()));
        AdductNode m_wrong_2 = mockNode(5, "W2", HPlus.addIonAndAdduct(m_na.getMass()- Potassium.getModificationMass()));
        // add some losses
        MolecularFormula H2O = MolecularFormula.parseOrThrow("H2O");
        AdductNode l1 = mockNode(6, "H+-H2O", m_h.getMass() - H2O.getMass());
        AdductNode l2 = mockNode(7, "H+-H4O2", l1.getMass() - H2O.getMass());
        AdductNode l3 = mockNode(8, "Na+-H2O", m_na.getMass() - H2O.getMass());
        // add edges
        mockEdge(m_h, m_na, -9f, new AdductRelationship(HPlus, NaPlus));

        mockEdge(m_na, m_k, -6f, new AdductRelationship(NaPlus, Potassium));
        mockEdge(m_h, m_nh3, -7f, new AdductRelationship(HPlus, NH3));
        mockEdge(m_k, m_h, -7.5f, new AdductRelationship(Potassium, HPlus));
        // add contradicting edges
        mockEdge(m_h, m_wrong_1, -10f, new AdductRelationship(NaPlus, Potassium));
        mockEdge(m_na, m_wrong_2, -7f, new AdductRelationship(Potassium, HPlus));
        // add losses
        mockEdge(m_h, l1, -7, new LossRelationship(H2O));
        mockEdge(l1, l2, -5, new LossRelationship(H2O));
        mockEdge(m_na, l3, -6, new LossRelationship(H2O));
        mockEdge(l3, l1, -6, new AdductRelationship(NaPlus, HPlus));

        return new AdductNode[]{m_h,m_na, m_k, m_nh3, m_wrong_1, m_wrong_2, l1, l2, l3};
    }



    public AdductNode[] simpleExampleNetworkWithLossesAndIsotopes() {
        final double M = 128.0;
        final double ISO_1 = IsotopePattern.ISO_RANGES[0].getMinimum()+(IsotopePattern.ISO_RANGES[0].getMaximum()-IsotopePattern.ISO_RANGES[0].getMinimum())/2d;
        final double ISO_2 = IsotopePattern.ISO_RANGES[1].getMinimum()+(IsotopePattern.ISO_RANGES[1].getMaximum()-IsotopePattern.ISO_RANGES[1].getMinimum())/2d;
        PrecursorIonType HPlus = getPrecursorIonType("[M+H]+");
        PrecursorIonType NaPlus = getPrecursorIonType("[M+Na]+");
        PrecursorIonType Potassium = getPrecursorIonType("[M+K]+");
        PrecursorIonType NH3 = getPrecursorIonType("[M+NH3+H]+");

        //
        AdductNode m_h, m_na, m_k, m_nh3;
        m_h = mockNode(0, "H+", HPlus.addIonAndAdduct(M)); m_na = mockNode(1, "Na+", NaPlus.addIonAndAdduct(M));
        m_k =mockNode(2, "K+", Potassium.addIonAndAdduct(M)); m_nh3 = mockNode(3, "NH4+", NH3.addIonAndAdduct(M));
        // add some contradicting nodes
        AdductNode m_wrong_1 = mockNode(4, "W1", Potassium.addIonAndAdduct(m_h.getMass()-NaPlus.getModificationMass()));
        AdductNode m_wrong_2 = mockNode(5, "W2", HPlus.addIonAndAdduct(m_na.getMass()- Potassium.getModificationMass()));
        // add some adduct losses
        MolecularFormula H2O = MolecularFormula.parseOrThrow("H2O");
        AdductNode l1 = mockNode(6, "H+-H2O", m_h.getMass() - H2O.getMass());
        // add some isotope losses
        AdductNode i1 = mockNode(7, "H+i1", m_h.getMass() + ISO_1);
        AdductNode i2 = mockNode(8, "H+i2", m_h.getMass() + ISO_2);
        AdductNode i3 = mockNode(9, "H-H2O+i1", l1.getMass() + ISO_1);

        // add edges
        mockEdge(m_h, m_na, -9f, new AdductRelationship(HPlus, NaPlus));

        mockEdge(m_na, m_k, -6f, new AdductRelationship(NaPlus, Potassium));
        mockEdge(m_h, m_nh3, -7f, new AdductRelationship(HPlus, NH3));
        mockEdge(m_k, m_h, -7.5f, new AdductRelationship(Potassium, HPlus));
        // add contradicting edges
        mockEdge(m_h, m_wrong_1, -10f, new AdductRelationship(NaPlus, Potassium));
        mockEdge(m_na, m_wrong_2, -7f, new AdductRelationship(Potassium, HPlus));
        // add losses
        mockEdge(m_h, l1, -7, new LossRelationship(H2O));
        mockEdge(m_h, i1, -8, new IsotopeRelationship(1));
        mockEdge(m_h, i2, -8, new IsotopeRelationship(2));
        mockEdge(l1, i3, -8, new IsotopeRelationship(1));


        return new AdductNode[]{m_h,m_na, m_k, m_nh3, m_wrong_1, m_wrong_2, l1, i1,i2,i3};
    }

    public AdductNode[] simpleExampleNetworkWithLossesAndIsotopesAndMultimeres() {
        final double M = 128.0;
        final double ISO_1 = IsotopePattern.ISO_RANGES[0].getMinimum()+(IsotopePattern.ISO_RANGES[0].getMaximum()-IsotopePattern.ISO_RANGES[0].getMinimum())/2d;
        final double ISO_2 = IsotopePattern.ISO_RANGES[1].getMinimum()+(IsotopePattern.ISO_RANGES[1].getMaximum()-IsotopePattern.ISO_RANGES[1].getMinimum())/2d;
        PrecursorIonType HPlus = getPrecursorIonType("[M+H]+");
        PrecursorIonType NaPlus = getPrecursorIonType("[M+Na]+");
        PrecursorIonType Potassium = getPrecursorIonType("[M+K]+");
        PrecursorIonType NH3 = getPrecursorIonType("[M+NH3+H]+");

        //
        AdductNode m_h, m_na, m_k, m_nh3;
        m_h = mockNode(0, "H+", HPlus.addIonAndAdduct(M)); m_na = mockNode(1, "Na+", NaPlus.addIonAndAdduct(M));
        m_k =mockNode(2, "K+", Potassium.addIonAndAdduct(M)); m_nh3 = mockNode(3, "NH4+", NH3.addIonAndAdduct(M));
        // add some contradicting nodes
        AdductNode m_wrong_1 = mockNode(4, "W1", Potassium.addIonAndAdduct(m_h.getMass()-NaPlus.getModificationMass()));
        AdductNode m_wrong_2 = mockNode(5, "W2", HPlus.addIonAndAdduct(m_na.getMass()- Potassium.getModificationMass()));
        // add some adduct losses
        MolecularFormula H2O = MolecularFormula.parseOrThrow("H2O");
        AdductNode l1 = mockNode(6, "H+-H2O", m_h.getMass() - H2O.getMass());
        // add some isotope losses
        AdductNode i1 = mockNode(7, "H+i1", m_h.getMass() + ISO_1);
        AdductNode i2 = mockNode(8, "H+i2", m_h.getMass() + ISO_2);
        AdductNode i3 = mockNode(9, "H-H2O+i1", l1.getMass() + ISO_1);

        // add some multimere nodes
        AdductNode m1 = mockNode(10, "2M+H+", m_h.getMass()*2 - HPlus.getModificationMass());
        AdductNode m2 = mockNode(11, "2M+Na+", m_na.getMass()*2 - NaPlus.getModificationMass());
        AdductNode m3 = mockNode(12, "2M+1i+H+", m1.getMass() + ISO_1);
        AdductNode m4 = mockNode(13, "2M-H2O+H+", m1.getMass() - H2O.getMass());

        // add edges
        mockEdge(m_h, m_na, -9f, new AdductRelationship(HPlus, NaPlus));

        mockEdge(m_na, m_k, -6f, new AdductRelationship(NaPlus, Potassium));
        mockEdge(m_h, m_nh3, -7f, new AdductRelationship(HPlus, NH3));
        mockEdge(m_k, m_h, -7.5f, new AdductRelationship(Potassium, HPlus));
        // add contradicting edges
        mockEdge(m_h, m_wrong_1, -10f, new AdductRelationship(NaPlus, Potassium));
        mockEdge(m_na, m_wrong_2, -7f, new AdductRelationship(Potassium, HPlus));
        // add losses
        mockEdge(m_h, l1, -7, new LossRelationship(H2O));
        mockEdge(m_h, i1, -8, new IsotopeRelationship(1));
        mockEdge(m_h, i2, -8, new IsotopeRelationship(2));
        mockEdge(l1, i3, -8, new IsotopeRelationship(1));

        mockEdge(m_h, m1, -9f, new AdductRelationship(getPrecursorIonType("[M+H]+"), getPrecursorIonType("[2M+H]+")));
        mockEdge(m_na, m2, -9f, new AdductRelationship(getPrecursorIonType("[M+Na]+"), getPrecursorIonType("[2M+Na]+")));
        mockEdge(m1, m3, -9f, new IsotopeRelationship(1));
        mockEdge(m1, m4, -7, new LossRelationship(H2O));
        mockEdge(l1,m4, -8f, new AdductRelationship(getPrecursorIonType("[M-H2O+H]+"), getPrecursorIonType("[2M-H2O+H]+")));


        return new AdductNode[]{m_h,m_na, m_k, m_nh3, m_wrong_1, m_wrong_2, l1, i1,i2,i3,m1, m2, m3,m4};
    }

    private static class AdductNodeMock extends AdductNode {
        private final String name;
        public AdductNodeMock(int index, String name, AlignedFeatures al) {
            super(al, index);
            this.name = name;
        }
        @Override public String toString() {
            return name;
        }

    }


    public AdductNode mockNode(int index, String name, double mass) {
        final AlignedFeatures mockFeature = new AlignedFeatures();
        mockFeature.setAverageMass(mass);
        return new AdductNodeMock(index, name, mockFeature);
    }
    public AdductEdge mockEdge(AdductNode u, AdductNode v, float score, KnownMassDelta... explanations) {
        AdductEdge adductEdge = new AdductEdge(u, v, explanations);
        adductEdge.pvalue = score;
        u.getEdges().add(adductEdge);
        v.getEdges().add(adductEdge);
        return adductEdge;
    }

}
