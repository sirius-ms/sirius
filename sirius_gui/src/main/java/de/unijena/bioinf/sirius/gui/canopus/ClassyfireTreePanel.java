package de.unijena.bioinf.sirius.gui.canopus;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.canopus.Canopus;
import de.unijena.bioinf.myxo.gui.tree.render.NodeColor;
import de.unijena.bioinf.myxo.gui.tree.render.NodeType;
import de.unijena.bioinf.myxo.gui.tree.render.TreeRenderPanel;
import de.unijena.bioinf.myxo.gui.tree.structure.DefaultTreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.DefaultTreeNode;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.sirius.gui.fingerid.FingerIdData;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClassyfireTreePanel extends TreeRenderPanel {

    ClassyFireFingerprintVersion version;
    MaskedFingerprintVersion mask;
    Canopus canopus;

    public ClassyfireTreePanel() {
        super();
        this.canopus = MainFrame.MF.getCsiFingerId().getCanopus();
        this.version = canopus.getClassyFireFingerprintVersion();
        this.mask = canopus.getCanopusMask();
    }

    protected static ClassyfireNode buildTreeFromClassifications(final MaskedFingerprintVersion masked, ProbabilityFingerprint fingerprint, double probabilityThreshold) {
        final ClassyFireFingerprintVersion version = (ClassyFireFingerprintVersion) masked.getMaskedFingerprintVersion();
        // first: find all nodes with probability above the threshold
        TIntObjectHashMap<ClassyfireNode> nodes = new TIntObjectHashMap<>();
        for (FPIter i : fingerprint) {
            if (i.getProbability() >= probabilityThreshold) {
                System.out.println(((ClassyfireProperty)i.getMolecularProperty()).getName() + ": " + i.getProbability());
                // add fingerprint and all of its parents
                ClassyfireProperty prop = (ClassyfireProperty) i.getMolecularProperty();
                do {
                    if (nodes.get(prop.getChemOntId())==null) {
                        final int index = version.getIndexOfMolecularProperty(prop);
                        final double probability = masked.hasProperty(index) ? fingerprint.getProbability(index) : 1d;
                        final ClassyfireNode node = new ClassyfireNode(prop, probability);
                        nodes.put(prop.getChemOntId(), node);
                    }
                    prop = prop.getParent();
                } while (prop != null);
            }
        }
        // connect nodes
        for (ClassyfireNode node : nodes.valueCollection()) {
            if (node.underlyingProperty.getParent()!=null) {
               final ClassyfireNode parent = nodes.get(node.underlyingProperty.getParent().getChemOntId());
               final DefaultTreeEdge edge = new DefaultTreeEdge();
               edge.setSource(parent);
               edge.setTarget(node);
               node.setInEdge(edge);
               parent.addOutEdge(edge);
            }
        }
        // find root
        for (ClassyfireNode node : nodes.valueCollection()) {
            if (node.getInEdge()==null) return node;
        }
        return null;
    }

    public void updateTree(ExperimentContainer experiment, SiriusResultElement sre) {
        if (sre==null || sre.getFingerIdData()==null || sre.getFingerIdData().getPlatts()==null) {
            showTree(null);
        } else {
            final ProbabilityFingerprint canopus = sre.getFingerIdData().getCanopusFingerprint();
            if (canopus==null) {
                computeCanopus(sre);
            } else {
                showTree(canopus);
            }
        }
    }

    private void computeCanopus(final SiriusResultElement sre) {
        final FingerIdData data = sre.getFingerIdData();
        final ProbabilityFingerprint platts = data.getPlatts();
        final MolecularFormula formula = sre.getMolecularFormula();
        final SwingWorker<ProbabilityFingerprint,ProbabilityFingerprint> worker = new SwingWorker<ProbabilityFingerprint, ProbabilityFingerprint>() {
            @Override
            protected ProbabilityFingerprint doInBackground() throws Exception {
                final ProbabilityFingerprint fp = canopus.predictClassificationFingerprint(formula, platts );
                publish(fp);
                return fp;
            }

            @Override
            protected void process(List<ProbabilityFingerprint> chunks) {
                super.process(chunks);
                data.setCanopusFingerprint(chunks.get(0));
                showTree(chunks.get(0));
            }
        };
        worker.execute();
    }

    private void showTree(ProbabilityFingerprint canopusFingerprint) {
        if (canopusFingerprint==null) {
            super.showTree(null, NodeType.preview, NodeColor.rgScore);
        } else {
            final ClassyfireNode n = buildTreeFromClassifications(mask, canopusFingerprint, 0.33d);
            showTree(n, NodeType.preview, NodeColor.rgScore);
        }
    }

    protected static class ClassyfireNode extends DefaultTreeNode {
        protected ClassyfireProperty underlyingProperty;
        protected double probability;

        public ClassyfireNode(ClassyfireProperty underlyingProperty, double probability) {
            this.underlyingProperty = underlyingProperty;
            this.probability = probability;
            this.setMolecularFormula(underlyingProperty.getName());
            setScore(Math.log(probability));
        }
    }

    protected void readFonts() {
        try {
            InputStream fontFile = getClass().getResourceAsStream("/ttf/DejaVuSans-Bold.ttf");
            Font tempFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            propertyFont = tempFont.deriveFont(12f);
            formulaFont = tempFont.deriveFont(14f);
            lossFont = tempFont.deriveFont(10f);

            smallFormulaFont = tempFont.deriveFont(9f);

            fontFile = getClass().getResourceAsStream("/ttf/DejaVuSans.ttf");
            tempFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            valueFont = tempFont.deriveFont(12f);

            smallValueFont = tempFont.deriveFont(8f);
        } catch (Exception e) {
            LoggerFactory.getLogger(TreeRenderPanel.class).error(e.getMessage(),e);
        }
    }

    @Override
    protected void buildNodeImages(TreeNode node) {
        ClassyfireNode cn = (ClassyfireNode)node;
        String mf = cn.underlyingProperty.getName();
        int formulaLength = formulaFM.stringWidth(mf);

        int horSize = formulaLength + 10;
        int hh = formulaFont.getSize()+2;
        BufferedImage image = new BufferedImage(horSize, hh, BufferedImage.TYPE_INT_RGB);

        if (previewNodesWidth < horSize) previewNodesWidth = horSize;

        Graphics2D g2 = (Graphics2D) image.getGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(this.backColor);
        g2.fillRect(0, 0, horSize, hh);

        g2.setColor(nodeColorManager.getColor(node));
        g2.fillRoundRect(0, 0, horSize, hh, 15, 15);

        g2.setColor(Color.black);
        g2.drawRoundRect(0, 0, horSize - 1, hh-1, 15, 15);

        g2.setFont(formulaFont);
        g2.drawString(mf, (horSize - formulaLength) / 2, 12);
        previewNodes.put(node, image);

    }


    protected void paintToolTip(Graphics2D g2, TreeNode node) {
        ClassyfireNode cnode = ((ClassyfireNode)node);
        PositionContainer cont = positonsMap.get(node);

        String description = cnode.underlyingProperty.getDescription();

        // split description into parts
        List<String> lines = new ArrayList<>();
        int ccount = 0;
        StringBuilder buf = new StringBuilder();
        for (String word : description.split("\\s+")) {
            buf.append(word).append(" ");
            ccount += (1+word.length());
            if (ccount > 92) {
                lines.add(buf.toString());
                buf.delete(0,buf.length());
                ccount = 0;
            }
        }
        if (buf.length()>0)
            lines.add(buf.toString());

        int totalHeight = 0;
        int width = 0;

        String name = cnode.underlyingProperty.getName();
        String prob = String.format(Locale.US, "%.1f %%  ",cnode.probability*100d);

        width = formulaFM.stringWidth(name + "  " + prob);
        for (String line : lines) {
            width = Math.max(width, propertyFM.stringWidth(line));
        }
        totalHeight = (1+lines.size())*(propertyFM.getHeight()+1) + formulaFM.getHeight() + 4;
        System.out.println(width +  " / " + totalHeight);


        Composite org = g2.getComposite();

        JViewport viewport = scrollPane.getViewport();
        Point point = viewport.getViewPosition();


        int westVPBorder = (int) point.getX();
        int northVPBorder = (int) point.getY();
        int eastVPBorder = westVPBorder + viewport.getWidth();
        int southVPBorder = northVPBorder + viewport.getHeight();

        int startX;
        int startY;

        if (cont.getWestX() < westVPBorder) startX = westVPBorder;
        else if (cont.getWestX() + width > eastVPBorder) startX = eastVPBorder - width;
        else startX = cont.getWestX();

        if (cont.getNorthY() < northVPBorder) startY = northVPBorder;
        else if (cont.getNorthY() + totalHeight > southVPBorder) startY = southVPBorder - totalHeight;
        else startY = cont.getNorthY();

        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f);
        g2.setComposite(ac);
        g2.setColor(new Color(222, 225, 248));
        g2.fillRoundRect(startX, startY, width, totalHeight, 7, 7);

        g2.setColor(Color.black);
        g2.drawRoundRect(startX, startY, width - 1, totalHeight - 1, 7, 7);
        g2.setComposite(org);

        g2.setFont(formulaFont);
        g2.drawString(name, startX, startY + formulaFM.getHeight());
        g2.drawString(prob, startX + width - formulaFM.stringWidth(prob), startY + formulaFM.getHeight());

        g2.setFont(propertyFont);
        int lh = formulaFM.getHeight()+4;
        for (String line : lines) {
            lh += propertyFM.getHeight()+1;
            g2.drawString(line, startX, startY + lh);
        }

    }

}
