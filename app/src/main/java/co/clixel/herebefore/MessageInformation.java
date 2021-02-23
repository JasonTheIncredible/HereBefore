package co.clixel.herebefore;

import androidx.annotation.Keep;

@Keep
class MessageInformation {

    public String userUUID, message, imageUrl, videoUrl, firebaseUid;
    public Object date;
    public Boolean userIsWithinShape;

    @Keep
    MessageInformation() {
    }

    public void setDate(Object date) {
        this.date = date;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setUserIsWithinShape(Boolean userIsWithinShape) {
        this.userIsWithinShape = userIsWithinShape;
    }

    public void setUserUUID(String userUUID) {
        this.userUUID = userUUID;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}