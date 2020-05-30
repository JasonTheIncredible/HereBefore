package co.clixel.herebefore;

import androidx.annotation.Keep;

@Keep
class UserInformation {

    public String userUUID, email, token;

    @Keep
    UserInformation() {
    }

    public void setUserUUID(String userUUID) {
        this.userUUID = userUUID;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setToken(String token) {
        this.token = token;
    }
}