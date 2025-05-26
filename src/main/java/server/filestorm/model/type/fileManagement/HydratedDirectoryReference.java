package server.filestorm.model.type.fileManagement;

import java.io.Serializable;
import java.util.HashMap;

public class HydratedDirectoryReference implements Serializable {
    private String name;
    private HashMap<String, ChunkReference> hydratedChunkRefs; // Map<NameOfChunk, ChunkReference>
    private HashMap<String, DirectoryReference> directoryRefs; // Map<NameOfDirectory, ref>

    public HydratedDirectoryReference() {
        this.name = null;
        this.hydratedChunkRefs = new HashMap<String, ChunkReference>();
        this.directoryRefs = new HashMap<String, DirectoryReference>();
    }
    
    public HydratedDirectoryReference(String name) {
        this.name = name;
        this.hydratedChunkRefs = new HashMap<String, ChunkReference>();
        this.directoryRefs = new HashMap<String, DirectoryReference>();
    }

    public HydratedDirectoryReference(String name, HashMap<String, DirectoryReference> directoryRefs) {
        this.name = name;
        this.hydratedChunkRefs = new HashMap<String, ChunkReference>();
        this.directoryRefs = directoryRefs;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HashMap<String, ChunkReference> getHydratedChunkRefs() {
        return this.hydratedChunkRefs;
    }

    public void setHydratedChunkRefs(HashMap<String, ChunkReference> hydratedChunkRefs) {
        this.hydratedChunkRefs = hydratedChunkRefs;
    }

    public HashMap<String, DirectoryReference> getDirectoryRefs() {
        return this.directoryRefs;
    }

    public void setDirectoryRefs(HashMap<String, DirectoryReference> directoryRefs) {
        this.directoryRefs = directoryRefs;
    }

    public void addHydratedChunkRef(ChunkReference chunkRef) {
        this.hydratedChunkRefs.put(chunkRef.getName(), chunkRef);
    }
}
