package server.filestorm.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import server.filestorm.exception.FileManagementException;
import server.filestorm.exception.ProcessingException;
import server.filestorm.exception.StorageException;
import server.filestorm.model.entity.Chunk;
import server.filestorm.model.entity.Chunk.ShareOption;
import server.filestorm.model.repository.ChunkRepository;
import server.filestorm.model.entity.User;
import server.filestorm.model.type.fileManagement.ChunkReference;

@Service
public class SharingService {
    @Autowired
    private ChunkRepository chunkRepository;

    @Autowired
    private LinkGeneratorService linkGeneratorService;

    @Transactional
    public Optional<ChunkReference> updateChunkShareOption(Chunk c, String shareOption) throws StorageException {
        ShareOption option = matchStringToShareOption(shareOption);
        c.setShareOption(option);
        chunkRepository.save(c);
        return Optional.of(new ChunkReference(c));
    }

    private ShareOption matchStringToShareOption(String shareOption) throws StorageException {
        try {
            return Enum.valueOf(ShareOption.class, shareOption.toUpperCase());
        } catch (Exception e) {
            throw new StorageException("Such share option does not exist.");
        }
    }

    /**
     * Clears the Users, currently in share_with, and deletes the share_link.
     * 
     * @param c Chunk from which to delete.
     */
    @Transactional
    public ChunkReference deleteShareWithAndShareLink(Chunk c) {
        // delete all Users from shred_with
        c.setShareWith(new HashSet<>());
        c.setShareLink(null);
        chunkRepository.save(c);
        return new ChunkReference(c);
    }

    /**
     * Clears the Users, currently in share_with, and adds a newly generated link
     * for sharing in share_link.
     * 
     * @param c Chunk for processing.
     * @return A ChunkReference of the processed Chunk.
     * @throws ProcessingException From
     *                             LinkGeneratorService.generateFileSharingLink(c.getId(),
     *                             c.getName()).
     */
    @Transactional
    public ChunkReference deleteShareWithAndCreateShareLink(Chunk c) throws ProcessingException {
        c.setShareWith(new HashSet<>());
        String link = this.linkGeneratorService.generateFileSharingLink(c.getId(), c.getName());
        c.setShareLink(link);
        chunkRepository.save(c);
        return new ChunkReference(c);
    }

    /**
     * Returns the Users - username and id - with whom this Chunk is shared.
     * 
     * @param c The Chunk which is to be processed.
     * @return The Users - username and id - which are in the share_with field of
     *         the Chunk.
     */
    public LinkedHashMap<String, Integer> getUsersFromShareWith(Chunk c) {
        LinkedHashMap<String, Integer> users = new LinkedHashMap<String, Integer>();
        Iterator<User> itr = c.getShareWith().iterator();
        while (itr.hasNext()) {
            User u = itr.next();
            users.put(u.getUsername(), u.getId());
        }
        return users;
    }

    /**
     * Returns a list of ChunkReferences of the files the User is sharing.
     * 
     * @param u The User who's files must searched through.
     * @return A list of ChunkReferences of the files the User is sharing.
     * @throws FileManagementException When DB could not be successfully queried.
     */
    public ArrayList<ChunkReference> getFilesUserIsSharing(User u) throws FileManagementException {
        List<Chunk> chunks = chunkRepository.getFilesUserIsSharing(u)
                .orElseThrow(() -> new FileManagementException("Could not find shared files."));
        ArrayList<ChunkReference> refs = new ArrayList<ChunkReference>();
        for (Chunk c : chunks) {
            refs.add(new ChunkReference(c));
        }
        return refs;
    }

    @Transactional
    public Boolean shareFileWithUser(Chunk c, User userReceiver) {
        c.addUserToShareWithList(userReceiver);
        chunkRepository.save(c);
        return true;
    }

    /**
     * Returns a list of ChunkReferences of files shared with this User.
     * 
     * @param u The User, with who the files are shared.
     * @return Returns a list of ChunkRefernces of shred with this User.
     */
    public ArrayList<ChunkReference> getFilesSharedWithUser(User u) {
        ArrayList<ChunkReference> chunkReferences = new ArrayList<ChunkReference>();
        Set<Chunk> sharedChunks = u.getChunksSharedWithMe();
        for (Chunk c : sharedChunks) {
            chunkReferences.add(new ChunkReference(c));
        }
        return chunkReferences;
    }
}
