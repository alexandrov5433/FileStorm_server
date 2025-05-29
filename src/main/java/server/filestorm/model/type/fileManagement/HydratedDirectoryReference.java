package server.filestorm.model.type.fileManagement;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class HydratedDirectoryReference implements Serializable {
    private String name;
    private HashMap<String, ChunkReference> hydratedChunkRefs; // Map<NameOfChunk, ChunkReference>
    private HashMap<String, Integer> simpleDirectoryRefs; // Map<NameOfDirectory, ref>

    public HydratedDirectoryReference() {
        this.name = null;
        this.hydratedChunkRefs = new HashMap<String, ChunkReference>();
        this.simpleDirectoryRefs = new HashMap<String, Integer>();
    }
    
    public HydratedDirectoryReference(String name) {
        this.name = name;
        this.hydratedChunkRefs = new HashMap<String, ChunkReference>();
        this.simpleDirectoryRefs = new HashMap<String, Integer>();
    }

    public HydratedDirectoryReference(String name, HashMap<String, DirectoryReference> directoryRefs) {
        this.name = name;
        this.hydratedChunkRefs = new HashMap<String, ChunkReference>();

        HashMap<String, Integer> simpleDirRef = new HashMap<String, Integer>();
        Iterator<Entry<String, DirectoryReference>> itr = directoryRefs.entrySet().iterator();
        while (itr.hasNext()) {
            Entry<String, DirectoryReference> entry = itr.next();
            String dirName = entry.getKey();
            DirectoryReference dirRef = entry.getValue();

            simpleDirRef.put(dirName, dirRef.getContentSize());
        }
        this.simpleDirectoryRefs = simpleDirRef;
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

    public HashMap<String, Integer> getSimpleDirectoryRefs() {
        return this.simpleDirectoryRefs;
    }

    public void setSimpleDirectoryRefs(HashMap<String, Integer> directoryRefs) {
        this.simpleDirectoryRefs = directoryRefs;
    }

    public void addHydratedChunkRef(ChunkReference chunkRef) {
        this.hydratedChunkRefs.put(chunkRef.getName(), chunkRef);
    }
}
