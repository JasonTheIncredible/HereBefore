package co.clixel.herebefore;

import com.google.android.gms.maps.model.CircleOptions;

// Getters and setters for chatCircles.
public class CircleInformation {

    public CircleOptions circleOptions;
    public String uuid;

    CircleInformation(){
    }

    void setCircleOptions (CircleOptions circleOptions) {
        this.circleOptions = circleOptions;
    }

    void setUUID(String uuid) {
        this.uuid = uuid;
    }
}