package server.filestorm.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import server.filestorm.exception.AuthenticationException;
import server.filestorm.model.entity.Chunk;
import server.filestorm.model.entity.Directory;
import server.filestorm.model.entity.User;
import server.filestorm.model.repository.UserRepository;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public User findUserByUsername(String username) {
        return userRepository.findUserByUsername(username).orElse(null);
    }

    public User findUserByUsernameAndId(String username, Long id) {
        return userRepository.findUserByUsernameAndId(username, id).orElse(null);
    }

    public void increaseBytesInStorage(Long userId, Long bytesToAdd) {
        userRepository.increaseBytesInStorage(userId, bytesToAdd);
    }

    public void increaseBytesInStorage(User user, Long bytesToAdd) {
        userRepository.increaseBytesInStorage(user.getId(), bytesToAdd);
    }

    public void decreaseBytesInStorage(Long userId, Long bytesToRemove) {
        userRepository.decreaseBytesInStorage(userId, bytesToRemove);
    }

    public void decreaseBytesInStorage(User user, Long bytesToRemove) {
        userRepository.decreaseBytesInStorage(user.getId(), bytesToRemove);
    }

    public Long getCurrentBytesInStorage(Long userId) {
        return userRepository.getCurrentBytesInStorage(userId)
            .orElseThrow(() -> new AuthenticationException("A user with this ID was not found."));
    }

    public Long getCurrentBytesInStorage(User user) {
        return userRepository.getCurrentBytesInStorage(user.getId())
            .orElseThrow(() -> new AuthenticationException("A user with this ID was not found."));
    }

    /**
     * Checks is the given email is available - not taken by an other user - by searching for an existing user with this exact email.
     * @param email Email to search for.
     * @return true if the email may be used - is not already in use.
     */
    public Boolean isEmailAvailable(String email) {
        User user = userRepository.searchForUserWithThisEmail(email).orElse(null);
        return user == null ? true : false;
    }

    /**
     * Checks is the given username is available - not taken by an other user - by searching for an existing user with this exact username.
     * @param username The username to search for.
     * @return true if the username may be used - is not already in use.
     */
    public Boolean isUsernameAvailable(String username) {
        User user = userRepository.findUserByUsername(username).orElse(null);
        return user == null ? true : false;
    }

    /**
     * Finds a User with the given id.
     * 
     * @param userId The id of the User.
     * @return The User, if found.
     * @throws AuthenticationException When a User with this id can not be found.
     */
    public User findById(Long userId) throws AuthenticationException {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found."));
    }

    public LinkedHashMap<String, Long> queryUsersByNameForFileSharing(String username, String usernameToExclude, Chunk fileToShare) {
        // null-check username
        username = username == null ? "" : username;

        // escape username
        username = username.replaceAll("[^A-Za-z0-9_]", "");

        LinkedHashMap<String, Long> users = new LinkedHashMap<>();

        ArrayList<String> usernamesWithWhomTheFileIsShared = fileToShare.getShareWith().stream()
            .map(User::getUsername)
            .collect(Collectors.toCollection(ArrayList::new));

        // no need to query on empty string; return empty result
        if (username.equals(""))
            return users;

        userRepository.queryUsersByUsername(username).ifPresent(queryResult -> {
            // queryResult is [{u.username, u.id}]
            for (Object[] el : queryResult) {
                if (el[0].equals(usernameToExclude)) {
                    continue;
                }
                if (usernamesWithWhomTheFileIsShared.indexOf(el[0]) != -1) {
                    continue;
                }
                users.put((String) el[0], (Long) el[1]);
            }
        });
        return users;
    }

    public void addRootStorageDirToUser(User user, Directory rootStorageDir) {
        user.setRootStorageDir(rootStorageDir);
    }
}
