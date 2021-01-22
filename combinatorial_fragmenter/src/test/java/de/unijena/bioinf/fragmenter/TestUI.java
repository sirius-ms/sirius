package de.unijena.bioinf.fragmenter;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;
import org.openscience.cdk.depict.DepictionGenerator;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.silent.MolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class TestUI {


    public static void main(String[] args) {
        try {
            final IAtomContainer M = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(
                   // "CC(CCC(O)=O)C1CCC2C3C(CC(=O)C12C)C1(C)CCC(CC1CC3=O)=NNC1=C(C=C(C=C1)[N+]([O-])=O)[N+]([O-])=O"
                    "CC1=CC=C(C=C1)C(=O)NC2=CC=C(C=C2)C"
            );

            {
                // make atoms and bonds referencable
                for (int i=0; i < M.getAtomCount(); i++) {
                    IAtom a = M.getAtom(i);
                    a.setProperty("REFID", i);
                }
                // make atoms and bonds referencable
                for (int i=0; i < M.getBondCount(); i++) {
                    IBond a = M.getBond(i);
                    a.setProperty("REFID", i);
                }
            }

            final HashMap<BitSet, CombinatorialFragment> fs = new HashMap<>();
            final MolecularGraph G = new MolecularGraph(M);
            final List<Cut> cuts = new ArrayList<>();
            final CombinatorialFragmenter.Callback callback = new CombinatorialFragmenter.Callback() {
                @Override
                public void cut(CombinatorialFragment parent, IBond[] bonds, CombinatorialFragment[] fragments) {
                    if (!(fs.containsKey(fragments[0].bitset) && fs.containsKey(fragments[1].bitset))) {
                        fs.put(fragments[0].bitset, fragments[0]);
                        fs.put(fragments[1].bitset, fragments[1]);
                        cuts.add(new Cut(parent.toMolecule(), fragments[0].getAtoms(), fragments[1].getAtoms(), bonds));

                    }
                }
            };
            for (CombinatorialFragment f : new CombinatorialFragmenter(G).cutAllBonds(G.asFragment(), callback)) {
/*
                for (CombinatorialFragment g : new CombinatorialFragmenter(G).cutAllBonds(f, callback)) {

                }
*/
            }

            System.out.println(cuts.size());
            System.out.println(G.bonds.length);
            final JFrame frame = new JFrame("Fragments");


            JList<Cut> list = new JList<>(cuts.toArray(Cut[]::new));
            final DepictionGenerator gen = new DepictionGenerator();
            list.setCellRenderer(new ListCellRenderer<Cut>() {
                @Override
                public Component getListCellRendererComponent(JList<? extends Cut> list, Cut value, int index, boolean isSelected, boolean cellHasFocus) {
                    try {
                        final Set<IChemObject> a = new HashSet<>();
                        a.addAll(Arrays.asList(value.a));
                        final Set<IChemObject> b = new HashSet<>();
                        b.addAll(Arrays.asList(value.b));
                        for (IBond bond : value.molecule.bonds()) {
                            if (a.contains(bond.getAtom(0)) && a.contains(bond.getAtom(1)))
                                a.add(bond);
                            if (b.contains(bond.getAtom(0)) && b.contains(bond.getAtom(1)))
                                b.add(bond);
                        }
                        return new Image(gen.withSize(400,400).withFillToFit().withTerminalCarbons().withHighlight(a, Color.ORANGE).withHighlight(b, Color.CYAN).withHighlight(Arrays.asList(value.bonds), Color.RED).depict(value.molecule).toImg());
                    } catch (CDKException e) {
                        e.printStackTrace();
                        return new JLabel("");
                    }
                }
            });

            final JScrollPane pane = new JScrollPane(list);
            frame.getContentPane().add(pane);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        } catch (InvalidSmilesException e) {
            e.printStackTrace();
        }

    }

    protected static class Cut {
        private IAtomContainer molecule;
        private IBond[]  bonds;
        private IAtom[] a, b;

        public Cut(IAtomContainer molecule, IAtom[] a, IAtom[] b, IBond[] bonds) {
            this.bonds = bonds;
            this.a = a; this.b = b;
            this.molecule = molecule;
            if (a[0].getIndex() >= molecule.getAtomCount() || molecule.getAtom(a[0].getIndex())!=a[0]) {
                // re-reference molecule
                int mx = 0;
                for (IAtom x : molecule.atoms()) mx = Math.max(mx, (Integer)x.getProperty("REFID"));
                final IAtom[] refatoms = new IAtom[mx+1];
                for (int i=0; i < molecule.getAtomCount(); ++i) {
                    refatoms[((Integer)molecule.getAtom(i).getProperty("REFID")).intValue()] = molecule.getAtom(i);
                }
                mx = 0;
                for (IBond x : molecule.bonds()) mx = Math.max(mx, (Integer)x.getProperty("REFID"));
                final IBond[] refbonds = new IBond[mx+1];
                for (int i=0; i < molecule.getBondCount(); ++i) {
                    refbonds[((Integer)molecule.getBond(i).getProperty("REFID")).intValue()] = molecule.getBond(i);
                }
                for (int i=0; i < a.length; ++i) {
                    this.a[i] = refatoms[(Integer)this.a[i].getProperty("REFID")];
                }
                for (int i=0; i < b.length; ++i) {
                    this.b[i] = refatoms[(Integer)this.b[i].getProperty("REFID")];
                }
                for (int i=0; i < bonds.length; ++i) {
                    this.bonds[i] = refbonds[(Integer)this.bonds[i].getProperty("REFID")];
                }
            }
        }
    }

    protected static class Image extends JPanel {
        protected BufferedImage image;

        public Image(BufferedImage image) {
            this.image = image;
            this.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            g.drawImage(image, 0, 0, null);
        }
    }

}
