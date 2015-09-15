package de.unijena.bioinf.sirius.gui.mainframe.results;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import com.mysql.jdbc.JDBC4CallableStatement;

import de.unijena.bioinf.myxo.gui.tree.render.NodeColor;
import de.unijena.bioinf.myxo.gui.tree.render.TreeRenderFrame;
import de.unijena.bioinf.myxo.gui.tree.render.TreeRenderPanel;
import de.unijena.bioinf.myxo.gui.tree.render.NodeType;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

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
	
	private static final NodeType[] NODE_TYPES = {NodeType.small, NodeType.big, NodeType.score};
	
//	private static final String[] COLOR_TYPES = {"RGB Score", "RGB Intensity", "RBG Score", "RBG Intensity", "RG Score", "RG Intensity", "BGR Score", "BGR Intensity", "none"};
	
	private static final NodeColor[] COLOR_TYPES = {NodeColor.rgbScore,NodeColor.rgbIntensity,NodeColor.rgScore,NodeColor.rgIntensity,
		NodeColor.rwbScore,NodeColor.rwbIntensity,NodeColor.none};
	
	
	public TreeVisualizationPanel(Frame owner){
		
		this.owner = owner;
		this.sre = null;
		
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
		saveTreeB = new JButton("save tree");
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
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			jfc.setAcceptAllFileFilterUsed(false);
			jfc.addChoosableFileFilter(new FTreeJsonFilter());
			jfc.addChoosableFileFilter(new FTreeDotFilter());
			
			
			File selectedFile = null;
			
			while(selectedFile==null){
				int returnval = jfc.showSaveDialog(this);
				if(returnval == JFileChooser.APPROVE_OPTION){
					File selFile = jfc.getSelectedFile();
					
					String name = selFile.getName();
					if(jfc.getFileFilter() instanceof FTreeJsonFilter){
						if(!selFile.getAbsolutePath().endsWith(".json")){
							selFile = new File(selFile.getAbsolutePath()+".json");
						}
					}else if(jfc.getFileFilter() instanceof FTreeDotFilter){
						if(!selFile.getAbsolutePath().endsWith(".dot")){
							selFile = new File(selFile.getAbsolutePath()+".dot");
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
			
			if(selectedFile!=null){
				IdentificationResult ir = new IdentificationResult(sre.getRawTree(), sre.getRank());
				try{
					ir.writeTreeToFile(selectedFile);
				}catch(IOException e2){
					FileExceptionDialog fed = new FileExceptionDialog(owner, e2.getMessage());
				}
				System.out.println(selectedFile.getAbsolutePath());
			}
			
//			if(jfc.)
		}
	}

}

class FTreeJsonFilter extends FileFilter{

	@Override
	public boolean accept(File f) {
		if(f.isDirectory()) return true;
		String name = f.getName();
		if(name.endsWith(".json")){
			return true;
		}
		return false;
	}

	@Override
	public String getDescription() {
		return "JSON";
	}
	
}

class FTreeDotFilter extends FileFilter{

	@Override
	public boolean accept(File f) {
		if(f.isDirectory()) return true;
		String name = f.getName();
		if(name.endsWith(".dot")){
			return true;
		}
		return false;
	}

	@Override
	public String getDescription() {
		return "Dot";
	}
	
}

class FilePresentDialog extends JDialog implements ActionListener{
	
	private ReturnValue rv;
	
	private JButton ok, abort;
	
	public FilePresentDialog(Frame owner, String name) {
		super(owner,true);
		
		rv = ReturnValue.Abort;
		
		this.setLayout(new BorderLayout());
		JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,20,10));
		Icon icon = UIManager.getIcon("OptionPane.questionIcon");
		northPanel.add(new JLabel(icon));
		northPanel.add(new JLabel("The file \""+name+"\" is already present. Override it?"));
		this.add(northPanel,BorderLayout.CENTER);
		JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		ok = new JButton("Yes");
		ok.addActionListener(this);
		abort = new JButton("No");
		abort.addActionListener(this);
		south.add(ok);
		south.add(abort);
		this.add(south,BorderLayout.SOUTH);
		this.pack();
		this.setVisible(true);
		// TODO Auto-generated constructor stub
	}
	
	public ReturnValue getReturnValue(){
		return rv;
	}



	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==ok){
			rv = ReturnValue.Success;
		}else{
			rv = ReturnValue.Abort;
		}
		this.dispose();
	}
	
}

class FileExceptionDialog extends JDialog implements ActionListener{
	
	private JButton ok;
	
	public FileExceptionDialog(Frame owner, String message) {
		super(owner,true);
		
		this.setLayout(new BorderLayout());
		JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,20,10));
		Icon icon = UIManager.getIcon("OptionPane.errorIcon");
		northPanel.add(new JLabel(icon));
		northPanel.add(new JLabel(message));
		this.add(northPanel,BorderLayout.CENTER);
		JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		ok = new JButton("Ok");
		ok.addActionListener(this);
		south.add(ok);
		this.add(south,BorderLayout.SOUTH);
		this.pack();
		this.setVisible(true);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		this.dispose();
	}
	
}
