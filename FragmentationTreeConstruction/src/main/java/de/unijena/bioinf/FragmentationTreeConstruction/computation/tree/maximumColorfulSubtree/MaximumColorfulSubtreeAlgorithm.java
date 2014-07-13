package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.TreeScoring;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MaximumColorfulSubtreeAlgorithm {

    private final long maxCacheMemory;
    private volatile long usedCacheMemory;
    private volatile int[][][] staticKeyPool;
    private ReadWriteLock cacheLock;

    public MaximumColorfulSubtreeAlgorithm(long maxCacheMemory) {
        this.maxCacheMemory = maxCacheMemory;
        staticKeyPool = new int[33][][];
        cacheLock = new ReentrantReadWriteLock();
    }

    public MaximumColorfulSubtreeAlgorithm() {
        this(1024 * 1024l * 1024l);
    }

    private static int[] computeSubsetsFor(int bitset) {
        final int numberOfBits = Integer.bitCount(bitset);
        final int[] keys = new int[1 << numberOfBits];
        keys[0] = 0;
        if (bitset > 0) {
            final int minbit = Integer.lowestOneBit(bitset);
            int k = 0;
            for (int i = minbit; i <= bitset; ++i) {
                if ((i & bitset) == i) {
                    keys[++k] = i;
                }
            }
        }
        return keys;
    }

    public FTree compute(FGraph graph, int maxColorNumber) {
        final FTree tree = new DP(this, graph, maxColorNumber, false).runAlgorithm();
        cleanupCacheIfFull();
        return tree;
    }

    public List<FTree> computeMultipleTrees(FGraph graph, int maxColorNumber) {
        DP dp = new DP(this, graph, maxColorNumber, false);
        dp.compute();
        final List<FTree> trees = dp.backTrackAll();
        for (FTree tree : trees) {
            final double additionalScore = dp.attachRemainingColors(tree);
            final TreeScoring scoring = tree.getAnnotationOrThrow(TreeScoring.class);
            scoring.setOverallScore(scoring.getOverallScore() + additionalScore);
        }
        cleanupCacheIfFull();
        return trees;
    }

    void cleanupCacheIfFull() {
        cacheLock.readLock().lock();
        final boolean needCleanup = usedCacheMemory > maxCacheMemory;
        cacheLock.readLock().unlock();
        if (needCleanup) {
            cacheLock.writeLock().lock();
            Arrays.fill(staticKeyPool, null);
            usedCacheMemory = 0;
            cacheLock.writeLock().unlock();
        }
    }

    int[] subsetsFor(int bitset) {
        cacheLock.readLock().lock();
        final int size = Integer.numberOfLeadingZeros(bitset);
        try {
            if (staticKeyPool[size] != null && staticKeyPool[size][bitset] != null) {
                return staticKeyPool[size][bitset];
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        final int[] keys = computeSubsetsFor(bitset);
        cacheLock.writeLock().lock();
        setSubsets(size, bitset, keys);
        cacheLock.writeLock().unlock();
        return keys;
    }

    private void setSubsets(int size, int bitset, int[] keys) {
        if (staticKeyPool[size] == null) {
            staticKeyPool[size] = new int[(1 << (32 - size))][];
        }
        staticKeyPool[size][bitset] = keys;
        usedCacheMemory += keys.length * 4;
    }

}
