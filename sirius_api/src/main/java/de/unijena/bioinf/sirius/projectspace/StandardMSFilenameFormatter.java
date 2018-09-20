package de.unijena.bioinf.sirius.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2ExperimentAdditionalFields;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StandardMSFilenameFormatter implements FilenameFormatter {

    private String unparsedFormatString;
    private FormatString[] formatStrings;

    public StandardMSFilenameFormatter() {
        this.unparsedFormatString = "%index_%source_%name";
        formatStrings = new FormatString[]{
                new IndexFormat(), new FixedString("_"),
                new FilenameFormat(), new FixedString("_"),
                new NameFormat()
        };
    }

    public StandardMSFilenameFormatter(String formatString) throws ParseException {
        this.unparsedFormatString = formatString;
        formatStrings = parseFormat(unparsedFormatString);
    }

    private static final Pattern NormalCharactersString = Pattern.compile("([A-Za-z]+)(.*)");
    private static final Pattern InBracketsString = Pattern.compile("\\(([^)]+)\\)(.*)");
    private FormatString[] parseFormat(String formatString) throws ParseException {
        if (formatString.length()==0) throw new ParseException("File formatting string must be non empty", -1);
        String[] parts = formatString.split("%");
        List<FormatString> formatStrings = new ArrayList<>();
        //beginning is special
        if (parts[0].length()!=0){
            formatStrings.add(new FixedString(parts[0]));
        }
        parts = Arrays.copyOfRange(parts, 1, parts.length);
        for (String part : parts) {
            if (part.length()==0) continue;
            String keyword, fixedPart; //keyword is a variable. fixedPart is the part between variables;
            if (part.startsWith("(")){
                //keyword in brackets
                Matcher matcher = InBracketsString.matcher(part);
                if (!matcher.matches()) throw new ParseException("Cannot parse file formatting string: cannot match brackets.", formatString.indexOf(part));
                keyword = matcher.group(1);
                fixedPart = matcher.group(2);
            } else {
                Matcher matcher = NormalCharactersString.matcher(part);
                if (!matcher.matches()) throw new ParseException("Cannot parse file formatting string: did not recognize any keyword.", formatString.indexOf(part));
                keyword = matcher.group(1);
                fixedPart = matcher.group(2);
            }

            switch (keyword.toLowerCase()){
                case "name":
                case "compoundname":
                    formatStrings.add(new NameFormat());
                    break;
                case "filename":
                case "source":
                    formatStrings.add(new FilenameFormat());
                    break;
                case "index":
                case "idx":
                    formatStrings.add(new IndexFormat());
                    break;
                default:
                    formatStrings.add(new AnnotationString(keyword));
                    LoggerFactory.getLogger(StandardMSFilenameFormatter.class).warn("File formatting string contains non-default annotations '"+keyword+"'. If your input file does not contain this annotation this will result in errors.");
                    break;
            }

            if (fixedPart.length()>0) formatStrings.add(new FixedString(fixedPart));
        }

        return formatStrings.toArray(new FormatString[0]);
    }

    @Override
    public String formatName(ExperimentResult experimentResult, int index) {
        StringBuilder builder = new StringBuilder();
        for (FormatString formatString : formatStrings) {
            builder.append(formatString.format(experimentResult, index));
        }
        return builder.toString();
    }


    private interface FormatString {
        String format(ExperimentResult experimentResult, int index);
    }

    private class NameFormat implements FormatString {
        @Override
        public String format(ExperimentResult experimentResult, int index) {
            return experimentResult.getExperimentName();
        }
    }

    private class FilenameFormat implements FormatString {
        @Override
        public String format(ExperimentResult experimentResult, int index) {
            return experimentResult.getExperimentSource();
        }
    }

    private class IndexFormat implements FormatString {
        @Override
        public String format(ExperimentResult experimentResult, int index) {
            return String.valueOf(index);
        }
    }

    private class FixedString implements FormatString {
        private String string;

        FixedString(String string){
            this.string = string;
        }

        @Override
        public String format(ExperimentResult experimentResult, int index) {
            return string;
        }
    }

    private class AnnotationString implements FormatString {
        private String annotation;

        public AnnotationString(String annotation) {
            this.annotation = annotation;
        }

        @Override
        public String format(ExperimentResult experimentResult, int index) {
            Ms2Experiment experiment = experimentResult.getExperiment();
            Map<String, String> map = experiment.getAnnotation(Ms2ExperimentAdditionalFields.class);
            if (map==null){
                throw new RuntimeException("Cannot format compound file name for "+experiment.getName()+": no annotations given.");
            }
            String value  = map.get(annotation);
            if (value==null){
                throw new RuntimeException("Cannot format compound file name for "+experiment.getName()+": annotation '"+annotation+"' unknown.");
            }
            return value;
        }
    }

}
