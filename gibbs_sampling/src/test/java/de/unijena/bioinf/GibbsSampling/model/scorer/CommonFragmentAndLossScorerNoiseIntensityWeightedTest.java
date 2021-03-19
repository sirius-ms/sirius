package de.unijena.bioinf.GibbsSampling.model.scorer;/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.GibbsSampling.model.ExamplePreparationUtils;
import de.unijena.bioinf.GibbsSampling.model.FragmentsCandidate;
import org.junit.Test;

import java.io.IOException;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class CommonFragmentAndLossScorerNoiseIntensityWeightedTest extends CommonFragmentAndLossScorerTest {


    @Test
    public void assertPeakIsNoise() {
        CommonFragmentAndLossScorerNoiseIntensityWeighted c = new CommonFragmentAndLossScorerNoiseIntensityWeighted(0);

        double[] intensities = new double[]{1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0.05, 0.02, 0.01, 0.005, 0.001, 0.0005, 0.0001};
        double[] noiseScores = new double[]{1.0, 0.9950552298783638, 0.9893242471797059, 0.9825398445660384, 0.9743102871284064, 0.9639963585122723, 0.9504602297080231, 0.931403312666137, 0.9011309152339133, 0.8385182100945479, 0.7590451647732577, 0.6202347100245089, 0.48198231538281655, 0.3065013359125933, 0.0, 0.0, 0.0};
        for (int i = 0; i < intensities.length; i++) {
            System.out.print(c.peakIsNoNoise(intensities[i]) + ", ");
            assertEquals(noiseScores[i], c.peakIsNoNoise(intensities[i]), 1e-15);
        }

    }


    @Test
    public void assertNormalization() throws IOException {
        CommonFragmentAndLossScorerNoiseIntensityWeighted c = new CommonFragmentAndLossScorerNoiseIntensityWeighted(0);
        double[] norm = new double[]{34.76523958175724, 18.447953335807952, 33.947789416589714};

        assertNormalization(c, norm, "/tiny-example");
    }

    @Test
    public void testScoreCommonPeaks() throws IOException {
        CommonFragmentAndLossScorerNoiseIntensityWeighted c = new CommonFragmentAndLossScorerNoiseIntensityWeighted(0);

        double[][] expectedFragmentScores = new double[][]{
                new double[]{48.793704383631365, 4.598044752581925, 0.7377711399747388},
                new double[]{4.598044752581925, 7.861420109983392, 0.0},
                new double[]{0.7377711399747388, 0.0, 46.48262777396501}
        };

        double[][] expectedLossScores = new double[][]{
                new double[]{15.26456812787712, 0.6611386583688609, 2.148753636215405, },
                new double[]{0.6611386583688609, 6.861420109983391, 0.0, },
                new double[]{2.148753636215405, 0.0, 14.701186551927554, }
        };

        testScoreCommonPeaks(c, expectedFragmentScores, expectedLossScores, true);
        testScoreCommonPeaks(c, expectedFragmentScores, expectedLossScores, false);

    }

    @Test
    public void testCreatePeaksWithExplanations() throws IOException {
        CommonFragmentAndLossScorer.PeakWithExplanation[][]  allFragmentPeaksExpected = parseFragmentPeaksWithExplanationFromString();
        CommonFragmentAndLossScorer.PeakWithExplanation[][]  allLossPeaksExpected = parseLossPeaksWithExplanationFromString();

        Map<Ms2Experiment, List<FTree>> data = ExamplePreparationUtils.getData("/tiny-example", 15, true);
        FragmentsCandidate[][] candidates = ExamplePreparationUtils.parseExpectedCandidatesFromString(data);

        CommonFragmentAndLossScorerNoiseIntensityWeighted c = new CommonFragmentAndLossScorerNoiseIntensityWeighted(0.0);
        CommonFragmentAndLossScorer.PeakWithExplanation[][] allFragmentPeaks = extractFragmentPeaks(candidates, c, true);
        CommonFragmentAndLossScorer.PeakWithExplanation[][] allLossPeaks = extractFragmentPeaks(candidates, c, false);


        assertEqualWithoutIndex(allFragmentPeaksExpected, allFragmentPeaks);
        assertEqualWithoutIndex(allLossPeaksExpected, allLossPeaks);

    }





    @Test
    public void testPreparation() throws Exception {
        CommonFragmentAndLossScorer c = new CommonFragmentAndLossScorerNoiseIntensityWeighted(0);

        double minNumberMatchedPeaksLossesExpected = 1.0;
        double thresholdExpected = 0.0;
        double[] normalizationExpected = new double[]{34.76523958175724, 18.447953335807952, 33.947789416589714};
//        BitSet[] maybeSimilarExpected = new BitSet[]{BitSet.valueOf(new long[]{6}), new BitSet(), new BitSet()};//BitSet {1,2}
        BitSet[] maybeSimilarExpected = new BitSet[]{new BitSet(), BitSet.valueOf(new long[]{1}), BitSet.valueOf(new long[]{1})};//BitSet {0}
        assertAfterCalculatingWeights(c, minNumberMatchedPeaksLossesExpected, thresholdExpected, normalizationExpected, maybeSimilarExpected);
    }


    @Override
    public void testScoreFragmentCandidates() throws IOException {
        //parent test
    }

    protected CommonFragmentAndLossScorer.PeakWithExplanation[][] parseFragmentPeaksWithExplanationFromString() {
        String[][] allPeaks = new String[][]{
                new String[]{
                        "C14H26O, 210.198365452, 0.1358819281691904",
                        "C17H22, 226.172150704, 0.1358819281691904",
                        "C14H28O2, 228.208930136, 0.5652816491117328",
                        "C15H26O2, 238.193280072, 0.3814764145399541",
                        "C17H24O, 244.182715388, 0.5652816491117328",
                        "C14H30O3, 246.21949481999997, 0.32185507700583943",
                        "C19H20, 248.15650064, 0.1358819281691904",
                        "C18H22O, 254.167065324, 0.3814764145399541",
                        "C15H28O3, 256.20384475599997, 1.0",
                        "C17H26O2, 262.193280072, 0.32185507700583943",
                        "C19H22O, 266.16706532399996, 0.5652816491117328",
                        "C18H24O2, 272.177630008, 1.0",
                        "C15H30O4, 274.21440944, 0.307465851716522",
                        "C20H20O, 276.15141525999996, 0.3814764145399541",
                        "C19H24O2, 284.177630008, 0.32185507700583943",
                        "C16H32O4, 288.230059504, 0.11704742257256917",
                        "C18H26O3, 290.18819469199997, 0.307465851716522",
                        "C20H22O2, 294.161979944, 1.0",
                        "C17H30O4, 298.21440944, 0.11169770609099047",
                        "C19H28O3, 304.20384475599997, 0.11704742257256917",
                        "C20H24O3, 312.17254462799997, 0.307465851716522",
                        "C20H26O3, 314.18819469199997, 0.11169770609099047",
                        "C17H32O5, 316.22497412399997, 0.4386607778874459",
                        "C21H26O3, 326.18819469199997, 0.11704742257256917",
                        "C20H28O4, 332.198759376, 0.4386607778874459",
                        "C17H34O6, 334.235538808, 0.09538023314446617",
                        "C22H24O3, 336.17254462799997, 0.11169770609099047",
                        "C18H36O6, 348.25118887199994, 0.09872973596268007",
                        "C20H30O5, 350.20932406, 0.09538023314446617",
                        "C22H26O4, 354.183109312, 0.4386607778874459",
                        "C19H34O6, 358.235538808, 0.08231866966161533",
                        "C21H32O5, 364.22497412399997, 0.09872973596268007",
                        "C22H28O5, 372.193673996, 0.09538023314446617",
                        "C22H30O5, 374.20932406, 0.08231866966161533",
                        "C19H36O7, 376.246103492, 0.10326015621259517",
                        "C22H34O5, 378.24062418799997, 0.28374667159035355",
                        "C23H30O5, 386.20932406, 0.09872973596268007",
                        "C22H32O6, 392.21988874399995, 0.10326015621259517",
                        "C26H26N4, 394.215746832, 0.28374667159035355",
                        "C24H28O5, 396.193673996, 0.08231866966161533",
                        "C20H40O8, 408.27231824, 0.018252110770070874",
                        "C24H30O6, 414.20423868, 0.10326015621259517",
                        "C23H24N6O2|C27H28O4, 416.197416692, 0.28374667159035355",
                        "C24H36O6, 420.25118887199994, 0.3556319263970664",
                        "C29H32N2O, 424.25146364399995, 0.018252110770070874",
                        "C28H28N4O, 436.22631151599995, 0.3556319263970664",
                        "C21H40O9, 436.26723286, 0.13438539039142303",
                        "C24H38O7, 438.261753556, 0.046446345424509934",
                        "C25H34O7, 446.230453428, 0.018252110770070874",
                        "C30H32N2O2, 452.246378264, 0.13438539039142303",
                        "C28H30N4O2, 454.2368762, 0.046446345424509934",
                        "C25H26N6O3|C29H30O5, 458.20798137599996, 0.3556319263970664",
                        "C26H34O8, 474.22536804799995, 0.13438539039142303",
                        "C25H28N6O4|C29H32O6, 476.21854606, 0.046446345424509934",
                        "C26H40O8, 480.27231824, 0.12543763298139302",
                        "C30H32N4O3, 496.24744088399996, 0.12543763298139302",
                        "C26H42O9, 498.282882924, 0.061543017268438714",
                        "C30H34N4O4, 514.2580055679999, 0.061543017268438714",
                        "C27H30N6O5|C31H34O7, 518.229110744, 0.12543763298139302",
                        "C27H32N6O6|C31H36O8, 536.239675428, 0.061543017268438714",
                        "C28H46O11, 558.3040122919999, 1.0",
                        "C32H38N4O6, 574.279134936, 1.0",
                        "C29H36N6O8|C33H40O10, 596.260804796, 1.0",
                },
                new String[]{
                        "C13H16O, 188.120115132, 2.513634179912902E-4",
                        "C14H18O, 202.135765196, 4.908836567015895E-4",
                        "C14H16O2, 216.115029752, 6.180061943342101E-4",
                        "C17H22, 226.172150704, 0.0016214929451387197",
                        "C15H18O2, 230.130679816, 5.995888724450656E-4",
                        "C17H24O, 244.182715388, 0.005492800753226902",
                        "C18H22O, 254.167065324, 0.03484756646000355",
                        "C18H24O2, 272.177630008, 0.04246489788323715",
                        "C20H20O, 276.15141525999996, 0.006709887992134913",
                        "C18H26O3, 290.18819469199997, 0.00810893935507452",
                        "C20H22O2, 294.161979944, 0.22910582626802328",
                        "C22H28O2, 324.208930136, 4.719192861987568E-4",
                        "C22H24O3, 336.17254462799997, 0.005454692948093376",
                        "C22H30O3, 342.21949481999997, 0.006163199325742923",
                        "C22H26O4, 354.183109312, 0.35979133458459733",
                        "C22H32O4, 360.230059504, 0.0021805291105332623",
                        "C24H30O4, 382.21440944, 0.24985754877858454",
                        "C28H34O4, 434.245709568, 0.05058291050138326",
                        "C26H34O6|C27H30N4O2, 442.236207504, 1.0",
                        "C30H38O6|C31H34N4O2, 494.267507632, 0.011926506263372815",
                        "C32H42O6|C33H38N4O2, 522.29880776, 0.10386664054716438",
                        "C34H46O8|C35H42N4O4, 582.319937128, 1.0",
                },
                new String[]{
                        "C14H26O2, 226.193280072, 0.27795896230668543",
                        "C15H24O2, 236.177630008, 0.048287275093802075",
                        "C17H22O, 242.167065324, 0.27795896230668543",
                        "C14H28O3, 244.20384475599997, 0.15490183695424192",
                        "C18H20O, 252.15141526, 0.048287275093802075",
                        "C15H26O3, 254.18819469199997, 1.0",
                        "C17H24O2, 260.177630008, 0.15490183695424192",
                        "C19H20O, 264.15141525999996, 0.27795896230668543",
                        "C18H22O2, 270.161979944, 1.0",
                        "C15H28O4, 272.198759376, 0.608529115059614",
                        "C20H18O, 274.13576519599997, 0.048287275093802075",
                        "C19H22O2, 282.161979944, 0.15490183695424192",
                        "C16H30O4, 286.21440944, 0.016733073756886884",
                        "C18H24O3, 288.17254462799997, 0.608529115059614",
                        "C15H30O5, 290.20932406, 0.11623119957824768",
                        "C20H20O2, 292.14632988, 1.0",
                        "C17H28O4, 296.198759376, 0.06460786875566013",
                        "C19H26O3, 302.18819469199997, 0.016733073756886884",
                        "C18H26O4, 306.183109312, 0.11623119957824768",
                        "C20H22O3, 310.15689456399997, 0.608529115059614",
                        "C20H24O3, 312.17254462799997, 0.06460786875566013",
                        "C17H30O5, 314.20932406, 0.5363894048708605",
                        "C21H24O3, 324.17254462799997, 0.016733073756886884",
                        "C20H24O4, 328.167459248, 0.11623119957824768",
                        "C20H26O4, 330.183109312, 0.5363894048708605",
                        "C17H32O6, 332.21988874399995, 0.3047148569148157",
                        "C22H22O3, 334.15689456399997, 0.06460786875566013",
                        "C18H34O6, 346.235538808, 0.0792755947436882",
                        "C20H28O5, 348.193673996, 0.3047148569148157",
                        "C22H24O4, 352.167459248, 0.5363894048708605",
                        "C21H30O5, 362.20932406, 0.0792755947436882",
                        "C22H26O5, 370.178023932, 0.3047148569148157",
                        "C19H34O7, 374.230453428, 0.5376515762882368",
                        "C23H28O5, 384.193673996, 0.0792755947436882",
                        "C22H28O6, 388.18858861599995, 0.014283104158260366",
                        "C22H30O6, 390.20423868, 0.5376515762882368",
                        "C19H36O8, 392.241018112, 0.3240437990720521",
                        "C22H32O7, 408.214803364, 0.3240437990720521",
                        "C24H28O6, 412.18858861599995, 0.5376515762882368",
                        "C22H38O7, 414.261753556, 0.12779450797511283",
                        "C24H30O7, 430.1991533, 0.3240437990720521",
                        "C25H34O6, 430.235538808, 0.12779450797511283",
                        "C21H38O9, 434.251582796, 0.20349395899495204",
                        "C24H34O8, 450.22536804799995, 0.20349395899495204",
                        "C27H32O6, 452.21988874399995, 0.12779450797511283",
                        "C21H40O10, 452.26214747999995, 0.04498330191209444",
                        "C24H40O8, 456.27231824, 0.034656375465698275",
                        "C24H36O9, 468.235932732, 0.04498330191209444",
                        "C26H32O8, 472.209717984, 0.20349395899495204",
                        "C27H36O7, 472.246103492, 0.034656375465698275",
                        "C24H42O9, 474.282882924, 0.218883870272458",
                        "C26H34O9, 490.220282668, 0.04498330191209444",
                        "C27H38O8, 490.25666817599995, 0.218883870272458",
                        "C29H34O7, 494.230453428, 0.034656375465698275",
                        "C23H42O11, 494.272712164, 0.2049804507741606",
                        "C26H38O10, 510.246497416, 0.2049804507741606",
                        "C29H36O8, 512.241018112, 0.218883870272458",
                        "C28H36O10, 532.230847352, 0.2049804507741606",
                        "C28H50O13, 594.32514166, 1.0",
                        "C31H46O12, 610.298926912, 1.0",
                        "C33H44O12, 632.283276848, 1.0",
                },
        };
        return parseAllCandidatesFromString(allPeaks);
    }

    protected CommonFragmentAndLossScorer.PeakWithExplanation[][] parseLossPeaksWithExplanationFromString() {
        String[][] allPeaks = new String[][]{
                new String[]{
                        "C2H4O2, 60.021129368, 0.061543017268438714",
                        "C2H6O3, 78.03169405199999, 0.12543763298139302",
                        "C4H8O4, 120.042258736, 0.046446345424509934",
                        "C2H6N2O4|C3H2N6|C7H6O2, 122.03454338933334, 0.13438539039142303",
                        "C4H10O5, 138.05282341999998, 0.3556319263970664",
                        "C3H6N2O5|C4H2N6O|C8H6O3, 150.0294580093333, 0.018252110770070874",
                        "C6H12O6, 180.06338810399998, 0.28374667159035355",
                        "C10H6N4|C5H6N6O2|C9H10O4, 182.05745947466667, 0.10326015621259517",
                        "C10H8N4O|C5H8N6O3|C9H12O5, 200.06802415866665, 0.08231866966161533",
                        "C10H10O5|C11H6N4O|C6H6N6O3, 210.05237409466665, 0.09872973596268007",
                        "C11H12O5|C12H8N4O|C7H8N6O3, 224.06802415866665, 0.09538023314446617",
                        "C11H14O6|C12H10N4O2|C7H10N6O4, 242.07858884266668, 0.4386607778874459",
                        "C11H16O7|C12H12N4O3|C7H12N6O5, 260.0891535266666, 0.11169770609099047",
                        "C12H14O7|C13H10N4O3|C8H10N6O5, 270.0735034626666, 0.11704742257256917",
                        "C13H16O7|C14H12N4O3|C9H12N6O5, 284.0891535266666, 0.307465851716522",
                        "C13H18O8|C14H14N4O4|C9H14N6O6, 302.09971821066665, 1.0",
                        "C10H12N6O6|C14H16O8|C15H12N4O4, 312.08406814666665, 0.32185507700583943",
                        "C13H20O9|C14H16N4O5|C9H16N6O7, 320.1102828946666, 0.3814764145399541",
                        "C10H14N6O7|C14H18O9|C15H14N4O5, 330.0946328306666, 0.5652816491117328",
                        "C10H16N6O8|C14H20O10|C15H16N4O6, 348.1051975146666, 0.1358819281691904",
                },
                new String[]{
                        "C2H4O2, 60.021129368, 0.10386664054716438",
                        "C4H8O2, 88.052429496, 0.011926506263372815",
                        "C8H12O2, 140.083729624, 1.0",
                        "C6H12O4|C7H8N4, 148.07422756, 0.05058291050138326",
                        "C10H16O4|C11H12N4, 200.105527688, 0.24985754877858454",
                        "C12H14O4|C13H10N4, 222.089877624, 0.0021805291105332623",
                        "C12H20O4|C13H16N4, 228.136827816, 0.35979133458459733",
                        "C12H16O5|C13H12N4O, 240.10044230799997, 0.006163199325742923",
                        "C12H22O5|C13H18N4O, 246.14739249999997, 0.005454692948093376",
                        "C12H18O6|C13H14N4O2, 258.111006992, 4.719192861987568E-4",
                        "C14H24O6|C15H20N4O2, 288.157957184, 0.22910582626802328",
                        "C16H20O5|C17H16N4O, 292.13174243599997, 0.00810893935507452",
                        "C14H26O7|C15H22N4O3, 306.16852186799997, 0.006709887992134913",
                        "C16H22O6|C17H18N4O2, 310.14230712, 0.04246489788323715",
                        "C16H24O7|C17H20N4O3, 328.152871804, 0.03484756646000355",
                        "C18H18N4O3, 338.13789043599996, 0.005492800753226902",
                        "C19H28O6|C20H24N4O2, 352.189257312, 5.995888724450656E-4",
                        "C18H20N4O4, 356.14845512, 0.0016214929451387197",
                        "C21H26N4O2, 366.205576072, 6.180061943342101E-4",
                        "C20H28O7|C21H24N4O3, 380.18417193199997, 4.908836567015895E-4",
                        "C22H26N4O3, 394.20049069199996, 2.513634179912902E-4",
                },
                new String[]{
                        "C5H8O2, 100.052429496, 0.2049804507741606",
                        "C4H8O4, 120.042258736, 0.218883870272458",
                        "C4H10O5, 138.05282341999998, 0.034656375465698275",
                        "C7H10O3, 142.06299417999998, 0.04498330191209444",
                        "C7H12O4, 160.073558864, 0.20349395899495204",
                        "C6H12O6, 180.06338810399998, 0.12779450797511283",
                        "C9H14O5, 202.08412354799998, 0.3240437990720521",
                        "C9H16O6, 220.09468823199998, 0.5376515762882368",
                        "C11H16O6, 244.09468823199998, 0.014283104158260366",
                        "C10H16O7, 248.08960285199998, 0.0792755947436882",
                        "C11H18O7, 262.105252916, 0.3047148569148157",
                        "C11H20O8, 280.1158176, 0.5363894048708605",
                        "C11H22O9, 298.126382284, 0.06460786875566013",
                        "C13H20O8, 304.1158176, 0.11623119957824768",
                        "C12H20O9, 308.11073222, 0.016733073756886884",
                        "C13H22O9, 322.126382284, 0.608529115059614",
                        "C13H24O10, 340.13694696799996, 1.0",
                        "C14H22O10, 350.121296904, 0.15490183695424192",
                        "C13H26O11, 358.147511652, 0.048287275093802075",
                        "C14H24O11, 368.131861588, 0.27795896230668543",
                },
        };
        return parseAllCandidatesFromString(allPeaks);
    }


}
