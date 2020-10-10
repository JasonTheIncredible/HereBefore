package co.clixel.herebefore;

import androidx.annotation.Keep;

@Keep
class MessageInformation {

    public String userUUID, message, imageURL, videoURL, email;
    public Object date;
    public Boolean userIsWithinShape;
    public int position;

    @Keep
    MessageInformation() {
    }

    public void setDate(Object date) {
        this.date = date;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setUserIsWithinShape(Boolean userIsWithinShape) {
        this.userIsWithinShape = userIsWithinShape;
    }

    public void setUserUUID(String userUUID) {
        this.userUUID = userUUID;
    }

    public void setVideoURL(String videoURL) {
        this.videoURL = videoURL;
    }
}