package co.clixel.herebefore;

// Getters and setters for chatCircles.
public class ReportPostInformation {

    public String pushId, shapeUUID;
    public int lat, lon;

    ReportPostInformation() {
    }

    void setPushId(String pushId) {
        this.pushId = pushId;
    }

    void setLat(int lat) {
        this.lat = lat;
    }

    void setLon(int lon) {
        this.lon = lon;
    }

    void setShapeUUID(String shapeUUID) {
        this.shapeUUID = shapeUUID;
    }
}