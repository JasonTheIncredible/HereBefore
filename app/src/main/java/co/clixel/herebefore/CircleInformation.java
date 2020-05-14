package co.clixel.herebefore;

import androidx.annotation.Keep;

import com.google.android.gms.maps.model.CircleOptions;

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