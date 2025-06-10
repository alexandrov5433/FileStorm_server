package server.filestorm.model.type;

import java.util.Map;

public class CustomSession {

    private Map<String, Object> claims;
    private Long user_id;
    private String username;

    public CustomSession(Map<String, Object> claims) {
        this.claims = claims;
        this.user_id = (Long) this.claims.get("id");
        this.username = (String) this.claims.get("username");
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    public Long getUserId() {
        return user_id;
    }

    public String getUsername() {
        return username;
    }
}
