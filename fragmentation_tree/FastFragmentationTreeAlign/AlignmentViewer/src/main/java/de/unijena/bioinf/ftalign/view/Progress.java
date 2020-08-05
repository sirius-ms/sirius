
package de.unijena.bioinf.ftalign.view;

public interface Progress {

    public void start(int max);

    public void tick(int current, int max);


}
