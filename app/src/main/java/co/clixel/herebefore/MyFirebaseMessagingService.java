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

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMessagingService";
    private static final String DEFAULT_NOTIFICATION_CHANNEL_ID = "DEFAULT_NOTIFICATION_CHANNEL_ID";
    private static final int PENDING_INTENT_REQ_CODE = 69;

    @Override
    public void onNewToken(@NonNull final String token) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        SharedPreferences.Editor editor = sharedPreferences.edit()
                .putString("FIREBASE_TOKEN", token);
        editor.apply();

        // If user has a Google account, get email one way. Else, get email another way.
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        String email;
        if (acct != null) {

            email = acct.getEmail();
        } else {

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            email = sharedPreferences.getString("userToken", "null");
        }

        // Firebase does not allow ".", so replace them with ",".
        String userEmailFirebase = email.replace(".", ",");

        FirebaseDatabase.getInstance().getReference().child("Users").child(userEmailFirebase).child("Token").setValue("token", token);
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
        String shapeUUID = remoteMessage.getData().get("shapeUUID");
        String userUUID = remoteMessage.getData().get("userUUID");
        Double lat = Double.parseDouble(remoteMessage.getData().get("lat"));
        Double lon = Double.parseDouble(remoteMessage.getData().get("lon"));
        sendMessageNotification(title, message, notificationId, shapeUUID, userUUID, lat, lon);
    }

    /**
     * Build a push notification for a chat message
     */
    private void sendMessageNotification(String title, String message, String notificationId, String shapeUUID, String userUUID, Double lat, Double lon) {

        Log.d(TAG, "sendDmNotification: building a DM notification");

        Intent intent = new Intent(getBaseContext(), Map.class);
        intent.putExtra("notification_id", notificationId);
        intent.putExtra("shapeUUID", shapeUUID);
        intent.putExtra("userUUID", userUUID);
        intent.putExtra("lat", lat);
        intent.putExtra("lon", lon);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, PENDING_INTENT_REQ_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Instantiate a Builder object.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                DEFAULT_NOTIFICATION_CHANNEL_ID);

        // Add properties to the builder.
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
