package server.filestorm.model.type.fileManagement;

public class DirectoryCreationData {
    private String targetDirectoryPath;
    private String newDirectoryName;

    public DirectoryCreationData(String targetDirectoryPath, String newDirectoryName) {
        this.targetDirectoryPath = targetDirectoryPath;
        this.newDirectoryName = newDirectoryName;
    }

    public String getTargetDirectoryPath() {
        return targetDirectoryPath;
    }

    public void setTargetDirectoryPath(String targetDirectoryPath) {
        this.targetDirectoryPath = targetDirectoryPath;
    }

    public String getNewDirectoryName() {
        return newDirectoryName;
    }

    public void setNewDirectoryName(String newDirectoryName) {
        this.newDirectoryName = newDirectoryName;
    }
}
