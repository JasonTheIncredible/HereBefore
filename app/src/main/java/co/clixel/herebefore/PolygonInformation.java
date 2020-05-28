package co.clixel.herebefore;

import androidx.annotation.Keep;

import com.google.android.gms.maps.model.PolygonOptions;

@Keep
class PolygonInformation {

    public PolygonOptions polygonOptions;
    public double polygonArea;
    public String uuid;

    @Keep
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