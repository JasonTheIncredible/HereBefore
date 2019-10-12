package co.clixel.herebefore;

// Getters and setters for messages.
public class MessageInformation {

    public String message;
    public Object date;
    public String uuid;

    MessageInformation(){
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setDate(Object date) {
        this.date = date;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }
}