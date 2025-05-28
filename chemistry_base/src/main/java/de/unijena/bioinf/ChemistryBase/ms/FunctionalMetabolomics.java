package de.unijena.bioinf.ChemistryBase.ms;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FunctionalMetabolomics implements Ms2ExperimentAnnotation {

    public static final String ACCEPT = ":MCheM:accept", REJECT = ":MCheM:reject";
    public static final String PREFIX = ":MCheM:";

    public record ReactionClass(String name, List<Reaction> reactions, double massDelta) {}

    public record Reaction(String reactionSmarts, String eductSmarts, boolean explicitHydrogens) {}

    private final static FunctionalMetabolomics EmptyAnnotation = new FunctionalMetabolomics(new ReactionClass[0]);

    public static FunctionalMetabolomics none() {
        return EmptyAnnotation;
    }

    private final ReactionClass[] reactionClasses;

    //just for Jackson
    private FunctionalMetabolomics() {
        this(new ReactionClass[0]);
    }

    public FunctionalMetabolomics(ReactionClass[] reactionClasses) {
        this.reactionClasses = reactionClasses;
    }

    @JsonIgnore
    public boolean isPresent() {
        return reactionClasses.length>0;
    }

    public List<ReactionClass> getReactionClasses() {
        return Collections.unmodifiableList(Arrays.asList(reactionClasses));
    }

    public static FunctionalMetabolomics parseOnlineReactivityFromMZMineFormat(String value) throws IOException {
        value=value.strip();
        if (value.isEmpty()) return FunctionalMetabolomics.none(); // ignore annotation, allow empty
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(value);
        HashMap<String, FunctionalMetabolomics.ReactionClass> reactions = new HashMap<>();
        if (node.isArray()) {
            for (int k=0; k < node.size(); ++k) {
                parseFmet(node.get(k), reactions);
            }
        } else if (node.isObject()) {
            parseFmet(node, reactions);
        } else {
            throw new IOException("Expect either an array or an JSON object as value for the >online-reactivity annotation");
        }
        return new FunctionalMetabolomics(reactions.values().toArray(ReactionClass[]::new));
    }

    private static void parseFmet(JsonNode node, HashMap<String, FunctionalMetabolomics.ReactionClass> classes) {
        String reactionName = node.get("reaction_name").asText();
        double deltaMz = node.get("delta_mz").asDouble(Double.NaN);
        ReactionClass reactionClass = classes.computeIfAbsent(reactionName, (x) -> new ReactionClass(reactionName, new ArrayList<>(), deltaMz));
        String eductSmarts = node.get("educt_smarts").asText();
        String reactionSmarts = node.get("reaction_smarts").asText(null);
        reactionClass.reactions.add(new Reaction(reactionSmarts, eductSmarts, true));
    }

    public String toString() {
        if (this == none())
            return "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @DefaultInstanceProvider
    public static FunctionalMetabolomics fromString(@DefaultProperty @Nullable String value) {
        if (Utils.isNullOrBlank(value))
            return none();

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(value, FunctionalMetabolomics.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
