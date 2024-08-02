package evaluate;

import matching.algorithm.MCESDist2;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {

    private static String[][] readTableFile(BufferedReader fileReader, String sep) throws IOException {
        final ArrayList<String[]> rows = new ArrayList<>();
        String currentLine = fileReader.readLine();
        while(currentLine != null){
            rows.add(currentLine.split(sep));
            currentLine = fileReader.readLine();
        }

        final String[][] rowsArray = new String[rows.size()][rows.get(0).length];
        for(int i = 0; i < rows.size(); i++) rowsArray[i] = rows.get(i);
        return rowsArray;
    }


    public static void main(String[] args){
        try {
            final File moleculePairTSVFile = new File(args[0]);
            final File outputFile = new File(args[1]);
            final int mol1Idx = Integer.parseInt(args[2]);
            final int mol2Idx = Integer.parseInt(args[3]);
            final MCESDist2.MatchingType matchingType = MCESDist2.MatchingType.valueOf(args[4]);

            final String clmNames;
            final String[][] rows;
            try(final BufferedReader fileReader = Files.newBufferedReader(moleculePairTSVFile.toPath())) {
                clmNames = fileReader.readLine();
                rows = readTableFile(fileReader, "\t");
            }

            final SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            ArrayList<Callable<String[]>> tasks = new ArrayList<>(rows.length);
            for(final String[] row : rows){
                Callable<String[]> task = () -> {
                    try{
                        // Init
                        final IAtomContainer mol1 = smiParser.parseSmiles(row[mol1Idx]);
                        final IAtomContainer mol2 = smiParser.parseSmiles(row[mol2Idx]);
                        final MCESDist2 mces = new MCESDist2(mol1, mol2, matchingType);

                        final String[] newRow = new String[row.length+8];
                        System.arraycopy(row, 0, newRow, 0, row.length);

                        // COMPUTATION OF MCES AND LOWER BOUNDS:
                        // 1. MCES distance:
                        long timeStamp = System.nanoTime();
                        double distance = mces.compare();
                        long elapsedTimeDistance = System.nanoTime() - timeStamp;
                        newRow[row.length] = String.valueOf(distance);
                        newRow[row.length+4] = String.valueOf(elapsedTimeDistance);

                        // 2. Lower Bounds:
                        final IAtomContainer mol1WithoutH = AtomContainerManipulator.removeHydrogens(mces.getFirstMolecule());
                        final IAtomContainer mol2WithoutH = AtomContainerManipulator.removeHydrogens(mces.getSecondMolecule());

                        // 2.1. Degree based lower bound:
                        timeStamp = System.nanoTime();
                        double degreeLB = mces.degreeBasedFilter(mol1WithoutH, mol2WithoutH);
                        long elapsedTimeDegreeLB = System.nanoTime() - timeStamp;
                        newRow[row.length+1] = String.valueOf(degreeLB);
                        newRow[row.length+5] = String.valueOf(elapsedTimeDegreeLB);

                        // 2.2. Weighted degree based lower bound:
                        timeStamp = System.nanoTime();
                        double weightedDegreeLB = mces.weightedDegreeBasedFilter(mol1WithoutH, mol2WithoutH);
                        long elapsedTimeWDegreeLB = System.nanoTime() - timeStamp;
                        newRow[row.length+2] = String.valueOf(weightedDegreeLB);
                        newRow[row.length+6] = String.valueOf(elapsedTimeWDegreeLB);

                        // 2.3. Neighborhood based lower bound:
                        timeStamp = System.nanoTime();
                        double neighborhoodLB = mces.neighborhoodBasedFilter(mol1WithoutH, mol2WithoutH);
                        long elapsedTimeNeighborLB = System.nanoTime() - timeStamp;
                        newRow[row.length+3] = String.valueOf(neighborhoodLB);
                        newRow[row.length+7] = String.valueOf(elapsedTimeNeighborLB);
                        return newRow;
                    } catch (CDKException e) {
                        throw new RuntimeException(e);
                    }
                };
                tasks.add(task);
            }

            final List<Future<String[]>> results = executor.invokeAll(tasks);
            executor.shutdown();

            try(BufferedWriter fileWriter = Files.newBufferedWriter(outputFile.toPath())){
                fileWriter.write(clmNames +
                        "\tmyMCES\tmyDegreeBound\tmyWeightedDegreeBound\tmyNeighborhoodBound"+
                        "\tmyMCES_Time\tmyDegreeBound_Time\tmyWeightedDegreeBound_Time\tmyNeighborhoodBound_Time");

                for(final Future<String[]> result : results){
                    final String[] row = result.get();

                    fileWriter.newLine();
                    fileWriter.write(row[0]);
                    for(int i = 1; i < row.length; i++) fileWriter.write("\t" + row[i]);
                }
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException | InterruptedException e){
            throw new RuntimeException(e);
        }
    }
}
