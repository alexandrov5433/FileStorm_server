package server.filestorm.model.type;

import org.springframework.web.multipart.MultipartFile;

public class FileUploadData {
    
    private MultipartFile file;
    private Long targetDirectoryId;

    public FileUploadData(MultipartFile file, Long targetDirectoryId) {
        this.file = file;
        this.targetDirectoryId = targetDirectoryId;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public Long getTargetDirectoryId() {
        return targetDirectoryId;
    }

    public void setTargetDirectoryId(Long targetDirectoryId) {
        this.targetDirectoryId = targetDirectoryId;
    }
}
