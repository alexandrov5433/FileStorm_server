package server.filestorm.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import server.filestorm.exception.ProcessingException;

public class StringUtil {

    /**
     * Makes the string unique by appending space with a counter like:" (1)". If a counter is present it is being incremented.
     * @param arg The string to make unique. String.trim() is applied before processing.
     * @return The updated string.
     */
    public static String appendUniqueCounter(String arg) {
        arg = arg.trim();
        Pattern patternCounterNoFileExtention = Pattern.compile("(.*) \\((\\d+)\\)$");
        Pattern patternCounterWithFileExtention = Pattern.compile("(.*) \\((\\d+)\\)(\\..*)$");
        Pattern patternFileExtention = Pattern.compile("(.*)(\\..*)$");

        Matcher matcherCounterNoFileExtention = patternCounterNoFileExtention.matcher(arg);
        Matcher matcherCounterWithFileExtention = patternCounterWithFileExtention.matcher(arg);
        Matcher matcherFileExtention = patternFileExtention.matcher(arg);

        if (matcherCounterNoFileExtention.matches()) {
            String main = matcherCounterNoFileExtention.group(1);
            String counter = matcherCounterNoFileExtention.group(2);
            int counterIncreased = Integer.parseInt(counter) + 1;
            return String.format("%1$s (%2$d)", main, counterIncreased);
        } else if (matcherCounterWithFileExtention.matches()) {
            String main = matcherCounterWithFileExtention.group(1);
            String counter = matcherCounterWithFileExtention.group(2);
            String fileExtention = matcherCounterWithFileExtention.group(3);
            int counterIncreased = Integer.parseInt(counter) + 1;
            return String.format("%1$s (%2$d)%3$s", main, counterIncreased, fileExtention);
        } else if (matcherFileExtention.matches()) {
            String main = matcherFileExtention.group(1);
            String fileExtention = matcherFileExtention.group(2);
            return String.format("%1$s (1)%2$s", main, fileExtention);
        } else {
            return arg + " (1)";
        }
    }

    /**
     * Sanitizes the given name and applies the String.trim() method. The following
     * characters are removed: \/?":*><|
     * 
     * @param name String to sanitize.
     * @return The sanitized and trimmed string.
     * @throws ProcessingException When the name argument is null or it's length is 0. Also when length becomes 0 after sanitization.
     */
    public static String sanitizeFileName(String name) throws ProcessingException {
        if (name == null || name.length() == 0) {
            throw new ProcessingException("The name is missing.");
        }
        name = name
                .replaceAll("\\\\|/|:|\\*|\\?|\"|<|>|\\|", "")
                .trim();
        if (name.length() == 0) {
            throw new ProcessingException("The name is missing.");
        }
        return name;
    }
}
