/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.persistence.model;

import one.microstream.persistence.util.Reloader;
import one.microstream.storage.embedded.types.EmbeddedStorage;
import one.microstream.storage.embedded.types.EmbeddedStorageManager;

public class MicroStreamProjectDb implements AutoCloseable {
    final EmbeddedStorageManager storageManager;
    final Reloader reloader;
    final MsProject msProject;

    public MicroStreamProjectDb() {
        this(EmbeddedStorage.start());
    }
    public MicroStreamProjectDb(EmbeddedStorageManager storageManager) {
        this.storageManager = storageManager;
        this.reloader = Reloader.New(storageManager.persistenceManager());
        Object tmpRoot = storageManager.root();
        if (tmpRoot != null){
            if (tmpRoot instanceof MsProject)
                msProject = (MsProject) tmpRoot;
            else
                throw new IllegalArgumentException("Database root is not of type: " + MsProject.class.getName() + " but of type " + (tmpRoot.getClass().getName()));
        }else {
            msProject = new MsProject();
            storageManager.setRoot(msProject);
            storageManager.storeRoot();
        }
    }

    public MsProject getProject() {
        return msProject;
    }

    public EmbeddedStorageManager getStorageManager() {
        return storageManager;
    }

    public <T> T revert(T objectToRevert){
        return reloader.reloadFlat(objectToRevert);
    }

    @Override
    public void close() throws Exception {
        storageManager.storeRoot();//todo needed?
        storageManager.close();
    }
}
