package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.sirius.ResultAnnotation;

/**
 * Annotation
 * that represents the makePath on the filesystem where the object,
 * usually an ExperimentResult, is stored.
 *
 * Objects that are inported read from file contain an ExperimentDirectory
 * Objects that were already written to file at som stage contain also an ExperimentDirectory
 * Objects that were never Serialized do NOT contain an ExperimentDirectory
*/
public class ExperimentDirectory implements ResultAnnotation {
    public static final int NO_INDEX = -1;
    private String directory;
    private int index = NO_INDEX;
    private boolean rewrite = false;

    public ExperimentDirectory(String directory) {
        this.directory = directory;
    }

    public String getDirectoryName() {
        return directory;
    }

    protected void setDirectoryName(String directory) {
        this.directory = directory;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isRewrite() {
        return rewrite;
    }

    public void setRewrite(boolean rewrite) {
        this.rewrite = rewrite;
    }

    public boolean hasNoIndex(){
        return index == NO_INDEX;
    }
}
