package de.unijena.bioinf.babelms.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaSet;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import gnu.trove.impl.hash.TIntLongHash;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TShortObjectHashMap;

import java.util.BitSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * - The DBMolecularFormulaCache stores queries to molecular formulas for a certain database
 * - a query covers ranges of 1 mDa.
 */
public class DBMolecularFormulaCache {
    private final TIntLongHashMap covered;
    private final MolecularFormulaSet known;
    private final CompoundQuery query;
    private final Lock queryLock;

    public DBMolecularFormulaCache(ChemicalAlphabet alphabet, CompoundQuery query) {
        this.covered = new TIntLongHashMap();
        this.known = new MolecularFormulaSet(alphabet);
        this.queryLock = new ReentrantLock();
        this.query = query;
    }

    public boolean isFormulaExist(MolecularFormula formula) {
        checkMass(formula.getMass());
        return known.contains(formula);
    }

    private void checkMass(double mass) {
        final int k = (int)Math.floor(mass*5d*64d);
        final long mask = 1l<<(k%64);
        final int slot = k/64;
        queryLock.lock();
        try {
            if ((covered.get(k/64) & (1l<<(k%64))) == 0) {

            }
        }

        queryLock.unlock();


        if ((covered.get(k/64) & (1l<<(k%64))) == 0) {
            checkMassRange(mass, new Deviation(10));
        }
    }

    public void checkMassRange(double mass, Deviation deviation) {
        final double startMass = mass - deviation.absoluteFor(mass);
        final double endMass = mass + deviation.absoluteFor(mass);
        final int startInt = (int)Math.floor(startMass*5d*64d);
        final int endInt = (int)Math.ceil(endMass*5d*64d);
        int currentSlot = -1;
        long currentSlotValue = 0;
        for (int k=startInt; k <= endInt; ++k) {
            final int slot = k / 64;
            if (currentSlot!=slot) {
                if (currentSlot>=0) covered.put(currentSlot, currentSlotValue);
                currentSlot = slot;
                currentSlotValue = covered.get(slot);
                if (currentSlotValue == -1l) {
                    k = (slot+1)*64;
                    continue;
                }
            }
            final int index = k % 64;
            final long mask = (1l<<index);
            if ((currentSlotValue & mask) == 0) {
                startQueryFor(k);
                currentSlotValue |= mask;
            }
        }
        if (currentSlot>=0) covered.put(currentSlot, currentSlotValue);
    }

    private void startQueryFor(int k) {
        final double width = 1d / (5d*64d);
        final double dev = width/2d;
        final double mass = k * width + dev;
        final Set<MolecularFormula> formulas = query.findMolecularFormulasByMass(mass, dev + 1e-4);
        for (MolecularFormula f : formulas) {
            known.add(f);
        }

    }

}
