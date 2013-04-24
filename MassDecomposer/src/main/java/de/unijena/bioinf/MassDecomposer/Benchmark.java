package de.unijena.bioinf.MassDecomposer;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.MassDecomposer.Chemistry.ChemicalAlphabetWrapper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class Benchmark {

    public static void main(String... args) {
        final Deviation d = new Deviation(10, 0.001d);//new Deviation(20, 0.002d);
        final ChemicalAlphabet alphabet = new ChemicalAlphabet(PeriodicTable.getInstance().getAllByName("C", "H", "N", "O", "P", "S"));
        /*
        final String[] testSetStrings = new String[]{
                "C17H15N3O2", "C22H29FO4", "C17H18N4O6S", "C21H25N", "C22H30N2O5", "C17H21N4O9P", "C15H14Cl2N2O3",
                "C17H22ClN3O", "C15H23NS", "C20H27N2O", "C12H18N2O", "C26H28ClNO", "C12H11I3N2O5", "C20H25NO", "C16H24N2O",
                "C12H17NO2", "C22H19N3O4", "C19H24ClNO", "C29H42O10", "C19H20F3NO4", "C18H23N", "C19H22N2S", "C17H25N", "C19H31NO",
                "C13H15N3O4S", "C17H20N2O2", "C9H6O4", "C26H35FO5", "C21H29N2O", "C21H38N", "C9H11ClN2O", "C19H19FN2O", "C11H15NO4S",
                "C28H30N2O2", "C14H12O3S", "C19H23NO3", "C8H12NO5PS2", "C21H27N", "C5H11NO2S", "C21H27NO4", "C21H28O4", "C22H23NO7",
                "C8H10N6", "C10H15NO3", "C20H17Cl3N2O2", "C15H14N2O3", "C14H14F3N5O6S", "C11H16ClNO2", "C20H21FN2O", "C25H35N3O6S",
                "C17H13N3O5S2", "C21H23NO3", "C12H16F3N", "C22H19NO", "C28H41N3O3", "C25H38O3", "C16H16ClNO2S", "C20H25NO", "C19H23ClN2O2S",
                "C26H27NO9", "C16H18N4O2", "C20H30NO3", "C22H28O3", "C14H15N5O6S", "C7H13BrN2O2", "C7H9N3O2", "C19H17ClN2O4", "C23H30N2O4",
                "C9H18N2O4", "C16H22N4O4", "C18H29NO3", "C14H17NO3", "C26H32O3", "C11H15NO3S", "C20H25NO2", "C10H13NO2", "C15H21NO2", "C21H29NO",
                "C20H17FO3S", "C20H28N2O3", "C16H14F3N3O2S", "C15H11ClF3NO4", "C8H12N2", "C20H26N4O", "C14H13ClO5S", "C7H7NO2", "C10H14ClN3OS",
                "C26H37N5O2", "C21H28O2", "C24H40N8O4", "C10H13NO2", "C26H30Cl2F3NO", "C18H18ClN3S", "C17H12Cl2N4O", "C12H17O4PS2", "C20H27O4P",
                "C7H10N2O2S", "C15H23N3O4S", "C15H16O9", "C27H36F2O6"
        };
        */
        final double[] testSet;
        {
            try {
                BufferedReader reader = new BufferedReader(new FileReader("/home/kai/data/ms/hillpeaks.txt"));
                final double[] values = new double[30000];
                int k=0;
                while (reader.ready()) values[k++] = Double.parseDouble(reader.readLine());
                testSet = Arrays.copyOf(values, k);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("Alphabet:");
        for (Element e : alphabet.getElements()) {
            System.out.println(e + ": " + e.getMass());
        }
        System.out.println("============");

        for (double blowup : new double[]{5963.337687d}) {
            double bestTime = Double.MAX_VALUE;
            System.out.print("original Decompose blowup( " + blowup +  " ): ");
            final MassDecomposer<Element> elementDecomposer = new MassDecomposer<Element>(1d/blowup,
                    new ChemicalAlphabetWrapper(alphabet));
            for (int i=0; i < 20; ++i) {
                int decomps = 0;
                double now = System.nanoTime();
                for (double x : testSet) {
                    decomps += elementDecomposer.decompose(x, d).size();
                }
                double after = System.nanoTime();
                if (after-now < bestTime) {
                    bestTime = after-now;
                }
                if (i==0) System.out.print("Error( " + elementDecomposer.getMaxError() + ") " + "#( " + decomps + ") ");
            }
            System.out.print((long)(bestTime*1e-6));
            System.out.println(" ms");
            bestTime = Double.MAX_VALUE;
            System.out.print("fast Decompose blowup( " + blowup +  " ): ");
            final MassDecomposer<Element> fastDecomposer = new MassDecomposerFast<Element>(1d/blowup,
                    new ChemicalAlphabetWrapper(alphabet));
            for (int i=0; i < 20; ++i) {
                int decomps = 0;
                double now = System.nanoTime();
                for (double x : testSet) {
                    decomps += fastDecomposer.decompose(x, d).size();
                }
                double after = System.nanoTime();
                if (after-now < bestTime) {
                    bestTime = after-now;
                }
                if (i==0) System.out.print("Error( " + fastDecomposer.getMaxError() + ") " + "#( " + decomps + ") ");
            }
            System.out.print((long)(bestTime*1e-6));
            System.out.println(" ms");
        }

    }
}
