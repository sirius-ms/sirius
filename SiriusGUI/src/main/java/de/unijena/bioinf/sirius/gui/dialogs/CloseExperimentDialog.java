package de.unijena.bioinf.sirius.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import de.unijena.bioinf.sirius.gui.structure.ReturnValue;

public class CloseExperimentDialog extends JDialog implements ActionListener{

	private CloseDialogReturnValue rv;
	
	private JButton save,no,abort;
	
	public CloseExperimentDialog(Frame owner, String question) {
		super(owner,true);
		
		rv = CloseDialogReturnValue.abort;
		
		this.setLayout(new BorderLayout());
		JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,20,10));
		Icon icon = UIManager.getIcon("OptionPane.questionIcon");
		northPanel.add(new JLabel(icon));
		northPanel.add(new JLabel(question));
		this.add(northPanel,BorderLayout.CENTER);
		JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		save = new JButton("Save");
		save.addActionListener(this);
		no = new JButton("No");
		no.addActionListener(this);
		abort = new JButton("Abort");
		abort.addActionListener(this);
		south.add(save);
		south.add(no);
		south.add(abort);
		this.add(south,BorderLayout.SOUTH);
		this.pack();
		setLocationRelativeTo(getParent());
		this.setVisible(true);
		// TODO Auto-generated constructor stub
	}
	
	public CloseDialogReturnValue getReturnValue(){
		return rv;
	}



	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==save){
			rv = CloseDialogReturnValue.save;
		}else if(e.getSource()==no){
			rv = CloseDialogReturnValue.no;
		}else{
			rv = CloseDialogReturnValue.abort;
		}
		this.dispose();
	}

}
