package de.unijena.bioinf.ms.middleware.controller;

import de.unijena.bioinf.fingerid.fingerprints.cache.IFingerprinterCache;
import de.unijena.bioinf.ms.middleware.model.reactions.Reaction;
import de.unijena.bioinf.ms.middleware.model.reactions.ReactionRequest;
import de.unijena.bioinf.ms.middleware.service.reactions.ReactionService;
import de.unijena.bioinf.ms.middleware.service.reactions.ReactionSequenceService;
import de.unijena.bioinf.webapi.WebAPI;
import reactionTool.sirius.model.ReactionSequence;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactionTool.sirius.ReactionToolHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/api/reactions")
@Tag(name = "Reactions", description = "[EXPERIMENTAL] Perform chemical reactions.")
@Slf4j
public class ReactionController {

    private final ReactionToolHandler reactionToolHandler;
    private final ReactionService reactionService;
    private final ReactionSequenceService reactionSequenceService;

    @org.springframework.beans.factory.annotation.Autowired
    public ReactionController(WebAPI<?> api, IFingerprinterCache ifpCache, ReactionService reactionService, ReactionSequenceService reactionSequenceService) {
        this.reactionToolHandler = new ReactionToolHandler(api, ifpCache);
        this.reactionService = reactionService;
        this.reactionSequenceService = reactionSequenceService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all reactions from the library.")
    public List<Reaction> getReactions() throws IOException {
        return reactionService.getReactions();
    }

    @GetMapping(value = "/library/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a specific reaction from the library by name.")
    public Reaction getReaction(@PathVariable String name) throws IOException {
        return reactionService.getReaction(name);
    }

    @PostMapping(value = "/library", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a new reaction to the library.")
    public void addReaction(@RequestBody Reaction reaction) throws IOException {
        reactionService.addReaction(reaction);
    }

    @DeleteMapping(value = "/library/{name}")
    @Operation(summary = "Delete a reaction from the library.")
    public void deleteReaction(@PathVariable String name) throws IOException {
        reactionService.deleteReaction(name);
    }

    @GetMapping(value = "/sequences", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all reaction sequences from the library.")
    public List<ReactionSequence> getSequences() throws IOException {
        return reactionSequenceService.getSequences();
    }

    @GetMapping(value = "/sequences/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a specific reaction sequence from the library by name.")
    public ReactionSequence getSequence(@PathVariable String name) throws IOException {
        return reactionSequenceService.getSequence(name);
    }

    @PostMapping(value = "/sequences/library", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a new reaction sequence to the library.")
    public void addSequence(@RequestBody ReactionSequence sequence) throws IOException {
        reactionSequenceService.addSequence(sequence);
    }

    @DeleteMapping(value = "/sequences/library/{name}")
    @Operation(summary = "Delete a reaction sequence from the library.")
    public void deleteSequence(@PathVariable String name) throws IOException {
        reactionSequenceService.deleteSequence(name);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Apply a sequence of reactions to a list of SMILES strings or structures from a database.", description = "[EXPERIMENTAL] Returns the final pool of SMILES strings.")
    public Page<String> applyReactions(@RequestBody ReactionRequest request, @RequestParam(required = false, defaultValue = "1000") int limit) throws IOException {
        List<String> sourceSmiles = new ArrayList<>();

        if (request.getInitialSmiles() != null) {
            sourceSmiles.addAll(request.getInitialSmiles());
        }
        if (request.getDatabaseName() != null && !request.getDatabaseName().isBlank()) {
            sourceSmiles.addAll(reactionToolHandler.extractSmiles(request.getDatabaseName()));
        }

        List<String> resultingSmiles = reactionToolHandler.process(request.getSequence(), sourceSmiles);

        if (request.getProductDatabaseName() != null && !request.getProductDatabaseName().isBlank()) {
            reactionToolHandler.createProductDatabase(resultingSmiles, request.getProductDatabaseName());
        }

        return new PageImpl<>(resultingSmiles.subList(0, resultingSmiles.size()>limit?limit:resultingSmiles.size()), PageRequest.of(0, resultingSmiles.size()>limit?limit:resultingSmiles.size()), resultingSmiles.size());
    }

}
