package de.unijena.bioinf.ms.middleware.controller;

import de.unijena.bioinf.fingerid.fingerprints.cache.IFingerprinterCache;
import de.unijena.bioinf.ms.middleware.model.reactions.ReactionRequest;
import de.unijena.bioinf.webapi.WebAPI;
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

    public ReactionController(WebAPI<?> api, IFingerprinterCache ifpCache) {
        this.reactionToolHandler = new ReactionToolHandler(api, ifpCache);
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
        System.out.println(resultingSmiles.size());

        if (request.getProductDatabaseName() != null && !request.getProductDatabaseName().isBlank()) {
            reactionToolHandler.createProductDatabase(resultingSmiles, request.getProductDatabaseName());
        }

        return new PageImpl<>(resultingSmiles.subList(0, resultingSmiles.size()>limit?limit:resultingSmiles.size()), PageRequest.of(0, resultingSmiles.size()>limit?limit:resultingSmiles.size()), resultingSmiles.size());
    }

}
