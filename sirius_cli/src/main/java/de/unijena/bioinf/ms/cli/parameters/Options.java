package de.unijena.bioinf.ms.cli.parameters;

import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import picocli.CommandLine;

@CommandLine.Command(defaultValueProvider = Options.Defaults.class, versionProvider = Options.Versions.class, footer = {}, footerHeading = "Please cite the following paper when using our tool: ")
public interface Options {

    class Defaults implements CommandLine.IDefaultValueProvider {
        public static final String PROPERTY_BASE = "de.unijena.bioinf.sirius.parameters";

        @Override
        public String defaultValue(CommandLine.Model.ArgSpec argSpec) {
            final String l = argSpec.paramLabel(); //this should be the field name per default
            if (l == null || l.isEmpty()) return null;
            return PropertyManager.PROPERTIES.getProperty(PROPERTY_BASE + "." + l);
        }
    }

    class Versions implements CommandLine.IVersionProvider {
        public static final String PROPERTY_BASE = "de.unijena.bioinf.sirius.parameters";

        @Override
        public String[] getVersion() throws Exception {
            return new String[]{ApplicationCore.VERSION_STRING(), "SIRIUS lib: " + FingerIDProperties.siriusVersion(), "CSI:FingerID lib: " + FingerIDProperties.fingeridVersion()};
        }
    }

    class DefaultPropertyTypeConverter implements CommandLine.ITypeConverter<Object>{

        @Override
        public Object convert(String value) throws Exception {
//            if (PropertyManager.DEFAULTS.isInstantiatableWithDefaults())
            return null;
        }
    }
}
