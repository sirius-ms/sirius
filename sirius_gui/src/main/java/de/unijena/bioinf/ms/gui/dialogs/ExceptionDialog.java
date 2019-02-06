package de.unijena.bioinf.ms.gui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ExceptionDialog extends JDialog implements ActionListener{
	
	private JButton close;
	protected JPanel south;
	
	public ExceptionDialog(Frame owner, String message) {
		super(owner,true);
		initDialog(message);
		this.setVisible(true);
	}
	
	public ExceptionDialog(Dialog owner, String message) {
		super(owner,true);
		initDialog(message);
		this.setVisible(true);
	}
	
	public void initDialog(String message) {
		
		this.setLayout(new BorderLayout());
		JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,20,10));
		Icon icon = UIManager.getIcon("OptionPane.errorIcon");
		northPanel.add(new JLabel(icon));
		northPanel.add(new JLabel(message));
		this.add(northPanel,BorderLayout.CENTER);
		south = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		close = new JButton("close");
		close.addActionListener(this);
		south.add(close);
		this.add(south,BorderLayout.SOUTH);
		this.pack();
		setLocationRelativeTo(getParent());

		// TODO Auto-generated constructor stub
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		this.dispose();
	}
	
}
