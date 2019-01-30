package de.unijena.bioinf.ms.io.load;

import de.unijena.bioinf.ms.gui.utils.ReturnValue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ExperimentNameDialog extends JDialog implements ActionListener{

	private JTextField tf;
	private JButton ok, abort;
	private String text;
	
	private ReturnValue value;
	
	public ExperimentNameDialog(JDialog owner,String name) {
		super(owner,"Compound name",true);
		value = ReturnValue.Abort;
		this.setLayout(new BorderLayout());
		tf = new JTextField(20);
		ok = new JButton("Ok");
		ok.addActionListener(this);
		abort = new JButton("Abort");
		abort.addActionListener(this);
		JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		north.add(tf);
		this.add(north,BorderLayout.NORTH);
		JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		south.add(ok);
		south.add(abort);
		this.add(south,BorderLayout.SOUTH);
		this.pack();
		tf.setText(name);
		setLocationRelativeTo(getParent());
		this.setVisible(true);
	}
	
	public ReturnValue getReturnValue(){
		return value;
	}
	
	public String getNewName(){
		return text;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==ok){
			text = tf.getText();
			value = ReturnValue.Success;
			this.setVisible(false);
		}else{
			text = tf.getText();
			value = ReturnValue.Abort;
			this.setVisible(false);
		}
	}

}
