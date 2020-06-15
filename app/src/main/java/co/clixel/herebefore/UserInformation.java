package co.clixel.herebefore;

import androidx.annotation.Keep;

@Keep
class UserInformation {

    public String email, userUUID;

    @Keep
    UserInformation() {
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUserUUID(String userUUID) {
        this.userUUID = userUUID;
    }
}