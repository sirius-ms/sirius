package de.unijena.bioinf.ChemistryBase;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaPacker;
import gnu.trove.list.array.TLongArrayList;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by kaidu on 01.04.14.
 */
public class Benchmark {


    public static void main(String[] args) {
        final String[] sample = new String[]{"C23H38O2", "C9H13N3O", "C6H5N3O4", "C24H32O7", "C17H16O4", "C18H31N2O27S4",
                "C40H56", "C7H13NO4", "C44H69NO12", "C24H42O4", "C20H18O4", "C20H26Br2O2", "C32H57N5O9", "C8H15NO9S2",
                "C14H20N2O2", "C11H11N5", "C3H7NO6S", "C20H21NO3", "C11H19NOS", "C10H12ClNO4", "C28H48O", "C36H63N12O6",
                "C33H46N2O10", "C45H54N8O10", "C15H14O10", "C9H11NO6", "C22H34O5", "C10H23O2PS2", "C13H16N10O5S",
                "C25H27N9O8S2", "C18H26ClN3", "C20H16NO4", "C55H74N4O5", "C12H26O", "C18H25O5P", "C22H24N2O4",
                "C15H22BrNO", "C17H14N2O7", "C12H8O", "C10H18O", "C15H18N2O6", "C15H21N3O2", "C37H48O10", "C21H30N4O5S",
                "C17H14O3", "C26H38N7O17P3S2", "C15H22N5O7P", "C5H15NO4P", "C13H18N5O8P", "C19H14O5", "C25H40NO8P",
                "C8H15O3PS", "C8H11NO2", "C10H16O20S4", "C10H7Cl5O", "C21H26Cl2N2O6", "C18H19NO3", "C30H42", "C7H8",
                "C13H7Cl4NO2", "C23H37NO5", "C36H48O36", "C25H26O12", "C21H24O6", "C12H3Cl5O", "C50H71N13O12", "C5H9NO4S",
                "C6H9N5", "C12H9ClO2", "C24H26N2O7S", "C20H16ClF3N2O2S", "C24H26O8", "C14H18N5O11P", "C25H39NO6S",
                "C30H50O25", "C29H38O9", "C14H20N2O16P2", "C8H12O5", "C2HCl3O", "C3H2O5", "C30H52", "C32H46O9", "C5H11NO2S",
                "C19H34O2", "C12H19NO13S", "C9H16N2O4", "C8H17N", "C18H34O3", "C40H50O2", "C7H15N3O5", "C28H36N4O4",
                "C21H27N3O7S", "C8H7NO2", "C41H65NO10", "C22H32O8", "C25H48N6O8", "C7H14N2O4", "C8H16N2O4",
                "C15H15ClN4O6S", "C9H14N2O7P", "C10H10N4O6", "C7H16O7", "C10H9NOS", "C21H35N3O8S", "C22H22O11",
                "C7H11NO5", "C19H24O6S", "C31H48N7O17P3S", "C28H36N2O4", "C14H8N2O8", "C13H28N4O7", "C5H11N3O2",
                "C14H18N6O7S", "C37H47NO12", "C26H37NO6", "C8H6N4O5", "C40H52O2", "C13H15O13S", "C23H34O2", "C10H18O3",
                "C15H10N2O2", "C11H8N2O3S2", "C24H39N8O18P3S", "C8H15N3O4", "C16H30O", "C8H14N4S", "C15H17NO4",
                "C16H22N2O2", "C10H18N4O4S3", "C8H11NO", "C12H8Cl4N2", "C21H16NO5", "C40H50O18", "C17H19N5O6S",
                "C24H36O18", "C23H31ClO6", "C24H27NO7", "C33H42N4O6", "C3H6N2O4", "C13H16N2O6", "C20H25N3O7S",
                "C7H15N5O4", "C23H20F2N5O5PS", "C3H10NO4P", "C33H52O5", "C52H78O3", "C12H19N3O8S", "C27H43NO3",
                "C33H40O21", "C8H10N2O4", "C12H18O3", "C11H13N4", "C17H30O7", "C16H23N5O6", "C29H36O11", "C7H12N4O3S2",
                "C24H28O2", "C12H24N9P3", "C42H72O14", "C6H6N4O4", "C12H7ClO2", "C22H31FO3", "C28H29F2N3O", "C3H9O5P",
                "C4H12NO4P", "C27H48O6", "C23H22O7", "C16H10", "C13H20N5O16P3", "N2H2", "C31H50N7O19P3S", "C12H16O14",
                "C16H18O9", "C10H13Cl2FN2O2S2", "C7H8N2O2", "C48H69NO12", "C7H10O4", "C3H8O10P2", "C6H4N2O4",
                "C11H16N4O4", "C38H50O6", "C14H13NO4", "C15H26N2", "C16H14Cl2N2O2", "C30H27O12", "C17H20O4",
                "C28H37ClO7", "C19H20N2O4", "C43H73O6P", "C15H11ClF3NO4", "C19H12", "C19H15FN2O4", "C6H12O",
                "C10H15NO3", "C19H21N5O2", "C38H60O9", "C8H8O7", "C42H66O15", "C4H11O4P", "C33H52O9"};
        final MolecularFormula[] formulas = new MolecularFormula[sample.length];
        for (int k = 0; k < sample.length; ++k) formulas[k] = MolecularFormula.parse(sample[k]);

        measureEncodingTime(sample);
        measureMath(formulas);


    }

    private static void measureMath(MolecularFormula[] formulas) {
        MolecularFormula[] sortedFormulas = formulas.clone();
        Arrays.sort(sortedFormulas);
        final long[] encoded = new long[formulas.length];
        final MolecularFormulaPacker packer = MolecularFormulaPacker.newPackerFor(formulas);
        for (int k = 0; k < encoded.length; ++k) encoded[k] = packer.encode(formulas[k]);
        final ArrayList<MolecularFormula> subtracted = new ArrayList<MolecularFormula>(formulas.length);
        final TLongArrayList subtractedEncoded = new TLongArrayList(formulas.length);

        final int N = formulas.length;
        long bestTime = Long.MAX_VALUE;
        {
            for (int k = 0; k < REPETITIONS; ++k) {
                subtracted.clear();
                long now = System.nanoTime();
                for (int i = 0; i < N; ++i) {
                    for (int j = i + 1; j < N; ++j) {
                        if (formulas[j].isSubtractable(formulas[i])) {
                            subtracted.add(formulas[j].subtract(formulas[i]));
                        }
                    }
                }
                bestTime = Math.min(bestTime, System.nanoTime() - now);
            }
        }
        System.out.println("Subtractable: " + subtracted.size());
        System.out.println("Subtracting formulas: " + bestTime);

        bestTime = Long.MAX_VALUE;
        {
            for (int k = 0; k < REPETITIONS; ++k) {
                subtractedEncoded.clear();
                long now = System.nanoTime();
                for (int i = 0; i < N; ++i) {
                    for (int j = i + 1; j < N; ++j) {
                        if (packer.isSubtractable(encoded[j], encoded[i])) {
                            subtractedEncoded.add(packer.subtract(encoded[j], encoded[i]));
                        }
                    }
                }
                bestTime = Math.min(bestTime, System.nanoTime() - now);
            }
        }
        System.out.println("Subtractable: " + subtractedEncoded.size());
        System.out.println("Subtracting encoded formulas: " + bestTime);
        final MolecularFormula[] added = new MolecularFormula[subtracted.size()];
        final long[] addedEncoded = new long[subtracted.size()];
        final MolecularFormula adder = MolecularFormula.parse("C6H12O6");
        final long adderEncoded = packer.encode(adder);
        bestTime = Long.MAX_VALUE;
        {
            for (int k = 0; k < REPETITIONS; ++k) {
                long now = System.nanoTime();
                for (int i = 0; i < formulas.length; ++i) {
                    added[i] = formulas[i].add(adder);
                }
                bestTime = Math.min(bestTime, System.nanoTime() - now);
            }
        }
        System.out.println("Adding formulas: " + bestTime);

        bestTime = Long.MAX_VALUE;
        {
            for (int k = 0; k < REPETITIONS; ++k) {
                long now = System.nanoTime();
                for (int i = 0; i < formulas.length; ++i) {
                    addedEncoded[i] = packer.add(encoded[i], adderEncoded);
                }
                bestTime = Math.min(bestTime, System.nanoTime() - now);
            }
        }
        System.out.println("Adding encoded formulas: " + bestTime);


    }

    private static void measureIteration(MolecularFormula[] formulas) {
        final long[] encoded = new long[formulas.length];
        final MolecularFormulaPacker packer = MolecularFormulaPacker.newPackerFor(formulas);
        for (int k = 0; k < encoded.length; ++k) encoded[k] = packer.encode(formulas[k]);

        long bestTime = Long.MAX_VALUE;
        {
            for (int k = 0; k < REPETITIONS; ++k) {
                long now = System.nanoTime();
                for (int i = 0; i < formulas.length; ++i) {

                }
                bestTime = Math.min(bestTime, System.nanoTime() - now);
            }
        }
    }

    final static int REPETITIONS = 1000;

    private static void measureEncodingTime(String[] sample) {
        long bestTime = Long.MAX_VALUE;
        final MolecularFormula[] formulas = new MolecularFormula[sample.length];
        final long[] encoded = new long[sample.length];
        {
            for (int k = 0; k < REPETITIONS; ++k) {
                long now = System.nanoTime();
                for (int i = 0; i < sample.length; ++i) {
                    formulas[i] = MolecularFormula.parse(sample[i]);
                }
                bestTime = Math.min(bestTime, System.nanoTime() - now);
            }
        }
        System.out.println("parsing formulas: " + bestTime);
        MolecularFormulaPacker packer = null;
        bestTime = Long.MAX_VALUE;
        {
            for (int k = 0; k < REPETITIONS; ++k) {
                long now = System.nanoTime();
                packer = MolecularFormulaPacker.newPackerFor(formulas);
                bestTime = Math.min(bestTime, System.nanoTime() - now);
            }
        }
        System.out.println("Creating packer for formulas: " + bestTime);
        bestTime = Long.MAX_VALUE;
        {
            for (int k = 0; k < REPETITIONS; ++k) {
                long now = System.nanoTime();
                for (int i = 0; i < sample.length; ++i) {
                    encoded[i] = packer.encode(formulas[i]);
                }
                bestTime = Math.min(bestTime, System.nanoTime() - now);
            }
        }
        System.out.println("Encoding: " + bestTime);
        bestTime = Long.MAX_VALUE;
        {
            for (int k = 0; k < REPETITIONS; ++k) {
                long now = System.nanoTime();
                for (int i = 0; i < sample.length; ++i) {
                    formulas[i] = packer.decode(encoded[i]);
                }
                bestTime = Math.min(bestTime, System.nanoTime() - now);
            }
        }
        System.out.println("Decoding: " + bestTime);
    }

    private String prettify(long num) {
        return String.valueOf(num / 1000000) + " ms";
    }

}
