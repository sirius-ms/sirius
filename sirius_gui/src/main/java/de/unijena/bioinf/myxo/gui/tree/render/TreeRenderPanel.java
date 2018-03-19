package de.unijena.bioinf.myxo.gui.tree.render;

import de.unijena.bioinf.myxo.gui.tree.render.color.*;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

import static java.lang.StrictMath.abs;

public class TreeRenderPanel extends JPanel implements ComponentListener, MouseMotionListener {

    protected static int NORTH_BORDER = 5, EAST_BORDER = 5, SOUTH_BORDER = 5, WEST_BORDER = 5;

    protected TreeNode root;

    protected HashMap<TreeNode, BufferedImage> nodes;

    protected HashMap<TreeNode, PositionContainer> positonsMap;
    protected TreeNode[][] nodePositionArray;

    protected int nodesWidth;
    protected int nodesHeight;

    protected int maxXPosition, maxYPosition;
    protected int firstXPixel, firstYPixel, pixelNumberX, pixelNumberY, horizontalPixelNumber, verticalPixelNumber;
//	protected int minimalWidth, minmalHeight;

    protected Font formulaFont, propertyFont, valueFont, lossFont;
    protected FontMetrics formulaFM, propertyFM, valueFM, lossFM;

    protected Font smallFormulaFont, smallValueFont;
    protected FontMetrics smallFormulaFM, smallValueFM;

    protected DecimalFormat scoreFormat;
    protected DecimalFormat massFormat;
    protected DecimalFormat intFormat;
    @SuppressWarnings("unused")
    protected DecimalFormat snFormat;

    protected BufferedImage image;

    protected List<TreeSet<LinearFunction>> edgeStorage = new ArrayList<>(20);

    @SuppressWarnings("unused")
    protected int cePropertyWidth, scorePropertyWidth;

    protected boolean treeInitNeeded;
    protected NodeType nodeType;

    protected NodeColorManager nodeColorManager;
    protected NodeColor nodeColor = NodeColor.none;

    protected TreeNode tooltipNode;

    protected JScrollPane scrollPane;

    protected Color backColor;
    private boolean isInitializedTheFirstTime = true;

    public TreeRenderPanel(TreeNode root) {

        this.root = root;
        if (this.root != null) {
            init(this.root, NodeType.small, NodeColor.none);
        }
        this.treeInitNeeded = true;
        this.revalidate();
        this.repaint();
    }

    @SuppressWarnings("unused")
    public TreeRenderPanel() {
        root = null;
    }

    @SuppressWarnings("unused")
    public void showTree(TreeNode root, NodeType nodeType, NodeColor nodeColor) {
        this.root = root;
        if (this.root != null) {
            init(this.root, nodeType, nodeColor);
        }
        this.treeInitNeeded = true;
        this.revalidate();
        this.repaint();
    }

    protected void init(TreeNode root, NodeType nodeType, NodeColor nodeColor) {

        TreePositionCalculator calc = new MinimalWidthGreedyTreePositionCalculator();
        calc.computeRelativePositions(root);

        PositionEdgeRearrangement positionEdgeRearrangement = new PositionEdgeRearrangement();
        positionEdgeRearrangement.rearrangeTreeNodes(root);

        if (nodeColorManager == null) {
            this.nodeColor = NodeColor.none;
            nodeColorManager = new DummyNodeColorManager();
        }

        nodes = new HashMap<TreeNode, BufferedImage>();
        positonsMap = new HashMap<TreeNode, PositionContainer>();

        scoreFormat = new DecimalFormat("###.#####");
        massFormat = new DecimalFormat("####.####");
        intFormat = new DecimalFormat("##.######");
        snFormat = new DecimalFormat("####.####");

        initalizeFonts();
        changeNodeColorStep1(nodeColor);
        initalizeTreeNodeImages(root);
        changeNodeTypeStep1(nodeType);

        tooltipNode = null;

        backColor = Color.white;

        calculateMinimalSize(root);
        calculatePositionArray();


        if (isInitializedTheFirstTime) {
            isInitializedTheFirstTime = false;

            this.addComponentListener(this);
            this.addMouseMotionListener(this);
        }

//		this.setPreferredSize(new Dimension(3000,2000));
    }

    public void changeBackgroundColor(Color backColor) {
        if (backColor != null) {
            this.backColor = backColor;
            this.treeInitNeeded = true;

            if (root != null) {
                initalizeTreeNodeImages(root);
                calculateMinimalSize(root);
                calculatePositionArray();
                this.revalidate();
                this.repaint();
            }

        }

    }

    public Color getBackgroundColor() {
        return this.backColor;
    }

    @Deprecated
    public void changeRenderParameters(Color backColor, NodeType type, NodeColorManager colorManager) {
        this.backColor = backColor;

        this.nodeType = type;


        this.nodeColorManager = colorManager;

        initalizeTreeNodeImages(root);
        calculateMinimalSize(root);
        calculatePositionArray();
        this.treeInitNeeded = true;
        this.revalidate();
        this.repaint();
    }

    public void setScrollPane(JScrollPane scrollPane) {
        this.scrollPane = scrollPane;
    }

    protected void changeNodeTypeStep1(NodeType type) {
        this.nodeType = type;
    }

    public void changeNodeType(NodeType type) {
        changeNodeTypeStep1(type);
        calculateMinimalSize(root);
        calculatePositionArray();
        this.treeInitNeeded = true;
        this.revalidate();
        this.repaint();
    }

    @SuppressWarnings("unused")
    public NodeType getNodeType() {
        return this.nodeType;
    }

    @SuppressWarnings("unused")
    public NodeColor getNodeColor() {
        return this.nodeColor;
    }

    protected void changeNodeColorStep1(NodeColor nodeColor) {
        if(nodeColor != null ) {
            this.nodeColor = nodeColor;
        }

        switch (this.nodeColor) {
            case rwbIntensity:
                this.nodeColorManager = new RelativeIntensityNodeColorManager();
                break;
            case rwbMassDeviation:
                this.nodeColorManager = new MassDeviationColorManager(root);
                break;
            default:
                this.nodeColorManager = new DummyNodeColorManager();
                break;
        }
    }

    public void changeNodeColor(NodeColor nodeColor) {
        if (root == null) return;
        changeNodeColorStep1(nodeColor);

        initalizeTreeNodeImages(root);
        calculateMinimalSize(root);
        calculatePositionArray();
        this.treeInitNeeded = true;
        this.revalidate();
        this.repaint();
    }

    @SuppressWarnings("unused")
    public NodeColorManager getNodeColorManager() {
        return this.nodeColorManager;
    }

    @SuppressWarnings("unused")
    public TreeNode getRootNode() {
        return this.root;
    }

    protected void readFonts() {
        try {
            InputStream fontFile = getClass().getResourceAsStream("/ttf/DejaVuSans-Bold.ttf");
            Font tempFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            propertyFont = tempFont.deriveFont(10f);
            formulaFont = tempFont.deriveFont(11f);
            lossFont = tempFont.deriveFont(10f);

            smallFormulaFont = tempFont.deriveFont(9f);

            fontFile = getClass().getResourceAsStream("/ttf/DejaVuSans.ttf");
            tempFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            valueFont = tempFont.deriveFont(10f);

            smallValueFont = tempFont.deriveFont(8f);
        } catch (Exception e) {
            LoggerFactory.getLogger(TreeRenderPanel.class).error(e.getMessage(),e);
        }
    }

    protected void calculateGridParameters() {

        final int offset_x;
        if (maxXPosition <= 1) {
            offset_x = (int) Math.round(this.getWidth() / 2d - Math.ceil(nodesWidth / 2d));
        } else offset_x = 0;

        firstXPixel = offset_x + WEST_BORDER + (int) Math.ceil(nodesWidth / 2.0);
        firstYPixel = NORTH_BORDER + (int) Math.ceil(nodesHeight / 2.0);

        int eastBorder = firstXPixel;
        int southBorder = firstYPixel;

        pixelNumberX = this.getWidth() - firstXPixel - eastBorder;
        pixelNumberY = this.getHeight() - firstYPixel - southBorder;

        horizontalPixelNumber = this.getWidth() - EAST_BORDER - WEST_BORDER;//-firstXPixel;
        verticalPixelNumber = this.getHeight() - NORTH_BORDER - SOUTH_BORDER;//-firstYPixel;

//		pixelNumberX = horizontalPixelNumber;
//		pixelNumberY = verticalPixelNumber;

    }

    protected int getXPosition(int xVal) {
        if (maxXPosition < 1) {
            return firstXPixel;
        } else {
            return (int) (firstXPixel + ((double) pixelNumberX / (double) maxXPosition) * xVal);
        }

//		return (int) ( pixelNumberX * relXValue + startX );
    }

    protected int getYPosition(int yVal) {
        if (maxYPosition < 1) {
            return firstYPixel;
        } else {
            return (int) (firstYPixel + ((double) pixelNumberY / (double) maxYPosition) * yVal);
        }

//		return (int) ( pixelNumberY * relYValue + startY );
    }

    public void initTreeImage() {

        if (root == null) return;

//		image = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
        image = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g3 = (Graphics2D) image.getGraphics();

        g3.setColor(this.backColor);
        g3.fillRect(0, 0, (int) this.getSize().getWidth(), (int) this.getSize().getHeight());
        g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        calculateGridParameters();

        g3.setColor(Color.black);

        this.positonsMap.clear();
        printNodes(g3, root);

        this.constructEdgeLinearFunctions(root);

        printEdgeLines(g3);
        if (nodeType != NodeType.preview && nodeType != NodeType.thumbnail) printTreeEdgeLabels(g3);

        this.treeInitNeeded = false;

    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (root == null) {
            g2.setColor(this.backColor);
            g2.fillRect(0, 0, this.getWidth(), this.getHeight());
        } else {
            if (this.treeInitNeeded) {
                initTreeImage();
            }

            g2.drawImage(image, 0, 0, null);
            if (tooltipNode != null) paintToolTip(g2, tooltipNode);
        }

    }

    @SuppressWarnings("unused")
    public BufferedImage getImage() {
        if (root == null) {
            return null;
        } else {
            if (this.treeInitNeeded) {
                initTreeImage();
            }
            return this.image;
        }
    }

    protected void paintToolTip(Graphics2D g2, TreeNode node) {

        PositionContainer cont = positonsMap.get(node);

        String massProp = "peak mass:";
        String absIntProp = "abolute intensity:";
        String relIntProp = "relative intensity:";
//		String signalNoiseProp = "signal noise:";
//		String colEnergyProp   = "collision energy:";
        String scoreProp = "score:";

        int massPropWidth = propertyFM.stringWidth(massProp);
        int absIntPropWidth = propertyFM.stringWidth(absIntProp);
        int relIntPropWidth = propertyFM.stringWidth(relIntProp);
//		int signalNoisePropWidth = propertyFM.stringWidth(signalNoiseProp);
//		int colEnergyPropWidth = propertyFM.stringWidth(colEnergyProp);
        int scorePropWidth = propertyFM.stringWidth(scoreProp);

//		int leftSideMax = Math.max(massPropWidth,Math.max(absIntPropWidth, Math.max(relIntPropWidth, signalNoisePropWidth)));
//		leftSideMax = Math.max(leftSideMax,Math.max(colEnergyPropWidth, scorePropWidth));

        int leftSideMax = Math.max(massPropWidth, Math.max(absIntPropWidth, Math.max(scorePropWidth, relIntPropWidth)));

        String massVal = massFormat.format(node.getPeakMass()) + " Da";
        String absIntVal = intFormat.format(node.getPeakAbsoluteIntensity());
        String relIntVal = intFormat.format(node.getPeakRelativeIntensity());
//		String snVal        = snFormat.format(node.getPeakSignalToNoise());
//		String colEnergyVal = node.getCollisionEnergy();
        String scoreVal = scoreFormat.format(node.getScore());

        int massValWidth = valueFM.stringWidth(massVal);
        int absIntValWidth = valueFM.stringWidth(absIntVal);
        int relIntValWidth = valueFM.stringWidth(relIntVal);
//		int snValWidth = valueFM.stringWidth(snVal);
//		int colEnergyValWidth = valueFM.stringWidth(colEnergyVal);
        int scoreValWidth = valueFM.stringWidth(scoreVal);

//		int rightSideMax = Math.max(massValWidth, Math.max(absIntValWidth, Math.max(relIntValWidth, snValWidth)));
//		rightSideMax = Math.max(rightSideMax,Math.max(colEnergyValWidth, scoreValWidth));

        int rightSideMax = Math.max(massValWidth, Math.max(absIntValWidth, Math.max(relIntValWidth, scoreValWidth)));

        int southSize = leftSideMax + 5 + rightSideMax;

        String mf = node.getMolecularFormula();

        int mfWidth = formulaFM.stringWidth(mf);

        int horSize = 10 + Math.max(southSize, mfWidth);
        int vertSize = 75;

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
        else if (cont.getWestX() + horSize > eastVPBorder) startX = eastVPBorder - horSize;
        else startX = cont.getWestX();

        if (cont.getNorthY() < northVPBorder) startY = northVPBorder;
        else if (cont.getNorthY() + vertSize > southVPBorder) startY = southVPBorder - vertSize;
        else startY = cont.getNorthY();

        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f);
        g2.setComposite(ac);
        g2.setColor(new Color(222, 225, 248));
        g2.fillRoundRect(startX, startY, horSize, vertSize, 7, 7);

        g2.setColor(Color.black);
        g2.drawRoundRect(startX, startY, horSize - 1, vertSize - 1, 7, 7);
        g2.setComposite(org);

        g2.setFont(formulaFont);
        g2.drawString(mf, startX + horSize / 2 - mfWidth / 2, startY + formulaFM.getHeight());

        int southStartY = startY + formulaFM.getHeight() + 2;

        int counter = southStartY + propertyFM.getHeight();
        g2.setFont(propertyFont);

        g2.drawString(massProp, startX + 5, counter);
        counter += propertyFM.getHeight();
        g2.drawString(absIntProp, startX + 5, counter);
        counter += propertyFM.getHeight();
        g2.drawString(relIntProp, startX + 5, counter);
        counter += propertyFM.getHeight();
//		g2.drawString(signalNoiseProp, startX + 5, counter);
//		counter += propertyFM.getHeight();	
//		g2.drawString(colEnergyProp, startX + 5, counter);
//		counter += propertyFM.getHeight();	
        g2.drawString(scoreProp, startX + 5, counter);
        counter += propertyFM.getHeight();

        counter = southStartY + propertyFM.getHeight();
        g2.setFont(valueFont);

        g2.drawString(massVal, startX + 10 + leftSideMax, counter);
        counter += propertyFM.getHeight();
        g2.drawString(absIntVal, startX + 10 + leftSideMax, counter);
        counter += propertyFM.getHeight();
        g2.drawString(relIntVal, startX + 10 + leftSideMax, counter);
        counter += propertyFM.getHeight();
//		g2.drawString(snVal, startX + 10 + leftSideMax, counter);
//		counter += propertyFM.getHeight();	
//		g2.drawString(colEnergyVal, startX + 10 + leftSideMax, counter);
//		counter += propertyFM.getHeight();	
        g2.drawString(scoreVal, startX + 10 + leftSideMax, counter);
        counter += propertyFM.getHeight();

    }

    protected void initalizeFonts() {
        this.readFonts();
        propertyFM = this.getFontMetrics(this.propertyFont);
        valueFM = this.getFontMetrics(this.valueFont);
        formulaFM = this.getFontMetrics(this.formulaFont);
        lossFM = this.getFontMetrics(this.lossFont);
        smallFormulaFM = this.getFontMetrics(this.smallFormulaFont);
        smallValueFM = this.getFontMetrics(this.smallValueFont);

        cePropertyWidth = propertyFM.stringWidth("ce:");
        scorePropertyWidth = propertyFM.stringWidth("score:");
    }

    protected void initalizeTreeNodeImages(TreeNode root) {

        nodes.clear();
//		maximalNodes.clear();

        if (root == null) return;

 /*       bigNodesWidth = 0;
        withScoresNodesWidth = 0;
//		maximalNodesWidth = 0;
        bigNodesHeight = 25;
        withScoresNodesHeight = 41;
//		maximalNodesHeight = 0;
*/
        nodesWidth = 0;
        nodesHeight = 20;
      /*  previewNodesWidth = 10;
        previewNodesHeight = 10;
        thumbnailNodesWidth = 6;
        thumbnailNodesHeight = 6;*/

        processImages(root);

        this.treeInitNeeded = true;

    }

    protected void calculateMinimalSize(TreeNode root) {
        if (root == null) return;
        ArrayDeque<TreeNode> nodes = new ArrayDeque<TreeNode>();
        nodes.addFirst(root);

        maxXPosition = 0;
        maxYPosition = 0;

        TreeSet<Integer> xPositions = new TreeSet<Integer>();
        TreeSet<Integer> yPositions = new TreeSet<Integer>();

        while (!nodes.isEmpty()) {
            TreeNode node = nodes.removeFirst();
            for (TreeEdge edge : node.getOutEdges()) nodes.addFirst(edge.getTarget());
            xPositions.add(node.getHorizontalPosition());
            yPositions.add(node.getVerticalPosition());
            if (maxXPosition < node.getHorizontalPosition()) maxXPosition = node.getHorizontalPosition();
            if (maxYPosition < node.getVerticalPosition()) maxYPosition = node.getVerticalPosition();
        }

        int xGridPoints = maxXPosition + 1;
        int yGridPoints = maxYPosition + 1;

        int nodeXPixelNumber = xGridPoints * nodesWidth;
        int nodeYPixelNumber = yGridPoints * nodesHeight;

        int minHorizontalDistance = this.nodeType == NodeType.preview || this.nodeType == NodeType.thumbnail ? 5 : 10;
        int minVerticalDistance = this.nodeType == NodeType.preview || this.nodeType == NodeType.thumbnail ? 10 : 50;

        int gapXPixelNumber = (xGridPoints - 1) * minHorizontalDistance;
        int gapYPixelNumber = (yGridPoints - 1) * minVerticalDistance;

        int minimalNodesWidth = nodeXPixelNumber + gapXPixelNumber + WEST_BORDER + EAST_BORDER;
        int minimalNodesHeight = nodeYPixelNumber + gapYPixelNumber + NORTH_BORDER + SOUTH_BORDER;

        this.setPreferredSize(new Dimension(minimalNodesWidth, minimalNodesHeight));
        this.setMinimumSize(new Dimension(minimalNodesWidth, minimalNodesHeight));
    }

    protected void calculatePositionArray() {
        if (root == null) return;
        this.nodePositionArray = new TreeNode[maxXPosition + 1][maxYPosition + 1];
        ArrayDeque<TreeNode> nodes = new ArrayDeque<>();
        nodes.addFirst(root);
        while (!nodes.isEmpty()) {
            TreeNode node = nodes.removeLast();
            nodePositionArray[node.getHorizontalPosition()][node.getVerticalPosition()] = node;
            if (node.getOutEdgeNumber() > 0) {
                for (TreeEdge edge : node.getOutEdges()) nodes.addFirst(edge.getTarget());
            }
        }
    }

    protected void processImages(TreeNode node) {
        buildNodeImages(node);
        for (TreeEdge edge : node.getOutEdges()) {
            processImages(edge.getTarget());
        }
    }

    protected void buildNodeImages(TreeNode node) {
        String mf = node.getMolecularFormula();
        String mass = massFormat.format(node.getPeakMass()) + " Da";
        String peakIntensity = massFormat.format(node.getPeakRelativeIntensity() * 100) + " %";
        String massDeviation = massFormat.format(node.getDeviationMass()) + " ppm";

        int formulaLength = smallFormulaFM.stringWidth(mf);
        int massLength = smallValueFM.stringWidth(mass);
        int peakIntensityLength = smallValueFM.stringWidth(peakIntensity);
        int massDeviationLength = smallValueFM.stringWidth(massDeviation);

        final int vertSize = 36;
        int horSize = Math.max( formulaLength,  massLength);
        horSize = Math.max(horSize, peakIntensityLength);
        horSize = Math.max(horSize, massDeviationLength);
        horSize = horSize + 6;

        BufferedImage image = new BufferedImage(horSize, vertSize, BufferedImage.TYPE_INT_RGB);

        nodesWidth = horSize;
        nodesHeight = vertSize;

        Graphics2D g2 = (Graphics2D) image.getGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(this.backColor);
        g2.fillRect(0, 0, horSize, vertSize);
        g2.setColor(nodeColorManager.getColor(node));

        g2.fillRoundRect(0, 0, horSize, vertSize, 7, 7);
        g2.setColor(Color.black);
        g2.drawRoundRect(0, 0, horSize - 1, vertSize -1, 7, 7);

        g2.setFont(smallFormulaFont);
        g2.drawString(mf, (horSize - formulaLength) / 2, 9);

        g2.setFont(smallValueFont);
        g2.drawString(mass, (horSize - massLength) / 2, 17);
        g2.drawString(massDeviation, (horSize - massDeviationLength) / 2, 25);
        g2.drawString(peakIntensity, (horSize - peakIntensityLength) / 2, 33);

        nodes.put(node, image);

    }


    protected void printNodes(Graphics2D g2, TreeNode node) {
        printNode(g2, node);
        for (TreeEdge edge : node.getOutEdges()) {
            printNodes(g2, edge.getTarget());
        }
    }

    protected void printNode(Graphics2D g2, TreeNode node) {
        int relX = node.getHorizontalPosition();
        int relY = node.getVerticalPosition();
        int absX = getXPosition(relX);
        int absY = getYPosition(relY);

        BufferedImage image = nodes.get(node);

        int startPosX = absX - (image.getWidth() / 2);
        int startPosY = absY - (image.getHeight() / 2);

        g2.drawImage(image, startPosX, startPosY, null);

        PositionContainer container = new PositionContainer();
        container.setNorthY(startPosY);
        container.setSouthY(startPosY + image.getHeight());
        container.setWestX(startPosX);
        container.setEastX(startPosX + image.getWidth());
        positonsMap.put(node, container);

    }

    protected void printEdgeLines(Graphics2D g2) {

        for (TreeSet<LinearFunction> functions : edgeStorage) {
            for (LinearFunction function : functions) {
                g2.drawLine(function.getStartX(), function.getStartY(), function.getEndX(), function.getEndY());
            }
        }

    }

    protected void constructEdgeLinearFunctions(TreeNode root) {
        edgeStorage = new ArrayList<>(20);

        ArrayList<TreeNode> nodeList = new ArrayList<>();
        nodeList.add(root);

        while (true) {
            if (nodeList.isEmpty()) {
                break;
            }
            TreeSet<LinearFunction> functionSet = new TreeSet<>(new LinearFunctionComarator());
            ArrayList<TreeNode> nextLayer = new ArrayList<>();

            for (TreeNode node : nodeList) {

                int nodeNumber = node.getOutEdgeNumber();
                if (nodeNumber == 0) {
                    continue;
                }

                List<TreeEdge> edges = node.getOutEdges();

                PositionContainer parentContainer = positonsMap.get(node);

                BufferedImage parentImage = nodes.get(node);

                double stepSize = parentImage.getWidth() / (nodeNumber + 1.0);
//				int index = 1;

                for (int i = 0; i < edges.size(); i++) {

                    TreeEdge edge = edges.get(i);

                    PositionContainer childContainer = positonsMap.get(edge.getTarget());
                    BufferedImage childImage = nodes.get(edge.getTarget());

                    int startX = (int) (parentContainer.getWestX() + (i + 1) * stepSize);
                    int startY = parentContainer.getSouthY();

                    int endX = childContainer.getWestX() + (childImage.getWidth() / 2);
                    int endY = childContainer.getNorthY();

                    functionSet.add(new LinearFunction(startX, endX, startY, endY, edge));

                }

                for (TreeEdge childEdge : edges) nextLayer.add(childEdge.getTarget());

            }

            nodeList = nextLayer;

            edgeStorage.add(functionSet);

        }

    }

    protected void printLabel(Graphics2D g2, String label, int xPos, int yPos, int labelWidth, int labelHeight, LinearFunction owner) {
        g2.drawString(label, xPos, yPos);
        owner.setEastLabelBorder(xPos);
        owner.setWestLabelBorder(xPos + labelWidth);
        owner.setSouthLabelBorder(yPos);
        owner.setNorthLabelBorder(yPos - labelHeight);
    }

    protected void printTreeEdgeLabels(Graphics2D g2) {
        g2.setFont(lossFont);
        g2.setColor(Color.black);

        for (TreeSet<LinearFunction> functions : edgeStorage) {
            for (LinearFunction function : functions) {

                TreeEdge edge = function.getTreeEdge();

                String label = edge.getLossFormula();

//				boolean trigger = label.equals("C3H3NO");

                int labelWidth = lossFM.stringWidth(label);
//				int labelHeight  = lossFM.getHeight(); 
                int labelDescent = lossFM.getDescent();
                int labelAscent = lossFM.getAscent();
                int labelHeight = labelDescent + labelAscent;
//				if(trigger) System.err.println("FM: "+labelDescent+" "+labelAscent+" "+labelHeight);

                LinearFunction lowerFunction = functions.lower(function);
                LinearFunction higherFunction = functions.higher(function);

                int idealPosition = (function.getEndY() - function.getStartY()) / 2 + function.getStartY();

                if (lowerFunction == null) { //wir sind ganz links

                    int yIndex = LinearFunction.getLastYIndexWithDistanceToLeftBorder(function, labelWidth + 10, labelHeight);

                    if (yIndex > 0) {
//						if(trigger) System.err.println("Fall 1");
                        int yPos = Math.min(yIndex, idealPosition);

                        printLabel(g2, label, function.getXPosition(yPos) - labelWidth - 5, yPos, labelWidth, labelHeight, function);
//						g2.drawString(label,function.getXPosition(yPos)-labelWidth-5,yPos);
                    } else {
//						if(trigger) System.err.println("Fall 2");

                        yIndex = LinearFunction.getLastYIndexWithDistanceToRightBorder(function, labelWidth + 10, labelHeight, this.getWidth());
//						yIndex = -1;
                        if (yIndex > 0) {
//							if(trigger) System.err.println("Fall 2a");
                            int yPos = Math.min(yIndex, idealPosition);

                            printLabel(g2, label, function.getXPosition(yPos) + 5, yPos, labelWidth, labelHeight, function);
//							g2.drawString(label,function.getXPosition(yPos)+5,yPos);
                        } else {

                            //TODO rechts davon kannschon was stehen

//							if(trigger) System.err.println("Fall 2b");
                            g2.setColor(this.backColor);
//							g2.setColor(Color.blue);
                            g2.fillRect(1, idealPosition - labelAscent - 2, labelWidth + 5, labelHeight + 4);
//							g2.fillRect(1,idealPosition-(labelHeight/2)-2,labelWidth+1, labelHeight+2);
                            g2.setColor(Color.black);

                            printLabel(g2, label, 1, idealPosition, labelWidth, labelHeight, function);
//							printLabel(g2, label, 1, idealPosition+(labelHeight/4), labelWidth, labelHeight, function);

//							g2.drawString(label,1,idealPosition);

                        }

                    }

                } else if (higherFunction == null) { // ganz rechts

                    int yIndex = LinearFunction.getLastYIndexWithDistanceToRightBorder(function, labelWidth + 10, labelHeight, this.getWidth());

                    if (yIndex > 0) {
                        int yPos = Math.min(yIndex, idealPosition);

                        printLabel(g2, label, function.getXPosition(yPos) + 5, yPos, labelWidth, labelHeight, function);
//						g2.drawString(label,function.getXPosition(yPos)+5,yPos);
                    } else {
                        yIndex = LinearFunction.getFirstYIndexWithDistance(lowerFunction, function, labelWidth + 10, labelHeight);

                        if (yIndex > 0) {
                            int yPos = Math.min(yIndex, idealPosition);

                            printLabel(g2, label, function.getXPosition(yPos) + 5, yPos, labelWidth, labelHeight, function);
//							g2.drawString(label,function.getXPosition(yPos)+5,yPos);
                        } else {
                            g2.setColor(this.backColor);
                            g2.fillRect(this.getWidth() - labelWidth - 5, idealPosition - labelAscent - 2, labelWidth + 5, labelHeight + 4);
//							g2.fillRect(this.getWidth()-labelWidth-5,idealPosition+1,labelWidth+6, labelHeight+2);
                            g2.setColor(Color.black);

                            printLabel(g2, label, this.getWidth() - labelWidth - 3, idealPosition, labelWidth, labelHeight, function);
//							printLabel(g2, label, this.getWidth()-labelWidth-3, idealPosition, labelWidth, labelHeight, function);
//							g2.drawString(label,this.getWidth()-labelWidth-3,idealPosition);
                        }
                    }

                } else { // irgendwo in der Mitte

                    int corrVal = -4;

                    int yIndex = LinearFunction.getFirstYIndexWithDistance(function, higherFunction, labelWidth + 10, labelHeight + corrVal);

                    if (yIndex > 0) {

                        int yPos = Math.max(yIndex, idealPosition);
                        int yPos2 = 0;
                        int xPos = function.getXPosition(yPos);
                        int xPos2 = 0;

                        if (function.getStartX() <= function.getEndX()) {
                            yPos2 = yPos;
                            xPos2 = xPos + 5;

                            printLabel(g2, label, xPos2, yPos2, labelWidth, labelHeight, function);
//							g2.drawString(label,xPos2,yPos2);
                        } else {
                            yPos2 = yPos;
                            xPos2 = function.getXPosition(yPos2 - (labelHeight + corrVal)) + 5;
                            yPos = yPos - (labelHeight + corrVal);
                            xPos = function.getXPosition(yPos);

                            printLabel(g2, label, xPos2, yPos2, labelWidth, labelHeight, function);
//							g2.drawString(label,xPos2,yPos2);
                        }

//						g2.setColor(Color.RED);
//						g2.fillRect(xPos-1, yPos-1, 3, 3);
//						g2.setColor(Color.BLACK);

                    } else {

                        yIndex = LinearFunction.getFirstYIndexWithDistance(lowerFunction, function, labelWidth + 10, labelHeight + corrVal);

                        if (yIndex > 0) {

                            int yLowerPos = Math.max(yIndex, idealPosition);

                            int yPos = 0;
                            int yPos2 = 0;
                            int xPos = 0;
                            int xPos2 = 0;

                            if (function.getStartX() <= function.getEndX()) {
                                yPos = yLowerPos;
                                xPos = function.getXPosition(yPos);

                                yPos2 = yPos;
                                xPos2 = function.getXPosition(yPos2 - (labelHeight + corrVal)) - labelWidth - 2;

                                printLabel(g2, label, xPos2, yPos, labelWidth, labelHeight, function);
//								g2.drawString(label,xPos2,yPos);								
                            } else {
                                yPos = yLowerPos;
                                xPos = function.getXPosition(yPos);

                                yPos2 = yLowerPos;
                                xPos2 = function.getXPosition(yPos2) - labelWidth - 5;

                                printLabel(g2, label, xPos2, yPos2, labelWidth, labelHeight, function);
//								g2.drawString(label,xPos2,yPos2);
                            }

//							g2.setColor(Color.BLUE);
//							g2.fillRect(xPos-1, yPos-1, 3, 3);
//							g2.setColor(Color.BLACK);

                        } else {  // zeichne das Label einfach über die Kante

//							int startPos = (function.getEndY()-function.getStartY())/2 + function.getStartY();
//							int endPos = function.getEndY() - (labelHeight+corrVal)/2 - 5;

                            ////////////////////////////

                            int northAncestorBorder = lowerFunction.getNorthLabelBorder();
                            int southAncestorBorder = lowerFunction.getSouthLabelBorder();
                            int eastAncestorBorder = lowerFunction.getEastLabelBorder();


                            int middleX = Math.abs((function.getEndX() - function.getStartX())) / 2 + Math.min(function.getStartX(), function.getEndX());
                            int middleY = (function.getEndY() - function.getStartY()) / 2 + function.getStartY();

                            //versuche in die Mitte zu zeichnen
                            int westBorder = middleX - labelWidth / 2;
                            int northBorder = middleY - (labelHeight + corrVal) / 2;
                            int southBorder = middleY + (labelHeight + corrVal) / 2;

                            int valX = labelWidth + 2;
                            int valY = labelHeight + corrVal + 2;

                            if (eastAncestorBorder >= westBorder) {
                                if ((northAncestorBorder <= northBorder && southAncestorBorder >= northBorder)
                                || (northAncestorBorder <= southBorder && southAncestorBorder >= southAncestorBorder)
                                || (northAncestorBorder >= northBorder && southAncestorBorder <= southBorder)) {
                                    int posY = southAncestorBorder + labelHeight + corrVal + 2;
                                    int posX = function.getXPosition(posY - (labelHeight + corrVal) / 2) - labelWidth / 2;
                                    g2.setColor(this.backColor);
                                    g2.fillRect(posX - 1, posY - (labelHeight + corrVal) - 1, valX, valY);
                                    g2.setColor(Color.BLACK);
                                    printLabel(g2, label, posX, posY, labelWidth, labelHeight, function);
                                } else {
                                    g2.setColor(this.backColor);
                                    g2.fillRect(westBorder - 1, northBorder - 1, valX, valY);
                                    g2.setColor(Color.BLACK);
                                    printLabel(g2, label, westBorder, southBorder, labelWidth, labelHeight, function);
                                }
                            } else {

                                g2.setColor(this.backColor);
                                g2.fillRect(westBorder - 1, northBorder - 1, valX, valY);
                                g2.setColor(Color.BLACK);
                                printLabel(g2, label, westBorder, southBorder, labelWidth, labelHeight, function);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void componentHidden(ComponentEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void componentMoved(ComponentEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void componentResized(ComponentEvent e) {
        this.treeInitNeeded = true;
        this.repaint();
    }

    @Override
    public void componentShown(ComponentEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseDragged(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int xPos = e.getX();
        int yPos = e.getY();
        if (xPos < WEST_BORDER || xPos >= WEST_BORDER + horizontalPixelNumber) {
            changeToolTipNode(null);
            return;
        }
        if (yPos < NORTH_BORDER || yPos >= NORTH_BORDER + verticalPixelNumber) {
            changeToolTipNode(null);
            return;
        }
        double xRel = xPos - WEST_BORDER;
        double yRel = yPos - NORTH_BORDER;
        int xIndex = (int) ((xRel / (horizontalPixelNumber)) * (maxXPosition + 1));
        int yIndex = (int) ((yRel / (verticalPixelNumber)) * (maxYPosition + 1));
        TreeNode tn = this.nodePositionArray[xIndex][yIndex];
        if (tn == null) {
            changeToolTipNode(null);
            return;
        }
        PositionContainer cont = positonsMap.get(tn);

        /**
         * @Marvin: container is created after drawing, but moveMoved can be called earlier (e.g. after tree structure changed)
         */
        if (cont == null) {
            return;
        }

        if (xPos < cont.getWestX() || xPos > cont.getEastX() || yPos < cont.getNorthY() || yPos > cont.getSouthY()) {
            changeToolTipNode(null);
            return;
        }

        changeToolTipNode(tn);
    }

    public void changeToolTipNode(TreeNode node) {
        if (node == null) {
            if (tooltipNode != null) {
                tooltipNode = null;
                repaint();
            }
        } else {
            if (tooltipNode != node) {
                tooltipNode = node;
                repaint();
            }
        }
    }


    protected static class PositionContainer {

        protected int westX, eastX, northY, southY;

        public int getWestX() {
            return westX;
        }

        public void setWestX(int westX) {
            this.westX = westX;
        }

        public int getEastX() {
            return eastX;
        }

        public void setEastX(int eastX) {
            this.eastX = eastX;
        }

        public int getNorthY() {
            return northY;
        }

        public void setNorthY(int northY) {
            this.northY = northY;
        }

        public int getSouthY() {
            return southY;
        }

        public void setSouthY(int southY) {
            this.southY = southY;
        }

    }

    protected static class LinearFunction {

        protected double m, n;
        protected int startX, startY, endX, endY;

        protected TreeEdge edge;

        protected int westLabelBorder, eastLabelBorder, northLabelBorder, southLabelBorder;

        public LinearFunction(int startX, int endX, int startY, int endY, TreeEdge edge) {

            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;

            if (startX != startY) {
                m = (double) (endY - startY) / (double) (endX - startX);
                n = startY - (startX * m);
            } else { // stehen übereinander
                m = Double.NaN;
                n = Double.NaN;
            }

            this.edge = edge;

            this.northLabelBorder = -1;
            this.eastLabelBorder = -1;
            this.southLabelBorder = -1;
            this.westLabelBorder = -1;

        }

        @SuppressWarnings("unused")
        public double getM() {
            return this.m;
        }

        @SuppressWarnings("unused")
        public double getN() {
            return this.n;
        }

        public int getStartX() {
            return this.startX;
        }

        public int getStartY() {
            return this.startY;
        }

        public int getEndX() {
            return this.endX;
        }

        public int getEndY() {
            return this.endY;
        }

        public TreeEdge getTreeEdge() {
            return edge;
        }

        public int getEastLabelBorder() {
            return this.eastLabelBorder;
        }

        @SuppressWarnings("unused")
        public int getWestLabelBorder() {
            return this.westLabelBorder;
        }

        public int getNorthLabelBorder() {
            return this.northLabelBorder;
        }

        public int getSouthLabelBorder() {
            return this.southLabelBorder;
        }

        public void setSouthLabelBorder(int southBorder) {
            this.southLabelBorder = southBorder;
        }

        public void setWestLabelBorder(int westBorder) {
            this.westLabelBorder = westBorder;
        }

        public void setNorthLabelBorder(int northBorder) {
            this.northLabelBorder = northBorder;
        }

        public void setEastLabelBorder(int eastBorder) {
            this.eastLabelBorder = eastBorder;
        }

        public int getXPosition(int yPos) {
            if (startX == endX) return startX;

            if (yPos <= startY) return startX;
            if (yPos >= endY) return endX;
            return (int) Math.round((yPos - n) / m);
        }

        @SuppressWarnings("unused")
        public int getYPosition(int xPos) {
            if (startX == endX) return Integer.MAX_VALUE;

            if (xPos <= startX) return startY;
            if (xPos >= endX) return endY;
            return (int) Math.round(xPos * m + n);
        }

        @SuppressWarnings("unused")
        public int getMiddleYPos() {
            return (endY - startY) / 2 + startY;
        }

        static int getFirstYIndexWithDistance(LinearFunction f1, LinearFunction f2, int rectSizeX, int rectSizeY) {

            if (f1.getStartY() != f2.getStartY() || f1.getEndY() != f2.getEndY())
                throw new RuntimeException("startY and endY must be identical");

            int startYPos = (f1.getEndY() - f1.getStartY()) / 2 + f1.getStartY();

            int endYPos = f1.getEndY() - 5;

            int firstYLabelPos = f1.getNorthLabelBorder() - 1;
            int lastYLabelPos = f1.getSouthLabelBorder() + 1;
            int lastXLabelPos = f1.getEastLabelBorder();

            if (f1.getStartX() >= f1.getEndX() && f2.getStartX() <= f2.getEndX()) {
                // -> / \

                for (int i = startYPos; i < endYPos; i++) {
                    int yTestVal1 = i - rectSizeY;
                    int yTestVal2 = i - rectSizeY;

                    int pos1 = f1.getXPosition(yTestVal1);
                    if (yTestVal1 >= firstYLabelPos && yTestVal1 <= lastYLabelPos) pos1 = Math.max(pos1, lastXLabelPos);

                    int pos2 = f2.getXPosition(yTestVal2);
                    if (pos2 - pos1 > rectSizeX) return i;
                }

            } else if (f1.getStartX() >= f1.getEndX() && f2.getStartX() >= f2.getEndX()) {
                // -> / /

                for (int i = startYPos; i < endYPos; i++) {
                    int yTestVal1 = i - rectSizeY;
                    int yTestVal2 = i;

                    int pos1 = f1.getXPosition(yTestVal1);
                    if (yTestVal1 >= firstYLabelPos && yTestVal1 <= lastYLabelPos) pos1 = Math.max(pos1, lastXLabelPos);

                    int pos2 = f2.getXPosition(yTestVal2);
                    if (pos2 - pos1 > rectSizeX) return i;
                }

            } else if (f1.getStartX() < f1.getEndX() && f2.getStartX() < f2.getEndX()) {
                // -> \ \

                for (int i = startYPos; i < endYPos; i++) {
                    int yTestVal1 = i;
                    int yTestVal2 = i - rectSizeY;

                    int pos1 = f1.getXPosition(yTestVal1);
                    if (yTestVal1 >= firstYLabelPos && yTestVal1 <= lastYLabelPos) pos1 = Math.max(pos1, lastXLabelPos);

                    int pos2 = f2.getXPosition(yTestVal2);
                    if (pos2 - pos1 > rectSizeX) return i;
                }

            } else {

                // -> \  /

                for (int i = startYPos; i < endYPos; i++) {
                    int yTestVal1 = i;
                    int yTestVal2 = i;

                    int pos1 = f1.getXPosition(yTestVal1);
                    if (yTestVal1 >= firstYLabelPos && yTestVal1 <= lastYLabelPos) pos1 = Math.max(pos1, lastXLabelPos);

                    int pos2 = f2.getXPosition(yTestVal2);
                    if (pos2 - pos1 > rectSizeX) return i;
                }

            }

            return -1;

        }

        static int getLastYIndexWithDistanceToLeftBorder(LinearFunction f1, int rectSizeX, int rectSizeY) {

            int startYPos = f1.getEndY() - 5;
            int endYPos = f1.getStartY() + 5 + rectSizeY;

            for (int i = startYPos; i >= endYPos; i--) {
                if (f1.getXPosition(i) > rectSizeX) return i;
            }

            return -1;
        }

        static int getLastYIndexWithDistanceToRightBorder(LinearFunction f1, int rectSizeX, int rectSizeY, int rightBorder) {

            int startYPos = f1.getEndY() - 5;
            int endYPos = f1.getStartY() + 5 + rectSizeY;

            for (int i = startYPos; i >= endYPos; i--) {
                if (f1.getXPosition(i) + rectSizeX < rightBorder) return i;
            }

            return -1;
        }

    }

    protected static class LinearFunctionComarator implements Comparator<LinearFunction> {

        @Override
        public int compare(LinearFunction o1, LinearFunction o2) {
            if (o1.getStartX() < o2.getStartX()) return -1;
            else if (o1.getStartX() == o2.getStartX()) return 0;
            else return 1;
        }

    }

}
