
package de.unijena.bioinf.IsotopePatternAnalysis;

/**
 * Created by kaidu on 15.03.2015.
 */
public class PatternExtractionTest {

   /* @Test
    public void testPatternExtraction() {

        final MutableMeasurementProfile profile = new MutableMeasurementProfile();
        profile.setAllowedMassDeviation(new Deviation(10,0.0002));
        profile.setFormulaConstraints(FormulaConstraints.create("C", "H", "N", "O", "P", "S", "Mg"));

        final SimpleMutableSpectrum glucose = new SimpleMutableSpectrum();
        glucose.addPeak(181.0707, 92.34);
        glucose.addPeak(182.0741, 6.34);
        glucose.addPeak(183.0753, 1.32);

        final SimpleMutableSpectrum chlorophyl = new SimpleMutableSpectrum();
        chlorophyl.addPeak(619.2402, 53.52);
        chlorophyl.addPeak(620.2427, 27.58);
        chlorophyl.addPeak(621.2415, 14.69);
        chlorophyl.addPeak(622.2431, 4.22);

        final SimpleMutableSpectrum fictional = new SimpleMutableSpectrum(); // C12S5H10
        fictional.addPeak(314.9459, 67.90);
        fictional.addPeak(315.9483, 11.61);
        fictional.addPeak(316.9422, 16.28);
        fictional.addPeak(317.9444, 2.55);
        fictional.addPeak(318.9387, 1.651);

        final SimpleMutableSpectrum fictional2 = new SimpleMutableSpectrum(); // C100H120
        fictional2.addPeak(1321.9463, 33.80);
        fictional2.addPeak(1322.9497, 37.02);
        fictional2.addPeak(1323.9531, 20.081);
        fictional2.addPeak(1324.9565, 7.19);
        fictional2.addPeak(1325.9598, 1.91);

        final SimpleMutableSpectrum merged = new SimpleMutableSpectrum();
        for (Peak p : glucose) merged.addPeak(p);
        for (Peak p : chlorophyl) merged.addPeak(p);
        for (Peak p : fictional) merged.addPeak(p);
        for (Peak p : fictional2) merged.addPeak(p);

        // add noise
        merged.addPeak(313.01, 0.01);
        merged.addPeak(320.001, 0.2);
        merged.addPeak(1320.082, 11);
        merged.addPeak(618.001, 120);

        final List<IsotopePattern> candidates = new ArrayList<IsotopePattern>(new ExtractAll().extractPatternMs1(profile, merged));
        Collections.sort(candidates, new Comparator<IsotopePattern>() {
            @Override
            public int compare(IsotopePattern o1, IsotopePattern o2) {
                return Double.compare(o1.getMonoisotopicMass(), o2.getMonoisotopicMass());
            }
        });

        assertEquals(5, candidates.size());
        assertTrue("isotope pattern of glucose with monoisotopic mass 181.0707", findCandidateWithMono(candidates, 181.0707));
        assertTrue("isotope pattern of C12S5H10 with monoisotopic mass 314.9459", findCandidateWithMono(candidates, 314.9459));
        assertTrue("isotope pattern of chlorophyl with monoisotopic mass 619.2402", findCandidateWithMono(candidates, 619.2402));
        assertTrue("isotope pattern of C100H120 with monoisotopic mass 1321.9463", findCandidateWithMono(candidates, 1321.9463));
        assertTrue("Maybe the isotope pattern at 1321.9463 is an overlap of two pattern. Therefore, allow both possibilities:", numberOfCandidatesWithMono(candidates,1321.9463)==2);
        assertEquals(314.9459, candidates.get(1).getMonoisotopicMass(), 1e-3);
        assertEquals(619.2402, candidates.get(2).getMonoisotopicMass(), 1e-3);
        assertEquals(1321.9463, candidates.get(3).getMonoisotopicMass(), 1e-3);

        assertEquals(glucose.size(), candidates.get(0).getPattern().size());
        assertEquals(fictional.size(), candidates.get(1).getPattern().size());
        assertEquals(chlorophyl.size(), candidates.get(2).getPattern().size());
        assertEquals(fictional2.size(), candidates.get(3).getPattern().size());

    }

    private static boolean findCandidateWithMono(List<IsotopePattern> patterns, double mono) {
        for (IsotopePattern p : patterns) {
            if (Math.abs(p.getMonoisotopicMass()-mono) < 1e-3) return true;
        }
        return false;
    }

    private static int numberOfCandidatesWithMono(List<IsotopePattern> patterns, double mono) {
        int c=0;
        for (IsotopePattern p : patterns) {
            if (Math.abs(p.getMonoisotopicMass()-mono) < 1e-3) ++c;
        }
        return c;
    }
*/
}
