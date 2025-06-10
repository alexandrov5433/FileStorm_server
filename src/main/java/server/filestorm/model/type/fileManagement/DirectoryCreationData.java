package server.filestorm.model.type.fileManagement;

public class DirectoryCreationData {
    private Long targetDirectoryId;
    private String newDirectoryName;

    public DirectoryCreationData(Long targetDirectoryId, String newDirectoryName) {
        this.targetDirectoryId = targetDirectoryId;
        this.newDirectoryName = newDirectoryName;
    }

    public Long getTargetDirectoryId() {
        return targetDirectoryId;
    }

    public void setTargetDirectoryId(Long targetDirectoryId) {
        this.targetDirectoryId = targetDirectoryId;
    }

    public String getNewDirectoryName() {
        return newDirectoryName;
    }

    public void setNewDirectoryName(String newDirectoryName) {
        this.newDirectoryName = newDirectoryName;
    }
}
