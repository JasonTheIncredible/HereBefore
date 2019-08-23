package co.clixel.herebefore;

import com.google.android.gms.maps.model.Circle;

// Getters and setters for chatCircles.
public class CircleInformation {

    private Circle circleInfo;

    public CircleInformation(){
    }

    public Circle getCircle() {
        return circleInfo;
    }

    public void setCircle(Circle circle) {
        this.circleInfo = circle;
    }
}