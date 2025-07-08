package server.filestorm.model.type.search;

import server.filestorm.model.type.fileManagement.ChunkReference;

public class FileSearchResult {
    private Object[] directoryPath; // [[directoryId, directoryName]]
    private ChunkReference chunk;

    public FileSearchResult() {
        this.directoryPath = null;
        this.chunk = null;
    }

    public FileSearchResult(Object[] directoryPath, ChunkReference chunk) {
        this.directoryPath = directoryPath;
        this.chunk = chunk;
    }

    public void setDirectoryPath(Object[] directoryPath) {
        this.directoryPath = directoryPath;
    }

    public Object[] getDirectoryPath() {
        return directoryPath;
    }

    public void setChunk(ChunkReference chunk) {
        this.chunk = chunk;
    }

    public ChunkReference getChunk() {
        return chunk;
    }
}