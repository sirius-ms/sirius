
package de.unijena.bioinf.ftalign;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author Kai Dührkop
 */
public class CIDBase {

    private final HashMap<String, Integer> cids;

    public CIDBase() {
        this.cids = new HashMap<String, Integer>(1000);
    }
    
    public Integer get(String name) {
        return cids.get(name);
    }

    public boolean read(File cidfile) throws IOException {
        return read(cidfile, 0, 1);
    }

    public boolean read(File cidfile, final int nameCol, final int cidCol) throws IOException {
        final boolean[] complete = new boolean[1];
        complete[0] = true;
        CSVReader.read(cidfile, new CSVHandler(){
            private Integer cid;
            private String name;
            @Override
            public void entry(int row, int col, String entry) {
                if (row > 0) {
                    if (col == cidCol) {
                        try {
                            cid = Integer.parseInt(entry);
                        } catch (NumberFormatException exc) {
                            cid = null;
                            complete[0] = false;
                        }
                    } else if (col == nameCol) {
                        name = entry;
                    }
                }
            }

            @Override
            public void endOfRow(int row) {
                if (cid != null) cids.put(name, cid);
            }
        });
        return complete[0];
    }


}
