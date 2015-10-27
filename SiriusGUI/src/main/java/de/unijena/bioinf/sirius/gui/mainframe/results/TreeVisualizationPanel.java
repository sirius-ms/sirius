package de.unijena.bioinf.sirius.gui.mainframe.results;

import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.myxo.gui.tree.render.NodeColor;
import de.unijena.bioinf.myxo.gui.tree.render.NodeType;
import de.unijena.bioinf.myxo.gui.tree.render.TreeRenderPanel;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.sirius.gui.configs.ConfigStorage;
import de.unijena.bioinf.sirius.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.sirius.gui.dialogs.FilePresentDialog;
import de.unijena.bioinf.sirius.gui.io.DotIO;
import de.unijena.bioinf.sirius.gui.io.RasterGraphicsIO;
import de.unijena.bioinf.sirius.gui.structure.FileFormat;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.structure.TreeCopyTool;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;

public class TreeVisualizationPanel extends JPanel implements ActionListener{

	private JScrollPane pane;
	
	private JComboBox<NodeType> nodeType;
	private JComboBox<NodeColor> colorType;
	private TreeRenderPanel renderPanel;
	private ScoreVisualizationPanel svp;
	private JLabel legendText;
	private JButton saveTreeB;
	
	private Frame owner;
	
	private SiriusResultElement sre;
	
	private ConfigStorage config;
	
	private static final NodeType[] NODE_TYPES = {NodeType.small, NodeType.big, NodeType.score};
	
//	private static final String[] COLOR_TYPES = {"RGB Score", "RGB Intensity", "RBG Score", "RBG Intensity", "RG Score", "RG Intensity", "BGR Score", "BGR Intensity", "none"};
	
	private static final NodeColor[] COLOR_TYPES = {NodeColor.rgbScore,NodeColor.rgbIntensity,NodeColor.rgScore,NodeColor.rgIntensity,
		NodeColor.rwbScore,NodeColor.rwbIntensity,NodeColor.none};
	
	
	public TreeVisualizationPanel(Frame owner,ConfigStorage config){
		
		this.owner = owner;
		this.sre = null;
		
		this.config = config;
		
		this.setLayout(new BorderLayout());
//		this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"tree view"));
		
		JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		nodeType = new JComboBox<>(NODE_TYPES);
		nodeType.addActionListener(this);
		northPanel.add(new JLabel("node style "));
		northPanel.add(nodeType);
		colorType = new JComboBox<>(COLOR_TYPES);
		colorType.addActionListener(this);
		northPanel.add(new JLabel(" node color style "));
		northPanel.add(colorType);
		saveTreeB = new JButton("Export tree",new ImageIcon(TreeVisualizationPanel.class.getResource("/icons/document-export.png")));
		saveTreeB.addActionListener(this);
		saveTreeB.setEnabled(false);
		northPanel.add(new JLabel("  "));
		northPanel.add(saveTreeB);
		
		this.add(northPanel,BorderLayout.NORTH);
		
		renderPanel = new TreeRenderPanel(); 
		renderPanel.changeBackgroundColor(Color.white);
		
		pane = new JScrollPane(renderPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		renderPanel.setScrollPane(pane);
		
//		this.add(panel,BorderLayout.CENTER);
		this.add(pane,BorderLayout.CENTER);
		
		JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		svp = new ScoreVisualizationPanel();
		legendText = new JLabel("score ");
		southPanel.add(legendText);
		southPanel.add(svp);
		this.add(southPanel,BorderLayout.SOUTH);
//		this.setSize(1024,600);
//		this.setVisible(true);
	}
	
	public void showTree(SiriusResultElement sre){
		this.sre = sre;
		if(sre!=null){
			TreeNode root = sre.getTree();
			NodeType nt = this.nodeType.getItemAt(this.nodeType.getSelectedIndex());
			NodeColor nc = COLOR_TYPES[colorType.getSelectedIndex()];
			this.renderPanel.showTree(root,nt,nc);
			if(nc==NodeColor.rgbScore||nc==NodeColor.rgbScore||nc==NodeColor.rwbScore){
				legendText.setText("score ");
			}else if(nc==NodeColor.none){
				legendText.setText(" ");
			}else{
				legendText.setText("intensity");
			}
//			pane.invalidate();
			this.svp.setNodeColorManager(this.renderPanel.getNodeColorManager());
			this.svp.repaint();
			this.saveTreeB.setEnabled(true);
		}else{
			this.renderPanel.showTree(null, null, null);
			this.svp.setNodeColorManager(null);
			this.svp.repaint();
			this.saveTreeB.setEnabled(false);
			
		}
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == this.nodeType){
			NodeType nt = this.nodeType.getItemAt(this.nodeType.getSelectedIndex());
			this.renderPanel.changeNodeType(nt);
		}else if(e.getSource() == this.colorType){
			NodeColor nc = COLOR_TYPES[colorType.getSelectedIndex()];
			this.renderPanel.changeNodeColor(nc);
			this.svp.setNodeColorManager(this.renderPanel.getNodeColorManager());
			if(nc==NodeColor.rgbScore||nc==NodeColor.rgbScore||nc==NodeColor.rwbScore){
				legendText.setText("score ");
			}else if(nc==NodeColor.none){
				legendText.setText(" ");
			}else{
				legendText.setText("intensity");
			}
			this.svp.repaint();
		}else if(e.getSource()== this.saveTreeB){
			JFileChooser jfc = new JFileChooser();
			jfc.setCurrentDirectory(config.getDefaultTreeExportPath());
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			jfc.setAcceptAllFileFilterUsed(false);
			
			FileFilter dotFilter = new FTreeDotFilter();
			FileFilter gifFilter = new FTreeGIFFilter();
			FileFilter jpgFilter = new FTreeJPGFilter();
			FileFilter pngFilter = new FTreePNGFilter();
			FileFilter jsonFilter = new FTreeJSONFilter();
			
			
			jfc.addChoosableFileFilter(dotFilter);
			jfc.addChoosableFileFilter(gifFilter);
			jfc.addChoosableFileFilter(jpgFilter);
			jfc.addChoosableFileFilter(pngFilter);
			jfc.addChoosableFileFilter(jsonFilter);
//			jfc.addChoosableFileFilter(new FTreeJsonFilter());
			
			FileFormat defaultFF = config.getDefaultTreeFileFormat();
			if(defaultFF==FileFormat.dot){
				jfc.setFileFilter(dotFilter);
			}else if(defaultFF==FileFormat.gif){
				jfc.setFileFilter(gifFilter);
			}else if(defaultFF==FileFormat.jpg){
				jfc.setFileFilter(jpgFilter);
			}else if(defaultFF==FileFormat.png){
				jfc.setFileFilter(pngFilter);
			} else if (defaultFF==FileFormat.json) {
				jfc.setFileFilter(jsonFilter);
			}
			
			
			File selectedFile = null;
			FileFormat ff = FileFormat.none;
			
			while(selectedFile==null){
				int returnval = jfc.showSaveDialog(this);
				if(returnval == JFileChooser.APPROVE_OPTION){
					File selFile = jfc.getSelectedFile();
					
					config.setDefaultTreeExportPath(selFile.getParentFile());
					
					String name = selFile.getName();
					if(jfc.getFileFilter() == dotFilter){
						ff = FileFormat.dot;
						if(!selFile.getAbsolutePath().endsWith(".dot")){
							selFile = new File(selFile.getAbsolutePath()+".dot");
						}
					}else if(jfc.getFileFilter() == gifFilter){
						ff = FileFormat.gif;
						if(!selFile.getAbsolutePath().endsWith(".gif")){
							selFile = new File(selFile.getAbsolutePath()+".gif");
						}
					}else if(jfc.getFileFilter() == jpgFilter){
						ff = FileFormat.jpg;
						if(!selFile.getAbsolutePath().endsWith(".jpg")){
							selFile = new File(selFile.getAbsolutePath()+".jpg");
						}
					}else if(jfc.getFileFilter() == pngFilter) {
						ff = FileFormat.png;
						if (!selFile.getAbsolutePath().endsWith(".png")) {
							selFile = new File(selFile.getAbsolutePath() + ".png");
						}
					} else if (jfc.getFileFilter() == jsonFilter) {
						ff = FileFormat.json;
						if (!selFile.getAbsolutePath().endsWith(".json")) {
							selFile = new File(selFile.getAbsolutePath() + ".json");
						}
					}else{
						throw new RuntimeException(jfc.getFileFilter().getClass().getName());
					}
					
					if(selFile.exists()){
						FilePresentDialog fpd = new FilePresentDialog(owner, selFile.getName());
						ReturnValue rv = fpd.getReturnValue();
						if(rv==ReturnValue.Success){
							selectedFile = selFile;
						}
//						int rt = JOptionPane.showConfirmDialog(this, "The file \""+selFile.getName()+"\" is already present. Override it?");
					}else{
						selectedFile = selFile;	
					}
				}else{
					break;
				}
			}
			
			if(ff!=FileFormat.none){
				config.setDefaultTreeFileFormat(ff);
			}
			
			if(selectedFile!=null&&ff!=FileFormat.none){
				
				try{
					if(ff==FileFormat.dot){
						DotIO.writeTree(selectedFile, sre.getTree(), sre.getScore());
					}else if(ff==FileFormat.gif){
						RasterGraphicsIO.writeGIF(selectedFile, getTreeImage());
					}else if(ff==FileFormat.jpg){
						RasterGraphicsIO.writeJPG(selectedFile, getTreeImage());
					}else if(ff==FileFormat.png){
						RasterGraphicsIO.writePNG(selectedFile, getTreeImage());
					} else if (ff == FileFormat.json) {
						new FTJsonWriter().writeTreeToFile(selectedFile, sre.getRawTree());
					}
				}catch(Exception e2){
					ExceptionDialog fed = new ExceptionDialog(owner, e2.getMessage());
					e2.printStackTrace();
				}
				
				
				
//				IdentificationResult ir = new IdentificationResult(sre.getRawTree(), sre.getRank());
//				try{
//					ir.writeTreeToFile(selectedFile);
//				}catch(IOException e2){
//					ExceptionDialog fed = new ExceptionDialog(owner, e2.getMessage());
//				}
//				System.out.println(selectedFile.getAbsolutePath());
			}
			
//			if(jfc.)
		}
	}
	
	private BufferedImage getTreeImage(){
		TreeNode root = TreeCopyTool.copyTree(this.sre.getTree());
		NodeColor color = this.renderPanel.getNodeColor();
		NodeType type = this.renderPanel.getNodeType();
		TreeRenderPanel panel = new TreeRenderPanel();
		panel.showTree(root, type, color);
		Dimension dim = panel.getMinimumSize();
		panel.setSize(new Dimension((int)dim.getWidth(),(int)dim.getHeight()));
//		panel.setSize(new Dimension(1000,400));
		panel.changeNodeType(type);
		return panel.getImage();
	}

}

abstract class FTreeFilter extends FileFilter{
	
	private String fileSuffix,description;
	
	public FTreeFilter(String fileSuffix, String description) {
		this.fileSuffix = fileSuffix;
		this.description = description;
	}

	@Override
	public boolean accept(File f) {
		if(f.isDirectory()) return true;
		String name = f.getName();
		if(name.endsWith(fileSuffix)){
			return true;
		}
		return false;
	}

	@Override
	public String getDescription() {
		return description;
	}
	
}

class FTreeJsonFilter extends FTreeFilter{
	
	public FTreeJsonFilter() {
		super(".json","JSON");
	}
	
}

class FTreeDotFilter extends FTreeFilter{
	
	public FTreeDotFilter() {
		super(".dot","Dot");
	}
	
}

class FTreeJPGFilter extends FTreeFilter{
	
	public FTreeJPGFilter() {
		super(".jpg","JPEG");
	}
	
}

class FTreeGIFFilter extends FTreeFilter{
	
	public FTreeGIFFilter() {
		super(".gif","GIF");
	}
	
}

class FTreePNGFilter extends FTreeFilter{
	
	public FTreePNGFilter() {
		super(".png","PNG");
	}
	
}

class FTreeJSONFilter extends FTreeFilter {

	public FTreeJSONFilter() {
		super(".json", "JSON");
	}
}


