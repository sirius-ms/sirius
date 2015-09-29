package de.unijena.bioinf.sirius.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import de.unijena.bioinf.sirius.gui.structure.ReturnValue;

public class DragAndDropOpenDialog extends JDialog implements ActionListener{
	
	private JButton batchB, expB, abortB;
	
	private DragAndDropOpenDialogReturnValue rv;

	public DragAndDropOpenDialog(JFrame owner) {
		super(owner,true);
		
		rv = DragAndDropOpenDialogReturnValue.abort;
		
		this.setLayout(new BorderLayout());
		JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,20,10));
		Icon icon = UIManager.getIcon("OptionPane.questionIcon");
		northPanel.add(new JLabel(icon));
		northPanel.add(new JLabel("How should the files be imported?"));
		
		
		this.add(northPanel,BorderLayout.CENTER);
		JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		expB = new JButton("One experiment for all files");
		expB.addActionListener(this);
		batchB = new JButton("One experiment per file");
		batchB.addActionListener(this);
		abortB = new JButton("Abort");
		abortB.addActionListener(this);
		south.add(expB);
		south.add(batchB);
		south.add(abortB);
		this.add(south,BorderLayout.SOUTH);
		this.pack();
		this.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==abortB){
			this.rv = DragAndDropOpenDialogReturnValue.abort;
		}else if(e.getSource()==batchB){
			this.rv = DragAndDropOpenDialogReturnValue.oneExperimentPerFile;
		}else if(e.getSource()==expB){
			this.rv = DragAndDropOpenDialogReturnValue.oneExperimentForAll;
		}
		this.dispose();
	}
	
	public DragAndDropOpenDialogReturnValue getReturnValue(){
		return this.rv;
	}

}
