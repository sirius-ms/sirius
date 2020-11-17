package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.babelms.MsIO;
import de.unijena.bioinf.babelms.descriptor.Descriptor;
import de.unijena.bioinf.babelms.descriptor.DescriptorRegistry;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.ms.annotations.TreeAnnotation;
import de.unijena.bioinf.sirius.Sirius;
import org.openscience.cdk.depict.DepictionGenerator;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class TestUi3 {

    public static void main(String[] args) {
        final Viewer v = new Viewer();
        v.setVisible(true);
    }

    public static Instance createFragmentList(File file) {
        try {

            final Ms2Experiment experiment = MsIO.readExperimentFromFile(file).next();
            final FTree tree = new Sirius().compute(experiment, experiment.getMolecularFormula()).getTree();
            final IAtomContainer M = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(experiment.getAnnotation(Smiles.class).get().smiles);
            final HashMap<MolecularFormula, List<Fragment>> formulas = new HashMap<>();
            for (Fragment f : tree.getFragmentsWithoutRoot()) formulas.computeIfAbsent(f.getFormula().withoutHydrogen(), (x)->new ArrayList<>()).add(f);
            final MolecularGraph graph = new MolecularGraph(M);
            final DirectedBondTypeScoring scoring = new DirectedBondTypeScoring();
            final PriorizedFragmenter fragmenter = new PriorizedFragmenter(graph, scoring.getScoringFor(graph,tree));

            final HashMap<Fragment, CombinatorialNode> bestMatch = new HashMap<>();
            final HashMap<Fragment, CombinatorialNode> secondBestMatch = new HashMap<>();


            final ArrayList<Entry> entries = new ArrayList<>();
            final LinkedHashMap<MolecularFormula, CombinatorialNode> matched = new LinkedHashMap<>();
            long time = System.currentTimeMillis();
            while (fragmenter.nextFragment()!=null) {
                final int remaining = tree.numberOfVertices()-bestMatch.size()-1;
                if (remaining==0) break;
                CombinatorialNode f = fragmenter.currentFragment;

                final boolean match = formulas.containsKey(f.fragment.getFormula());
                if (((match || f.totalScore>=-10)) && (f.getBondbreaks()<10))
                    fragmenter.acceptFragmentForFragmentation();
                if (match) {
                    if (insertBestMatching(bestMatch,formulas,f, secondBestMatch)) {
                        System.out.println(f.getFragment().toSMILES() + "\t" + f.totalScore + "\t" + f.getBondbreaks() + "\t" + f.fragment.getFormula() + "\t" + match + "\t" + remaining);
                        fragmenter.acceptFragmentForFragmentation();
                        time = System.currentTimeMillis();
                    }
                } else {
                    long lastTime = System.currentTimeMillis();
                    lastTime -= time;
                    if (lastTime >= 120000) {
                        System.out.println("Timeout");
                        break;
                    }
                }
            }


            for (Map.Entry<Fragment, CombinatorialNode> n : bestMatch.entrySet()) {
                final Entry e = new Entry(graph, n.getValue(), n.getKey());
                if (secondBestMatch.containsKey(n.getKey())) {
                    e.nextBest = new Entry(graph, secondBestMatch.get(n.getKey()), n.getKey());
                }
                entries.add(e);

            }
            return new Instance(graph,entries, tree.numberOfVertices()-1);


        } catch (InvalidSmilesException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean insertBestMatching(HashMap<Fragment, CombinatorialNode> mapping, HashMap<MolecularFormula, List<Fragment>> formulas, CombinatorialNode node, HashMap<Fragment, CombinatorialNode> secondBest) {
        final Iterator<Fragment> iterator = formulas.get(node.fragment.getFormula()).stream().sorted(Comparator.comparingInt(
                x -> node.fragment.hydrogenRearrangements(x.getFormula())
        )).iterator();
        while (iterator.hasNext()) {
            Fragment possibleMatch = iterator.next();
            //double rearrangementPenalty = Math.pow(10,Math.abs(f.fragment.hydrogenRearrangements(possibleMatch.getFormula()))-3)/3d;
            if (mapping.get(possibleMatch)==null) {
                mapping.put(possibleMatch,node);
                return true;
            }else if (mapping.get(possibleMatch).totalScore < node.totalScore || (mapping.get(possibleMatch).totalScore == node.totalScore && Math.abs(mapping.get(possibleMatch).fragment.hydrogenRearrangements(possibleMatch.getFormula())) > Math.abs(node.fragment.hydrogenRearrangements(possibleMatch.getFormula())))) {
                CombinatorialNode previousMatch = mapping.get(possibleMatch);
                mapping.put(possibleMatch, node);
                insertBestMatching(mapping,formulas,previousMatch,secondBest);
                return true;
            } else {
                // try another one
            }
        }
        if (secondBest!=null) {
            // ensure that second best does not contain same stuff
            Set<CombinatorialNode> xs = new HashSet<>();
            for (CombinatorialNode n : mapping.values()) xs.add(n);
            for (Fragment key : secondBest.keySet().toArray(Fragment[]::new)){
                if (xs.contains(secondBest.get(key))) secondBest.remove(key);
            }
            insertBestMatching(secondBest,formulas,node,null);
        }
        return false;
    }

    protected static class Entry {
        private IBond[] bondsToCut;
        private IAtom[] fragment;
        private IAtom[] hydrogenCarriers;
        private double score, totalScore;
        private int h;
        protected String formula;
        protected Fragment bestMatchingFragment;
        protected Entry nextBest;
        public Entry(MolecularGraph graph, CombinatorialNode node, Fragment treeFragment) {
            final List<CombinatorialEdge> optimalPathToRoot = node.getOptimalPathToRoot();
            final List<IBond> bonds = new ArrayList<>(optimalPathToRoot.size());
            List<IAtom> hydrogenCarriers = new ArrayList<>();
            for (CombinatorialEdge e : optimalPathToRoot) {
                bonds.add(e.cut1);
                if (e.cut2!=null) bonds.add(e.cut2);
                hydrogenCarriers.addAll(Arrays.asList(e.getAtomsOfFragment()));
            }
            this.hydrogenCarriers = hydrogenCarriers.toArray(IAtom[]::new);
            this.bondsToCut = bonds.toArray(IBond[]::new);
            this.fragment = node.getFragment().getAtoms();
            this.score = node.score;
            this.totalScore = node.totalScore;
            this.bestMatchingFragment = treeFragment;
            this.h = bestMatchingFragment==null ? 0 : node.getFragment().hydrogenRearrangements(bestMatchingFragment.getFormula());
            this.formula = bestMatchingFragment == null ? node.getFragment().getFormula().toString() : bestMatchingFragment.getFormula().toString();//node.getFragment().formula.toString();
        }
    }


    protected static Set<IChemObject> withBonds(IAtom[] atoms, IAtomContainer m, IBond[] without) {
        final Set<IChemObject> iChemObjects = withBonds(atoms, m);
        for (IBond b : without) iChemObjects.remove(b);
        return iChemObjects;
    }
    protected static Set<IChemObject> withBonds(IAtom[] atoms, IAtomContainer m) {
        Set<IChemObject> xs = new HashSet<>();
        for (IAtom a : atoms) xs.add(a);
        for (IBond b : m.bonds()) {
            if (xs.contains(b.getAtom(0)) && xs.contains(b.getAtom(1))) {
                xs.add(b);
            }
        }
        return xs;
    }

    protected static class ListPanel extends JPanel {
        JList<Entry> nodes;
        boolean suboptimal = false;
        public ListPanel(Instance instance) throws HeadlessException {
            this.nodes = new JList<>(new Vector<>(instance.entries));
            DepictionGenerator generator = new DepictionGenerator();
            JPanel empty = new JPanel();
            nodes.setCellRenderer(new ListCellRenderer<Entry>() {
                @Override
                public Component getListCellRendererComponent(JList<? extends Entry> list, Entry value, int index, boolean isSelected, boolean cellHasFocus) {
                    if (value==null) return empty;
                    String scorediff = (value.nextBest==null) ? "Distance between optimal and suboptimal is unknown." : String.format(Locale.US,"Distance between optimal and suboptimal is %.4f", value.totalScore - value.nextBest.totalScore);
                    final JPanel pan = new JPanel();
                    pan.setLayout(new BorderLayout());
                    if (suboptimal && value.nextBest!=null) {
                        pan.add(new CombNodeView(generator,instance,value), BorderLayout.WEST);

                        pan.add(new CombNodeView(generator,instance,value.nextBest), BorderLayout.EAST);pan.add(new JLabel(String.format(Locale.US, "%s %.3f, %d h-rearrang. %s",value.formula, value.totalScore,value.h, scorediff)), BorderLayout.SOUTH);
                    } else {
                        pan.add(new CombNodeView(generator,instance,value), BorderLayout.CENTER);
                        pan.add(new JLabel(String.format(Locale.US, "%s %.3f, %d h-rearrang. %s",value.formula, value.totalScore,value.h, scorediff)), BorderLayout.SOUTH);
                    }
                    return pan;
                }
            });
            setLayout(new BorderLayout());
            double sumScore = 0d;
            for (Entry e : instance.entries) sumScore += e.totalScore;
            Box bx = Box.createHorizontalBox();
            add(bx, BorderLayout.NORTH);
            bx.add(new JLabel(String.format(Locale.US, "%d / %d matches fragments. Score: %.4f", instance.entries.size(), instance.numberOfVertices, sumScore)));
            bx.add(Box.createHorizontalStrut(64),BorderLayout.NORTH);
            final JCheckBox show_suboptimal_solution = new JCheckBox("Show suboptimal solution");
            bx.add(show_suboptimal_solution);
            show_suboptimal_solution.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    if (show_suboptimal_solution.isSelected()) {
                        suboptimal=true;
                        repaint();
                        nodes.invalidate();
                    } else {
                        suboptimal=false;
                        repaint();
                        nodes.invalidate();
                    }
                }
            });
            add(new JScrollPane(nodes,  ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
            setVisible(true);
        };
    }

    protected static class Instance {
        private List<Entry> entries;
        private MolecularGraph graph;
        private int numberOfVertices;

        public Instance(MolecularGraph graph, List<Entry> entries, int numberOfVertices) {
            this.entries = entries;
            this.graph = graph;
            this.numberOfVertices = numberOfVertices;
        }
    }

    protected static class Viewer extends JFrame {

        public Viewer() throws HeadlessException {
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            getContentPane().setLayout(new BorderLayout());
            setMinimumSize(new Dimension(640,1024));
            pack();
            setDropTarget(new DropTarget() {
                public synchronized void drop(DropTargetDropEvent evt) {
                    try {
                        evt.acceptDrop(DnDConstants.ACTION_COPY);
                        List<File> droppedFiles = (List<File>)
                                evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        System.out.println(droppedFiles);
                        if (!droppedFiles.isEmpty()) {
                            evt.dropComplete(true);
                            new SwingWorker() {
                                Instance instance;
                                @Override
                                protected Object doInBackground() throws Exception {
                                    try {
                                        instance = createFragmentList(droppedFiles.get(0));
                                    } catch (Throwable e) {
                                        e.printStackTrace();
                                    }
                                    System.out.println("Fertisch");
                                    return instance;
                                }

                                @Override
                                protected void done() {
                                    System.out.println("DONE");
                                    setActiveInstance(instance);
                                }
                            }.execute();
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

        }

        public void setActiveInstance(Instance instance) {
            getContentPane().removeAll();
            if (instance!=null) {
                getContentPane().add(new ListPanel(instance),BorderLayout.CENTER);
                System.out.println("Render list");
            }
            validate();
        }
    }

    protected static class CombNodeView extends JPanel {
        private BufferedImage image;
        public CombNodeView(DepictionGenerator generator, Instance instance, Entry value) {
            final Set<IChemObject> frag = withBonds(value.fragment, instance.graph.molecule, value.bondsToCut);
            frag.removeAll(Arrays.asList(value.hydrogenCarriers));
            try {
                this.image = generator.withZoom(2d).withAromaticDisplay().withHighlight(frag, Color.BLUE).withHighlight(Arrays.asList(value.hydrogenCarriers),Color.MAGENTA).withHighlight(Arrays.asList(value.bondsToCut), Color.RED).depict(instance.graph.molecule).toImg();
                this.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
            } catch (CDKException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            g.drawImage(image, 0, 0, null);
        }
    }

    protected static class Image extends JPanel {
        protected BufferedImage image;
        protected String caption;
        protected final static Font font = new Font("Arial", Font.PLAIN, 20);

        public Image(BufferedImage image, String caption) {
            this.image = image;
            this.caption = caption;
            this.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()+20));
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            g.drawImage(image, 0, 0, null);
            g.setFont(font);
            g.drawString(caption, 0, image.getHeight()+20);
        }
    }
}