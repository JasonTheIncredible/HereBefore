package co.clixel.herebefore;

import androidx.annotation.Keep;

// Getters and setters for messages.
@Keep
public class MessageInformation {

    public String message, uuid, imageURL, videoURL;
    public Object date;
    public Boolean userIsWithinShape;

    @Keep
    MessageInformation() {
    }

    public void setDate(Object date) {
        this.date = date;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public void setVideoURL(String videoURL) {
        this.videoURL = videoURL;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public void setUserIsWithinShape(Boolean userIsWithinShape) {
        this.userIsWithinShape = userIsWithinShape;
    }
}