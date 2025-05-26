package server.filestorm.model.type;

import org.springframework.web.multipart.MultipartFile;

public class FileUploadData {
    
    private MultipartFile file;
    private String relativePath;

    /**
     * 
     * @param file The file which has to be saved.
     * @param relativePath The path to the directory in which the file must be saved. This part must start with the user-specific root storage directory. E.g. "11/my_docs/work", where "11" is the root storage directory of the user and "work" is the subdirectory, in which the file must be saved.
     */
    public FileUploadData(MultipartFile file, String relativePath) {
        this.file = file;
        this.relativePath = relativePath;
    }

    public MultipartFile getFile() {
        return this.file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getRelativePath() {
        return this.relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }
}
