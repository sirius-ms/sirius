package de.unijena.bioinf.fingerid.fingerprints.utils;

import com.google.common.collect.FluentIterable;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.ComponentGrouping;
import org.openscience.cdk.isomorphism.SmartsStereoMatch;
import org.openscience.cdk.isomorphism.Ullmann;
import org.openscience.cdk.isomorphism.matchers.IQueryAtom;
import org.openscience.cdk.isomorphism.matchers.QueryAtomContainer;
import org.openscience.cdk.isomorphism.matchers.smarts.AtomicNumberAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.LogicalOperatorAtom;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;
import org.openscience.cdk.smiles.smarts.parser.SMARTSParser;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that hacks into the SMART matching process and allows to map between the SMARTS string and the molecule atoms
 */
public class SmartsMapper {

    /**
     * each occurence of * or [!#1] is hijacked
     */
    public static final int HIJACK_ATOM_WILDCARDS = 1;

    /**
     * each occurence of ~ is hijacked
     */
    public static final int HIJACK_BOND_WILDCARDS = 2;


    private final int policy;

    public SmartsMapper() {
        this.policy = HIJACK_ATOM_WILDCARDS;
        // bonds are not supported yet =/
    }

    public static String toSmarts(List<Occurence> occurences) {
        return toSmarts(occurences,defaultFormatter);
    }

    public static String toSmarts(List<Occurence> occurences, Function<IAtom, String> formatter) {
        String originalSmarts = occurences.get(0).smarts;
        final StringBuilder buffer = new StringBuilder(originalSmarts.length());
        int b=0;
        for (int i=0; i < occurences.size(); ++i) {
            final Occurence c = occurences.get(i);
            int o = c.stringOffset;
            if (b < o) {
                buffer.append(originalSmarts.substring(b, o));
                b += (o-b);
            }
            buffer.append(formatter.apply(c.getAtom()));
            b += c.stringLength;
        }
        if (b < originalSmarts.length())
            buffer.append(originalSmarts.substring(b));
        return buffer.toString();
    }

    /**
     * check if two smarts match to exactly the same atoms in mol
     */
    public List<List<String>> groupSymetricSmarts(Collection<String> smarts, IAtomContainer mol) throws CDKException {
        try {
            if (smarts.isEmpty()) return Collections.emptyList();
            final String[] sms = smarts.toArray(new String[smarts.size()]);
            final SMARTSQueryTool tool = new SMARTSQueryTool(sms[0], SilentChemObjectBuilder.getInstance());
            final Set<List<Integer>>[] maps = new HashSet[sms.length];
            for (int k = 0; k < sms.length; ++k) {
                maps[k] = new HashSet<>();
                tool.setSmarts(sms[k]);
                tool.matches(mol);
                for (List<Integer> matches : tool.getMatchingAtoms()) {
                    Collections.sort(matches);
                    maps[k].add(matches);
                }
            }

            final HashMap<Set<List<Integer>>, List<String>> groups = new HashMap<>();
            for (int k = 0; k < maps.length; ++k) {
                if (!groups.containsKey(maps[k])) {
                    groups.put(maps[k], new ArrayList<>());
                }
                groups.get(maps[k]).add(sms[k]);
            }
            for (List<String> str : groups.values()) {
                Collections.sort(str);
            }
            return new ArrayList<>(groups.values());
        } catch (IllegalArgumentException sm) {
            throw new CDKException("Illegal SMARTS: " + smarts.toString() + "\n" + sm.getMessage());
        }
    }

    public HijackedSmarts hijack(String smarts) {
        return hijack(smarts,HijackingType.ATOM_WILDCARD);
    }
    public HijackedSmarts hijack(String smarts, HijackingType type) {
        return new HijackedSmarts(smarts,type);
    }

    public static class Occurence {
        private final IAtomContainer parentMolecule;
        private final String smarts;
        private final int index;
        private final int atomIndex;
        private final int stringOffset, stringLength;

        private Occurence(IAtomContainer parentMolecule, int index, String smarts, int atomIndex, int stringOffset, int stringLength) {
            this.parentMolecule = parentMolecule;
            this.atomIndex = atomIndex;
            this.stringOffset = stringOffset;
            this.stringLength = stringLength;
            this.smarts = smarts;
            this.index = index;
        }

        public int getIndex(){
            return index;
        }

        public int getConnectedHeteroAtoms() {
            final IAtom a = getAtom();
            return parentMolecule.getConnectedBondsCount(a);
        }

        public String symbol() {
            return defaultFormatter.apply(getAtom());
        }

        public IAtom getAtom() {
            return parentMolecule.getAtom(atomIndex);
        }
    }

    private final static Function<IAtom, String> defaultFormatter = (a)->a.isAromatic() ? a.getSymbol().toLowerCase() : a.getSymbol();

    public static enum HijackingType {
        ALL_ATOMS,
        ATOM_WILDCARD;
    }

    public class HijackedSmarts {
        private final static int ELEM_OFFSET = 200;
        private final String originalSmarts;
        private final String hijackedSmarts;
        private final QueryAtomContainer query;
        private int[] types;
        private int[] strOffsets, strLenghts;
        private final TIntIntHashMap atomid2occurence;

        private HijackedSmarts(String orig, HijackingType type) {
            this.originalSmarts = orig;
            this.hijackedSmarts = flaggify(originalSmarts, type);
            this.query = SMARTSParser.parse(hijackedSmarts, SilentChemObjectBuilder.getInstance());
            this.atomid2occurence = makeMapping();
        }

        public int numberOfWildcards() {
            return types.length;
        }

        public Set<String> getSmartsVariants(IAtomContainer atomContainer) {
            return getSmartsVariants(atomContainer, defaultFormatter);
        }

        public Set<String> getSmartsVariants(IAtomContainer atomContainer, Function<IAtom, String> formatter) {
            final HashSet<String> variants = new HashSet<>();
            for (List<Occurence> oc : match(atomContainer)) {
                variants.add(toSmarts(oc, formatter));
            }
            return variants;
        }

        public List<List<Occurence>> getNonSymetricMatches(IAtomContainer atomContainer) {
            List<int[]> mappings = FluentIterable.from(Ullmann.findSubstructure(query).matchAll(atomContainer))
                    .filter(new SmartsStereoMatch(query, atomContainer))
                    .filter(new ComponentGrouping(query, atomContainer)).toList();

            final HashMap<List<Integer>, List<List<Occurence>>> variants = new HashMap<>();
            for (int[] map : mappings) {

                final List<Integer> imap = new ArrayList<>(map.length);
                for (int i : map) imap.add(i);
                Collections.sort(imap);

                final List<Occurence> ocs = new ArrayList<>();
                for (int i=0; i < map.length; ++i) {
                    if (atomid2occurence.containsKey(i)) {
                        int o = atomid2occurence.get(i);
                        ocs.add(new Occurence(atomContainer, o, originalSmarts, map[i], strOffsets[o], strLenghts[o]));
                    }
                }
                Collections.sort(ocs, Comparator.comparingInt(a -> a.stringOffset));
                variants.computeIfAbsent(imap, (x)->new ArrayList<>()).add(ocs);
            }

            final List<List<Occurence>> finalList = new ArrayList<>();
            for (List<List<Occurence>> xs : variants.values()) {
                xs.sort(Comparator.comparing(x->toSmarts(x,defaultFormatter)));
                finalList.add(xs.get(0));
            }

            return finalList;
        }

        public List<List<Occurence>> match(IAtomContainer atomContainer) {
            List<List<Occurence>> list = new ArrayList<>();
            List<int[]> mappings = FluentIterable.from(Ullmann.findSubstructure(query).matchAll(atomContainer))
                    .filter(new SmartsStereoMatch(query, atomContainer))
                    .filter(new ComponentGrouping(query, atomContainer)).toList();

            for (int[] map : mappings) {
                final List<Occurence> ocs = new ArrayList<>();
                list.add(ocs);
                int k=0;
                for (int i=0; i < map.length; ++i) {
                    if (atomid2occurence.containsKey(i)) {
                        int o = atomid2occurence.get(i);
                        ocs.add(new Occurence(atomContainer,o, originalSmarts, map[i], strOffsets[o], strLenghts[o]));
                    }
                }
                Collections.sort(ocs, Comparator.comparingInt(a -> a.stringOffset));
            }
            return list;
        }

        public TIntIntHashMap mapBetween(IAtomContainer A, IAtomContainer B) {
            List<List<Occurence>> matchesA = match(A);
            List<List<Occurence>> matchesB = match(B);
            if (matchesA.isEmpty() || matchesB.isEmpty())
                return null;
            List<Occurence> a = matchesA.get(0);
            List<Occurence> b = matchesB.get(0);
            final TIntIntHashMap map = new TIntIntHashMap(A.getAtomCount(), 0.75f, -1,-1), atomi = new TIntIntHashMap(A.getAtomCount(), 0.75f, -1,-1), atomj = new TIntIntHashMap(A.getAtomCount(), 0.75f, -1,-1);
            for (Occurence o : a) {
                atomi.put(o.index, o.atomIndex);
            }
            for (Occurence o : b) {
                atomj.put(o.index, o.atomIndex);
            }
            for (int o : atomi.keys()) {
                if (atomj.get(o)>=0) {
                    map.put(atomi.get(o), atomj.get(o));
                }
            }
            return map;
        }

        private TIntIntHashMap makeMapping() {
            final TIntIntHashMap mapping = new TIntIntHashMap();
            for (int k=0; k < query.getAtomCount(); ++k) {
                final IQueryAtom atom = (IQueryAtom) query.getAtom(k);
                if (atom instanceof LogicalOperatorAtom) {
                    LogicalOperatorAtom log = (LogicalOperatorAtom)atom;
                    String op = log.getOperator();
                    if (op.equals("and")) {
                        if (log.getRight() instanceof LogicalOperatorAtom) {
                            IQueryAtom iq = ((LogicalOperatorAtom) log.getRight()).getLeft();
                            if (iq instanceof AtomicNumberAtom) {
                                mapping.put(k, ((AtomicNumberAtom) iq).getAtomicNumber()-200);
                            }
                        }
                    }
                }
            }
            return mapping;
        }

        private String flaggify(String smarts, HijackingType type) {
            final TIntArrayList tps = new TIntArrayList(), offs = new TIntArrayList(), lngs = new TIntArrayList();
            int k=ELEM_OFFSET;
            final Matcher m ;
            if (type==HijackingType.ATOM_WILDCARD) {
                m =Pattern.compile("\\[?(!#1|\\*)\\]?").matcher(smarts);
            } else {
                m =Pattern.compile("\\[([^\\]]+)\\]|([A-Za-z])").matcher(smarts);
            }
            boolean result = m.find();
            if (result) {
                StringBuffer sb = new StringBuffer();
                do {
                    {
                        if (m.start()-1>=0 && smarts.charAt(m.start()-1)=='[' && m.end()+1 < smarts.length() && smarts.charAt(m.end()+1)==']') {
                            offs.add(m.start()-1); lngs.add(m.group(0).length()+1);

                        } else {
                            offs.add(m.start()); lngs.add(m.group(0).length());
                        }
                    }
                    final String s = m.group(1)!=null ? m.group(1) : m.group(2);
                    m.appendReplacement(sb, "[" + s + ";!#" + k + "]");
                    tps.add(HIJACK_ATOM_WILDCARDS);
                    ++k;
                    result = m.find();
                } while (result);
                m.appendTail(sb);
                this.types = tps.toArray();
                this.strOffsets = offs.toArray();
                this.strLenghts = lngs.toArray();
                return sb.toString();
            }
            this.types = new int[0];
            this.strOffsets = offs.toArray();
            this.strLenghts = lngs.toArray();
            return smarts;
        }

        @Override
        public String toString() {
            return originalSmarts + "   ->   " + hijackedSmarts + "   (" + types.length + " occurences)";
        }
    }



}
