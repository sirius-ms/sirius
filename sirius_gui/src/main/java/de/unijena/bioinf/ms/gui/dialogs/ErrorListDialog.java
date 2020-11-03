/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

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
