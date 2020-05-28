package co.clixel.herebefore;

import androidx.annotation.Keep;

@Keep
class FeedbackInformation {

    public String feedback;
    public Object date;

    @Keep
    FeedbackInformation() {
    }

    public void setDate(Object date) {
        this.date = date;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}