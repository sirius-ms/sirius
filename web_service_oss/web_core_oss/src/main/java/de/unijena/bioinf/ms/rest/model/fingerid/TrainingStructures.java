package de.unijena.bioinf.ms.rest.model.fingerid;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.Set;

@Getter
@Builder
@Jacksonized
public class TrainingStructures {
        private final Set<String> kernelInchiKeys;
        private final Set<String> extraInchiKeys;

        public TrainingStructures(Set<String> kernelInchiKeys, Set<String> extraInchiKeys) {
            this.kernelInchiKeys = kernelInchiKeys;
            this.extraInchiKeys = extraInchiKeys;
        }
    }