package de.unijena.bioinf.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.ExtendedConnectivityProperty;
import de.unijena.bioinf.ChemistryBase.fp.SubstructureProperty;
import de.unijena.bioinf.sirius.gui.configs.Fonts;
import de.unijena.bioinf.fingerid.CandidateListDetailView;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.color.CDK2DAtomColors;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class StructurePreview extends JPanel implements Runnable {

    protected static Logger logger = LoggerFactory.getLogger(StructurePreview.class);
    protected final FingerprintVisualization[] visualizations;
    protected final SMARTSQueryTool queryTool;
    protected final Thread backgroundThread;
    protected final AtomContainerRenderer renderer;
    protected volatile MolecularPropertyTableEntry entry;
    protected volatile IAtomContainer[] depiction;
    protected volatile int state = 0; // needRefresh=0, recalculated=1, done=2
    protected volatile boolean shutdown = false;

    public StructurePreview(FingerprintVisualization[] visualizations) {
        setBackground(Color.WHITE);
        this.visualizations = visualizations;
        this.entry = null;
        this.backgroundThread = new Thread(this);
        this.queryTool = new SMARTSQueryTool("C=C", SilentChemObjectBuilder.getInstance());
        queryTool.setQueryCacheSize(visualizations.length);
        backgroundThread.start();
        state = 2;

        java.util.List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();
        generators.add(new BasicSceneGenerator());
        generators.add(new StandardGenerator(Fonts.FONT_BOLD.deriveFont(13f)));
        // setup the renderer
        this.renderer = new AtomContainerRenderer(generators, new AWTFontManager());

        renderer.getRenderer2DModel().set(StandardGenerator.Highlighting.class,
                StandardGenerator.HighlightStyle.OuterGlow);
        renderer.getRenderer2DModel().set(StandardGenerator.AtomColor.class,
                new CDK2DAtomColors());

        setPreferredSize(new Dimension(0, 220));
    }


    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (state < 1) return;
        final Graphics2D g2d = (Graphics2D) g;
        final IAtomContainer[] mols = depiction;
        if (mols == null || mols.length==0) {
            g2d.clearRect(0,0,getWidth(),getHeight());
            return;
        }
        final int widthPerCompound = getWidth() / 6;
        final int offset = (getWidth() - (mols.length*widthPerCompound))/2;
        final int length = depiction.length;
        for (int i = 0; i < length; ++i) {
            final int x=offset+widthPerCompound*i, y=5, w=widthPerCompound, h=200;
            if (depiction[i] == null) {
                g2d.clearRect(x,y,w,h);
            }else {
                renderer.paint(depiction[i], new AWTDrawVisitor(g2d),
                        new Rectangle2D.Double(x,y,w,h), true);
            }
        }


    }

    public void setMolecularProperty(MolecularPropertyTableEntry entry) {
        if (entry == this.entry) return;
        synchronized (this) {
            this.entry = entry;
            state = 0;
            this.notify();
        }
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                if (state == 0 && entry != null) {
                    final FingerprintVisualization viz;
                    final MolecularPropertyTableEntry entry;
                    synchronized (this) {
                        entry = this.entry;
                        viz = this.visualizations[entry.absoluteIndex];
                    }
                    IAtomContainer[] depiction;
                    if (viz==null) {
                        depiction = new IAtomContainer[0];
                    } else {
                        final SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
                        parser.kekulise(true);
                        StructureDiagramGenerator sdg = new StructureDiagramGenerator();

                        if (entry.getMolecularProperty() instanceof SubstructureProperty) {
                            depiction = new IAtomContainer[viz.getNumberOfExamples()];
                            for (int i = 0; i < viz.getNumberOfExamples(); ++i) {
                                try {
                                    depiction[i] = parser.parseSmiles(viz.getExample(i));
                                    sdg.setMolecule(depiction[i]);
                                    sdg.generateCoordinates();
                                    depiction[i] = sdg.getMolecule();
                                    highlightWithFp(depiction[i], (SubstructureProperty) entry.getMolecularProperty());
                                } catch (CDKException | NullPointerException e) {
                                    logger.error(e.getMessage(), e);
                                    depiction[i] = null; // do not draw the molecule
                                }
                            }
                        } else if (entry.getMolecularProperty() instanceof ExtendedConnectivityProperty) {
                            depiction = new IAtomContainer[1];
                            try {
                                depiction[0] = parser.parseSmiles(viz.getExample(1));
                                sdg.setMolecule(depiction[0]);
                                sdg.generateCoordinates();
                                depiction[0] = sdg.getMolecule();
                                highlight(depiction[0], viz.getExample(0));
                            } catch (CDKException | NullPointerException e) {
                                logger.error(e.getMessage(), e);
                                depiction[0] = null; // do not draw the molecule
                            }
                        } else {
                            depiction = new IAtomContainer[0];
                        }
                        // remove null elements from depiction array
                        int nonNull=0;
                        for (int i=0; i < depiction.length; ++i)
                            if (depiction[i]!=null)
                                ++nonNull;
                        if (nonNull != depiction.length) {
                            final IAtomContainer[] compact = new IAtomContainer[nonNull];
                            int k=0;
                            for (int i=0; i < depiction.length; ++i)
                                if (depiction[i]!=null)
                                    compact[k++] = depiction[i];
                            depiction = compact;
                        }
                    }
                    synchronized (this) {
                        this.depiction = depiction;
                        state = 1;
                    }
                    repaint();
                } else {
                    synchronized (this) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(),e);
            }
        }
    }

    private void highlightWithFp(IAtomContainer molecule, SubstructureProperty molecularProperty) throws CDKException {
        queryTool.setSmarts(molecularProperty.getSmarts());
        if (queryTool.matches(molecule)) {
            final List<Integer> inds = queryTool.getMatchingAtoms().get(0);
            if (inds.isEmpty()) {
                hightlightAll(molecule);
                return;
            }
            final HashSet<IAtom> atoms = new HashSet<>(inds.size());
            for (int i : inds) {
                atoms.add(molecule.getAtom(i));
            }
            highlightAtomsAndBonds(molecule, atoms);
        } else {
            hightlightAll(molecule);
        }
    }

    private void highlight(IAtomContainer molecule, String highlightingExample) throws CDKException {
        queryTool.setSmarts(highlightingExample);
        if (queryTool.matches(molecule)) {
            final List<Integer> inds = queryTool.getMatchingAtoms().get(0);
            final HashSet<IAtom> atoms = new HashSet<>(inds.size());
            for (int i : inds) {
                atoms.add(molecule.getAtom(i));
            }
            highlightAtomsAndBonds(molecule, atoms);
        }
    }

    private void highlightAtomsAndBonds(IAtomContainer molecule, HashSet<IAtom> atoms) {
        for (IAtom atom : atoms) {
            atom.setProperty(StandardGenerator.HIGHLIGHT_COLOR, CandidateListDetailView.PRIMARY_HIGHLIGHTED_COLOR);
            for (IBond b : molecule.getConnectedBondsList(atom)) {
                if (atoms.contains(b.getAtom(0)) && atoms.contains(b.getAtom(1))) {
                    b.setProperty(StandardGenerator.HIGHLIGHT_COLOR, CandidateListDetailView.PRIMARY_HIGHLIGHTED_COLOR);
                }
            }
        }
    }

    private void hightlightAll(IAtomContainer molecule) {
        for (IAtom atom : molecule.atoms()) {
            atom.setProperty(StandardGenerator.HIGHLIGHT_COLOR, CandidateListDetailView.PRIMARY_HIGHLIGHTED_COLOR);
        }
        for (IBond bond : molecule.bonds()) {
            bond.setProperty(StandardGenerator.HIGHLIGHT_COLOR, CandidateListDetailView.PRIMARY_HIGHLIGHTED_COLOR);
        }
    }
}
