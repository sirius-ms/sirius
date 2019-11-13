package de.unijena.bioinf.ms.gui.dialogs.input;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DragAndDropOpenDialog extends JDialog implements ActionListener{
	public enum ReturnValue {
		oneExperimentForAll, oneExperimentPerFile, abort
	}

	private JButton batchB, expB, abortB;
	
	private ReturnValue rv;

	public DragAndDropOpenDialog(JFrame owner) {
		super(owner,true);
		
		rv = ReturnValue.abort;
		
		this.setLayout(new BorderLayout());
		JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,20,10));
		Icon icon = UIManager.getIcon("OptionPane.questionIcon");
		northPanel.add(new JLabel(icon));
		northPanel.add(new JLabel("How should the files be imported?"));
		
		
		this.add(northPanel,BorderLayout.CENTER);
		JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		expB = new JButton("As one experiment");
		expB.addActionListener(this);
		batchB = new JButton("As multiple experiments");
		batchB.addActionListener(this);
		abortB = new JButton("Abort");
		abortB.addActionListener(this);
		south.add(expB);
		south.add(batchB);
		south.add(abortB);
		this.add(south,BorderLayout.SOUTH);
		this.pack();
		setLocationRelativeTo(getParent());
		this.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==abortB){
			this.rv = ReturnValue.abort;
		}else if(e.getSource()==batchB){
			this.rv = ReturnValue.oneExperimentPerFile;
		}else if(e.getSource()==expB){
			this.rv = ReturnValue.oneExperimentForAll;
		}
		this.dispose();
	}
	
	public ReturnValue getReturnValue(){
		return this.rv;
	}

}
