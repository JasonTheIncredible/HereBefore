package co.clixel.herebefore;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.UUID;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final int DEFAULT_NOTIFICATION_CHANNEL_ID = 2834;

    @Override
    public void onNewToken(@NonNull String token) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        SharedPreferences.Editor editor = sharedPreferences.edit()
                .putString("FIREBASE_TOKEN", token);
        editor.apply();
    }

    @Override
    public void onDeletedMessages() {

        super.onDeletedMessages();
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {

        String notificationBody = "";
        String notificationTitle = "";
        String notificationData = "";
        try {

            if (remoteMessage.getNotification() != null) {

                notificationData = remoteMessage.getData().toString();
                notificationTitle = remoteMessage.getNotification().getTitle();
                notificationBody = remoteMessage.getNotification().getBody();
            }
        } catch (NullPointerException e) {

            Log.e(TAG, "onMessageReceived: NullPointerException: " + e.getMessage());
        }

        Log.d(TAG, "onMessageReceived: data: " + notificationData);
        Log.d(TAG, "onMessageReceived: notification body: " + notificationBody);
        Log.d(TAG, "onMessageReceived: notification title: " + notificationTitle);


        String dataType = remoteMessage.getData().get("data_type");
        if (dataType != null) {

            if (dataType.equals("direct_message")) {

                Log.d(TAG, "onMessageReceived: new incoming message.");
                String title = remoteMessage.getData().get("title");
                String message = remoteMessage.getData().get("message");
                String messageId = remoteMessage.getData().get(UUID.randomUUID().toString());
                sendMessageNotification(title, message, messageId);
            }
        }
    }

    /**
     * Build a push notification for a chat message
     */
    private void sendMessageNotification(String title, String message, String messageId) {

        Log.d(TAG, "sendChatmessageNotification: building a chatmessage notification");

        //get the notification id
        int notificationId = buildNotificationId(messageId);

        // Instantiate a Builder object.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                getString(DEFAULT_NOTIFICATION_CHANNEL_ID));
        // Creates an Intent for the Activity
        Intent pendingIntent = new Intent(this, Map.class);
        // Sets the Activity to start in a new, empty task
        pendingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // Creates the PendingIntent
        PendingIntent notifyPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        pendingIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        // Add properties to the builder
        builder.setContentTitle(title)
                .setAutoCancel(true)
                .setSubText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setOnlyAlertOnce(true);

        builder.setContentIntent(notifyPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (mNotificationManager != null) {

            mNotificationManager.notify(notificationId, builder.build());
        }
    }


    private int buildNotificationId(String id) {

        Log.d(TAG, "buildNotificationId: building a notification id.");

        int notificationId = 0;
        for (int i = 0; i < 9; i++) {

            notificationId = notificationId + id.charAt(0);
        }

        Log.d(TAG, "buildNotificationId: id: " + id);
        Log.d(TAG, "buildNotificationId: notification id:" + notificationId);
        return notificationId;
    }
}
