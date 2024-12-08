package de.unijena.bioinf.ms.middleware.model.compute;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobSubmissionTest {

    @Test
    void mergeCombinedConfigMap() {
        JobSubmission defaultConfig = JobSubmission.createDefaultInstance(true);
        assertNotNull(defaultConfig.getConfigMap());
        assertEquals("default", defaultConfig.getConfigMap().get("AlgorithmProfile"));
        defaultConfig.mergeCombinedConfigMap();
        assertEquals("QTOF", defaultConfig.getConfigMap().get("AlgorithmProfile"));
    }
}