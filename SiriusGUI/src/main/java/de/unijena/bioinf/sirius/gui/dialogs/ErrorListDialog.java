package de.unijena.bioinf.sirius.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

public class ErrorListDialog extends JDialog implements ActionListener{

	private JButton ok;
	private JTextArea ta;
	
	
	public ErrorListDialog(Dialog owner, List<String> errors){
		super(owner,true);
		initDialog(errors);
	}
	
	public ErrorListDialog(Frame owner, List<String> errors) {
		super(owner,true);
		initDialog(errors);
	}
	
	public void initDialog(List<String> errors) {
		
		this.setLayout(new BorderLayout());
//		JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,20,10));
		JPanel northPanel = new JPanel(new BorderLayout());
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<errors.size()-1;i++){
			sb.append(errors.get(i)+"\n");
		}
		sb.append(errors.get(errors.size()-1));
		ta = new JTextArea(sb.toString());
		ta.setEditable(false);
		Icon icon = UIManager.getIcon("OptionPane.errorIcon");
		JPanel iconPanel = new JPanel(new BorderLayout());
		iconPanel.add(new JLabel(icon),BorderLayout.WEST);
		iconPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
		northPanel.add(iconPanel,BorderLayout.WEST);
//		ta.setBackground(new Color((SystemColor.control).getRGB()));
		
		
		JPanel northEastPanel = new JPanel(new BorderLayout());
		northEastPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		JPanel temp = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		temp.add(new JLabel(errors.size()+" errors have occurred."));
		northEastPanel.add(temp,BorderLayout.NORTH);
		JScrollPane jsp = new JScrollPane(ta,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		northEastPanel.add(jsp,BorderLayout.CENTER);
		northPanel.add(northEastPanel,BorderLayout.CENTER);
		this.add(northPanel,BorderLayout.CENTER);
		JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		ok = new JButton("Ok");
		ok.addActionListener(this);
		south.add(ok);
		this.add(south,BorderLayout.SOUTH);
		this.pack();
		int newHeight = Math.min(240,(int) this.getPreferredSize().getHeight());
		this.setSize(new Dimension(600,newHeight));
		setLocationRelativeTo(getParent());
		this.setVisible(true);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		this.dispose();
	}

}
