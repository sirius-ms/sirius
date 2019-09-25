package de.unijena.bioinf.ms.gui.compute.jjobs;

import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.SwingJJobContainer;

public interface GuiObservableJJob<R> extends JJob<R> {
    SwingJJobContainer<R> asSwingJob();
}
