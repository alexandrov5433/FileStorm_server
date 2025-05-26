package server.filestorm.model.type.authentication;

public class AuthValidationResult {
    public String fieldName;
    public Boolean isValid;
    public String message;

    public AuthValidationResult(String fieldName, Boolean isValid, String message) {
        this.fieldName = fieldName;
        this.isValid = isValid;
        this.message = message;
    }
}
