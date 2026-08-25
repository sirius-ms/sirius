# SIRIUS Java SDK
This is the SIRIUS Software development kit (SDK) for Java to connect to the generic Sirius Nightsky API.
It provides models and clients to interact with the API as well as features to start/stop/locate the SIRIUS application.

This SDK is intended to be shipped with your software to connect to a SIRIUS installation on the user system. It
does **not** contain the SIRIUS Software itself or any algorithms/methods.

The class `SiriusSDK` serves a single entry point to all features. It allows you to start SIRIUS and gives access all
API endpoints.

## Usage
Detailed documentation about the different endpoints and models can be found in
[sirius-sdk.openapi](sirius-sdk.openapi/README.md): an index of every endpoint and every model, generated
from the API specification and therefore always current for this version. The endpoint table links to
[per-endpoint](sirius-sdk.openapi/docs) pages with parameters, return types and status codes.

### Example Code

```java
        // Search and Start SIRIUS installation in background.
        // If a compatible SIRIUS instance is already running this will be used instead
        // Closing the SiriusSDK shuts down SIRIUS depending on the ShutdownMode.
        try (SiriusSDK sirius = SiriusSDK.startAndConnectLocally(SiriusSDK.ShutdownMode.AUTO, false)) {
            // print some infos about the SIRIUS instance
            System.out.println(sirius.infos().getInfo(null, null));
            ProjectInfo project = sirius.projects().createProject("myProject", "/tmp/" + UUID.randomUUID(), null);

            // Import peak-list data from files.
            sirius.projects().importPreprocessedData(project.getProjectId(), List.of(
                    new File("/mydata/spectra1.mgf"),
                    new File("/mydata/spectra1.ms"),
                    new File("/mydata/spectra1.cef")
            ), true, true);
            
            //get default compute configuration/parameters from SIRIUS (optional)
            JobSubmission sub = sirius.jobs().getDefaultJobConfig(false, null, null);
            sub.getZodiacParams().setEnabled(false); //enable/disable tools

            //submit job to be computed in background
            Job job = sirius.jobs().startJob(project.getProjectId(), sub, null);
            job = sirius.awaitJob(project.getProjectId(), job.getId());

            // fetch results from the project
            sirius.features().getAlignedFeatures(project.getProjectId(), null, List.of(AlignedFeatureOptField.TOP_ANNOTATIONS))
                    .forEach(System.out::println);

        } catch (Exception e) {
            e.printStackTrace();
        }
```
### Import Option 1:
Import peak-list data from files. Can be executed synchronously or asynchronously as a job
```java
sirius.projects().importPreprocessedData(project.getProjectId(), List.of(
    new File("/mydata/spectra1.mgf"),
    new File("/mydata/spectra1.ms"),
    new File("/mydata/spectra1.cef")
), true, true);
```
### Import Option 2:
Import LCMS Runs from file and Find and Align features during import. Can be executed synchronously
(`importMsRunData`) or asynchronously as a job (`importMsRunDataAsJob`). Feature finding and alignment are
configured through `LcmsSubmissionParameters` instead of positional flags. A fresh instance is a sensible
default: unset mass deviations, noise intensity and retention-time tolerance are estimated from the data,
which is what SIRIUS recommends. Set `sampleNames`/`sampleTypes` if you need to map results back to your own
sample identifiers or have blank runs taken into account.
```java
Job importJob = sirius.projects().importMsRunDataAsJob(project.getProjectId(), List.of(
    new File("/myRuns/run1a.mzml"),
    new File("/myRuns/run2a.mzml"),
    new File("/myRuns/run1b.mzml"),
    new File("/myRuns/run2b.mzml")
), new LcmsSubmissionParameters().alignLCMSRuns(true), null);
```
### Import Option 3:
Direct data import. Synchronously only.
```java
FeatureImport featureToImport = new FeatureImport()
                    .charge(1)
                    .addDetectedAdductsItem("[M+H]+")
                    .externalFeatureId("MyIdForMapping").ionMass(285.0787)
                    .mergedMs1(
                            new BasicSpectrum()
                                    .addPeaksItem(new SimplePeak().mz(285.0789).intensity(210252.13))
                                    .addPeaksItem(new SimplePeak().mz(286.0822).intensity(36264.31))
                                    .addPeaksItem(new SimplePeak().mz(287.0766).intensity(70364.01))
                                    .addPeaksItem(new SimplePeak().mz(288.0791).intensity(12274.46))
                                    .addPeaksItem(new SimplePeak().mz(289.0840).intensity(1037.72)))
                    .addMs2SpectraItem(
                            new BasicSpectrum()
                                    .precursorMz(285.07872)
                                    .addPeaksItem(new SimplePeak().mz(91.0545).intensity(317.62))
                                    .addPeaksItem(new SimplePeak().mz(105.0333).intensity(503.78))
                                    .addPeaksItem(new SimplePeak().mz(154.0415).intensity(3030.97))
                                    .addPeaksItem(new SimplePeak().mz(167.0116).intensity(240.42))
                                    .addPeaksItem(new SimplePeak().mz(172.0628).intensity(297.89))
                                    .addPeaksItem(new SimplePeak().mz(179.0369).intensity(207.02))
                                    .addPeaksItem(new SimplePeak().mz(180.0199).intensity(349.96))
                                    .addPeaksItem(new SimplePeak().mz(182.0367).intensity(780.00))
                                    .addPeaksItem(new SimplePeak().mz(193.0883).intensity(1824.38))
                                    .addPeaksItem(new SimplePeak().mz(221.1065).intensity(307.91))
                                    .addPeaksItem(new SimplePeak().mz(222.1147).intensity(2002.34))
                                    .addPeaksItem(new SimplePeak().mz(228.0573).intensity(1800.88))
                                    .addPeaksItem(new SimplePeak().mz(241.0527).intensity(301.77))
                                    .addPeaksItem(new SimplePeak().mz(255.0662).intensity(207.54))
                                    .addPeaksItem(new SimplePeak().mz(257.0839).intensity(3000.70))
                                    .addPeaksItem(new SimplePeak().mz(285.0787).intensity(18479.91))
                                    .addPeaksItem(new SimplePeak().mz(285.2895).intensity(268.90)));

List<AlignedFeature> importedFeatures = sirius.features()
    .addAlignedFeatures(project.getProjectId(), List.of(featureToImport), null, null);
```

## Connecting to SIRIUS
`SiriusSDK` covers the cases where the SDK manages the SIRIUS process for you; `SiriusClient`, its
superclass, covers the case where something else does.

```java
// Start a SIRIUS installation found on this system, or reuse a compatible one that already runs.
SiriusSDK started = SiriusSDK.startAndConnectLocally(SiriusSDK.ShutdownMode.AUTO, false);

// Start against an isolated workspace, so the instance shares no state with the user's own SIRIUS.
SiriusSDK isolated = SiriusSDK.startAndConnectLocallyIsolated(
        SiriusSDK.ShutdownMode.ALWAYS, false, true, false, null, Path.of("/tmp/my-workspace"));

// Attach to a running instance without ever starting one. Returns null if none is reachable.
SiriusSDK found = SiriusSDK.findAndConnectLocally(SiriusSDK.ShutdownMode.NEVER, false);

// Talk to an instance whose address you already know - another machine, a container, a fixed port.
SiriusClient remote = new SiriusClient("http://sirius-host:8080", null);
```

Note that a locally started SIRIUS binds a free port rather than a fixed one, so discover the address via
`getBasePath()` instead of assuming a port.

`ShutdownMode` decides what closing the client does to the SIRIUS process:

| Mode | Effect on `close()` |
|---|---|
| `AUTO` | Shuts SIRIUS down only if this client started it |
| `ALWAYS` | Shuts SIRIUS down either way |
| `NEVER` | Leaves SIRIUS running either way |

## Waiting for jobs
Computations are submitted as jobs and run in the background. `awaitJob` polls one until it reaches a
terminal state; `awaitAndDeleteJob` additionally removes it from the job list afterwards. The long
overloads add a timeout and an `InterruptionCheck` so waiting stays cancellable from your own code.

```java
Job done = sirius.awaitJob(projectId, job.getId());

Job detailed = sirius.awaitJob(projectId, job.getId(), 2, 600, false, true, () -> {
    if (Thread.currentThread().isInterrupted())
        throw new InterruptedException("Cancelled by caller.");
});
```

## Listening to events
Instead of polling, you can subscribe to the server-sent-event stream and be told when jobs progress,
projects change or an import finishes. Call `enableEventListening` first - no events are delivered before
the stream is open - then register listeners per project.

```java
sirius.enableEventListening(DataEventType.JOB, DataEventType.PROJECT);

PropertyChangeListener listener = evt -> {
    DataObjectEvent<?> event = (DataObjectEvent<?>) evt.getNewValue();
    // The payload type follows event.getDataType(); ask for the one you care about.
    DataObjectEvents.toDataObjectEventData(event, Job.class)
            .ifPresent(j -> System.out.println(j.getId() + " -> " + j.getProgress()));
};

sirius.addEventListener(listener, projectId, DataEventType.JOB);
// ... and when you are done with it, or the listener leaks for the lifetime of the client:
sirius.removeEventListener(listener);
```

`DataEventType.JOB`, `PROJECT`, `DATA_IMPORT` and `BACKGROUND_COMPUTATIONS_STATE` carry typed payloads
(`Job`, `ProjectChangeEvent`, `DataImportEvent`, `BackgroundComputationsStateEvent`), which
`DataObjectEvents.toDataObjectEventData` unwraps for you. A `java.util.concurrent.Flow.Subscriber` can be
registered instead of a `PropertyChangeListener` if that suits your code better, and `addJobEventListener`
narrows the subscription to a single job.

## Error handling
Endpoint calls fail with Spring's `WebClientResponseException`, whose own message is just the status line.
The response body carries a `ProblemDetail` with the actual reason, which the client unwraps:

```java
try {
    sirius.projects().getProject("does-not-exist", null);
} catch (WebClientResponseException e) {
    System.err.println(sirius.unwrapErrorMessage(e));
    sirius.unwrapErrorResponse(e).ifPresent(problem -> System.err.println(problem.getDetail()));
}
```

`unwrapErrorMessage` falls back to the throwable's own message when there is no problem detail to read, so
it is safe to use unconditionally.

## Deprecated method names
`compounds()`, `features()` and `jobs()` return `*ApiCompat` subclasses of the generated API classes. These
add nothing but the pre-rename method names - `getAlignedFeaturesPaged` for `getAlignedFeaturesPage`, and so
on - kept as `@Deprecated(forRemoval = true)` delegates so that a rename in the API does not break existing
integrations immediately. They will be removed in the next major release; the javadoc of each names its
replacement.

## Modules
| Module | Purpose |
|---|---|
| `sirius-sdk` | The entry point: `SiriusSDK`/`SiriusClient`, process handling, event stream |
| `sirius-sdk.openapi` | Generated models and endpoint clients. Consumed transitively - do not depend on it directly |
| `sirius-sdk.jjobs` | Optional adapter exposing an API job as a `de.unijena.bioinf:jjobs-core` `JJob` via `SseProgressJJob`, for integration with jjobs-based scheduling |


## Availability
The `sirius-sdk` is available as maven artifact. The coordinates below are kept in sync with the published
version by the `refreshSdkReadme` Gradle task - edit the task, not the version strings.

### Gradle
Insert following snippet into your `build.gradle` to add this package as dependency:

<!--begin gradle-dependency-->
```groovy
  repositories {
    maven {
        url 'https://gitlab.com/api/v4/projects/66031889/packages/maven'
    }
}

dependencies {
    implementation "io.sirius-ms:sirius-sdk:3.1+sirius6.5.4"
}
```
<!--end gradle-dependency-->

### Maven
Insert following snippet into your project's POM to add this package as dependency:

<!--begin maven-dependency-->
```xml
<repositories>
  <repository>
    <id>gitlab-maven</id>
    <url>https://gitlab.com/api/v4/projects/66031889/packages/maven</url>
  </repository>
</repositories>

<dependency>
  <groupId>io.sirius-ms</groupId>
  <artifactId>sirius-sdk</artifactId>
  <version>3.1+sirius6.5.4</version>
  <scope>compile</scope>
</dependency>
```
<!--end maven-dependency-->
