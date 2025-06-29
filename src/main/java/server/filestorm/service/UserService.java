package server.filestorm.service;

import java.util.LinkedHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import server.filestorm.exception.AuthenticationException;
import server.filestorm.model.entity.Directory;
import server.filestorm.model.entity.User;
import server.filestorm.model.repository.UserRepository;

@Service
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

    public LinkedHashMap<String, Long> queryUsersByName(String username, String usernameToExclude) {
        // null-check username
        username = username == null ? "" : username;

        // escape username
        username = username.replaceAll("[^A-Za-z0-9_]", "");

        LinkedHashMap<String, Long> users = new LinkedHashMap<>();

        // no need to query on empty string; return empty result
        if (username.equals(""))
            return users;

        userRepository.queryUsersByUsername(username).ifPresent(queryResult -> {
            for (Object[] el : queryResult) {
                if (el[0].equals(usernameToExclude)) {
                    continue;
                }
                users.put((String) el[0], (Long) el[1]);
            }
        });
        return users;
    }

    @Transactional
    public void addRootStorageDirToUser(User user, Directory rootStorageDir) {
        user.setRootStorageDir(rootStorageDir);
    }
}
