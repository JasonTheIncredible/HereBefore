package co.clixel.herebefore;

import com.google.android.gms.maps.model.Circle;

// Getters and setters for chatCircles.
public class CircleInformation {

    private Circle circleInfo;
    private String uuid;

    CircleInformation(){
    }

    public Circle getCircle() {
        return circleInfo;
    }

    void setCircle(Circle circle) {
        this.circleInfo = circle;
    }

    public String getUuid() {
        return uuid;
    }

    void setUUID(String uuid) {
        this.uuid = uuid;
    }
}