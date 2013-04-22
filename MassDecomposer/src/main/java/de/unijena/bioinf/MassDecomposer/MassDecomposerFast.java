package de.unijena.bioinf.MassDecomposer;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;

import java.util.*;

/**
 * Decomposes a given mass over an alphabet, returning all decompositions which mass equals the given mass
 * considering a given deviation.
 * In contrast to {@link MassDecomposer} MassDecomposerFast calculates the decompositions with the help of an ERT containing deviation information, not requiring to iterate over all different integer mass values.
 *
 * @see @see <a href="http://bio.informatik.uni-jena.de/bib2html/downloads/2005/BoeckerLiptak_MoneyChangingProblem_COCOON_2005.pdf">The Money Changing Problem revisited by Sebastian Böcker and Zsuzsanna Lipták</a>
 * @param <T> type of the alphabet's characters
 */
public class MassDecomposerFast<T> extends MassDecomposer<T>{

    protected List<long[][]> ERTs;
    private final static boolean print = false;

    /**
     * @param precision mass precision. A precision of 1e-3 means that three positions after decimal point are
     *                  considered for input masses
     * @param alphabet the alphabet the mass is decomposed over
     */
    public MassDecomposerFast(double precision, Alphabet<T> alphabet) {
        super(precision, alphabet);
    }

    /**
     * Check if a mass is decomposable. This is done in constant time (especially: it is very very very fast!).
     * But it doesn't check if there is a valid decomposition. Therefore, even if the method returns true,
     * all decompositions may be invalid for the given validator or given bounds.
     * #decompose(mass) uses this function before starting the decomposition, therefore this method should only
     * be used if you don't want to start the decomposition algorithm.
     * @param mass
     * @return
     */
    @Override
    public boolean maybeDecomposable(double mass, Deviation dev) {
        init();
        //normal version seems to be faster, because it returns after first hit
        final Interval range = integerBound(mass, dev.absoluteFor(mass));
        final long a = weights.get(0).getIntegerMass();
        for (long i = range.getMin(); i <= range.getMax(); ++i) {
            final int r = toInt(i % a);
            if (i >= ERTs.get(0)[r][weights.size()-1]) return true;
        }
        return false;
//        final Interval interval = integerBound(mass);
//        final long a = weights.get(0).getIntegerMass();
//        final int deviation = toInt(interval.getMax()-interval.getMin());
//        if (Math.pow(2, ERTs.size()-1)<=deviation){
//            calcERT(deviation);
//        }
//
//        long[][] currentERT;
//        if (deviation==0) currentERT = ERTs.get(0);
//        else currentERT = ERTs.get(32-Integer.numberOfLeadingZeros(deviation));
//        int ERTdev = Integer.highestOneBit(deviation);
//
//        long r = interval.getMax()%a;
//        if (interval.getMax() >= currentERT[toInt(r)][weights.size()-1]) return true;
//        int pos = toInt(r)-deviation+ERTdev;
//        if (pos<0) pos += currentERT.length;
//        if (interval.getMax() >= currentERT[pos][weights.size()-1]) return true;
//
//        return false;
    }


    /**
     * computes all decompositions for the given mass. The runtime depends only on the number of characters and the
     * number of decompositions. Therefore this method is very fast as long as the number of decompositions is low.
     * Unfortunately, the number of decompositions increases nearly exponential in the number of characters and in the
     * input mass.
     *
     * This function can be called in multiple threads in parallel, because it does not modify the decomposer
     * @param mass input mass which should be decomposed
     * @param boundaries a map which maps some characters of the alphabet to an interval. The decomposer will discard
     *                   all decompositions for which the amound of the characters is not in the given interval. This is
     *                   done as early as possible, resulting a better performance than checking the boundary during the
     *                   validation step
     * @return list of decompositions. Use the {@link #getCharacterIndizes} and {@link Alphabet#get(int)} method to map the
     *         indizes of compomere to the characters.
     */
    @Override
    public List<int[]> decompose(final double mass, Deviation dev, Map<T, Interval> boundaries){
        init();
        if (mass == 0d) return Collections.emptyList();
        if (mass < 0d) throw new IllegalArgumentException("Expect positive mass for decomposition");
        final double absError = dev.absoluteFor(mass);
        final int[] minValues = new int[weights.size()];
        final int[] boundsarray = new int[weights.size()];
        boolean minAllZero = true;
        double calcMass = mass;
        Arrays.fill(boundsarray, Integer.MAX_VALUE);
        if (boundaries!=null && !boundaries.isEmpty()) {
            for (int i = 0; i < boundsarray.length; i++) {
                T el = weights.get(i).getOwner();
                Interval range = boundaries.get(el);
                if (range != null) {
                    boundsarray[i] = toInt(range.getMax() - toInt(range.getMin()));
                    minValues[i] = toInt(range.getMin());
                    if (minValues[i] > 0) {
                        minAllZero = false;
                        calcMass -= weights.get(i).getMass() * range.getMin();
                    }
                }
            }
        }
        final ArrayList<int[]> results = new ArrayList<int[]>();
        ArrayList<int[]> rawDecompositions = null;
        final Interval interval = integerBound(calcMass, absError);
        if (!minAllZero && (Math.abs(calcMass) <= absError)) results.add(minValues);
        //new decomposition with the whole integer interval at once
        if (interval.getMax()<interval.getMin()) rawDecompositions = new ArrayList<int[]>();
        else rawDecompositions = integerDecompose(interval.getMax(), toInt(interval.getMax() - interval.getMin()), boundsarray);
        for (int i=0; i < rawDecompositions.size(); ++i) {
            final int[] decomp = rawDecompositions.get(i);
            if (!minAllZero) {
                for (int j=0; j < minValues.length; ++j) {
                    decomp[j] += minValues[j];
                }
            }
            if (Math.abs(calcMass(decomp) - mass) > absError) continue;    //changed testen ob auch ohne diese Zeile gleiches Ergebnis
            if ((validator == null) || validator.validate(decomp, orderedCharacterIds, alphabet)) {
                results.add(decomp);
            }
        }
        return results;
    }

    /**
     * decomposes an interval of masses with mass as UPPER mass and all other masses below within deviation
     * Example: mass = 18, deviation 3 -> decompose 18,17,16,15
     * @param mass
     * @param deviation
     * @param bounds
     * @return
     */
    protected ArrayList<int[]> integerDecompose(long mass, int deviation, int[] bounds){
        assert (deviation<weights.get(0).getIntegerMass()); //todo throw Exception or not that problematic?
        //calculate the required ERTs
        if (Math.pow(2, ERTs.size()-1)<=deviation){
            calcERT(deviation);
        }

        //take ERT with required deviation
        long[][] currentERT;
        if (deviation==0) currentERT = ERTs.get(0);
        else currentERT = ERTs.get(32-Integer.numberOfLeadingZeros(deviation)); //ERTs.get(((int)(Math.log(deviation)/Math.log(2))+1));
        int ERTdev = Integer.highestOneBit(deviation); //Math.pow(2, ((int)(Math.log(deviation)/Math.log(2))));
        ArrayList<int[]> result = new ArrayList<int[]>();

        int k = weights.size();
        int[] c = new int[k], deepCopy;
        long[] j = new long[k], m = new long[k], lbound = new long[k], r = new long[k];
        boolean flagWhile = false; // flag wether we are in the while-loop or not
        final long a = weights.get(0).getIntegerMass();
        // Init
        for (int i=1; i<k; ++i){
            lbound[i] = Long.MAX_VALUE; // this is just to ensure, that lbound < m in the first iteration
        }

        int i = k-1;
        m[i] = mass; // m[i] corresponds to M, m[i-1] ^= m
        while (i != k){
            if (i == 0){
                deepCopy = new int[weights.size()];
                for (int index=0; index<c.length; ++index) deepCopy[index] = c[index];
                deepCopy[0] = (int) (m[i]/a);
                if (deepCopy[0] <= bounds[0]) result.add(deepCopy);
                ++i; // "return" from recursion
                flagWhile = true; // in this recursion-depth we are in the while-loop, cause the next recursion (the one we just exited) was called
                m[i-1] -= weights.get(i).getLcm(); // execute the rest of the while
                c[i] += weights.get(i).getL();
            } else {
                if (flagWhile){
                    //de.fsu.ms2tool.application.io.output.AlgorithmLogging.get.info("lbound: " +lbound[i]+" m: "+m[i-1]);
                    if (m[i-1] >= lbound[i] && c[i] <= bounds[i]){ //currently in while loop
                        //de.fsu.ms2tool.application.io.output.AlgorithmLogging.get.info("i: "+(i-1)+" m: "+m[i-1]);
                        --i; // "do" recursive call
                    } else {
                        flagWhile = false; //
                    }
                } else { //we are in the for-loop
                    if (j[i] < weights.get(i).getL() && m[i]-j[i]*weights.get(i).getIntegerMass()>=0){
                        c[i] = (int) j[i];
                        m[i-1] = m[i]-j[i]*weights.get(i).getIntegerMass();
                        r[i] = m[i-1]%a;
                        //lbound[i] = currentERT[(int)r[i]][i-1];
                        //changed from normal algorithm: you have to look up the minimum at 2 position
                        int pos = (int)r[i]-deviation+ERTdev;
                        if (pos<0) pos += currentERT.length;
                        lbound[i] = Math.min(currentERT[(int)r[i]][i-1], currentERT[pos][i-1]);
                        flagWhile = true; // call the while loop
                        ++j[i];
                    } else { //exit for loop
                        // reset "function variables"
                        lbound[i] = Long.MAX_VALUE;
                        j[i] = 0;
                        c[i] = 0;
                        ++i; // "return" from recursion
                        if (i != k) { // only if we are not done
                            flagWhile = true; // in this recursion-depth we are in the while-loop, cause the next recursion was called
                            m[i-1] -= weights.get(i).getLcm(); // execute the rest of the while
                            c[i] += weights.get(i).getL();
                        }
                    }
                }
            } // end if i == 0
        } // end while

        return result;
    } // end function


    /**
     * Initializes the decomposer. Computes the extended residue table. This have to be done only one time for
     * a given alphabet, independently from the masses you want to decompose. This method is called automatically
     * if you compute the decompositions, so call it only if you want to control the time of the initialisation.
     */
    @Override
    public void init() {
        if (ERTs != null) return;
        ERTs = new ArrayList<long[][]>();
        discretizeMasses();
        divideByGCD();
        computeLCMs();
        calcERT();
        computeErrors();
    }


    /**
     * calculates ERTs to look up whether a mass or lower masses within a certain deviation are decomposable.
     * only ERTs for deviation 2^x are calculated
     * @param deviation
     */
    protected void calcERT(int deviation){
        //calculate ERT with deviation 2^n from ERT with 2^(n-1) deviation
        long[][] lastERT = ERTs.get(ERTs.size()-1);
        long[][] nextERT = new long[lastERT.length][weights.size()];
        if (ERTs.size()==1){
            //first line compares biggest residue and 0
            for (int j = 0; j < weights.size(); j++) {
                nextERT[0][j] = Math.min(lastERT[nextERT.length-1][j], lastERT[0][j]);
            }
            for (int i = 1; i < nextERT.length; i++) {
                for (int j = 0; j < weights.size(); j++) {
                    nextERT[i][j] = Math.min(lastERT[i][j], lastERT[i-1][j]);
                }
            }
        } else {
            int step = (int)Math.pow(2, ERTs.size()-2);
            for (int i = step; i < nextERT.length; i++) {
                for (int j = 0; j < weights.size(); j++) {
                    nextERT[i][j] = Math.min(lastERT[i][j], lastERT[i-step][j]);
                }
            }
            //first lines compared with last lines (greatest residues) because of modulo's cyclic characteristic
            for (int i = 0; i < step; i++) {
                for (int j = 0; j < weights.size(); j++) {
                    nextERT[i][j] = Math.min(lastERT[i][j], lastERT[i+nextERT.length-step][j]);
                }
            }
        }

        ERTs.add(nextERT);
        if (Math.pow(2, ERTs.size()-1)>deviation) return; //deviation in ERT is sufficient for used deviation
        calcERT(deviation);
    }

    @Override
    protected void calcERT(){
        long firstLongVal = weights.get(0).getIntegerMass();
        long[][] ERT = new long[(int)firstLongVal][weights.size()];
        long d, r, n, argmin;

        //Init
        ERT[0][0] = 0;
        for (int i = 1; i < ERT.length; ++i){
            ERT[i][0] = Long.MAX_VALUE; // should be infinity
        }

        //Filling the Table, j loops over columns
        for (int j = 1; j < ERT[0].length; ++j){
            ERT[0][j] = 0; // Init again
            d = gcd(firstLongVal, weights.get(j).getIntegerMass());
            for (long p = 0; p < d; p++){ // Need to start d Round Robin loops
                if (p == 0) {
                    n = 0; // 0 is the min in the complete RT or the first p-loop
                } else {
                    n = Long.MAX_VALUE; // should be infinity
                    argmin = p;
                    for (long i = p; i<ERT.length; i += d){ // Find Minimum in specific part of ERT
                        if (ERT[(int)i][j-1] < n){
                            n = ERT[(int)i][j-1];
                            argmin = i;
                        }
                    }
                    ERT[(int)argmin][j]= n;
                }
                if (n == Long.MAX_VALUE){ // Minimum of the specific part of ERT was infinity
                    for (long i = p; i<ERT.length; i += d){ // Fill specific part of ERT with infinity
                        ERT[(int)i][j] = Long.MAX_VALUE;
                    }
                } else { // Do normal loop
                    for (long i = 1; i < ERT.length/d; ++i){ // i is just a counter
                        n += weights.get(j).getIntegerMass();
                        r = n % firstLongVal;
                        if (ERT[(int)r][j-1] < n) n = ERT[(int)r][j-1]; // get the min
                        ERT[(int)r][j] = n;
                    }
                }
            } // end for p
        } // end for j

        ERTs.add(ERT);
    }

}
