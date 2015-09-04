package de.unijena.bioinf.sirius.gui.mainframe.results;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import de.unijena.bioinf.myxo.gui.tree.render.NodeColor;
import de.unijena.bioinf.myxo.gui.tree.render.TreeRenderFrame;
import de.unijena.bioinf.myxo.gui.tree.render.TreeRenderPanel;
import de.unijena.bioinf.myxo.gui.tree.render.NodeType;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

public class TreeVisualizationPanel extends JPanel implements ActionListener{

	private JScrollPane pane;
	
	private JComboBox<NodeType> nodeType;
	private JComboBox<NodeColor> colorType;
	private TreeRenderPanel renderPanel;
	private ScoreVisualizationPanel svp;
	private JLabel legendText;
	
	private static final NodeType[] NODE_TYPES = {NodeType.small, NodeType.big, NodeType.score};
	
//	private static final String[] COLOR_TYPES = {"RGB Score", "RGB Intensity", "RBG Score", "RBG Intensity", "RG Score", "RG Intensity", "BGR Score", "BGR Intensity", "none"};
	
	private static final NodeColor[] COLOR_TYPES = {NodeColor.rgbScore,NodeColor.rgbIntensity,NodeColor.rgScore,NodeColor.rgIntensity,
		NodeColor.rwbScore,NodeColor.rwbIntensity,NodeColor.none};
	
	
	public TreeVisualizationPanel(){
		
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
	
	public void showTree(TreeNode root){
//		System.err.println("showTree");
		if(root!=null){
//			System.err.println("size: "+renderPanel.getSize().getWidth()+" "+renderPanel.getSize().getHeight());
//			System.err.println("psize: "+renderPanel.getPreferredSize().getWidth()+" "+renderPanel.getPreferredSize().getHeight());
//			System.err.println("msize: "+renderPanel.getMinimumSize().getWidth()+" "+renderPanel.getMinimumSize().getHeight());
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
//			System.err.println("size: "+renderPanel.getSize().getWidth()+" "+renderPanel.getSize().getHeight());
//			System.err.println("psize: "+renderPanel.getPreferredSize().getWidth()+" "+renderPanel.getPreferredSize().getHeight());
//			System.err.println("msize: "+renderPanel.getMinimumSize().getWidth()+" "+renderPanel.getMinimumSize().getHeight());
			pane.invalidate();
			this.svp.setNodeColorManager(this.renderPanel.getNodeColorManager());
			this.svp.repaint();
		}else{
			this.renderPanel.showTree(root, null, null);
			this.svp.setNodeColorManager(null);
			this.svp.repaint();
			
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
		}
	}

}
