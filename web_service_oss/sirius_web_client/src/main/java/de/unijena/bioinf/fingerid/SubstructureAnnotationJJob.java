package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.fragmenter.InsilicoFragmentationPeakAnnotator;
import de.unijena.bioinf.fragmenter.InsilicoFragmentationResult;
import de.unijena.bioinf.jjobs.BasicMasterJJob;

import java.util.*;

public class SubstructureAnnotationJJob extends BasicMasterJJob<Map<FTree, SubstructureAnnotationResult>> {

    Map<FTree, FBCandidates> input;


    int topKOnly;

    public SubstructureAnnotationJJob() {
        this(5);
    }

    public SubstructureAnnotationJJob(int topKOnly) {
        super(JobType.CPU);
        this.topKOnly = topKOnly;
    }

    @Override
    protected Map<FTree, SubstructureAnnotationResult> compute() throws Exception {

        final Map<Scored<CompoundCandidate>, FTree> candidatesMap = new HashMap<>();

        for (Map.Entry<FTree, FBCandidates> entry : input.entrySet())
            for (Scored<CompoundCandidate> candidate : entry.getValue().getResults())
                candidatesMap.put(candidate, entry.getKey());

        if (topKOnly <= 0)
            topKOnly = candidatesMap.size();

        final Map<FTree, List<InsilicoFragmentationPeakAnnotator.Job>> jobMap = new HashMap<>();
        final Map<InsilicoFragmentationPeakAnnotator.Job, Scored<CompoundCandidate>> jobToCandidate = new HashMap<>();

        InsilicoFragmentationPeakAnnotator fragmenter = new InsilicoFragmentationPeakAnnotator();
        candidatesMap.keySet().stream().sorted(Comparator.<Scored<CompoundCandidate>>reverseOrder().thenComparing((Scored<CompoundCandidate> s) -> s.getCandidate().getInchiKey2D())).limit(topKOnly)
                .forEach(c -> {
                    InsilicoFragmentationPeakAnnotator.Job j = submitSubJob(fragmenter.makeJJob(candidatesMap.get(c), c.getCandidate().getSmiles()));
                    jobMap.computeIfAbsent(j.getTree(), f -> new ArrayList<>()).add(j);
                    jobToCandidate.put(j, c);
                });

        final Map<FTree, SubstructureAnnotationResult> result = new HashMap<>();
        for (Map.Entry<FTree, List<InsilicoFragmentationPeakAnnotator.Job>> entry : jobMap.entrySet()) {
            Map<String, InsilicoFragmentationResult> r = new HashMap<>(entry.getValue().size());
            entry.getValue().forEach(job -> r.put(jobToCandidate.get(job).getCandidate().getInchiKey2D(), job.takeResult()));
            result.put(entry.getKey(), SubstructureAnnotationResult.builder().inchiToFragmentationResult(r).build());
        }

        return result;
    }

    public void setInput(Map<FTree, FBCandidates> input) {
        this.input = input;
    }
}
