
package de.unijena.bioinf.ftalign.parallelograms;

import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;

public class ParaNodes {

    private Fragment x;
    private Fragment y;
    private Fragment u;
    private Fragment v;

    public ParaNodes(Fragment x, Fragment y, Fragment u, Fragment v) {
        this.x = x;
        this.y = y;
        this.u = u;
        this.v = v;
    }

    public Fragment getX() {
        return x;
    }

    public Fragment getY() {
        return y;
    }

    public Fragment getU() {
        return u;
    }

    public Fragment getV() {
        return v;
    }

}
