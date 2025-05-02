/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ExceptionDialog extends JDialog implements ActionListener{
	
	private JButton close;
	protected JPanel south;
	
	public ExceptionDialog(Window owner, String message) {
		this(owner, message, "Error");
	}
	public ExceptionDialog(Window owner, String message, String title) {
		super(owner, DEFAULT_MODALITY_TYPE);
		setTitle(title);
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
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		this.dispose();
	}
	
}
