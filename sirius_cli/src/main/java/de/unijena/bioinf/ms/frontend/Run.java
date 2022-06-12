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

package de.unijena.bioinf.ms.frontend;

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.List;


/**
 * This is our Commandline tool
 * <p>
 * Here we parse parameters, configure technical stuff,
 * read input, merge workspace, configure Algorithms/Workflows and write output.
 * <p>
 * Basic Idea:
 * <p>
 * Note: A project-space can be input and output at the same time!
 * Some methods will use it as input and check whether
 * the needed input is present (e.g Zodiac).
 * Other Methods only produce output to the project-space (e.g. SIRIUS).
 * So they need to merge their results with the existing ones.
 */
public class Run extends ApplicationCore {
    protected final static Logger logger = LoggerFactory.getLogger(Run.class);
    protected Workflow flow;
    private final WorkflowBuilder<?> builder;

    public Run(WorkflowBuilder<?> builder) {
        this.builder = builder;
        this.builder.initRootSpec();
    }


    public void compute() {
        if (!isWorkflowDefined())
            throw new IllegalStateException("No Workflow defined for computation.");
        flow.run();
    }

    public boolean isWorkflowDefined(){
        return flow != null;
    }

    /*Returns true if a workflow was parsed*/
    public boolean parseArgs(String[] args) {
        if (args == null || args.length < 1)
            args = new String[]{"--help"};
        logger.info("Running with following arguments: " + Arrays.toString(args));
        final CommandLine commandline = new CommandLine(builder.getRootSpec());
        commandline.setCaseInsensitiveEnumValuesAllowed(true);
        commandline.registerConverter(DefaultParameter.class, new DefaultParameter.Converter());
        flow = commandline.parseWithHandler(builder.makeParseResultHandler(), args);
        List<Exception> l = commandline.getParseResult().errors();
        CommandLine.ParseResult r = commandline.getParseResult();
        return flow != null; //todo maybe workflow validation would be nice here???
    }

    public void cancel() {
        if (flow != null)
            flow.cancel();
    }

    public Workflow getFlow() {
        return flow;
    }
}
