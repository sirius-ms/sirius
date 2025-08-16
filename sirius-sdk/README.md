# SIRIUS Java SDK
This is the SIRIUS Software development kit (SDK) for Java to connect to the generic Sirius Nightsky API.
It provides models and clients to interact with the API as well as features to start/stop/locate the SIRIUS application.

This SDK is intended to be shipped with your software to connect to a SIRIUS installation on the user system. It
does **not** contain the SIRIUS Software itself or any algorithms/methods.

The class `SiriusSDK` serves a single entry point to all features. It allows you to start SIRIUS and gives access all
API endpoints.

## Usage
Detailed documentation about the different endpoints can be found [here]().

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
            sirius.projects().importPreprocessedData(project.getProjectId(), true, true, List.of(
                    new File("/mydata/spectra1.mgf"),
                    new File("/mydata/spectra1.ms"),
                    new File("/mydata/spectra1.cef")
            ));
            
            //get default compute configuration/parameters from SIRIUS (optional)
            JobSubmission sub = sirius.jobs().getDefaultJobConfig(false);
            sub.getZodiacParams().setEnabled(false); //enable/disable tools

            //submit job to be computed in background
            Job job = sirius.jobs().startJob(project.getProjectId(), sub, null);
            job = sirius.awaitJob(project.getProjectId(), job.getId());

            // fetch results from the project
            sirius.features().getAlignedFeatures(project.getProjectId(), List.of(AlignedFeatureOptField.TOPANNOTATIONS))
                    .forEach(System.out::println);

        } catch (Exception e) {
            e.printStackTrace();
        }
```
### Import Option 1:
Import peak-list data from files. Can be executed synchronously or asynchronously as a job
```java
sirius.projects().importPreprocessedData(project.getProjectId(), true, true, List.of(
    new File("/mydata/spectra1.mgf"),
    new File("/mydata/spectra1.ms"),
    new File("/mydata/spectra1.cef")
));
```
### Import Option 2:
Import LCMS Runs from file and Find and Align features during import. Can be executed synchronously or asynchronously as a job.
```java
Job importJob = sirius.projects().importMsRunDataAsJob(project.getProjectId(), true, true, List.of(
    new File("/myRuns/run1a.mzml"),
    new File("/myRuns/run2a.mzml"),
    new File("/myRuns/run1b.mzml"),
    new File("/myRuns/run2b.mzml")
));
```
### Import Option 3:
Direct data import. Synchronously only .
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



## Availability
The `sirius-sdk` is available as maven artifact.

### Gradle
Insert following snippet into your `build.gradle` to add this package as dependency:

```groovy
  repositories {
    maven {
        url 'https://gitlab.com/api/v4/projects/66031889/packages/maven'
    }
}

dependencies {
    implementation "io.sirius-ms:sirius-sdk:3.1+sirius6.3.0"
}
```

### Maven
Insert following snippet into your project's POM to add this package as dependency:

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
  <version>3.1+sirius6.3.0</version>
  <scope>compile</scope>
</dependency>
```
