package server.filestorm.model.type.fileManagement;

import java.io.Serializable;
import java.util.HashMap;

public class DirectoryReference implements Serializable {

    private String name;
    private HashMap<String, Integer> chunkRefs; // Map<NameOfChunk, idOfChunk>
    private HashMap<String, DirectoryReference> directoryRefs; // Map<NameOfDirectory, ref>

    public DirectoryReference() {
        this.name = null;
        this.chunkRefs = new HashMap<String, Integer>();
        this.directoryRefs = new HashMap<String, DirectoryReference>();
    }

    public DirectoryReference(String name) {
        this.name = name;
        this.chunkRefs = new HashMap<String, Integer>();
        this.directoryRefs = new HashMap<String, DirectoryReference>();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HashMap<String, Integer> getChunkRefs() {
        return this.chunkRefs;
    }

    public void setChunkRefs(HashMap<String, Integer> chunkRefs) {
        this.chunkRefs = chunkRefs;
    }

    public HashMap<String, DirectoryReference> getDirectoryRefs() {
        return this.directoryRefs;
    }

    public void setDirectoryRefs(HashMap<String, DirectoryReference> directoryRefs) {
        this.directoryRefs = directoryRefs;
    }

    public void addChunkRef(String chunkName, Integer chunkId) {
        this.chunkRefs.put(chunkName, chunkId);
    }

    public Boolean removeChunkRef(String chunkName, Integer chunkId) {
        return this.chunkRefs.remove(chunkName, chunkId);
    }
}
