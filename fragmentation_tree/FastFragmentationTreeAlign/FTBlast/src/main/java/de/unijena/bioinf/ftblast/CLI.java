
package de.unijena.bioinf.ftblast;

import au.com.bytecode.opencsv.CSVReader;
import de.unijena.bioinf.ChemistryBase.data.DoubleDataMatrix;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by kaidu on 19.07.2014.
 */
public class CLI {

    /*
    Usage:
    ftblast matrix1 matrix2 matrix3
     */
    public static void main(String[] args) {
        final List<Iterator<String[]>> csvParser = new ArrayList<Iterator<String[]>>();
        final ArrayList<String> names = new ArrayList<String>();
        final ArrayList<CSVReader> readers = new ArrayList<CSVReader>();
        for (String arg : args) {
            final File f = new File(arg);
            try {
                final CSVReader fileReader = new CSVReader(FileUtils.ensureBuffering(new FileReader(f)));
                names.add(f.getName().substring(0, f.getName().lastIndexOf('.')));
                final String[] headerLine = fileReader.readNext();
                csvParser.add(new Iterator<String[]>() {
                    String[] line = headerLine;

                    @Override
                    public boolean hasNext() {
                        return line != null;
                    }

                    @Override
                    public String[] next() {
                        final String[] ret = line;
                        try {
                            line = fileReader.readNext();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return ret;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                });
            } catch (FileNotFoundException e) {
                System.err.println("Cannot open " + f.toString() + ":\n" + e);
            } catch (IOException e) {
                System.err.println("Cannot open " + f.toString() + ":\n" + e);
            }
        }
        final DoubleDataMatrix matrix = DoubleDataMatrix.overlayIntersection(csvParser, names, null);

    }

}
