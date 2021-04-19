package co.clixel.herebefore;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.Fragment;

import com.github.appintro.AppIntro;
import com.github.appintro.AppIntroFragment;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.firebase.auth.FirebaseAuth;

public class MyAppIntro extends AppIntro {

    private static final String TAG = "MyAppIntro";
    private boolean fromSettings = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        // Check if the user entered MyAppIntro from Settings.
        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            fromSettings = extras.getBoolean("fromSettings");
        }

        addSlide(AppIntroFragment.newInstance(
                "Welcome!",
                "Thanks for downloading Here Before!" + "\n" + "\n" + "Here Before enables users to leave location-based messages / pictures / videos in the world for others to find.",
                R.drawable.ic_logo_foreground,
                0xFF000582
        ));

        addSlide(AppIntroFragment.newInstance(
                "Entering a Circle",
                "To create a new circle at your current location, tap the large circle button to take a picture or hold the large circle button to take a video. To enter an existing circle, double tap a circle on the map.",
                R.drawable.myappintro_entering_circle,
                0xFF000582
        ));

        addSlide(AppIntroFragment.newInstance(
                "Leaving a Message",
                "New circles require a picture or video to be included in the first message. Any picture you take will be saved to your device's gallery. As circles are location-based, every message requires loading your location relative to the circle.",
                R.drawable.myappintro_leaving_a_message,
                0xFF000582
        ));

        addSlide(AppIntroFragment.newInstance(
                "Inside vs Outside a Circle",
                "Messages left by users inside a circle will be centered to the left of the screen, while messages left by users outside the circle will be centered to the right. The text at the top of the screen shows your current location relative to the circle.",
                R.drawable.myappintro_inside_vs_outside_a_circle,
                0xFF000582
        ));

        addSlide(AppIntroFragment.newInstance(
                "Sending a Direct Mention",
                "Messages left by others will include '@xxxxx...'. Type '@' followed by 'xxxxx' and select the corresponding option from the pop-up menu to leave a message which sends a notification to the respective user. If you need to see more letters when creating a mention, you can tap the '@xxxxx...' to expand it.",
                R.drawable.myappintro_sending_a_dm,
                0xFF000582
        ));

        addSlide(AppIntroFragment.newInstance(
                "Chat, Direct Mentions, and Settings",
                "You will see a bar at the bottom of your screen with three options. The first shows the circle's chat, the second shows any direct mentions (DMs) you have received, and the third shows the settings menu.",
                R.drawable.myappintro_chat_dms_settings,
                0xFF000582
        ));

        addSlide(AppIntroFragment.newInstance(
                "Your DMs",
                "Any DMs you receive can by found by tapping on the middle section of the bottom bar. Any unread DMs will be highlighted. You can tap on a DM to go to the circle where the DM was originally posted. DMs posted inside a circle will be centered to the left and DMs posted outside a circle will be centered to the right.",
                R.drawable.myappintro_your_dms,
                0xFF000582
        ));

        addSlide(AppIntroFragment.newInstance(
                "Map DMs, Settings and Appearance",
                "Once signed in, you can access the DMs and Settings menu without entering a circle by tapping on the corresponding buttons near the top right of the map. You can change the appearance of the map by tapping on the green button at the top left.",
                R.drawable.myappintro_map_dms_settings_appearance,
                0xFF000582
        ));

        addSlide(AppIntroFragment.newInstance(
                "Report Inappropriate Content",
                "To anonymously report inappropriate content, long press anywhere on a message until the 'Report Post' pop-up appears. Tap the pop-up and Here Before will be notified. Thanks for the help!",
                R.drawable.myappintro_report_content,
                0xFF000582
        ));
    }

    @Override
    protected void onSkipPressed(Fragment currentFragment) {

        super.onSkipPressed(currentFragment);
        Log.i(TAG, "onSkipPressed()");

        Intent Activity;
        if (fromSettings) {

            onBackPressed();
            finish();
            return;
        } else if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(MyAppIntro.this) != null) {

            // User signed in.
            Activity = new Intent(MyAppIntro.this, Map.class);
        } else {

            // User NOT signed in.
            Activity = new Intent(MyAppIntro.this, SignIn.class);
        }

        startActivity(Activity);
        finish();
    }

    @Override
    protected void onDonePressed(Fragment currentFragment) {

        super.onDonePressed(currentFragment);
        Log.i(TAG, "onDonePressed()");

        Intent Activity;
        if (fromSettings) {

            onBackPressed();
            finish();
            return;
        } else if (FirebaseAuth.getInstance().getCurrentUser() != null || GoogleSignIn.getLastSignedInAccount(MyAppIntro.this) != null) {

            // User signed in.
            Activity = new Intent(MyAppIntro.this, Map.class);
        } else {

            // User NOT signed in.
            Activity = new Intent(MyAppIntro.this, SignIn.class);
        }

        startActivity(Activity);
        finish();
    }
}
