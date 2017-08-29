package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.fp.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TanimotoTest {

    @Test
    public void testDeterministicFingerprint() {

        final short[] indizes1 = new short[]{ 5, 10, 15, 20, 25, 30  };
        final short[] indizes2 = new short[]{ 5, 8,  15, 19, 22, 30 };
        // intersection: 3, union: 9, tanimoto: 1/3

        final ArrayFingerprint fp1 = new ArrayFingerprint(CdkFingerprintVersion.getDefault(), indizes1);
        final ArrayFingerprint fp2 = new ArrayFingerprint(CdkFingerprintVersion.getDefault(), indizes2);

        assertEquals(1d/3d, Tanimoto.tanimoto(fp1, fp2), 1e-6);
        assertEquals(1d/3d, fp1.tanimoto(fp2), 1e-6);

    }

    @Test
    public void testProbabilisticFingerprint() {
        final MaskedFingerprintVersion M = MaskedFingerprintVersion.buildMaskFor(CdkFingerprintVersion.getDefault()).disableAll().enable(0,100).toMask();
        final short[] indizes1 = new short[]{ 5, 10, 15, 20, 25, 30  };
        final short[] indizes2 = new short[]{ 5, 8,  15, 19, 22, 30 };
        // intersection: 3, union: 9, tanimoto: 1/3

        final double[] probabilities = new double[M.size()];
        for (short index : indizes2) probabilities[index] = 1.0d;

        ArrayFingerprint fp1 = new ArrayFingerprint(M, indizes1);
        ProbabilityFingerprint fp2 = new ProbabilityFingerprint(M, probabilities);

        assertEquals(1d/3d, Tanimoto.tanimoto(fp1, fp2), 1e-6);

        for (short index : indizes2) probabilities[index] = 0d;
        final short[] indizes3 = new short[]{ 5, 10 };

        probabilities[5] = 0.5;
        probabilities[8] = 0.5;
        fp1 = new ArrayFingerprint(M, indizes3);
        fp2 = new ProbabilityFingerprint(M, probabilities);
        assertEquals(5d/24d, Tanimoto.probabilisticTanimoto(fp1, fp2).expectationValue(), 1e-6);

        final double[] probabilities3 = new double[M.size()];
        for (short index : indizes3) probabilities3[index] = 1.0d;
        ProbabilityFingerprint fp3 = new ProbabilityFingerprint(M, probabilities3);

        assertEquals(5d/24d, Tanimoto.probabilisticTanimoto(fp2, fp3).expectationValue(), 1e-6);

        final double[] probabilities4 = new double[M.size()];
        for (short index : indizes2) probabilities4[index] = 1.0d;
        probabilities4[5] = 0.5;
        probabilities4[8] = 0.5;
        fp1 = new ArrayFingerprint(M, indizes1);
        fp2 = new ProbabilityFingerprint(M, probabilities4);
        assertEquals(1d/3d, Tanimoto.probabilisticTanimotoFixedLength(fp2, fp1).expectationValue(), 1e-6);


    }

}
