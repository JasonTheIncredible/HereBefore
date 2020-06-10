package co.clixel.herebefore;

import androidx.annotation.Keep;

import com.google.android.gms.maps.model.CircleOptions;

@Keep
class CircleInformation {

    public CircleOptions circleOptions;
    public String shapeUUID;

    @Keep
    CircleInformation() {
    }

    void setCircleOptions(CircleOptions circleOptions) {
        this.circleOptions = circleOptions;
    }

    void setShapeUUID(String shapeUUID) {
        this.shapeUUID = shapeUUID;
    }
}