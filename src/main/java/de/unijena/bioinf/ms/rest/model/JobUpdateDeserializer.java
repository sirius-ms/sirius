package de.unijena.bioinf.ms.rest.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JobUpdateDeserializer extends JsonDeserializer<JobUpdate<?>> {
    @Override
    public JobUpdate<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final TreeNode tree = mapper.readTree(p);
        final JobUpdateBase baseInfo = mapper.readValue(tree.traverse(), JobUpdateBase.class);
        final Object data = mapper.readValue(tree.get("data").traverse(), baseInfo.jobId.jobTable.classRef);
        return new JobUpdate<>(baseInfo, data);
    }


}
