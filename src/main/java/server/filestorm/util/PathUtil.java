package server.filestorm.util;

import server.filestorm.exception.ProcessingException;

public class PathUtil {

    /**
     * Removes "/" form start and end and replaces separator charaters - different
     * than '/' - with '/'. E.g. "\11/work\something/" =>
     * "11/work/something"
     * 
     * @param path String to standerdize.
     * @return The standerdized string.
     * @throws ProcessingException When path is null or it's length is 0.
     */
    public static String standardizeRelativePathString(String path) throws ProcessingException {
        if (path == null || path.length() == 0) {
            throw new ProcessingException("Invalid path.");
        }
        path = path.trim().replaceAll("\\\\", "/");
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    // /**
    //  * Joins the two strings with a "/" by placing the newName argument at the end
    //  * (right side) of the path argument.
    //  * 
    //  * @param path    The left side of the string.
    //  *                PathUtil.standardizeRelativePathString() is applied.
    //  * @param newName The right side of the string. PathUtil.sanitizeFileName() is
    //  *                applied.
    //  * @return The joined string.
    //  * @throws ProcessingException
    //  */
    // public static String concatNameAtEndOfPath(String path, String newName) throws ProcessingException {
    //     path = PathUtil.standardizeRelativePathString(path);
    //     newName = PathUtil.sanitizeFileName(newName);
    //     return (path + "/" + newName);
    // }

    // /**
    //  * Verifies that the first part of the path equals the userId and that the path
    //  * does not start or end with '/'.
    //  * 
    //  * @param path   Relative path string to check.
    //  * @param userId The id of the user, with which the given path string must
    //  *               start.
    //  * @throws ProcessingException When: path is null, it's length is 0, does not
    //  *                             start with user's ID, starts or ends with "/".
    //  */
    // public static void verifyRelativePath(String path, Integer userId) throws ProcessingException {
    //     if (path == null || path.length() == 0) {
    //         throw new ProcessingException("Invalid path.");
    //     }
    //     boolean isStartAndEndValid = (!path.startsWith("/") && !path.endsWith("/"));
    //     String[] parts = path.split("/");
    //     boolean startsWithUserId = parts[0].equals(Integer.toString(userId));
    //     if (!isStartAndEndValid || !startsWithUserId) {
    //         throw new ProcessingException("Invalid path.");
    //     }
    //     ;
    // }

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
