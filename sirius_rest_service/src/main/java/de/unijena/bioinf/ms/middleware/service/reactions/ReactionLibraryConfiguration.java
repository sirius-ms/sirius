package de.unijena.bioinf.ms.middleware.service.reactions;

import de.unijena.bioinf.ms.frontend.core.Workspace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactionTool.sirius.library.ReactionLibrary;

/**
 * Wires the framework-independent {@link ReactionLibrary} as a Spring-managed singleton. This is the
 * only place that knows where the database file lives: the library itself takes the path as a
 * constructor argument and has no dependency on Spring or the SIRIUS workspace. Spring closes the
 * library on context shutdown via the declared destroy method.
 */
@Configuration
public class ReactionLibraryConfiguration {

    @Bean(destroyMethod = "close")
    public ReactionLibrary reactionLibrary() {
        return new ReactionLibrary(Workspace.WORKSPACE.resolve("reactions.db"));
    }
}
