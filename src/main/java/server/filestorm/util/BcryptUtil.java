package server.filestorm.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class BcryptUtil {
    
    public static String hash(String valueToHash) {
        String hashedResult = BCrypt.withDefaults().hashToString(12, valueToHash.toCharArray());
        return hashedResult;
    }

    public static Boolean verify(String plaintextPass, String hash) {
        BCrypt.Result res = BCrypt.verifyer().verify(plaintextPass.toCharArray(), hash);
        return res.verified;
    }
}
