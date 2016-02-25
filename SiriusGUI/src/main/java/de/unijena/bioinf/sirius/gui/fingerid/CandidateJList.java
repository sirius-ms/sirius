/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.sirius.gui.configs.ConfigStorage;
import de.unijena.bioinf.sirius.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.sirius.gui.dialogs.FilePresentDialog;
import de.unijena.bioinf.sirius.gui.filefilter.SupportedExportCSVFormatsFilter;
import de.unijena.bioinf.sirius.gui.io.DotIO;
import de.unijena.bioinf.sirius.gui.io.RasterGraphicsIO;
import de.unijena.bioinf.sirius.gui.structure.FileFormat;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.*;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CandidateJList extends JPanel implements MouseListener {

    private final static int CELL_SIZE = 15;
    private static final int MIN_CELL_SIZE = 3;

    protected CSIFingerIdComputation computation;
    private ConfigStorage config;
    protected FingerIdData data;
    protected JList<CompoundCandidate> candidateList;
    protected StructureSearcher structureSearcher;
    protected Thread structureSearcherThread;

    protected Font nameFont, propertyFont, rankFont;
    protected Frame owner;

    protected int highlightMissing=-1, highlightAgree=-1, highlightedCandidate=-1;

    protected void initFonts() {
        try {
            InputStream fontFile = getClass().getResourceAsStream("/ttf/DejaVuSans-Bold.ttf");
            Font tempFont = null;
            tempFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            nameFont = tempFont.deriveFont(13f);
            propertyFont = tempFont.deriveFont(16f);
            rankFont = tempFont.deriveFont(32f);
        } catch (FontFormatException | IOException e) {
            nameFont = propertyFont = rankFont = Font.getFont(Font.SANS_SERIF);
        }
    }

    public CandidateJList(Frame owner, CSIFingerIdComputation computation, ConfigStorage config, FingerIdData data) {
        this.computation = computation;
        this.config = config;
        this.owner = owner;
        initFonts();
        setLayout(new BorderLayout());
        this.data = data;
        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
        add(northPanel, BorderLayout.NORTH);

        final JButton exportToCSV = new JButton("export list", new ImageIcon(CandidateJList.class.getResource("/icons/document-export.png")));
        exportToCSV.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doExport();
            }
        });
        northPanel.add(exportToCSV);

        candidateList = new JList<CompoundCandidate>(new ListModel());
        candidateList.setCellRenderer(new CandidateCellRenderer());
        candidateList.setPrototypeCellValue(new CompoundCandidate(Compound.getPrototypeCompound(), 0d, 1, 0));
        final JScrollPane scrollPane = new JScrollPane(candidateList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        candidateList.addMouseListener(this);
        this.structureSearcher = new StructureSearcher(computation, data==null ? 0 : data.compounds.length);
        this.structureSearcherThread = new Thread(structureSearcher);
        structureSearcherThread.start();
        setVisible(true);
    }

    private void doExport() {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(config.getDefaultTreeExportPath());
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setAcceptAllFileFilterUsed(false);
        FileFilter csvFileFilter = new SupportedExportCSVFormatsFilter();
        jfc.addChoosableFileFilter(csvFileFilter);
        File selectedFile = null;
        while(selectedFile==null){
            int returnval = jfc.showSaveDialog(this);
            if(returnval == JFileChooser.APPROVE_OPTION){
                File selFile = jfc.getSelectedFile();

                config.setDefaultCompoundsExportPath(selFile.getParentFile());

                String name = selFile.getName();
                if(selFile.exists()){
                    FilePresentDialog fpd = new FilePresentDialog(owner, selFile.getName());
                    ReturnValue rv = fpd.getReturnValue();
                    if(rv==ReturnValue.Success){
                        selectedFile = selFile;
                    }
                }else{
                    selectedFile = selFile;
                }
            }else{
                break;
            }
        }

        if(selectedFile!=null){

            try{
                new CSVExporter().exportToFile(selectedFile, data);
            }catch(Exception e2){
                ExceptionDialog fed = new ExceptionDialog(owner, e2.getMessage());
                e2.printStackTrace();
            }
        }
    }

    public void dispose() {
        structureSearcher.stop();
    }

    public void refresh(FingerIdData data) {
        this.data = data;
        ((ListModel)candidateList.getModel()).change();
        this.structureSearcher.reloadList((ListModel) candidateList.getModel(), -1);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        highlightedCandidate=-1;
        highlightAgree=-1;
        highlightMissing=-1;
        final Point point = e.getPoint();
        final int index = candidateList.locationToIndex(point);
        if (index < 0) return;
        final CompoundCandidate candidate = candidateList.getModel().getElementAt(index);
        highlightedCandidate = candidate.rank;
        final Rectangle relativeRect = candidateList.getCellBounds(index, index);
        final boolean in;
        int rx, ry;
        {
            final Rectangle box = candidate.agreement.getBounds();
            final int absX = box.x+relativeRect.x;
            final int absY = box.y+relativeRect.y;
            final int absX2 = box.width+absX;
            final int absY2 = box.height+absY;
            in=point.x >= absX && point.y >= absY && point.x < absX2 && point.y < absY2;
            rx = point.x-absX;
            ry = point.y-absY;
        }
        if (in) {
            final int row = ry/ CELL_SIZE;
            final int col = rx/ CELL_SIZE;
            highlightAgree = candidate.agreement.indexAt(row, col);
            structureSearcher.reloadList((ListModel)candidateList.getModel(), highlightAgree);
        } else {
            final Rectangle box = candidate.missings.getBounds();
            final int absX = box.x+relativeRect.x;
            final int absY = box.y+relativeRect.y;
            final int absX2 = box.width+absX;
            final int absY2 = box.height+absY;
            if (point.x >= absX && point.y >= absY && point.x < absX2 && point.y < absY2) {
                rx = point.x-absX;
                ry = point.y-absY;
                final int row = ry/ CELL_SIZE;
                final int col = rx/ CELL_SIZE;
                highlightMissing = candidate.missings.indexAt(row, col);
                structureSearcher.reloadList((ListModel)candidateList.getModel(), highlightMissing);
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    public class ListModel extends AbstractListModel<CompoundCandidate> {
        ArrayList<CompoundCandidate> candidates;
        public ListModel() {
            this.candidates = new ArrayList<>();
            change();
        }

        @Override
        public int getSize() {
            return candidates.size();
        }

        public void refreshList() {
            fireContentsChanged(this, 0, getSize());
        }

        public void change(int elem) {
            fireContentsChanged(this, elem, elem);
        }

        public void change(int from, int to) {
            fireContentsChanged(this, from, to);
        }

        public void change() {
            if (data!=null) {
                candidates.clear();
                for (int i=0; i < data.compounds.length; ++i) {
                    candidates.add(new CompoundCandidate(data.compounds[i],data.scores[i], i+1, i));
                }
            } else candidates = new ArrayList<>();
            refreshList();
        }

        @Override
        public CompoundCandidate getElementAt(int index) {
            return candidates.get(index);
        }
    }

    public class CandidateCellRenderer extends JPanel implements ListCellRenderer<CompoundCandidate> {

        private CompoundImage image;
        private Color even, odd;
        private DescriptionPanel descriptionPanel;
        private CompoundCandidate currentCandidate;

        public CandidateCellRenderer() {
            setLayout(new BorderLayout());
            image = new CompoundImage();
            descriptionPanel = new DescriptionPanel();
            even = Color.WHITE;
            odd = new Color(213,227,238);
            add(image, BorderLayout.WEST);
            add(descriptionPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends CompoundCandidate> list, CompoundCandidate value, int index, boolean isSelected, boolean cellHasFocus) {
            image.molecule = value;
            image.backgroundColor = (index%2==0 ? even : odd);
            setOpaque(true);
            setBackground(image.backgroundColor);
            descriptionPanel.setCompound(value);
            currentCandidate = value;
            return this;
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            // memoize coordinates of agreement boxes
            final Rectangle ra = descriptionPanel.ag.getBounds();
            final Rectangle rv = descriptionPanel.vio.getBounds();
            // add offset of parents
            ra.setLocation(ra.x + descriptionPanel.getX(), ra.y + descriptionPanel.getY());
            rv.setLocation(rv.x + descriptionPanel.getX(), rv.y + descriptionPanel.getY());
            ra.setLocation(ra.x + descriptionPanel.agpanel.getX(), ra.y + descriptionPanel.agpanel.getY());
            rv.setLocation(rv.x + descriptionPanel.viopanel.getX(), rv.y + descriptionPanel.viopanel.getY());

            currentCandidate.agreement.setBounds(ra.x, ra.y, ra.width, ra.height);
            currentCandidate.missings.setBounds(rv.x, rv.y, rv.width, rv.height);
        }
    }

    private class CompoundImage extends JPanel {

        protected CompoundCandidate molecule;
        protected AtomContainerRenderer renderer;
        protected Color backgroundColor;

        public CompoundImage() {
            setOpaque(false);
            setPreferredSize(new Dimension(374, 215));
            // make generators
            java.util.List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();
            generators.add(new BasicSceneGenerator());
            generators.add(new BasicBondGenerator());
            generators.add(new RingGenerator());
            generators.add(new BasicAtomGenerator());
            generators.add(new HighlightGenerator());

            // setup the renderer
            this.renderer = new AtomContainerRenderer(generators, new AWTFontManager());
            setVisible(true);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            final Graphics2D gg = (Graphics2D)g;
            StructureDiagramGenerator sdg = new StructureDiagramGenerator();
            sdg.setMolecule(molecule.compound.getMolecule(), false);
            try {
                sdg.generateCoordinates();
            } catch (CDKException e) {
                e.printStackTrace();
            }
            renderer.getRenderer2DModel().set(BasicSceneGenerator.BackgroundColor.class, backgroundColor);
            synchronized (molecule.compound) {
                renderer.paint(molecule.compound.getMolecule(), new AWTDrawVisitor(gg),
                        new Rectangle2D.Double(7, 14, 360, 200), true);
            }
            if (molecule.compound.name!=null) {
                gg.setFont(nameFont);
                gg.drawString(molecule.compound.name, 3, 16);
            }
            gg.setFont(rankFont);
            gg.drawString(String.valueOf(molecule.rank), 3, 100);
        }
    }

    public class FingerprintView extends JPanel {

        FingerprintAgreement agreement;
        Color color;

        public FingerprintView(Color color) {
            this.color = color;
            setOpaque(false);
            setPreferredSize(new Dimension(Integer.MAX_VALUE, 80));
        }

        @Override
        public void paint(Graphics graphics) {
            super.paint(graphics);
            if (agreement==null || agreement.indizes.length==0) return;
            final Graphics2D g = (Graphics2D)graphics;
            final int numberOfCols = Math.min(agreement.indizes.length, (getWidth()-2)/ CELL_SIZE);
            final int numberOfRows = ((agreement.indizes.length+numberOfCols-1)/numberOfCols);
            agreement.setNumberOfCols(numberOfCols);
            final int W = numberOfCols* CELL_SIZE;
            final int H = numberOfRows* CELL_SIZE;
            final int sizeOfLastRow = agreement.indizes.length % numberOfCols;
            /*
            g.setColor(Color.BLACK);
            if (sizeOfLastRow==0) {
                g.fillRect(0, 0, W, H);
            } else {
                g.fillRect(0, 0, W, H- CELL_SIZE);
                g.fillRect(0, CELL_SIZE *(numberOfRows-1), CELL_SIZE *sizeOfLastRow, CELL_SIZE);
            }
            */
            final float[] components = new float[3];
            Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), components);

            // highlight current INDEX

            final int useable_cell_size = CELL_SIZE-1;
            for (int i=0; i < agreement.indizes.length; ++i) {
                final float weight = (float)agreement.weights[i];
                final int row = i/numberOfCols;
                final int col = i%numberOfCols;

                final double weight2 = Math.max(0.25, agreement.weights2[i]);
                final int reduction = (int)Math.round((useable_cell_size-(((useable_cell_size-MIN_CELL_SIZE)/0.75)*weight2))/2d) + 2;
                final int b;
                if (agreement.indizes[i]==highlightAgree || agreement.indizes[i]==highlightMissing) {
                    g.setColor(Color.BLUE);
                    b=2;
                } else {
                    b=1;
                    g.setColor(Color.BLACK);
                }
                g.fillRect((CELL_SIZE *col)+reduction-b, (CELL_SIZE *row)+reduction-b, (CELL_SIZE-reduction-reduction)+b+b, (CELL_SIZE-reduction-reduction)+b+b);

                g.setColor(Color.getHSBColor(components[0], components[1],weight));
                g.fillRect(reduction + CELL_SIZE * col, reduction + CELL_SIZE * row, CELL_SIZE -reduction - reduction, CELL_SIZE - reduction - reduction );
            }



        }
    }

    public class DatabasePanel extends JPanel {
        private CompoundCandidate candidate;
        private Font ownFont;
        private Color bgColor= new Color(155,166,219);
        public DatabasePanel() {
            setOpaque(false);
            setLayout(new FlowLayout(FlowLayout.LEFT));
            setBorder(new EmptyBorder(5,2,2,2));
            ownFont = getFont().deriveFont(Font.BOLD, 14);
        }

        public void setCompound(CompoundCandidate candidate) {
            removeAll();
            if (candidate==null || candidate.compound==null || candidate.compound.databases==null) return;
            final ArrayList<String> dbNames = new ArrayList<>(candidate.compound.databases.keySet());
            Collections.sort(dbNames);
            final FontMetrics m = getFontMetrics(ownFont);
            for (String name : dbNames) {
                final TextLayout tlayout = new TextLayout(name, ownFont, new FontRenderContext(null, false, false));
                final Rectangle2D r = tlayout.getBounds();
                add(new DatabaseLabel(name, (int)r.getWidth() +24, (int)r.getHeight() + 24, bgColor, ownFont));
            }
        }
/*
        @Override
        public void paint(Graphics graphics) {
            super.paint(graphics);
            final Graphics2D g = (Graphics2D)graphics;
            if (candidate!=null) {
                final ArrayList<String> dbNames = new ArrayList<>(candidate.compound.databases.keySet());
                Collections.sort(dbNames);
                g.setFont(ownFont);
                final FontMetrics fm = g.getFontMetrics();
                int x=0, y=0;
                for (String dbname : dbNames) {
                    final Rectangle2D r = fm.getStringBounds(dbname, g);
                    final int w = (int)r.getWidth();
                    final int h = (int)r.getHeight();
                    g.setColor(new Color(43,94,139));
                    g.fillRoundRect(x,y, w+4, h+4, w, h);
                    g.setColor(Color.WHITE);
                    g.drawString(dbname, x+2, h-2);
                    x += w + 5;

                }
            }
        }
        */
    }

    private static class DatabaseLabel extends JPanel {
        private String name;
        private Color color;
        public DatabaseLabel(String name, int width, int height, Color color, Font font) {
            this.name = name;
            this.color = color;
            setFont(font);
            setOpaque(false);
            setPreferredSize(new Dimension(width, height));
        }

        @Override
        public void paint(Graphics graphics) {
            super.paint(graphics);
            final Graphics2D g = (Graphics2D)graphics;
            final FontMetrics m = getFontMetrics(getFont());
            final int tw = m.stringWidth(name);
            final int th = m.getHeight();
            final int w = tw + 16;
            final int h = th + 12;
            g.setColor(color);
            g.fillRoundRect(2,2,w,h,4,4);
            g.setColor(Color.BLACK);
            g.drawRoundRect(2,2,w,h,4,4);
            g.setColor(Color.WHITE);
            g.drawString(name, 2 + (w-tw)/2, h - (h-th)/2);
        }
    }

    public class DescriptionPanel extends JPanel {

        protected JLabel inchi, agreements, violations;
        protected FingerprintView ag, vio;
        protected JPanel agpanel, viopanel;
        protected DatabasePanel databasePanel;

        public DescriptionPanel() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(5,2,2,2));
            inchi = new JLabel("",SwingConstants.LEFT);
            agpanel = new JPanel();
            agpanel.setOpaque(false);
            agpanel.setLayout(new BoxLayout(agpanel, BoxLayout.Y_AXIS));
            agpanel.setBorder(new EmptyBorder(5,0,0,2));
            viopanel = new JPanel();
            viopanel.setOpaque(false);
            viopanel.setBorder(new EmptyBorder(2,0,0,5));
            viopanel.setLayout(new BoxLayout(viopanel, BoxLayout.Y_AXIS));
            agreements = new JLabel("True Positive Predictions:", SwingConstants.LEFT);
            Map<TextAttribute, Object> map = new HashMap<TextAttribute, Object>();
            map.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            map.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
            violations = new JLabel("False Positive Predictions:", SwingConstants.LEFT);
            agreements.setFont(agreements.getFont().deriveFont(map));
            violations.setFont(violations.getFont().deriveFont(map));
            add(inchi);
            ag = new FingerprintView(Color.GREEN);
            vio = new FingerprintView(Color.RED);
            agpanel.add(agreements);
            agpanel.add(ag);
            viopanel.add(violations);
            viopanel.add(vio);
            add(agpanel);
            add(viopanel);
            final JLabel dbl = new JLabel("Databases");
            dbl.setFont(agreements.getFont().deriveFont(map));
            databasePanel = new DatabasePanel();
            add(dbl);
            add(databasePanel);

            setVisible(true);
        }

        public void setCompound(CompoundCandidate value) {
            setFont(propertyFont);
            inchi.setText(value.compound.inchi.in2D);
            databasePanel.setCompound(value);
            if (data==null) {
                ag.agreement = null;
                vio.agreement = null;
            } else {
                ag.agreement = value.getAgreement(computation, data.platts);
                vio.agreement = value.getMissings(computation, data.platts);
            }

        }
    }

}
