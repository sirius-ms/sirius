
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

package de.unijena.bioinf.fingerid.fingerprints;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.*;
import org.openscience.cdk.graph.AllCycles;
import org.openscience.cdk.graph.ConnectedComponents;
import org.openscience.cdk.graph.GraphUtil;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.isomorphism.Pattern;
import org.openscience.cdk.isomorphism.Ullmann;
import org.openscience.cdk.isomorphism.matchers.smarts.SmartsMatchers;
import org.openscience.cdk.ringsearch.RingSearch;
import org.openscience.cdk.smiles.smarts.parser.SMARTSParser;
import org.openscience.cdk.tools.ILoggingTool;
import org.openscience.cdk.tools.LoggingToolFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * Created by kaidu on 10.10.14.
 */
@Deprecated
public class FixedMACCSFingerprinter extends AbstractFingerprinter implements IFingerprinter {

    private static ILoggingTool logger =
            LoggingToolFactory.createLoggingTool(MACCSFingerprinter.class);

    private static final String KEY_DEFINITIONS = "data/maccs.txt";

    private volatile MaccsKey[] keys = null;

    @Deprecated
    public FixedMACCSFingerprinter() {
    }

    @Deprecated
    public FixedMACCSFingerprinter(IChemObjectBuilder builder) {
        try {
            keys = readKeyDef(builder);
        } catch (IOException e) {
            logger.debug(e);
        } catch (CDKException e) {
            logger.debug(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public IBitFingerprint getBitFingerprint(IAtomContainer container) throws CDKException {

        MaccsKey[] keys = keys(container.getBuilder());
        BitSet fp = new BitSet(keys.length);

        // init SMARTS invariants (connectivity, degree, etc)
        SmartsMatchers.prepare(container, false);

        for (int i = 0; i < keys.length; i++) {
            Pattern pattern = keys[i].pattern;
            if (pattern == null)
                continue;

            // check if there are at least 'count' unique hits, key.count = 0
            // means find at least one match hence we add 1 to out limit
            if (pattern.matchAll(container)
                    .uniqueAtoms()
                    .atLeast(keys[i].count + 1))
                fp.set(i);
        }

        // at this point we have skipped the entries whose pattern is "?"
        // (bits 1,44,125,166) so let try and do those features by hand

        // bit 125 aromatic ring count > 1
        // bit 101 a ring with more than 8 members

        RingFound ringFound = findAllRings(container, container.getAtomCount());
        int ringCount = 0;
        for (int i = 0; i < ringFound.ringset.getAtomContainerCount(); i++) {
            IAtomContainer ring = ringFound.ringset.getAtomContainer(i);
            boolean allAromatic = true;
            if (ringCount < 2) { // already found enough aromatic rings
                for (IBond bond : ring.bonds()) {
                    if (!bond.getFlag(CDKConstants.ISAROMATIC)) {
                        allAromatic = false;
                        break;
                    }
                }
            }
            if (allAromatic) ringCount++;
            if (ringCount > 1)
                fp.set(124);
            if (ring.getAtomCount() >= 8)
                fp.set(100);
        }
        if (ringFound.completed == false) System.err.println("Warning: RingFinger did not complete for "+container.getID());

        // bit 166 (*).(*) we can match this in SMARTS but it's faster to just
        // count the number of component
        ConnectedComponents cc = new ConnectedComponents(GraphUtil.toAdjList(container));
        if (cc.nComponents() > 1)
            fp.set(165);


        return new BitSetFingerprint(fp);
    }

    private static class RingFound {
        private final IRingSet ringset;
        private final boolean completed;

        private RingFound(IRingSet ringset, boolean completed) {
            this.ringset = ringset;
            this.completed = completed;
        }
    }

    private static RingFound findAllRings(IAtomContainer container, int maxRingSize)
            throws CDKException {
        GraphUtil.EdgeToBondMap edges = GraphUtil.EdgeToBondMap.withSpaceFor(container);
        int[][] graph = GraphUtil.toAdjList(container, edges);

        RingSearch rs = new RingSearch(container, graph);

        IRingSet ringSet = container.getBuilder().newInstance(IRingSet.class);
        for (int[] isolated : rs.isolated()) {
            if (isolated.length <= maxRingSize) {
                IRing ring = toRing(container, edges, GraphUtil.cycle(graph, isolated));


                ringSet.addAtomContainer(ring);
            }
        }
        boolean complete = true;
        for (int[] fused : rs.fused()) {
            AllCycles ac = new AllCycles(GraphUtil.subgraph(graph, fused), Math.min(maxRingSize, fused.length), 1440);
            if (!ac.completed()) {
                complete = false;
            }
            for (int[] path : ac.paths()) {
                IRing ring = toRing(container, edges, path, fused);

                ringSet.addAtomContainer(ring);
            }
        }
        return new RingFound(ringSet, complete);
    }

    private static IRing toRing(IAtomContainer container, GraphUtil.EdgeToBondMap edges, int[] cycle, int[] mapping) {
        IRing ring = container.getBuilder().newInstance(IRing.class, Integer.valueOf(0));

        int len = cycle.length - 1;

        IAtom[] atoms = new IAtom[len];
        IBond[] bonds = new IBond[len];
        for (int i = 0; i < len; i++) {
            atoms[i] = container.getAtom(mapping[cycle[i]]);
            bonds[i] = edges.get(mapping[cycle[i]], mapping[cycle[(i + 1)]]);

            atoms[i].setFlag(CDKConstants.ISINRING, true);
        }
        ring.setAtoms(atoms);
        ring.setBonds(bonds);

        return ring;
    }

    private static IRing toRing(IAtomContainer container, GraphUtil.EdgeToBondMap edges, int[] cycle) {
        IRing ring = container.getBuilder().newInstance(IRing.class, Integer.valueOf(0));

        int len = cycle.length - 1;

        IAtom[] atoms = new IAtom[len];
        IBond[] bonds = new IBond[len];
        for (int i = 0; i < len; i++) {
            atoms[i] = container.getAtom(cycle[i]);
            bonds[i] = edges.get(cycle[i], cycle[(i + 1)]);
            atoms[i].setFlag(CDKConstants.ISINRING, true);
        }
        ring.setAtoms(atoms);
        ring.setBonds(bonds);

        return ring;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Integer> getRawFingerprint(IAtomContainer iAtomContainer) throws CDKException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        if (keys != null)
            return keys.length;
        else {
            try {
                return keys(DefaultChemObjectBuilder.getInstance()).length;
            } catch (CDKException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private MaccsKey[] readKeyDef(final IChemObjectBuilder builder) throws IOException, CDKException {
        List<MaccsKey> keys = new ArrayList<MaccsKey>(166);
        BufferedReader reader = new BufferedReader(new InputStreamReader(MACCSFingerprinter.class.getResourceAsStream(KEY_DEFINITIONS)));

        // now process the keys
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.charAt(0) == '#')
                continue;
            String data = line.substring(0, line.indexOf('|')).trim();
            String[] toks = data.split("\\s");

            keys.add(new MaccsKey(toks[1],
                    createPattern(toks[1], builder),
                    Integer.parseInt(toks[2])));
        }
        if (keys.size() != 166)
            throw new CDKException("Found " + keys.size()
                    + " keys during setup. Should be 166");
        return keys.toArray(new MaccsKey[166]);
    }

    private class MaccsKey {
        private String smarts;
        private int count;
        private Pattern pattern;

        private MaccsKey(String smarts, Pattern pattern, int count) {
            this.smarts = smarts;
            this.pattern = pattern;
            this.count = count;
        }

        public String getSmarts() {
            return smarts;
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICountFingerprint getCountFingerprint(IAtomContainer container)
            throws CDKException {
        throw new UnsupportedOperationException();
    }

    private final Object lock = new Object();

    /**
     * Access MACCS keys definitions.
     *
     * @return array of MACCS keys.
     * @throws CDKException maccs keys could not be loaded
     */
    private MaccsKey[] keys(final IChemObjectBuilder builder) throws CDKException {
        MaccsKey[] result = keys;
        if (result == null) {
            synchronized (lock) {
                result = keys;
                if (result == null) {
                    try {
                        keys = result = readKeyDef(builder);
                    } catch (IOException e) {
                        throw new CDKException("could not read MACCS definitions", e);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Create a pattern for the provided SMARTS - if the SMARTS is '?' a pattern
     * is not created.
     *
     * @param smarts  a smarts pattern
     * @param builder chem object builder
     * @return the pattern to match
     */
    private Pattern createPattern(String smarts, IChemObjectBuilder builder) {
        if (smarts.equals("?"))
            return null;
        return Ullmann.findSubstructure(SMARTSParser.parse(smarts, builder));
    }
}
