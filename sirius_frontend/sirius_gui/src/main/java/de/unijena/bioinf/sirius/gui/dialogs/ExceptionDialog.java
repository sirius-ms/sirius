package de.unijena.bioinf.sirius.gui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ExceptionDialog extends JDialog implements ActionListener{
	
	private JButton ok;
	
	public ExceptionDialog(Frame owner, String message) {
		super(owner,true);
		initDialog(message);
	}
	
	public ExceptionDialog(Dialog owner, String message) {
		super(owner,true);
		initDialog(message);
	}
	
	public void initDialog(String message) {
		
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
		setLocationRelativeTo(getParent());
		this.setVisible(true);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		this.dispose();
	}
	
}
