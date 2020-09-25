package co.clixel.herebefore;

import androidx.annotation.Keep;

import java.util.ArrayList;

@Keep
class MessageInformation {

    public ArrayList<String> removedMentionDuplicates;
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

    public void setUserUUID(String userUUID) {
        this.userUUID = userUUID;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public void setPosition(int position) {
        this.position = position;
    }

    public void setRemovedMentionDuplicates(ArrayList<String> removedMentionDuplicates) {
        this.removedMentionDuplicates = removedMentionDuplicates;
    }

    public void setUserIsWithinShape(Boolean userIsWithinShape) {
        this.userIsWithinShape = userIsWithinShape;
    }
}