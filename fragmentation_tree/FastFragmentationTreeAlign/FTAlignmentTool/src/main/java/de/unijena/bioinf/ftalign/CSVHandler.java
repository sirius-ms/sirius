
package de.unijena.bioinf.ftalign;

/**
 * @author Kai Dührkop
 */
public interface CSVHandler {
    
    public void entry(int row, int col, String entry);
    public void endOfRow(int row);
    
}
