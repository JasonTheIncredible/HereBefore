package co.clixel.herebefore;

import com.google.android.gms.maps.model.Circle;

public class CircleInformation {

    private Circle circleInfo;
    private String uuid;

    public CircleInformation(){
    }

    public Circle getCircle() {
        return circleInfo;
    }

    public void setCircle(Circle circle) {
        this.circleInfo = circle;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
