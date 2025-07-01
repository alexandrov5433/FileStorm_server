package server.filestorm.model.type;

public class BulkDownloadData {
    private Long[] chunks;
    private Long[] directories;

    public BulkDownloadData() {
        this.chunks = null;
        this.directories = null;
    }

    public BulkDownloadData(Long[] chunks, Long[] directories) {
        this.chunks = chunks;
        this.directories = directories;
    }

    public Long[] getChunks() {
        return chunks;
    }

    public void setChunks(Long[] chunks) {
        this.chunks = chunks;
    }

    public Long[] getDirectories() {
        return directories;
    }

    public void setDirectories(Long[] directories) {
        this.directories = directories;
    }
}
