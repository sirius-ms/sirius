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

package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.jjobs.BasicJJob;

/**
 * Only one preprocessing Subtool is allowed per Workflow. They can only run in last position
 * These tools are intended to transform some Instances into some non standard output or to do some cleanup
 * The default here is to write summaries. So changing the default here can be used to change summary writing
 * behavior
 */
public abstract class PreprocessingJob<P extends Iterable<? extends Instance>> extends BasicJJob<P> {
//    protected InputFilesOptions input;
//    protected ProjectSpaceManager space;

    public PreprocessingJob() {
                super(JobType.SCHEDULER);

    }

    /*public PreprocessingJob(@Nullable InputFilesOptions input, @Nullable ProjectSpaceManager space) {
        super(JobType.SCHEDULER);
//        this.input = input;
//        this.space = space;
    }*/

/*    public void setInput(InputFilesOptions input) {
        this.input = input;
    }

    public void setProjectSpace(ProjectSpaceManager space) {
        this.space = space;
    }*/
}
