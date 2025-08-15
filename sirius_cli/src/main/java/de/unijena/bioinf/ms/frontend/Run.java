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
import de.unijena.bioinf.ms.frontend.subtools.login.LoginOptions;
import de.unijena.bioinf.ms.frontend.workflow.InstanceBufferFactory;
import de.unijena.bioinf.ms.frontend.workflow.SimpleInstanceBuffer;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    protected CommandLine.ParseResult result;
    protected Workflow flow;
    private final WorkflowBuilder builder;
    private final boolean checkPermissions;

    public Run(WorkflowBuilder builder, boolean checkPermissions) {
        this.checkPermissions = checkPermissions;
        this.builder = builder;
        this.builder.initRootSpec();
    }


    public void compute() {
        if (!isWorkflowDefined())
            throw new IllegalStateException("No Workflow defined for computation.");
        flow.run();
    }

    public boolean isWorkflowDefined() {
        return flow != null;
    }

    /*Returns true if a workflow was parsed*/
    public CommandLine.ParseResult parseArgs(String[] args) {
        if (args == null || args.length < 1)
            args = new String[]{"--help"};

        if (!List.of(args).contains("login") && !List.of(args).contains("password") && !List.of(args).contains("pwd"))
            logger.info("Running with following arguments: {}", Arrays.toString(args));
        final CommandLine commandline = new CommandLine(builder.getRootSpec());
        commandline.setCaseInsensitiveEnumValuesAllowed(true);
        commandline.registerConverter(DefaultParameter.class, new DefaultParameter.Converter());
        result = parseResultAndCheckPermission(commandline, commandline.parseArgs(args));
        return result;
    }

    private CommandLine.ParseResult parseResultAndCheckPermission(@NotNull final CommandLine commandline, @NotNull final CommandLine.ParseResult result) {
        if (!checkPermissions)
            return result;
        if (result.isUsageHelpRequested() || result.isVersionHelpRequested())
            return result;
        if (result.hasSubcommand() && result.subcommand().commandSpec().commandLine().getCommand() instanceof LoginOptions)
            return result;

        String message = null;
        WebAPI webApi = ApplicationCore.WEB_API;
        if (!webApi.getAuthService().isLoggedIn()) {
            message = "Login ERROR: Please Login to use the SIRIUS command line tool!";

            System.err.println();
            logger.error(message);

            System.out.println();
            System.out.println(message);
            //todo retrieve from option classes to make this refactorable
            return commandline.parseArgs("--noCite", "login", "-h");
        }

        @Nullable Subscription sub = webApi.getActiveSubscription();

        if (sub == null) {
            message = "License ERROR: No active subscription found, please request and/or select a valid subscription!";
        } else if (sub.isNotStarted()) {
            message = "License ERROR: Your active subscription has not yet started! Please select a different subscription or renew the active one!";
        } else if (sub.isExpired()) {
            message = "License ERROR: Your active subscription is expired! Please select a different subscription or renew the active one!";
        } else if (sub.getAllowedFeatures() == null) {
            message = "License ERROR: Could not find allowed features in active subscription. This is likely a bug. Please contact Support!";
        } else if (!sub.getAllowedFeatures().cli()) {
            message = "License ERROR: Command line tool usage is not permitted by your active subscription. Please select a different subscription or upgrade the active one!";
        }

        if (message != null) {
            System.err.println();
            logger.error(message);

            System.out.println();
            System.out.println(message);

            //todo retrieve from option classes to make this refactorable
            return commandline.parseArgs("--noCite", "login", "--show", "--license-info");
        }

        return result;
    }

    public Workflow makeWorkflow() {
        return makeWorkflow(new SimpleInstanceBuffer.Factory());
    }

    public Workflow makeWorkflow(@NotNull InstanceBufferFactory<?> bufferFactory) {
        flow = builder.makeParseResultHandler(bufferFactory).handleParseResult(result);
        return flow;
    }

    public void cancel() {
        if (flow != null)
            flow.cancel();
    }
}
