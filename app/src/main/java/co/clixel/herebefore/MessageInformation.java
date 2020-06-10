package co.clixel.herebefore;

import androidx.annotation.Keep;

import java.util.ArrayList;

@Keep
class MessageInformation {

    public ArrayList removedMentionDuplicates;
    public String userUUID, message, shapeUUID, token, imageURL, videoURL;
    public Object date;
    public Boolean userIsWithinShape;

    @Keep
    MessageInformation() {
    }

    public void setDate(Object date) {
        this.date = date;
    }

    public void setUserUUID(String userUUID) {
        this.userUUID = userUUID;
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

    public void setShapeUUID(String shapeUUID) {
        this.shapeUUID = shapeUUID;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setRemovedMentionDuplicates(ArrayList removedMentionDuplicates) {
        this.removedMentionDuplicates = removedMentionDuplicates;
    }

    public void setUserIsWithinShape(Boolean userIsWithinShape) {
        this.userIsWithinShape = userIsWithinShape;
    }
}