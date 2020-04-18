package co.clixel.herebefore;

import com.google.android.gms.maps.model.CircleOptions;

// Getters and setters for chatCircles.
public class ReportPost {

    public String uuid;
    public int position;

    ReportPost() {
    }

    void setUUID(String uuid) {
        this.uuid = uuid;
    }

    void setPosition(int position) {
        this.position = position;
    }
}