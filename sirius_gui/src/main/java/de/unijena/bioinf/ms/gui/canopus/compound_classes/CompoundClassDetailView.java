package de.unijena.bioinf.ms.gui.canopus.compound_classes;

import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.fingerid.CanopusResult;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;

import org.jdesktop.swingx.WrapLayout;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.List;

public class CompoundClassDetailView extends JPanel implements ActiveElementChangedListener<FormulaResultBean, InstanceBean>  {

    protected static String[] typeNames = new String[]{
            "Kingdom", "Superclass", "Class", "Subclass"
    };
    protected ClassyfireProperty mainClass;
    protected ClassyfireProperty[] lineage;
    protected List<ClassyfireProperty> secondaryClasses;

    protected JPanel mainClassPanel, descriptionPanel, alternativeClassPanels;

    protected JPanel container;

    public CompoundClassDetailView(FormulaList siriusResultElements) {
        super();
        //setPreferredSize(new Dimension(Integer.MAX_VALUE, 256));
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        mainClassPanel = new JPanel();
        mainClassPanel.setLayout(new WrapLayout(WrapLayout.LEFT));

        descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new WrapLayout(WrapLayout.LEFT));

        alternativeClassPanels = new JPanel();
        alternativeClassPanels.setLayout(new WrapLayout(WrapLayout.LEFT));
        /*
        container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        */

        container=new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        add(new JScrollPane(container, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));

        siriusResultElements.addActiveResultChangedListener(this);

    }

    public void updateContainer() {
        container.removeAll();
        if (mainClass!=null) {
            // add lineage
            mainClassPanel.removeAll();
            for (int k=0; k < lineage.length; ++k) {
                int level = lineage.length-k;
                String tpname = (level <= typeNames.length) ? typeNames[level-1] : ("Level " + level);
                mainClassPanel.add(new ClassifClass(lineage[k],tpname, k==0));
                if (k < lineage.length-1) {
                    final JLabel comp = new JLabel("\u27be");
                    comp.setFont(Fonts.FONT_BOLD.deriveFont(18f));
                    mainClassPanel.add(comp);
                }
            }
            container.add(mainClassPanel);
            {
                descriptionPanel.removeAll();
                final JTextPane comp = new JTextPane();
                descriptionPanel.add(comp);
                comp.setText(description());
                comp.setBorder(BorderFactory.createLoweredBevelBorder());
            }
            container.add(descriptionPanel);

            container.add(new JSeparator(JSeparator.HORIZONTAL));

            {
                alternativeClassPanels.removeAll();
                // alternative classes
                for (int k=0; k < secondaryClasses.size(); ++k) {
                    alternativeClassPanels.add(new ClassifClass(secondaryClasses.get(k),"alternative", false));
                }
            }
            container.add(alternativeClassPanels);
            repaint();
        }
    }

    private String description() {
        if (mainClass==null) return "This compound is not classified yet.";
        String m = mainClass.getDescription();
        return "This compound belongs to the class " + mainClass.getName() + ", which describes " + Character.toLowerCase(m.charAt(0)) + m.substring(1,m.length());
    }

    public void setPrediction(ProbabilityFingerprint classyfireFingerprint) {
        FingerprintVersion version = classyfireFingerprint.getFingerprintVersion();
        if (version instanceof MaskedFingerprintVersion) version = ((MaskedFingerprintVersion) version).getMaskedFingerprintVersion();
        if (!(version instanceof ClassyFireFingerprintVersion)) {
            LoggerFactory.getLogger(CompoundClassDetailView.class).error("Classyfire fingerprint has wrong versio: " + version);
            clear();
            return;
        }
        ClassyFireFingerprintVersion classif = (ClassyFireFingerprintVersion)version;
        ClassyfireProperty main = classif.getPrimaryClass(classyfireFingerprint);
        Set<ClassyfireProperty> alternatives = new HashSet<>(Arrays.asList(classif.getPredictedLeafs(classyfireFingerprint, 0.5)));
        alternatives.remove(main);
        ClassyfireProperty[] anc = main.getAncestors();
        ClassyfireProperty[] lin = new ClassyfireProperty[anc.length];
        int k=0;
        lin[0] = main;
        // ignore chemical entities
        for (int j=0; j < anc.length-1; ++j) {
            lin[++k] = anc[j];
        }
        this.lineage = lin;
        this.mainClass = main;
        this.secondaryClasses = new ArrayList<>(alternatives);
        secondaryClasses.sort(Comparator.comparingInt(x->-x.getPriority()));
        updateContainer();
    }

    public void clear() {
        this.lineage = null;
        this.mainClass = null;
        this.secondaryClasses = null;
        updateContainer();
    }



    @Override
    public void resultsChanged(InstanceBean experiment, FormulaResultBean sre, List<FormulaResultBean> resultElements, ListSelectionModel selections) {
        final Optional<CanopusResult> canopusResult = Optional.ofNullable(sre).flatMap(FormulaResultBean::getCanopusResult);
        if (canopusResult.isPresent()) setPrediction(canopusResult.get().getCanopusFingerprint());
        else clear();

    }

    protected static class ClassifClass extends JPanel implements MouseListener {

        protected final ClassyfireProperty klass;
        protected final String type;

        protected TextLayout typeName, className;
        protected Rectangle classBox, typeBox;

        protected Font typeFont, classFont;

        protected static final int PADDING = 4, SAFETY_DISTANCE = 4, GAP_TOP=4;
        protected boolean main;

        public ClassifClass(ClassyfireProperty klass, String type, boolean main) {
            super();
            this.main = main;
            this.klass = klass;
            this.type = type;
            typeFont = Fonts.FONT_BOLD.deriveFont(10f);
            typeName = new TextLayout(type, typeFont, new FontRenderContext(null, false, false));
            classFont = Fonts.FONT_BOLD.deriveFont(13f);
            className = new TextLayout(klass.getName(), classFont, new FontRenderContext(null, false, false));
            setOpaque(false);
            classBox = className.getPixelBounds(null,0,0);
            typeBox = typeName.getPixelBounds(null,0,0);
            setPreferredSize(new Dimension(Math.max(classBox.width,typeBox.width)+2*PADDING+SAFETY_DISTANCE, classBox.height+typeBox.height + 2*PADDING + SAFETY_DISTANCE+GAP_TOP));
            setMinimumSize(new Dimension(Math.max(classBox.width,typeBox.width)+2*PADDING+SAFETY_DISTANCE, classBox.height+typeBox.height + 2*PADDING + SAFETY_DISTANCE+GAP_TOP));
            addMouseListener(this);

        }

        @Override
        public void paint(Graphics g_) {
            final Graphics2D g = (Graphics2D)g_;
            final Color color = main ? Colors.CLASSIFIER_MAIN : Colors.CLASSIFIER_OTHER;
            g.setColor(color);
            final int boxwidth = Math.max(classBox.width,typeBox.width)+2*PADDING;
            final int boxheight = classBox.height+2*PADDING+GAP_TOP;
            g.fillRoundRect(0, typeBox.height, boxwidth, boxheight, 4, 4);
            g.setColor(Color.BLACK);
            g.drawRoundRect(0, typeBox.height, boxwidth, boxheight, 4, 4);
            g.setColor(color);
            final int gap = (boxwidth-typeBox.width)/2;
            g.drawRect(gap, typeBox.height,typeBox.width,Math.min(typeBox.height,classBox.height));
            g.setFont(classFont);
            g.setColor(Color.BLACK);
            g.drawString(klass.getName(), PADDING, classBox.height+typeBox.height+PADDING+GAP_TOP);

            g.setFont(typeFont);
            g.setColor(Color.BLACK);
            g.drawString(type, gap, typeBox.height+GAP_TOP);

        }

        @Override
        public void mouseClicked(MouseEvent e) {
            try {
                Desktop.getDesktop().browse(URI.create(String.format("http://classyfire.wishartlab.com/tax_nodes/C%07d",klass.getChemOntId())));
            } catch (IOException ex) {
                LoggerFactory.getLogger(CompoundClassDetailView.class).error("Failed to open webbrowser");
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
    }
}
