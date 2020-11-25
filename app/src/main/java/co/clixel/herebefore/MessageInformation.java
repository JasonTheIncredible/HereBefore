package co.clixel.herebefore;

import android.util.Pair;

import androidx.annotation.Keep;

import java.util.ArrayList;

@Keep
class MessageInformation {

    public String userUUID, message, imageUrl, videoUrl, email;
    public Object date;
    public Boolean userIsWithinShape;
    public int position;
    public ArrayList<Pair<String, Integer>> mentionPositionPairs;

    @Keep
    MessageInformation() {
    }

    public void setDate(Object date) {
        this.date = date;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public void setMentionPositionPairs(ArrayList<Pair<String, Integer>> mentionPositionPairs) {
        this.mentionPositionPairs = mentionPositionPairs;
    }

    public void setUserUUID(String userUUID) {
        this.userUUID = userUUID;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}