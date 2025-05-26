package server.filestorm.model.type.authentication;

import java.util.regex.Pattern;

public class RegistrationData {
    private String username;
    private String password;
    private String repassword;
    private String email;

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setRepassword(String repassword) {
        this.repassword = repassword;
    }

    public String getRepassword() {
        return repassword;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public AuthValidationResult[] getValidationData() {
        return new AuthValidationResult[] {
                this.validateUsername(),
                this.validatePassword(),
                this.validateRepassword(),
                this.validateEmail()
        };
    }

    public Boolean isDataValid() {
        AuthValidationResult[] arr = this.getValidationData();
        for (int i = 0; i < arr.length; i++) {
            if (!arr[i].isValid) {
                return false;
            }
        }
        return true;
    }

    private AuthValidationResult validateUsername() {
        boolean res = Pattern.matches("^[A-Za-z0-9_]{1,30}$", this.username == null ? "" : this.username);
        return new AuthValidationResult("username", res, res ? "" : "Username is not valid.");
    }

    private AuthValidationResult validatePassword() {
        boolean res = Pattern.matches("^[A-Za-z0-9@_+?!-]{5,50}$", this.password == null ? "" : this.password);
        return new AuthValidationResult("password", res, res ? "" : "Password is not valid.");
    }

    private AuthValidationResult validateRepassword() {
        boolean res;
        if (this.password == null || this.repassword == null) {
            res = false;
        } else {
            res = this.password.equals(this.repassword);
        }
        return new AuthValidationResult("repassword", res, res ? "" : "Passwords do not match.");
    }

    private AuthValidationResult validateEmail() {
        boolean res;
        if (this.email == null) {
            res = false;
        } else {
            res = Pattern.matches(
                    "^([^\\x00-\\x20\\x22\\x28\\x29\\x2c\\x2e\\x3a-\\x3c\\x3e\\x40\\x5b-\\x5d\\x7f-\\xff]+|\\x22([^\\x0d\\x22\\x5c\\x80-\\xff]|\\x5c[\\x00-\\x7f])*\\x22)(\\x2e([^\\x00-\\x20\\x22\\x28\\x29\\x2c\\x2e\\x3a-\\x3c\\x3e\\x40\\x5b-\\x5d\\x7f-\\xff]+|\\x22([^\\x0d\\x22\\x5c\\x80-\\xff]|\\x5c[\\x00-\\x7f])*\\x22))*\\x40([^\\x00-\\x20\\x22\\x28\\x29\\x2c\\x2e\\x3a-\\x3c\\x3e\\x40\\x5b-\\x5d\\x7f-\\xff]+|\\x5b([^\\x0d\\x5b-\\x5d\\x80-\\xff]|\\x5c[\\x00-\\x7f])*\\x5d)(\\x2e([^\\x00-\\x20\\x22\\x28\\x29\\x2c\\x2e\\x3a-\\x3c\\x3e\\x40\\x5b-\\x5d\\x7f-\\xff]+|\\x5b([^\\x0d\\x5b-\\x5d\\x80-\\xff]|\\x5c[\\x00-\\x7f])*\\x5d))*$",
                    this.email);
            if (res && !this.email.substring(this.email.indexOf("@")).contains(".")) {
                // check is done only when res is true it insure the existance of "@" in the
                // string. Otherwise String.substring throws an error because String.indexOf
                // returns -1, which is an invalid starting index.
                res = false;
            }
        }
        return new AuthValidationResult("email", res, res ? "" : "Email is not valid.");
    }
}
