package co.clixel.herebefore;

import com.google.android.gms.maps.model.PolygonOptions;

// Getters and setters for chatCircles.
public class PolygonInformation {

    public PolygonOptions polygonOptions;
    public double polygonArea;
    public String uuid;

    PolygonInformation() {
    }

    void setPolygonOptions(PolygonOptions polygonOptions) {
        this.polygonOptions = polygonOptions;
    }

    void setArea(double polygonArea) {
        this.polygonArea = polygonArea;
    }

    void setUUID(String uuid) {
        this.uuid = uuid;
    }
}