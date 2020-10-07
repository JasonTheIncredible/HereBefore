package co.clixel.herebefore;

public class ReportPostInformation {

    public String pushID, shapeUUID;
    public int lat, lon;

    ReportPostInformation() {
    }

    void setLat(int lat) {
        this.lat = lat;
    }

    void setLon(int lon) {
        this.lon = lon;
    }

    void setPushID(String pushID) {
        this.pushID = pushID;
    }

    void setShapeUUID(String shapeUUID) {
        this.shapeUUID = shapeUUID;
    }
}