

/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.fingerid;

import lombok.Getter;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.isomorphism.SmartsStereoMatch;
import org.openscience.cdk.isomorphism.Ullmann;
import org.openscience.cdk.isomorphism.matchers.IQueryAtom;
import org.openscience.cdk.isomorphism.matchers.QueryAtomContainer;
import org.openscience.cdk.isomorphism.matchers.smarts.SmartsMatchers;
import org.openscience.cdk.isomorphism.mcss.RMap;
import org.openscience.cdk.smiles.smarts.parser.SMARTSParser;
import org.openscience.cdk.smiles.smarts.parser.TokenMgrError;

import java.util.*;
import java.util.stream.Collectors;


/**
 * This class provides a easy to use wrapper around SMARTS matching functionality. <p> User code that wants to do
 * SMARTS matching should use this rather than using SMARTSParser (and UniversalIsomorphismTester) directly. Example
 * usage would be
 * <p>
 * <pre>
 * SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
 * IAtomContainer atomContainer = sp.parseSmiles(&quot;CC(=O)OC(=O)C&quot;);
 * SMARTSQueryTool querytool = new SMARTSQueryTool(&quot;O=CO&quot;);
 * boolean status = querytool.matches(atomContainer);
 * if (status) {
 *    int nmatch = querytool.countMatches();
 *    List mappings = querytool.getMatchingAtoms();
 *    for (int i = 0; i &lt; nmatch; i++) {
 *       List atomIndices = (List) mappings.get(i);
 *    }
 * }
 * </pre>
 * <h2>Unsupported Features</h2> <ul> <li>Component level grouping <li>Stereochemistry <li>Reaction support </ul>
 * <h2>SMARTS Extensions</h2>
 * <p>
 * Currently the CDK supports the following SMARTS symbols, that are not described in the Daylight specification.
 * However they are supported by other packages and are noted as such.
 * <p>
 * <table border=1>
 * <caption>Supported SMARTS symbols</caption>
 * <thead> <tr> <th>Symbol</th><th>Meaning</th><th>Default</th><th>Notes</th> </tr>
 * </thead> <tbody> <tr> <td>Gx</td><td>Periodic group number</td><td>None</td><td>x must be specified and must be a
 * number between 1 and 18. This symbol is supported by the MOE SMARTS implementation</td> <tr> <td>#X</td><td>Any
 * non-carbon heavy element</td><td>None</td><td>This symbol is supported by the MOE SMARTS implementation</td> </tr>
 * <tr> <td>^x</td><td>Any atom with the a specified hybridization state</td><td>None</td><td>x must be specified and
 * should be between 1 and 8 (inclusive), corresponding to SP1, SP2, SP3, SP3D1, SP3D2 SP3D3, SP3D4 and SP3D5. Supported
 * by the OpenEye SMARTS implementation</td> </tr> </tbody> </table>
 * <p>
 * <h2>Notes</h2> <ul> <li>As <a href="http://sourceforge.net/mailarchive/message.php?msg_name=4964F605.1070502%40emolecules.com">described</a>
 * by Craig James the <code>h&lt;n&gt;</code> SMARTS pattern should not be used. It was included in the Daylight spec
 * for backwards compatibility. To match hydrogens, use the <code>H&lt;n&gt;</code> pattern.</li> <li>The wild card
 * pattern (<code>*</code>) will not match hydrogens (explicit or implicit) unless an isotope is specified. In other
 * words, <code>*</code> gives two hits against <code>C[2H]</code> but 1 hit against <code>C[H]</code>. This also means
 * that it gives no hits against <code>[H][H]</code>. This is contrary to what is shown by Daylights <a
 * href="http://www.daylight.com/daycgi_tutorials/depictmatch.cgi">depictmatch</a> service, but is based on this <a
 * href="https://sourceforge.net/mailarchive/message.php?msg_name=4964FF9D.3040004%40emolecules.com">discussion</a>. A
 * work around to get <code>*</code> to match <code>[H][H]</code> is to write it in the form <code>[1H][1H]</code>.
 * <p>
 * It's not entirely clear what the behavior of * should be with respect to hydrogens. it is possible that the code will
 * be updated so that <code>*</code> will not match <i>any</i> hydrogen in the future.</li> <li>The
 * org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector only considers single rings and two fused non-spiro
 * rings. As a result, it does not properly detect aromaticity in polycyclic systems such as
 * <code>[O-]C(=O)c1ccccc1c2c3ccc([O-])cc3oc4cc(=O)ccc24</code>. Thus SMARTS patterns that depend on proper aromaticity
 * detection may not work correctly in such polycyclic systems</li> </ul>
 *
 * @author Rajarshi Guha
 * cdk.created 2007-04-08
 * cdk.module smarts
 * cdk.githash
 * cdk.keyword SMARTS
 * cdk.keyword substructure search
 * cdk.bug 1760973
 * cdk.bug 1761027
 */
@Deprecated
public class FasterSmartsQueryTool {

    private String smarts;
    private IAtomContainer atomContainer = null;
    private QueryAtomContainer query = null;
    private List<int[]> mappings;

    /**
     * Defines which set of rings to define rings in the target.
     */
    private enum RingSet {

        /**
         * Smallest Set of Smallest Rings (or Minimum Cycle Basis - but not
         * strictly the same). Defines what is typically thought of as a 'ring'
         * however the non-uniqueness leads to ambiguous matching.
         */
        SmallestSetOfSmallestRings {
            @Override
            IRingSet ringSet(IAtomContainer m) {
                return Cycles.sssr(m).toRingSet();
            }
        },

        /**
         * Intersect of all Minimum Cycle Bases (or SSSR) and thus is a subset.
         * The set is unique but may excludes rings (e.g. from bridged systems).
         */
        EssentialRings {
            @Override
            IRingSet ringSet(IAtomContainer m) {
                return Cycles.essential(m).toRingSet();
            }
        },

        /**
         * Union of all Minimum Cycle Bases (or SSSR) and thus is a superset.
         * The set is unique but may include more rings then is necessary.
         */
        RelevantRings {
            @Override
            IRingSet ringSet(IAtomContainer m) {
                return Cycles.relevant(m).toRingSet();
            }
        };

        /**
         * Compute a ring set for a molecule.
         *
         * @param m molecule
         * @return the ring set for the molecule
         */
        abstract IRingSet ringSet(IAtomContainer m);
    }

    /**
     * Which short cyclic set should be used.
     */
    private RingSet ringSet = RingSet.EssentialRings;

    private final IChemObjectBuilder builder;

    /**
     * Aromaticity perception - dealing with SMARTS we should use the Daylight
     * model. This can be set to a different model using {@link #setAromaticity(Aromaticity)}.
     */
    private Aromaticity aromaticity = new Aromaticity(ElectronDonation.daylight(),
            Cycles.allOrVertexShort());

    /**
     * Logical flag indicates whether the aromaticity model should be skipped.
     * Generally this should be left as false to ensure the structures being
     * matched are all treated the same. The flag can however be turned off if
     * the molecules being tests are known to all have the same aromaticity
     * model.
     */
    private boolean skipAromaticity = false;

    // a simplistic cache to store parsed SMARTS queries
    private int MAX_ENTRIES = 20;
    Map<String, QueryAtomContainer> cache = new LinkedHashMap<String, QueryAtomContainer>(MAX_ENTRIES + 1,
            .75F, true) {

        @Override
        public boolean removeEldestEntry(Map.Entry eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    /**
     * Create a new SMARTS query tool for the specified SMARTS string. Query
     * objects will contain a reference to the specified {@link
     * IChemObjectBuilder}.
     *
     * @param smarts SMARTS query string
     * @throws IllegalArgumentException if the SMARTS string can not be handled
     */
    public FasterSmartsQueryTool(String smarts, IChemObjectBuilder builder) {
        this.builder = builder;
        this.smarts = smarts;
        try {
            initializeQuery();
        } catch (TokenMgrError error) {
            throw new IllegalArgumentException("Error parsing SMARTS", error);
        } catch (CDKException error) {
            throw new IllegalArgumentException("Error parsing SMARTS", error);
        }
    }

    /**
     * Set the maximum size of the query cache.
     *
     * @param maxEntries The maximum number of entries
     */
    public void setQueryCacheSize(int maxEntries) {
        MAX_ENTRIES = maxEntries;
    }

    /**
     * Indicates that ring properties should use the Smallest Set of Smallest
     * Rings. The set is not unique and may lead to ambiguous matches.
     *
     * @see #useEssentialRings()
     * @see #useRelevantRings()
     */
    public void useSmallestSetOfSmallestRings() {
        this.ringSet = RingSet.SmallestSetOfSmallestRings;
    }

    /**
     * Indicates that ring properties should use the Relevant Rings. The set is
     * unique and includes all of the SSSR but may be exponential in size.
     *
     * @see #useSmallestSetOfSmallestRings()
     * @see #useEssentialRings()
     */
    public void useRelevantRings() {
        this.ringSet = RingSet.RelevantRings;
    }

    /**
     * Indicates that ring properties should use the Essential Rings (default).
     * The set is unique but only includes a subset of the SSSR.
     *
     * @see #useSmallestSetOfSmallestRings()
     * @see #useEssentialRings()
     */
    public void useEssentialRings() {
        this.ringSet = RingSet.EssentialRings;
    }

    /**
     * Set the aromaticity perception to use. Different aromaticity models
     * may required certain attributes to be set (e.g. atom typing). These
     * will not be automatically configured and should be preset before matching.
     * <p>
     * <blockquote><pre>
     * SMARTSQueryTool sqt = new SMARTSQueryTool(...);
     * sqt.setAromaticity(new Aromaticity(ElectronDonation.cdk(),
     *                                    Cycles.cdkAromaticSet));
     * for (IAtomContainer molecule : molecules) {
     *
     *     // CDK Aromatic model needs atom types
     *     AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
     *
     *     sqt.matches(molecule);
     * }
     * </pre></blockquote>
     *
     * @param aromaticity the new aromaticity perception
     * @see ElectronDonation
     * @see Cycles
     */
    public void setAromaticity(Aromaticity aromaticity) {
        this.aromaticity = Objects.requireNonNull(aromaticity, "aromaticity was not provided");
    }

    /**
     * Returns the current SMARTS pattern being used.
     *
     * @return The SMARTS pattern
     */
    public String getSmarts() {
        return smarts;
    }

    /**
     * Set a new SMARTS pattern.
     *
     * @param smarts The new SMARTS pattern
     * @throws CDKException if there is an error in parsing the pattern
     */
    public void setSmarts(String smarts) throws CDKException {
        this.smarts = smarts;
        initializeQuery();
    }

    /**
     * Perform a SMARTS match and check whether the query is present in the target molecule. <p> This function simply
     * checks whether the query pattern matches the specified molecule. However the function will also, internally, save
     * the mapping of query atoms to the target molecule
     * <p>
     * <b>Note</b>: This method performs a simple caching scheme, by comparing the current molecule to the previous
     * molecule by reference. If you repeatedly match different SMARTS on the same molecule, this method will avoid
     * initializing ( ring perception, aromaticity etc.) the molecule each time. If however, you modify the molecule
     * between such multiple matchings you should use the other form of this method to force initialization.
     *
     * @param atomContainer The target moleculoe
     * @return true if the pattern is found in the target molecule, false otherwise
     * @throws CDKException if there is an error in ring, aromaticity or isomorphism perception
     * @see #getMatchingAtoms()
     * @see #countMatches()
     */
    public boolean matches(IAtomContainer atomContainer) throws CDKException {
        this.atomContainer = atomContainer;
        if (this.atomContainer.getProperty("SmartPrepared")!=null) {

        } else {
            initializeMolecule();
        }

        // lets see if we have a single atom query
        if (query.getAtomCount() == 1) {
            // lets get the query atom
            IQueryAtom queryAtom = (IQueryAtom) query.getAtom(0);

            mappings = new ArrayList<int[]>();
            for (int i = 0; i < atomContainer.getAtomCount(); i++) {
                if (queryAtom.matches(atomContainer.getAtom(i))) {
                    mappings.add(new int[]{i});
                }
            }
        } else {
            mappings = Ullmann.findSubstructure(query).matchAll(atomContainer).stream()
                    .filter(new SmartsStereoMatch(query, atomContainer)).collect(Collectors.toList());
        }

        SmartsStereoMatch s = new SmartsStereoMatch(query, atomContainer);

        return !mappings.isEmpty();
    }
    /**
     * Returns the number of times the pattern was found in the target molecule. <p> This function should be called
     * after {@link #matches(IAtomContainer)}. If not, the results may be undefined.
     *
     * @return The number of times the pattern was found in the target molecule
     */
    public int countMatches() {
        return mappings.size();
    }

    /**
     * Get the atoms in the target molecule that match the query pattern. <p> Since there may be multiple matches, the
     * return value is a List of List objects. Each List object contains the indices of the atoms in the target
     * molecule, that match the query pattern
     *
     * @return A List of List of atom indices in the target molecule
     */
    public List<List<Integer>> getMatchingAtoms() {
        List<List<Integer>> matched = new ArrayList<List<Integer>>(mappings.size());
        for (int[] mapping : mappings)
            matched.add(Arrays.stream(mapping).boxed().collect(Collectors.toList()));
        return matched;
    }

    /**
     * Get the atoms in the target molecule that match the query pattern. <p> Since there may be multiple matches, the
     * return value is a List of List objects. Each List object contains the unique set of indices of the atoms in the
     * target molecule, that match the query pattern
     *
     * @return A List of List of atom indices in the target molecule
     */
    public List<List<Integer>> getUniqueMatchingAtoms() {
        List<List<Integer>> matched = new ArrayList<List<Integer>>(mappings.size());
        Set<BitSet> atomSets = new HashSet<>(mappings.size());
        for (int[] mapping : mappings) {
            BitSet atomSet = new BitSet();
            for (int x : mapping)
                atomSet.set(x);
            if (atomSets.add(atomSet)) matched.add(Arrays.stream(mapping).boxed().collect(Collectors.toList()));
        }
        return matched;
    }

    /**
     * Prepare the target molecule for analysis. <p> We perform ring perception and aromaticity detection and set up
     * the appropriate properties. Right now, this function is called each time we need to do a query and this is
     * inefficient.
     *
     * @throws CDKException if there is a problem in ring perception or aromaticity detection, which is usually related
     *                      to a timeout in the ring finding code.
     */
    private void initializeMolecule() throws CDKException {

        // initialise required invariants - the query has ISINRING set if
        // the query contains ring queries [R?] [r?] [x?] etc.
        SmartsMatchers.prepare(atomContainer, true);

        // providing skip aromaticity has not be set apply the desired
        // aromaticity model
        try {
            if (!skipAromaticity) {
                aromaticity.apply(atomContainer);
            }
        } catch (CDKException e) {
            throw new CDKException(e.toString(), e);
        }
        atomContainer.setProperty("SmartPrepared", true);
    }

    private void initializeQuery() throws CDKException {
        mappings = null;
        query = cache.get(smarts);
        if (query == null) {
            query = SMARTSParser.parse(smarts, builder);
            cache.put(smarts, query);
        }
    }

    private List<Set<Integer>> matchedAtoms(List<List<RMap>> bondMapping, IAtomContainer atomContainer) {

        List<Set<Integer>> atomMapping = new ArrayList<Set<Integer>>();
        // loop over each mapping
        for (List<RMap> mapping : bondMapping) {

            Set<Integer> tmp = new TreeSet<Integer>();
            IAtom atom1 = null;
            IAtom atom2 = null;
            // loop over this mapping
            for (RMap map : mapping) {

                int bondID = map.getId1();

                // get the atoms in this bond
                IBond bond = atomContainer.getBond(bondID);
                atom1 = bond.getAtom(0);
                atom2 = bond.getAtom(1);

                Integer idx1 = atomContainer.getAtomNumber(atom1);
                Integer idx2 = atomContainer.getAtomNumber(atom2);

                if (!tmp.contains(idx1)) tmp.add(idx1);
                if (!tmp.contains(idx2)) tmp.add(idx2);
            }
            if (tmp.size() == query.getAtomCount()) atomMapping.add(tmp);

            // If there is only one bond, check if it matches both ways.
            if (mapping.size() == 1 && atom1.getAtomicNumber().equals(atom2.getAtomicNumber())) {
                atomMapping.add(new TreeSet<Integer>(tmp));
            }
        }

        return atomMapping;
    }
}

