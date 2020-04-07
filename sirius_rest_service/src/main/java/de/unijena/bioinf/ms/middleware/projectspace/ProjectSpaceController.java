package de.unijena.bioinf.ms.middleware.projectspace;

import de.unijena.bioinf.ms.middleware.BaseApiController;
import de.unijena.bioinf.ms.middleware.SiriusContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping(value = "/api/projects")
public class ProjectSpaceController extends BaseApiController {

    @Autowired
    public ProjectSpaceController(SiriusContext context) {
        super(context);
    }

    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<ProjectSpaceId> getProjectSpaces() {
        return context.listAllProjectSpaces();
    }

    @GetMapping(value = "/{name}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ProjectSpaceId getProjectSpace(@PathVariable String name) {
        return context.getProjectSpace(name).map(x -> new ProjectSpaceId(name, x.getRootPath())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no project space with name '" + name + "'"));
    }

    @PutMapping(value = "/{name}",  produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ProjectSpaceId openProjectSpace(@PathVariable String name, @RequestParam(required = true) Path path) throws IOException {
        return context.openProjectSpace(new ProjectSpaceId(name, path));
    }

    @PostMapping(value = "/new",  produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ProjectSpaceId openProjectSpace(@RequestParam(required = true) Path path) throws IOException {
        final String name = path.getFileName().toString();
        return context.ensureUniqueName(name, (newName)-> {
            try {
                return context.openProjectSpace(new ProjectSpaceId(newName,path));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


}
