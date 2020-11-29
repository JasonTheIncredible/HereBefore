package co.clixel.herebefore;

import android.util.Pair;

import androidx.annotation.Keep;

import java.util.ArrayList;

public class DmInformation {

    public String userUUID, message, shapeUUID, imageUrl, videoUrl;
    public Object date;
    public Boolean userIsWithinShape, seenByUser, shapeIsCircle;
    public int position;
    public double size;
    public double lat, lon;
    public ArrayList<Pair<String, Integer>> userPositionPairs;

    @Keep
    DmInformation() {
    }

    public void setDate(Object date) {
        this.date = date;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setSeenByUser(Boolean seenByUser) {
        this.seenByUser = seenByUser;
    }

    public void setShapeIsCircle(Boolean shapeIsCircle) {
        this.shapeIsCircle = shapeIsCircle;
    }

    public void setShapeUUID(String shapeUUID) {
        this.shapeUUID = shapeUUID;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public void setUserIsWithinShape(Boolean userIsWithinShape) {
        this.userIsWithinShape = userIsWithinShape;
    }

    public void setUserPositionPairs(ArrayList<Pair<String, Integer>> userPositionPairs) {
        this.userPositionPairs = userPositionPairs;
    }

    public void setUserUUID(String userUUID) {
        this.userUUID = userUUID;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
