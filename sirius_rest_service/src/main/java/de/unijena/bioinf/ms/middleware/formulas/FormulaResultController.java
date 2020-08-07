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

package de.unijena.bioinf.ms.middleware.formulas;

import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.middleware.BaseApiController;
import de.unijena.bioinf.ms.middleware.SiriusContext;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/projects/{pid}/compounds/{cid}")
public class FormulaResultController extends BaseApiController {

    @Autowired
    public FormulaResultController(SiriusContext context) {
        super(context);
    }

    @GetMapping(value = "/formulas", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<FormulaId> getFormulaIds(@PathVariable String pid, @PathVariable String cid) {
        SiriusProjectSpace space = projectSpace(pid);
        return space.findCompound(cid).map(ccId -> {
            try {
                return space.getCompound(ccId);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).map(con -> con.getResults().values().stream().map(FormulaId::new).collect(Collectors.toList())).orElse(Collections.emptyList());
    }

    @GetMapping(value = "/formulas/{fid}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public FormulaId getFormulaResult(@PathVariable String pid, @PathVariable String cid, @PathVariable String fid) {
        SiriusProjectSpace space = projectSpace(pid);
        return getCompound(space, cid).map(c ->
                c.findResult(fid).map(ffid -> {
                    try {
                        return space.getFormulaResult(ffid, FormulaScoring.class);
                    } catch (IOException e) {
                        return null;
                    }
                }).orElse(null)).
                map(fr -> {
                    FormulaId formulaId = new FormulaId(fr.getId());
                    fr.getAnnotation(FormulaScoring.class).ifPresent(fs -> formulaId.setResultScores(new FormulaResultScores(fs)));
                    return formulaId;
        }).orElse(null);
    }

    private Optional<CompoundContainer> getCompound(SiriusProjectSpace space, String cid, Class<? extends DataAnnotation>... components) {
        return space.findCompound(cid).map(ccId -> {
            try {
                return space.getCompound(ccId, components);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });
    }


}

