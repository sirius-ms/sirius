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

public class FilePresentDialog extends QuestionDialog{
	
	public FilePresentDialog(Frame owner, String name) {
		super(owner,"The file \""+name+"\" is already present. Override it?");
	}
	
}