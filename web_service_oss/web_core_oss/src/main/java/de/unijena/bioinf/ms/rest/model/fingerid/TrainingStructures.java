package de.unijena.bioinf.ms.rest.model.fingerid;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
@AllArgsConstructor
public class TrainingStructures {
        private final Set<String> kernelInchiKeys;
        private final Set<String> extraInchiKeys;
    }