package de.unijena.bioinf.projectspace;

import java.util.EventListener;

public interface ProjectSpaceListener extends EventListener {

    public void projectSpaceChanged(ProjectSpaceEvent event);

}
