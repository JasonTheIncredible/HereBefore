package co.clixel.herebefore;

// Getters and setters for messages.
public class MessageInformation {

    public String message;
    public Object date;
    public String uuid, imageURL, videoURL;

    MessageInformation() {
    }

    public void setDate(Object date) {
        this.date = date;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public void setVideoURL(String videoURL) {
        this.videoURL = videoURL;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }
}