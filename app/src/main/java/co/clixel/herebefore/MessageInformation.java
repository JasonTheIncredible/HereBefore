package co.clixel.herebefore;

// Getters and setters for messages.
public class MessageInformation {

    private String message;
    private Object date;
    private String uuid;

    public MessageInformation(){
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getDate() {
        return date;
    }

    public void setDate(Object date) {
        this.date = date;
    }

    public String getUUID() {
        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }
}