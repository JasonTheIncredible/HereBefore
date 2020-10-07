package co.clixel.herebefore;

import androidx.annotation.Keep;

import com.google.android.gms.maps.model.PolygonOptions;

@Keep
class PolygonInformation {

    public PolygonOptions polygonOptions;
    public double polygonArea;
    public String shapeUUID;

    @Keep
    PolygonInformation() {
    }

    void setArea(double polygonArea) {
        this.polygonArea = polygonArea;
    }

    void setPolygonOptions(PolygonOptions polygonOptions) {
        this.polygonOptions = polygonOptions;
    }

    void setShapeUUID(String shapeUUID) {
        this.shapeUUID = shapeUUID;
    }
}