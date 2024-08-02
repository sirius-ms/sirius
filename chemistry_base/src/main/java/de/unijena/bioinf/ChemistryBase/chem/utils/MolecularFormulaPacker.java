
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.chem.*;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntProcedure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implements methods from MolecularFormula in a static manner
 * Packed Molecular formula are only 8 bytes large, but have several restrictions:
 * - there is a maximum number of occurences for each element
 * - there is a maximum number of different elements
 * - negative amounts for elements are not allowed
 * - getting molecule mass or the amount of elements is slower than for MolecularFormula
 * <p>
 * Comparing two PackedFormula is slightly faster than comparing
 * molecular formulas. So use PackedFormula if you have a huge set
 * of formulas you only want to compare
 */
public class MolecularFormulaPacker {

    // DEFAULT PACKER
    // H: 0-1024 (10 bits)
    // C: 0-256  (8 bits)
    // N: 0-32  (5 bits)
    // O: 0-32 (5 bits)
    // S: 0-16 (4 bits)
    // P: 0-16 (4 bits)
    // F: 0-32 (5 bits)
    // I: 0-8 (3 bits)
    // Cl: 0-16 (4 bits)
    // Br: 0-16 (4 bits)
    // X1: 0-8
    // X2: 0-8
    // X3: 0-8
    // X4: 0-8


    private final int[] BIT_SIZES;
    private final int[] SHIFTS;
    private final long[] MASKS;
    private final Element[] elements;
    private final TIntIntHashMap elementMapper;
    private final byte C, H, N, O;

    // first bit is sign. Some operation doesn't work with sign because 2 complement
    private final static int NUMBER_OF_BITS = 63;

    private final long OverflowFastPath;

    /**
     * Creates a new MolecularFormulaPacker such that all formulas given in the formulas array can be encoded
     * and decoded without loss of information.
     */
    public static MolecularFormulaPacker newPackerFor(MolecularFormula... formulas) {
        final PeriodicTable table = PeriodicTable.getInstance();
        final TIntIntHashMap elements = new TIntIntHashMap(18);
        for (MolecularFormula f : formulas) {
            f.visit(new FormulaVisitor<Object>() {
                @Override
                public Object visit(Element element, int amount) {
                    if (amount > 0) {
                        final int oldam = elements.get(element.getId());
                        if (amount > oldam) elements.put(element.getId(), amount);
                    }
                    return null;
                }
            });
        }
        final Element[] elems = new Element[elements.size()];
        elements.forEachKey(new TIntProcedure() {
            int k = 0;

            @Override
            public boolean execute(int value) {
                elems[k++] = table.get(value);
                return true;
            }
        });
        final ChemicalAlphabet alphabet = new ChemicalAlphabet(elems);
        final int[] upperbounds = new int[elements.size()];
        for (int k = 0; k < upperbounds.length; ++k)
            upperbounds[k] = elements.get(alphabet.get(k).getId());
        return newPacker(alphabet, upperbounds);
    }

    /**
     * Creates a new MolecularFormulaPacker with the given Encoding.
     *
     * @param alphabet chemical alphabet with elements that have to be encoded
     * @param bitsizes integer array that assigns for each element in the alphabet the number of bits for encoding
     * @return
     */
    public static MolecularFormulaPacker newPackerWithEncoding(ChemicalAlphabet alphabet, int[] bitsizes) {
        if (bitsizes.length != alphabet.size()) throw new FormulaEncodingError("Number of elements in alphabet and " +
                "encodings in array differ");
        int free_bits = NUMBER_OF_BITS;
        for (int k = 0; k < bitsizes.length; ++k) {
            if (bitsizes[k] < 0) throw new FormulaEncodingError("Negative number of bits is not allowed.");
            free_bits -= bitsizes[k];
        }
        if (free_bits < 0) throw new FormulaEncodingError("Invalid encoding: " + (NUMBER_OF_BITS - free_bits) +
                "bits cannot be packed in a 64 bit integer without sign bit.");
        final Element[] elements = alphabet.getElements().toArray(new Element[alphabet.size()]);
        return new MolecularFormulaPacker(elements, bitsizes.clone());
    }

    /**
     * Creates a new MolecularFormulaPacker that can encode molecular formulas with reasonable size
     * of the given alphabet
     *
     * @param alphabet
     * @return
     */
    public static MolecularFormulaPacker newPacker(ChemicalAlphabet alphabet) {
        return newPacker(alphabet, new int[alphabet.size()]);
    }

    /**
     * Creates a new MolecularFormulaPacker that can encode molecular formulas of the given alphabet
     * Guarantees that all formulas can be encoded which elements occurs not more frequently than the number in
     * the upperbounds array. The packer may be able to encode even higher frequency if there are enough free bits.
     *
     * @param alphabet
     * @param upperbounds assigns a maximum frequence to each element. The packer have to be able to encode all formulas
     *                    with elements of this frequency
     * @return
     */
    public static MolecularFormulaPacker newPacker(ChemicalAlphabet alphabet, int[] upperbounds) {
        final Element[] elements = alphabet.getElements().toArray(new Element[alphabet.size()]);
        final int[] bitsizes = new int[elements.length];
        int freebits = NUMBER_OF_BITS;
        // step 1: give each element as much bits as the given upperbound enforces
        for (int k = 0; k < upperbounds.length; ++k) {
            final int size = numofbits(upperbounds[k] + 1);
            if (size > freebits)
                throw new FormulaEncodingError("Not enough bits to encode the alphabet with the given upperbounds");
            bitsizes[k] = size;
            freebits -= size;
        }
        // step 2: distribute the free bits to the elements in order of their mass.
        if (freebits > 0) {
            final Element[] massOrderedElements = elements.clone();
            final TIntIntHashMap ordering = new TIntIntHashMap(massOrderedElements.length, 0.75f, -1, -1);
            int k = 0;
            for (Element e : massOrderedElements) ordering.put(e.getId(), k++);
            Arrays.sort(massOrderedElements);
            int sum = Math.max(1000, massOrderedElements[massOrderedElements.length - 1].getIntegerMass() * 10);
            for (int i = 0; i < massOrderedElements.length; ++i) {
                final int a = massOrderedElements[i].getIntegerMass();
                final int size = Math.min(freebits, Math.max(0, numofbits(sum / a)));
                final int oldsize = bitsizes[ordering.get(massOrderedElements[i].getId())];
                bitsizes[ordering.get(massOrderedElements[i].getId())] = Math.max(size, oldsize);
                freebits -= Math.max(0, size - oldsize);
                if (freebits <= 0) break;
            }
        }
        // step 3: if there are still free bits, distribute them uniformly
        outer:
        while (freebits > 0) {
            for (int k = 0; k < bitsizes.length; ++k) {
                ++bitsizes[k];
                if (--freebits <= 0) break outer;
            }
        }
        return new MolecularFormulaPacker(elements, bitsizes);
    }

    private static int numofbits(int value) {
        if (value == 0) return 0;
        return 32 - Integer.numberOfLeadingZeros(value - 1);
    }

    private MolecularFormulaPacker(final Element[] elements, int[] BIT_SIZES) {
        this.elements = elements;
        this.BIT_SIZES = BIT_SIZES;
        SHIFTS = new int[BIT_SIZES.length];
        MASKS = new long[BIT_SIZES.length];
        final long[] UNSHIFTED_MASKS = new long[BIT_SIZES.length];
        int b = 0;
        for (int i = 0; i < BIT_SIZES.length; ++i) {
            SHIFTS[i] = b;
            UNSHIFTED_MASKS[i] = ((1 << BIT_SIZES[i]) - 1);
            MASKS[i] = UNSHIFTED_MASKS[i] << b;
            b += BIT_SIZES[i];
        }
        elementMapper = new TIntIntHashMap(elements.length * 2, 0.6f, -1, -1);
        int k = 0;
        for (Element e : elements) elementMapper.put(e.getId(), k++);
        final PeriodicTable table = PeriodicTable.getInstance();
        C = (byte) elementMapper.get(table.getByName("C").getId());
        H = (byte) elementMapper.get(table.getByName("H").getId());
        N = (byte) elementMapper.get(table.getByName("N").getId());
        O = (byte) elementMapper.get(table.getByName("O").getId());
        b = 0;
        k = 0;
        for (int i = 0; i < BIT_SIZES.length; ++i) {
            k += BIT_SIZES[i];
            b |= ((1 << k) - 1);
        }
        OverflowFastPath = b;
    }

    /**
     * Creates a packer serialized previously as string
     *
     * @param value
     * @return
     */
    public static MolecularFormulaPacker fromString(String value) {
        final String[] lines = value.split("\n");
        final Element[] elements = new Element[lines.length];
        final int[] BIT_SIZES = new int[elements.length];
        final PeriodicTable table = PeriodicTable.getInstance();
        for (int k = 0; k < lines.length; ++k) {
            final String[] cols = lines[k].split("\t", 2);
            elements[k] = table.getByName(cols[0]);
            BIT_SIZES[k] = Integer.parseInt(cols[1]);
        }
        return new MolecularFormulaPacker(elements, BIT_SIZES);

    }

    /**
     * serialize a packer as string
     *
     * @return
     */
    public String serializeToString() {
        final StringBuilder buffer = new StringBuilder(elements.length * 5);
        for (int k = 0; k < elements.length; ++k) {
            buffer.append(elements[k].getSymbol());
            buffer.append('\t');
            buffer.append(BIT_SIZES[k]);
            buffer.append('\n');
        }
        return buffer.toString();
    }


    public double getMass(final long formula) {
        if (formula == 0) return 0d;
        double mass = 0d;
        for (int k = 0; k < elements.length; ++k) {
            final int amount = numberOfXthElement(formula, k);
            if (amount > 0) {
                mass += elements[k].getMass() * amount;
            }
        }
        return mass;
    }


    public int getIntMass(final long formula) {
        if (formula == 0) return 0;
        int mass = 0;
        for (int k = 0; k < elements.length; ++k) {
            final int amount = numberOfXthElement(formula, k);
            if (amount > 0) {
                mass += elements[k].getIntegerMass() * amount;
            }
        }
        return mass;
    }

    public List<Element> elements(final long formula) {
        final ArrayList<Element> elems = new ArrayList<Element>(elements.length);
        for (int k = 0; k < elements.length; ++k)
            if (numberOfXthElement(formula, k) > 0) elems.add(elements[k]);
        return elems;
    }


    public Element[] elementArray(final long formula) {
        final List<Element> elems = elements(formula);
        return elems.toArray(new Element[elems.size()]);
    }

    public int numberOf(final long formula, final Element element) {
        return numberOfXthElement(formula, elementMapper.get(element.getId()));
    }

    private int numberOfXthElement(long formula, int k) {
        if (k < BIT_SIZES.length) {
            return (int) ((formula & MASKS[k]) >> SHIFTS[k]);
        } else return 0;
    }


    public float rdbe(final long formula) {
        return doubledRDBE(formula) / 2f;
    }


    public int doubledRDBE(final long formula) {
        int rdbe = 2;
        for (int i = 0; i < elements.length; ++i) {
            rdbe += numberOfXthElement(formula, i) * (elements[i].getValence() - 2);
        }
        return rdbe;
    }


    public int atomCount(final long formula) {
        int num = 0;
        for (int k = 0; k < elements.length; ++k) {
            num += numberOfXthElement(formula, k);
        }
        return num;
    }


    public boolean isCHNO(final long formula) {
        final long chno = MASKS[C] | MASKS[H] | MASKS[N] | MASKS[O];
        return (formula | chno) == chno;
    }


    public boolean isCHNOPS(final long formula) {
        long chno = MASKS[C] | MASKS[H] | MASKS[N] | MASKS[O];
        int k = elementMapper.get(PeriodicTable.getInstance().getByName("P").getId());
        if (k >= 0) chno |= MASKS[k];
        k = elementMapper.get(PeriodicTable.getInstance().getByName("S").getId());
        if (k >= 0) chno |= MASKS[k];
        return (formula | chno) == chno;
    }


    public boolean maybeCharged(final long formula) {
        return doubledRDBE(formula) % 2 == 1;
    }

    /**
     * @return the ratio of the hydrogen amount to the carbon amount
     */
    public float hydrogen2CarbonRatio(long formula) {
        final int carbon = numberOfCarbons(formula);
        final int hydrogen = numberOfHydrogens(formula);
        return (float) hydrogen / (carbon == 0 ? 0.8f : (float) carbon);
    }

    /**
     * @return the ratio of the non-hydrogen and non-carbon atoms to the number carbon atoms
     */
    public float hetero2CarbonRatio(long formula) {
        final int carbon = numberOfCarbons(formula);
        final int hetero = atomCount(formula) - carbon - numberOfHydrogens(formula);
        return (float) hetero / (carbon == 0 ? 0.8f : (float) carbon);
    }

    public float heteroWithoutOxygenToCarbonRatio(long formula) {
        final int carbon = numberOfCarbons(formula);
        final int hetero = atomCount(formula) - (carbon + numberOfHydrogens(formula) + numberOfOxygens(formula));
        return (float) hetero / (carbon == 0 ? 0.8f : (float) carbon);
    }

    /**
     * @return the ratio of the non-hydrogen and non-oxygen to the number of oxygens
     */
    public float hetero2OxygenRatio(long formula) {
        final int oxygen = numberOfOxygens(formula);
        final int hetero = atomCount(formula) - oxygen - numberOfHydrogens(formula);
        return (float) hetero / (oxygen == 0 ? 0.8f : (float) oxygen);
    }


    public int numberOfHydrogens(final long formula) {
        return numberOfXthElement(formula, H);
    }


    public int numberOfOxygens(final long formula) {
        return numberOfXthElement(formula, O);
    }


    public int numberOfNitrogens(final long formula) {
        return numberOfXthElement(formula, N);
    }


    public int numberOfCarbons(final long formula) {
        return numberOfXthElement(formula, C);
    }


    public boolean isSubtractable(long a, long b) {
        if ((a & b) == a) return true;
        for (int k = 0; k < BIT_SIZES.length; ++k) {
            final int elemA = numberOfXthElement(a, k);
            final int elemB = numberOfXthElement(b, k);
            if (elemA - elemB < 0) return false;
        }
        return true;
    }


    public long add(long a, long b) {
        if ((OverflowFastPath & a) == a && (OverflowFastPath & b) == b) {
            return a + b;
        } else {
            // slow path: check each element independend from each other
            for (int k = 0; k < BIT_SIZES.length; ++k) {
                final int elemA = numberOfXthElement(a, k);
                if (elemA == 0) continue;
                final int elemB = numberOfXthElement(b, k);
                if (elemB == 0) continue;
                final int sum = elemA + elemB;
                if (sum >= (1 << BIT_SIZES[k]))
                    throw new FormulaEncodingError("Bit overflow. Sum of both formulas " + a + " and " + b
                            + " cannot be decoded with 64 bits");

            }
            return a + b;
        }
    }


    public long subtract(long a, long b) {
        if (!isSubtractable(a, b))
            throw new FormulaEncodingError("Cannot decode molecular formulas with negative amounts of elements");
        return a - b;
    }

    public short[] buffer(final long formula) {
        return buffer(formula, tableSelection());
    }

    public short[] buffer(final long formula, TableSelection selection) {
        int k = selection.size() - 1;
        for (; k >= 0; --k) {
            final int bitindex = elementMapper.get(selection.get(k).getId());
            if (bitindex < BIT_SIZES.length && bitindex >= 0 && numberOfXthElement(formula, bitindex) > 0) break;
        }
        final short[] buffer = new short[k + 1];
        for (; k >= 0; --k) {
            final int bitindex = elementMapper.get(selection.get(k).getId());
            if (bitindex < 0) continue;
            final short amount = (short) (bitindex < BIT_SIZES.length ? numberOfXthElement(formula, bitindex) : 0);
            buffer[k] = amount;
        }
        return buffer;
    }


    public boolean compare(long a, long b) {
        return a == b;
    }

    public long encode(MolecularFormula formula) {
        final Encoder encoder = new Encoder();
        formula.visit(encoder);
        return encoder.bits;
    }

    /**
     * returns a negative value if molecular formula cannot be encoded by
     * this encoding. Otherwise, returns the encoded value for this formula
     * @param formula
     * @return -1 iff encoding error, otherwise encoded formula
     */
    public long tryEncode(MolecularFormula formula) {
        final Encoder2 encoder = new Encoder2();
        formula.visit(encoder);
        return encoder.errorFlag ? -1 : encoder.bits;
    }

    private final class Encoder implements FormulaVisitor {

        private long bits = 0l;

        @Override
        public Object visit(Element element, int amount) {
            if (amount > 0) {
                final int pos = elementMapper.get(element.getId());
                if (pos < 0) throw new FormulaEncodingError(element + " is not part of encoding");
                if (amount < (1 << BIT_SIZES[pos])) {
                    bits |= ((long) amount << SHIFTS[pos]);
                } else {
                    throw new FormulaEncodingError("Cannot encode element " + element + " with amount of " + amount + " in "
                            + BIT_SIZES[pos] + " bits.");
                }
            }
            return null;
        }
    }
    private final class Encoder2 implements FormulaVisitor {

        private long bits = 0l;
        private boolean errorFlag = false;

        @Override
        public Object visit(Element element, int amount) {
            if (errorFlag) return null;
            if (amount > 0) {
                final int pos = elementMapper.get(element.getId());
                if (pos < 0) {
                    errorFlag=true;
                    return null;
                }
                if (amount < (1 << BIT_SIZES[pos])) {
                    bits |= ((long) amount << SHIFTS[pos]);
                } else {
                    errorFlag = true;
                }
            }
            return null;
        }
    }

    public MolecularFormula decode(final long formula) {
        final TableSelection selection = tableSelection();
        return MolecularFormula.fromCompomer(selection, buffer(formula, selection));
    }

    private TableSelection tableSelection() {
        return PeriodicTable.getInstance().getSelectionFor(elements);
    }


    public void visit(final long formula, FormulaVisitor<?> visitor) {
        for (int k = 0; k < elements.length; ++k) {
            final int amount = numberOfXthElement(formula, k);
            if (amount > 0) visitor.visit(elements[k], amount);
        }
    }
}
