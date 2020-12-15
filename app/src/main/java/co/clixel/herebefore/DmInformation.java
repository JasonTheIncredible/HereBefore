package co.clixel.herebefore;

import androidx.annotation.Keep;

public class DmInformation {

    public String userUUID, message, shapeUUID, imageUrl, videoUrl;
    public Object date;
    public Boolean userIsWithinShape, seenByUser;
    public double size;
    public double lat, lon;

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

    public void setSeenByUser(Boolean seenByUser) {
        this.seenByUser = seenByUser;
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

    public void setUserUUID(String userUUID) {
        this.userUUID = userUUID;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
