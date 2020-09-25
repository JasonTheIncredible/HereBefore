package co.clixel.herebefore;

import androidx.annotation.Keep;

public class DMInformation {

    public String userUUID, message, shapeUUID, imageURL, videoURL, email;
    public Object date;
    public Boolean userIsWithinShape, seenByUser, shapeIsCircle;
    public int position;
    public double size;
    // Should be Double instead of double so that it can be null and won't take up space in Firebase.
    public Double lat, lon;

    @Keep
    DMInformation() {
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

    public void setShapeUUID(String shapeUUID) {
        this.shapeUUID = shapeUUID;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setUserIsWithinShape(Boolean userIsWithinShape) {
        this.userIsWithinShape = userIsWithinShape;
    }

    public void setShapeIsCircle(Boolean shapeIsCircle) {
        this.shapeIsCircle = shapeIsCircle;
    }

    public void setSeenByUser(Boolean seenByUser) {
        this.seenByUser = seenByUser;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }
}
