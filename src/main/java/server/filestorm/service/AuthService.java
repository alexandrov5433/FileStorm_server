package server.filestorm.service;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import server.filestorm.config.ServerConfigurationProperties;
import server.filestorm.model.entity.User;
import server.filestorm.model.type.authentication.AuthResult;
import server.filestorm.model.type.authentication.AuthValidationResult;
import server.filestorm.model.type.authentication.LoginData;
import server.filestorm.model.type.authentication.RegistrationData;
import server.filestorm.util.BcryptUtil;

@Service
public class AuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private ServerConfigurationProperties confProps;

    public AuthResult<?> registerUser(RegistrationData data) {
        if (!data.isDataValid()) {
            return new AuthResult<AuthValidationResult[]>(true, data.getValidationData());
        }

        // check email and username availability
        boolean isUsernameAvailable = userService.isUsernameAvailable(data.getUsername());
        boolean isEmailAvailable = userService.isEmailAvailable(data.getEmail());
        if (!isUsernameAvailable ||!isEmailAvailable) {
            ArrayList<AuthValidationResult> authValidationResults = new ArrayList<AuthValidationResult>();
            if (!isUsernameAvailable) {
                authValidationResults.add(new AuthValidationResult("username", false, "Username is taken."));
            }
            if (!isEmailAvailable) {
                authValidationResults.add(new AuthValidationResult("email", false, "Email is in use."));
            }

            return new AuthResult<AuthValidationResult[]>(true, authValidationResults.toArray(new AuthValidationResult[0]));
        }

        String hashedPassword = BcryptUtil.hash(data.getPassword());

        User user = new User();
        user.setUsername(data.getUsername());
        user.setPassword(hashedPassword);
        user.setEmail(data.getEmail());
        user.setMaxStorageSpace(confProps.getAvailableStoragePerAccountBytes());

        userService.saveUser(user);

        return new AuthResult<User>(false, user);
    }

    public AuthResult<?> loginUser(LoginData data) {
        // check for null
        if (data.getUsername() == null) {
            return new AuthResult<AuthValidationResult[]>(true,
                    new AuthValidationResult[] {
                            new AuthValidationResult("username", false, "Username is missing.")
                    });

        }
        if (data.getPassword() == null) {
            return new AuthResult<AuthValidationResult[]>(true,
                    new AuthValidationResult[] {
                            new AuthValidationResult("password", false, "Password is missing.")
                    });

        }

        // search for user
        User user = userService.findUserByUsername(data.getUsername());
        if (user == null) {
            return new AuthResult<AuthValidationResult[]>(true,
                    new AuthValidationResult[] {
                            new AuthValidationResult("username", false,
                                    String.format("A user with username: '%1$s' does not exist.", data.getUsername()))
                    });
        }

        // check password match
        boolean isPasswordCorrect = BcryptUtil.verify(data.getPassword(), user.getPassword());
        if (!isPasswordCorrect) {
            return new AuthResult<AuthValidationResult[]>(true, new AuthValidationResult[] {
                    new AuthValidationResult("password", false, "Incorrect password.")
            });
        }

        return new AuthResult<User>(false, user);
    }

    public User validateSessionData(Long id, String username) {
        User user = userService.findUserByUsernameAndId(username, id);
        if (user == null) {
            return null;
        }
        return user;
    }
}
