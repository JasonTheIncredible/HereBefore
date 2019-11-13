package co.clixel.herebefore;

import com.google.android.gms.maps.model.PolygonOptions;

// Getters and setters for chatCircles.
public class PolygonInformation {

    public PolygonOptions polygonOptions;
    public String uuid;

    PolygonInformation(){
    }

    void setPolygonOptions (PolygonOptions polygonOptions) {
        this.polygonOptions = polygonOptions;
    }

    void setUUID(String uuid) {
        this.uuid = uuid;
    }
}