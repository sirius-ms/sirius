package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.*;

public class MostRelevantPeaksFilter implements PostProcessor {

    private int limit;

    /**
     * only consider peaks with decompositions
     * most relevant peaks are ...
     * "the 2x<limit> most intense peaks\n" +
       "the <limit> best peaks with mass*log(relIntensity)\n" +
       "the <limit> best peaks with mass*log(relIntensity) int he upper mass range\n" +
     * @param limit
     */
    public MostRelevantPeaksFilter(int limit){
        this.limit = limit;
    }

    @Override
    public ProcessedInput process(ProcessedInput input) {
        //for a high limit all peaks will be picked
        if (2*limit>=input.getMergedPeaks().size()) return input;

        //sort peaks in descending Intensity order
        //only take those with decompositions
        final List<ProcessedPeak> peaks = new ArrayList<ProcessedPeak>();
        for (ProcessedPeak processedPeak : input.getMergedPeaks()) {
            if (processedPeak.getDecompositions().size()>0) peaks.add(processedPeak);
        }
        Collections.sort(peaks, Collections.reverseOrder(new ProcessedPeak.RelativeIntensityComparator()));
        final Set<ProcessedPeak> filtered = new HashSet<ProcessedPeak>(Math.min(peaks.size(), 4*limit));

        // x
        int peaksExact = Math.min(peaks.size(), limit);
        // find lowest and highest peak masses
        double highestPeakMass = 0;
        double lowestPeakMass =Integer.MAX_VALUE;
        for (ProcessedPeak p : peaks){
            if (p.getMz()>highestPeakMass) highestPeakMass = p.getMz();
            if (p.getMz()<lowestPeakMass) lowestPeakMass = p.getMz();
        }


        // 1) choose the 2x most intense peaks
        int min = Math.min(peaks.size() ,2*peaksExact);
        filtered.addAll(peaks.subList(0, min));

        //todo really upper 20% ?? it only seems to be the upper 10%
        //todo why in GCMSTool log(relInt*100)?  -> would mean for this implementation log(relInt*100*100) because of maxInt=1.0
        // 2) choose the x best mass*logInt peaks in the upper 20 % mass range
        Comparator<ProcessedPeak> logRelIntensityMultipliedMassComparator = new Comparator<ProcessedPeak>() {
            @Override
            public int compare(ProcessedPeak p1, ProcessedPeak p2) {
                double number1 = 0;
                double number2 = 0;
                if (p1.getIntensity()>0){
                    number1 = Math.log(p1.getRelativeIntensity())*p1.getMass();
                }
                if (p2.getIntensity()>0){
                    number2 = Math.log(p2.getRelativeIntensity())*p2.getMass();

                }
                if (number1 < number2) return 1;
                else if (number1 == number2) return 0;
                return -1;
            }
        };



        Collections.sort(peaks, logRelIntensityMultipliedMassComparator);

        double areaStart = highestPeakMass - 0.1*highestPeakMass;
        int counter = 0;
        for (ProcessedPeak p : peaks){// peaks are sorted by their relative intensities
            if (p.getMass()>areaStart){
                if (!filtered.contains(p) && counter<peaksExact){
                    counter++;
                    filtered.add(p);
                }
            }
        }

        // 3) choose the x best mass*logInt peaks
        counter =0;
        while(counter<peaksExact && counter<peaks.size()){
            if (!filtered.contains(peaks.get(counter))){
                filtered.add(peaks.get(counter));
            }
            counter++;
        }


        List<ProcessedPeak> filteredList = new ArrayList<ProcessedPeak>(filtered);
        Collections.sort(filteredList, new ProcessedPeak.MassComparator());
        //don't delete parent peak
        ProcessedPeak parentPeak = input.getParentPeak();
        if (parentPeak != null){
            if (!filtered.contains(parentPeak)){
                filteredList.add(parentPeak);
            }
        }

        for (int i = 0; i < filteredList.size(); i++) {
             filteredList.get(i).setIndex(i);
        }

        return new ProcessedInput(input.getExperimentInformation(), filteredList, input.getParentPeak(), input.getParentMassDecompositions(),
                input.getPeakScores(), input.getPeakPairScores());
    }

    @Override
    public Stage getStage() {
        return Stage.AFTER_DECOMPOSING;
    }
}
