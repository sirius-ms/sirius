package de.unijena.bioinf.ms.gui.ms_viewer;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.utils.CompactComboBox;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.fingerid.FBCandidatesTopK;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.depict.DepictionGenerator;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InSilicoSelectionBox extends CompactComboBox<InSilicoSelectionBox.Item> implements ListCellRenderer<InSilicoSelectionBox.Item> {

    private Dimension sizePerCompound;

    private CompoundCanvas canvas;

    private final Item NO_ANO = new Item("No structure annotation");

    private boolean enabled;

    public InSilicoSelectionBox(Dimension sizePerCompound, int nrows) {
        super( new Dimension((int)sizePerCompound.getWidth(), (int)(sizePerCompound.getHeight()*nrows)));
        this.canvas = new CompoundCanvas(sizePerCompound);
        this.sizePerCompound = sizePerCompound;
        setDetailedRenderer(this);
        setLabeling(Item::getDescription);
        addItem(NO_ANO);
        setPrototypeDisplayValue(NO_ANO);
        disableIfEmpty();
    }

    public void activate() {
        this.enabled = true;
        disableIfEmpty();
    }
    public void deactivate() {
        this.enabled = false;
        disableIfEmpty();
    }


    public void disableIfEmpty() {
        setEnabled(enabled && getModel().getSize()>1);
    }

    public void setCandidateSet(List<Scored<CompoundCandidate>> results) {
        removeAllItems();
        addItem(NO_ANO);
        for (Scored<CompoundCandidate> c : results) {
            addItem(new Item(c.getCandidate()));
        }
        disableIfEmpty();
    }

    public void resultsChanged(@Nullable FormulaResultBean sre) {
        if (sre==null) {
            setCandidateSet(new ArrayList<>());
        } else {
            final Optional<FBCandidatesTopK> candidates = sre.getFingerIDCandidates();
            if (candidates.isPresent()) {
                final List<Scored<CompoundCandidate>> results = candidates.get().getResults();
                setCandidateSet(results);
            }
        }
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Item> list, Item value, int index, boolean isSelected, boolean cellHasFocus) {
        {
            Border border = null;
            if (isSelected) {
                border = new LineBorder(Colors.ICON_BLUE, 3);
            } else {
                border = new EmptyBorder(3,3,3,3);
            }
            canvas.setBorder(border);
        }
        canvas.setCompound(value.getMolecule());
        return canvas;
    }

    private static class CompoundCanvas extends JPanel {
        private IAtomContainer compound;
        private DepictionGenerator generator;
        private BufferedImage img;
        private Dimension size;
        private CompoundCanvas(Dimension size) {
            this.setPreferredSize(size);
            this.setSize(size);
            this.size = size;
            this.generator = new DepictionGenerator().withAromaticDisplay().withAtomColors().withBackgroundColor(new Color(0,0,0,0)).withSize(size.getWidth(),size.getHeight()).withFillToFit();
        }

        void setCompound(IAtomContainer m) {
            if (m!=compound) {
                compound = m;
                if (m==null) {
                    img=null;
                } else {
                    try {
                        img = generator.depict(compound).toImg();
                    } catch (CDKException e) {
                        e.printStackTrace();
                        img=null;
                    }
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img==null) {
                final String NONE = "No structure annotation";
                final Rectangle2D stringBounds = g.getFontMetrics(getFont()).getStringBounds(NONE, g);
                int x = (int)(size.getWidth() - stringBounds.getWidth())/2;
                int y = (int)(size.getHeight() - stringBounds.getHeight())/2;
                g.drawString(NONE,x,y);
            } else {
                g.drawImage(img,0,0,null);
            }

        }
    }

    public static class Item {
        private String description;
        private IAtomContainer molecule;
        private CompoundCandidate candidate;

        public Item(String description) {
            this.description = description;
            this.molecule = null;
            this.candidate = null;
        }
        public Item(CompoundCandidate candidate) {
            this.description = candidate.getName()!=null && /*WTF*/ !candidate.getName().equals("null") ? candidate.getName() : candidate.getSmiles();
            this.candidate = candidate;
            this.molecule = null;
        }

        public CompoundCandidate getCandidate() {
            return candidate;
        }

        public String getDescription() {
            return description;
        }

        public IAtomContainer getMolecule() {
            if (molecule!=null) return molecule;
            if (candidate!=null) {
                try {
                    final IAtomContainer mol = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(candidate.getSmiles());
                    AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
                    Aromaticity.cdkLegacy().apply(mol);
                    this.molecule = mol;
                    return mol;
                } catch (CDKException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}
