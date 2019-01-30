package de.unijena.bioinf.ms.io.projectspace;

import de.unijena.bioinf.sirius.ResultAnnotation;

/**
 * Annotation
 * that represents the relative path int a ProjectSpace where the ExperimentResult is stored.
 *
 * Objects that are imported read from file contain an ExperimentDirectory
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

    public boolean hasNoIndex(){
        return index == NO_INDEX;
    }

    public boolean hasIndex() {
        return !hasNoIndex();
    }

    protected void setIndex(int index) {
        this.index = index;
    }

    public boolean isRewrite() {
        return rewrite;
    }

    protected void setRewrite(boolean rewrite) {
        this.rewrite = rewrite;
    }

   /* @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExperimentDirectory that = (ExperimentDirectory) o;
        return directory.equals(that.directory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(directory);
    }*/
}
