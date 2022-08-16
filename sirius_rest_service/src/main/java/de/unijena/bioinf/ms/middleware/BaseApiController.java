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

package de.unijena.bioinf.ms.middleware;

import de.unijena.bioinf.projectspace.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@RequestMapping("/api")
public class BaseApiController {

    protected final SiriusContext context;

    public BaseApiController(SiriusContext context) {
        this.context = context;
    }

    protected ProjectSpaceManager<?> projectSpace(String pid) {
        return context.getProjectSpace(pid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no project space with name '" + pid + "'"));
    }

    protected Instance loadInstance(String pid, String cid) {
        return loadInstance(projectSpace(pid), cid);
    }

    protected Instance loadInstance(ProjectSpaceManager<?> ps, String cid) {
        return ps.getInstanceFromCompound(parseCID(ps, cid));
    }

    protected CompoundContainerId parseCID(String pid, String cid) {
        return parseCID(projectSpace(pid), cid);
    }

    protected CompoundContainerId parseCID(ProjectSpaceManager<?> ps, String cid) {
        return ps.projectSpace().findCompound(cid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no Compound with ID '" + cid + "' in project with name '" + ps.projectSpace().getLocation() + "'"));
    }

    protected FormulaResultId parseFID(String pid, String cid, String fid) {
        return parseFID(loadInstance(pid, cid), fid);
    }

    protected FormulaResultId parseFID(Instance instance, String fid) {
        return instance.loadCompoundContainer().findResult(fid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FormulaResult with FID '" + fid + "' not found!"));

    }

    protected String idString(String pid, String cid, String fid){
        return "'" + pid +"/"+ cid + "/" + fid + "'";
    }
}
