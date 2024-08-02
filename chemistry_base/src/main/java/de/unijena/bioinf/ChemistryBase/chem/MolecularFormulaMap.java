package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

/**
 * allow to store and query molecular formulas by their mass
 */
public class MolecularFormulaMap implements Iterable<MolecularFormula>, Serializable {

    @Serial
    private static final long serialVersionUID = 5375295572094691885L;


    transient private MolecularFormula[][] searchMatrix;

    public MolecularFormulaMap(Iterable<MolecularFormula> formulas) {
        // buildList
        List<List<MolecularFormula>> list = new ArrayList<>();
        for (MolecularFormula formula : formulas) {
            int roundedMass = (int)Math.round(formula.getMass());
            while (roundedMass >= list.size()) list.add(new ArrayList<>());
            list.get(roundedMass).add(formula);
        }
        this.searchMatrix = new MolecularFormula[list.size()][];
        for (int k=0; k < list.size(); ++k) {
            this.searchMatrix[k] = list.get(k).toArray(MolecularFormula[]::new);
            Arrays.sort(searchMatrix[k]);
        }
    }

    public boolean contains(MolecularFormula formula) {
        final Iterator<MolecularFormula> iter = iterator(formula.getMass()-1e-8, formula.getMass()+1e-8);
        while (iter.hasNext())
            if (iter.next().equals(formula))
                return true;
        return false;
    }

    public MolecularFormula[] searchMass(double mass, Deviation deviation) {
        return searchMass(mass- deviation.absoluteFor(mass), mass + deviation.absoluteFor(mass));
    }

    public MolecularFormula[] searchMass(double from, double to) {
        final Iterator<MolecularFormula> iter = iterator(from, to);
        final ArrayList<MolecularFormula> list = new ArrayList<>();
        while (iter.hasNext()) list.add(iter.next());
        return list.toArray(MolecularFormula[]::new);
    }

    @NotNull
    @Override
    public Iterator<MolecularFormula> iterator() {
        return new RangeIterator(0d, Double.POSITIVE_INFINITY);
    }

    public Iterator<MolecularFormula> iterator(double from, double to) {
        return new RangeIterator(from, to);
    }

    private void writeObject(ObjectOutputStream oos)
            throws IOException {
        oos.defaultWriteObject();
        // collect all chemical alphabets
        final HashMap<TableSelection, Integer> selections = new HashMap<>();
        for (MolecularFormula formula : this) {
            selections.putIfAbsent(formula.getTableSelection(), selections.size());
        }
        // first store the alphabets
        {
            final TableSelection[] ary = new TableSelection[selections.size()];
            for (Map.Entry<TableSelection,Integer> entry : selections.entrySet()) {
                ary[entry.getValue()] = entry.getKey();
            }
            oos.writeInt(ary.length);
            for (TableSelection s : ary) {
                int[] buf = new int[s.numberOfElements()];
                Iterator<Element> iterator = s.iterator();
                int i=0;
                while (iterator.hasNext()) {
                    buf[i++] = iterator.next().getId();
                }
                oos.writeInt(buf.length);
                for (int k : buf) oos.writeInt(k);
            }
        }
        // now store the complete table
        oos.writeInt(searchMatrix.length);
        for (int k=0; k < searchMatrix.length; ++k) {
            oos.writeInt(searchMatrix[k].length);
            for (int j=0; j < searchMatrix[k].length; ++j) {
                // write the alphabet
                MolecularFormula F = searchMatrix[k][j];
                oos.writeByte(selections.get(F.getTableSelection()).byteValue());
                short[] buf = F.buffer();
                oos.writeByte(buf.length);
                // write the buffer
                for (short val : buf)
                    oos.writeShort(val);
            }
        }
    }

    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        final PeriodicTable T = PeriodicTable.getInstance();
        // first read the alphabets
        final TableSelection[] selections = new TableSelection[ois.readInt()];
        for (int k=0; k < selections.length; ++k) {
            final List<Element> elems = new ArrayList<>();
            int n = ois.readInt();
            for (int j=0; j < n; ++j) elems.add(T.get(ois.readInt()));
            selections[k] = T.tryToAddTableSelectionIntoCache(new TableSelection(T, elems, true));
        }
        // now read the matrix
        this.searchMatrix = new MolecularFormula[ois.readInt()][];
        for (int k=0; k < searchMatrix.length; ++k) {
            searchMatrix[k] = new MolecularFormula[ois.readInt()];
            for (int j=0; j < searchMatrix[k].length; ++j) {
                final TableSelection sel = selections[ois.readByte()];
                final short[] buffer = new short[ois.readByte()];
                for (int m=0; m < buffer.length; ++m) buffer[m] = ois.readShort();
                searchMatrix[k][j] = MolecularFormula.fromCompomer(sel, buffer);
            }
        }
     }

    protected class RangeIterator implements Iterator<MolecularFormula> {

        private final double end;
        private int bucket, index;

        public RangeIterator(double start, double end) {
            this.end = end;
            start = Math.max(start, 0);
            this.bucket = (int)Math.round(start);
            this.index = -1;
            if (start < end) {
                fetchFirst(start);
            }
        }

        private void fetchFirst(double start) {
            int bucket = (int)Math.round(start);
            if (bucket >= searchMatrix.length) {
                return;
            }
            int found = Arrays.binarySearch(searchMatrix[bucket], start, new Comparator<Comparable<? extends Comparable<?>>>() {
                @Override
                public int compare(Comparable<? extends Comparable<?>> o1, Comparable<? extends Comparable<?>> o2) {
                    double left, right;
                    if (o1 instanceof MolecularFormula) left = ((MolecularFormula) o1).getMass();
                    else left = ((Double)o1);
                    if (o2 instanceof MolecularFormula) right = ((MolecularFormula) o2).getMass();
                    else right = ((Double)o2);
                    return Double.compare(left,right);

                }
            });
            if (found >= 0) {
                index = found;
            } else {
                index = -(found+1);
            }
            if (index >= searchMatrix[bucket].length) fetchNext();
        }

        private void fetchNext() {
            while (++index >= searchMatrix[bucket].length) {
                index=-1;
                  if (++bucket >= searchMatrix.length) {
                      return;
                  };
            };
            // check mass
            if (searchMatrix[bucket][index].getMass() >= end) {
                index=-1;
            }
        }

        @Override
        public boolean hasNext() {
            return index >= 0;
        }

        @Override
        public MolecularFormula next() {
            if (index < 0) throw new NoSuchElementException();
            final MolecularFormula formula = searchMatrix[bucket][index];
            fetchNext();
            return formula;
        }
    }


}