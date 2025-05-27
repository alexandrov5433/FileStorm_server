package server.filestorm.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import server.filestorm.model.type.ApiResponse;
import server.filestorm.model.type.CustomSession;
import server.filestorm.model.type.authentication.AuthResult;
import server.filestorm.model.type.authentication.AuthValidationResult;
import server.filestorm.model.type.authentication.LoginData;
import server.filestorm.model.type.authentication.RegistrationData;
import server.filestorm.model.type.fileManagement.DirectoryReference;
import server.filestorm.model.entity.User;
import server.filestorm.service.AuthService;
import server.filestorm.service.FileSystemService;
import server.filestorm.service.UserService;
import server.filestorm.util.CustomHttpServletRequestWrapper;
import server.filestorm.util.JwtUtil;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Authentication {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FileSystemService fileService;

    @Autowired
    private UserService userService;

    @PostMapping(path = "/api/auth/register")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> register(RegistrationData data) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        // create user if register data is valid
        AuthResult<?> authResult = authService.registerUser(data);

        if (authResult.getIsAuthError()) {
            res.setResult(ResponseEntity.badRequest()
                    .body(new ApiResponse<AuthValidationResult[]>("Login not successfull.",
                            (AuthValidationResult[]) authResult.getPayload())));
        } else {
            User user = (User) authResult.getPayload();

            // create JWT and cookie value
            Map<String, Object> claims = new HashMap<String, Object>();
            claims.put("id", user.getId());
            claims.put("username", user.getUsername());
            String token = jwtUtil.genareteToken(user.getUsername(), claims);
            String setCookieHeaderValue = String
                    .format("FileStormUserSession=%1$s; Path=/; Max-Age=31556952; HttpOnly; Secure;",
                            token); // 31556952s=1y

            // create user specific storage directory
            DirectoryReference rootUserDir = fileService
                    .createRootDirectoryForUser(Integer.toString(user.getId()));
            if (rootUserDir == null) {
                res.setResult(ResponseEntity.status(500)
                        .body(new ApiResponse<>(
                                "Could not create a new storage directory for user.")));
                return res;
            }

            // add user storage dir to his DB reference
            userService.addRootDirectory(user, rootUserDir);

            // remove password hash from User; this change is not saved to the DB so the
            // hash stays intact. Done by exception in the controller. When done in the
            // service, the password in the DB is also overridden.
            user.setPassword("TheHashedPasswordStaysOnTheServer");

            // responde 200 with cookie
            res.setResult(ResponseEntity.ok()
                    .header("Set-Cookie", setCookieHeaderValue)
                    .body(new ApiResponse<User>("Registration successful.", user)));
        }
        return res;
    }

    @PostMapping(path = "/api/auth/login")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> login(LoginData data) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        AuthResult<?> authResult = authService.loginUser(data);

        if (authResult.getIsAuthError()) {
            res.setResult(ResponseEntity.badRequest()
                    .body(new ApiResponse<AuthValidationResult[]>("Login not successfull.",
                            (AuthValidationResult[]) authResult.getPayload())));
        } else {
            User user = (User) authResult.getPayload();

            // create JWT and cookie value
            Map<String, Object> claims = new HashMap<String, Object>();
            claims.put("id", user.getId());
            claims.put("username", user.getUsername());
            String token = jwtUtil.genareteToken(user.getUsername(), claims);
            String setCookieHeaderValue = String
                    .format("FileStormUserSession=%1$s; Path=/; Max-Age=31556952; HttpOnly; Secure;",
                            token); // 31556952s=1y

            res.setResult(ResponseEntity.ok()
                    .header("Set-Cookie", setCookieHeaderValue)
                    .body(new ApiResponse<User>("Login successful.", user)));
        }
        return res;
    }

    @GetMapping("/api/auth/logout")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> logout() {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        // set cookie value to nothing and delete instantly
        String setCookieHeaderValue = String
                .format("FileStormUserSession=%1$s; Path=/; Max-Age=0; HttpOnly; Secure;", "");

        res.setResult(
                ResponseEntity.ok()
                        .header("Set-Cookie", setCookieHeaderValue)
                        .body(new ApiResponse<>("Logout successful.")));
        return res;
    }

    @GetMapping("/api/auth/validate-session")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> validateSession(CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        // delete cookie from client
        String setCookieHeaderDeletionValue = String
                .format("FileStormUserSession=%1$s; Path=/; Max-Age=0; HttpOnly; Secure;", "");

        if (session == null) {
            res.setResult(
                    ResponseEntity.badRequest()
                            .header("Set-Cookie", setCookieHeaderDeletionValue)
                            .body(new ApiResponse<>("No session.")));
        } else {
            Map<String, Object> claims = session.getClaims();
            Integer id = (Integer) claims.get("id");
            String username = (String) claims.get("username");
            if (id == null || username == null) {
                res.setResult(
                        ResponseEntity.badRequest()
                                .header("Set-Cookie", setCookieHeaderDeletionValue)
                                .body(new ApiResponse<>("No session.")));
                return res;
            }

            User user = authService.validateSessionData(id, username);
            if (user == null) {
                res.setResult(
                        ResponseEntity.badRequest()
                                .header("Set-Cookie", setCookieHeaderDeletionValue)
                                .body(new ApiResponse<>("No session.")));
            } else {
                res.setResult(
                        ResponseEntity.ok()
                                .body(new ApiResponse<User>("Session valid.", user)));
            }
        }
        return res;
    }

}
