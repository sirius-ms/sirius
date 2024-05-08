package de.unijena.bioinf.lcms.merge;

import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProjectedTrace;
import de.unijena.bioinf.lcms.trace.TraceRectangleMap;

import java.util.ArrayList;
import java.util.Iterator;

public interface MergeStorage {

    public TraceRectangleMap getRectangleMap();
    public void addProjectedTrace(int mergedTraceUiD, int sampleId, ProjectedTrace trace);
    public Iterator<ProjectedTrace> forEachProjectedTraceOf(int mergedTraceUid);
    public default ProjectedTrace[] getAllProjectedTracesOf(int mergeTraceUid) {
        ArrayList<ProjectedTrace> trcs = new ArrayList<>();
        Iterator<ProjectedTrace> iter = forEachProjectedTraceOf(mergeTraceUid);
        while (iter.hasNext()) trcs.add(iter.next());
        return trcs.toArray(ProjectedTrace[]::new);
    }
    public ProjectedTrace getProjectedTrace(int mergedTraceUiD, int sampleId);
    public void addIsotopeProjectedTrace(int parentTraceUiD, int isotopeId, int sampleId, ProjectedTrace trace);
    public ProjectedTrace getIsotopeProjectedTrace(int parentTraceUiD, int isotopeId, int sampleId);
    public ProjectedTrace[][] getIsotopePatternFor(int parentTraceUiD);


}
