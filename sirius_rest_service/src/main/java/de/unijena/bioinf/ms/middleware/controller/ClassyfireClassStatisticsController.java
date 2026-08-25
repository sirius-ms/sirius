package de.unijena.bioinf.ms.middleware.controller;

import de.unijena.bioinf.ms.middleware.controller.mixins.CompoundClassStatisticsController;
import de.unijena.bioinf.ms.middleware.model.compute.Job;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.QuantRowType;
import de.unijena.bioinf.ms.middleware.model.statistics.FoldChangeJobSubmission;
import de.unijena.bioinf.ms.middleware.model.statistics.StatisticsTable;
import de.unijena.bioinf.ms.middleware.service.compute.ComputeService;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;

@RestController
@RequestMapping(value = "/api/projects/{projectId}/classyfire-classes/statistics")
@Tag(name = "ClassyFire Class Statistics", description = "[EXPERIMENTAL] ...")
public class ClassyfireClassStatisticsController implements CompoundClassStatisticsController<AlignedFeature> {

    @Getter
    private final ComputeService computeService;
    @Getter
    private final ProjectsProvider<?> projectsProvider;

    @Autowired
    public ClassyfireClassStatisticsController(ComputeService computeService, ProjectsProvider<?> projectsProvider) {
        this.computeService = computeService;
        this.projectsProvider = projectsProvider;
    }

    @Override
    public QuantRowType getTarget() {
        return QuantRowType.CLASSYFIRE_CLASSES;
    }

    @Operation(operationId = "computeClassyfireClassFoldChangesExperimental")
    @Override
    public Job computeFoldChanges(String projectId, FoldChangeJobSubmission jobSubmission, EnumSet<Job.OptField> optFields) {
        return CompoundClassStatisticsController.super.computeFoldChanges(projectId, jobSubmission, optFields);
    }

    @Operation(operationId = "deleteClassyfireClassFoldChangesExperimental")
    @Override
    public void deleteFoldChanges(String projectId, @NotNull String leftGroupName, @NotNull String rightGroupName, AggregationType aggregation, QuantMeasure quantification) {
        CompoundClassStatisticsController.super.deleteFoldChanges(projectId, leftGroupName, rightGroupName, aggregation, quantification);
    }

    @Operation(operationId = "getClassyfireClassFoldChangeTableExperimental")
    @Override
    public StatisticsTable getFoldChangeTable(String projectId, AggregationType aggregation, QuantMeasure quantification) {
        return CompoundClassStatisticsController.super.getFoldChangeTable(projectId, aggregation, quantification);
    }
}