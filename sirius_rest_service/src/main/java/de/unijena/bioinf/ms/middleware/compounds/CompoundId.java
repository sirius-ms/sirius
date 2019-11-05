package de.unijena.bioinf.ms.middleware.compounds;

/**
 * The CompoundId contains the ID of a compound together with some read-only information that might be displayed in
 * some summary view.
 */
public class CompoundId {

    // identifier
    protected String id;

    // identifier source
    protected String name;
    protected long index;

    // additional attributes
    protected double ionMass;
    protected String ionType;

    //Summary of the results of the compounds
    protected CompoundSummary summary;
    protected CompoundMsData msData;


    public CompoundId(String id, String name, long index, double ionmass, String ionType) {
        this.id = id;
        this.name = name;
        this.index = index;
        this.ionMass = ionmass;
        this.ionType = ionType;
        this.summary = null;
    }

    public String getName() {
        return name;
    }

    public long getIndex() {
        return index;
    }

    public String getId() {
        return id;
    }

    public double getIonMass() {
        return ionMass;
    }

    public String getIonType() {
        return ionType;
    }

    public CompoundSummary getSummary() {
        return summary;
    }

    public void setSummary(CompoundSummary summary) {
        this.summary = summary;
    }

    public CompoundMsData getMsData() {
        return msData;
    }

    public void setMsData(CompoundMsData msData) {
        this.msData = msData;
    }
}
