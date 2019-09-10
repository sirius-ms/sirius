package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.AdditionalFields;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StandardMSFilenameFormatter implements FilenameFormatter {
    public static final PSProperty DEFAULT_EXPRESSION = new PSProperty("%source_%name");
    private String formatExpression;
    private FormatString[] formatStrings;

    public StandardMSFilenameFormatter() {
        this.formatExpression = DEFAULT_EXPRESSION.formatExpression;
        formatStrings = new FormatString[]{
                new FilenameFormat(), new FixedString("_"),
                new NameFormat()
        };
    }

    public StandardMSFilenameFormatter(String formatString) throws ParseException {
        this.formatExpression = formatString;
        formatStrings = parseFormat(formatExpression);
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
    public String apply(Ms2Experiment experimentResult) {
        StringBuilder builder = new StringBuilder();
        for (FormatString formatString : formatStrings) {
            builder.append(formatString.format(experimentResult));
        }
        return builder.toString();
    }

    private interface FormatString {
        String format(Ms2Experiment experimentResult);
    }

    private class NameFormat implements FormatString {
        @Override
        public String format(Ms2Experiment experimentResult) {
            return simplify(experimentResult.getName());
        }
    }

    private class FilenameFormat implements FormatString {
        @Override
        public String format(Ms2Experiment experimentResult) {
            return simplifyURL(experimentResult.getSource().getFile());
        }
    }

    /*private class IndexFormat implements FormatString {
        @Override
        public String format(Ms2Experiment experimentResult, int index) {
            return String.valueOf(index);
        }
    }*/

    private class FixedString implements FormatString {
        private String string;

        FixedString(String string){
            this.string = string;
        }

        @Override
        public String format(Ms2Experiment experimentResult) {
            return string;
        }
    }

    private class AnnotationString implements FormatString {
        private String annotation;

        public AnnotationString(String annotation) {
            this.annotation = annotation;
        }

        @Override
        public String format(Ms2Experiment experiment) {

            Map<String, String> map = experiment.getAnnotation(AdditionalFields.class);
            if (map==null){
                throw new RuntimeException("Cannot format compound file name for " + experiment.getName() + ": no annotations given.");
            }
            String value  = map.get(annotation);
            if (value==null){
                throw new RuntimeException("Cannot format compound file name for " + experiment.getName() + ": annotation '" + annotation + "' unknown.");
            }
            return value;
        }
    }

    private static String simplify(String name) {
        if (name.length() > 64)
            name = name.substring(0, 48);
        return name.replaceAll("[^A-Za-z0-9,\\-]+", "");
    }

    private static String simplifyURL(String filename) {
        filename = new File(filename).getName();
        int i = Math.min(48, filename.lastIndexOf('.'));
        return filename.substring(0, i);
    }


    @Override
    public String getFormatExpression() {
        return formatExpression;
    }
}
