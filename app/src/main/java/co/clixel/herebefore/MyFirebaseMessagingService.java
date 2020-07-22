package co.clixel.herebefore;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String DEFAULT_NOTIFICATION_CHANNEL_ID = "DEFAULT_NOTIFICATION_CHANNEL_ID";
    private static final int PENDING_INTENT_REQ_CODE = 69;

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

        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "onMessageReceived: New incoming message.");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        boolean notifications = sharedPreferences.getBoolean(SettingsFragment.KEY_NOTIFICATIONS_SWITCH, true);

        // If user turned off notifications, return.
        if (!notifications) {

            Log.d(TAG, "onMessageReceived -> return");
            return;
        }

        String title = remoteMessage.getData().get("title");
        String message = remoteMessage.getData().get("body");
        String notificationId = remoteMessage.getData().get("notification_id");
        sendMessageNotification(title, message, notificationId);
    }

    /**
     * Build a push notification for a chat message
     */
    private void sendMessageNotification(String title, String message, String notificationId) {

        Log.d(TAG, "sendChatmessageNotification: building a chatmessage notification");

        Intent intent = new Intent(getBaseContext(), Map.class);
        intent.putExtra("notification_id", notificationId);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, PENDING_INTENT_REQ_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Instantiate a Builder object.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                DEFAULT_NOTIFICATION_CHANNEL_ID);

        // Add properties to the builder
        builder.setContentTitle(title)
                .setAutoCancel(true)
                .setSubText(message)
                .setSmallIcon(R.mipmap.ic_logo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (mNotificationManager != null) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                NotificationChannel channel = new NotificationChannel(DEFAULT_NOTIFICATION_CHANNEL_ID, "Default channel", NotificationManager.IMPORTANCE_DEFAULT);
                mNotificationManager.createNotificationChannel(channel);
            }

            mNotificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}
