package co.clixel.herebefore;

import androidx.annotation.Keep;

import com.google.android.gms.maps.model.CircleOptions;

// Getters and setters for chatCircles.
@Keep
public class CircleInformation {

    public CircleOptions circleOptions;
    public String uuid;

    @Keep
    CircleInformation() {
    }

    void setCircleOptions(CircleOptions circleOptions) {
        this.circleOptions = circleOptions;
    }

    void setUUID(String uuid) {
        this.uuid = uuid;
    }
}