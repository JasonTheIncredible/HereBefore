package co.clixel.herebefore;

import androidx.annotation.Keep;

@Keep
class UserInformation {

    public String uuid, email, token;

    @Keep
    UserInformation() {
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setToken(String token) {
        this.token = token;
    }
}