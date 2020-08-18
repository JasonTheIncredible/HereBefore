package co.clixel.herebefore;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.github.appintro.AppIntro;
import com.github.appintro.AppIntroFragment;

public class MyAppIntro extends AppIntro {

    private static final String TAG = "AppIntro";;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        addSlide(AppIntroFragment.newInstance(
                "Welcome!",
                "Thanks for downloading Here Before!",
                R.drawable.ic_logo_foreground,
                0xFF663399
        ));

        addSlide(AppIntroFragment.newInstance(
                "Overview",
                "Here Before enables users to create permanent location-based chat rooms and anonymously leave messages / pictures / videos in the world for others to find.",
                R.drawable.ic_logo_foreground,
                0xFF663399
        ));

        addSlide(AppIntroFragment.newInstance(
                "Creating Chats",
                "Use the seekbar at the bottom of the Map to create a circular Chat, or use the menu to the left of the seekbar to create a polygonal, circular, or 'point' (small and focused around your location) Chat - choosing this option will immediately create and enter that shape's chat room.",
                R.drawable.myappintro_creating_chats,
                0xFF663399
        ));

        addSlide(AppIntroFragment.newInstance(
                "Customizing and Entering Chat",
                "Drag the markers connected to a Chat to customize its size, shape, and location. Click on a Chat to enter the corresponding chat room."
        ));

        addSlide(AppIntroFragment.newInstance(
                "Chat, Direct Messages, and Settings",
                "After entering a Chat, you will see a bar at the bottom with three options. The first one is the chat room. The second shows any Direct Messages (DMs) you have received. The third is the Settings menu."
        ));

        addSlide(AppIntroFragment.newInstance(
                "Messaging Inside vs Outside the Chat",
                "If you entered a Chat while your location was inside the shape on the map, your message will appear on the left side of the chat room. If your location was outside the shape, your message will appear on the right side."
        ));

        addSlide(AppIntroFragment.newInstance(
                "Sending a Direct Message",
                "Messages left by other users will include '@xxxxx...'. Type '@xxxxx' and select the corresponding option from the popup menu to message that user directly."
        ));

        addSlide(AppIntroFragment.newInstance(
                "Your Direct Messages",
                "Any DMs you receive can by found by clicking on the middle section of the bottom bar. Any new and unread DMs will be highlighted. You can click on a specific DM to immediately go to the Chat where that DM was originally posted."
        ));

        addSlide(AppIntroFragment.newInstance(
                "DMs and Settings from Map",
                "You can access the DMs and Settings menu without entering a Chat by clicking on the corresponding buttons on the Map. If you have any unread DMs, a number will appear over the DMs button."
        ));
    }

    @Override
    protected void onSkipPressed(Fragment currentFragment) {

        super.onSkipPressed(currentFragment);
        Log.i(TAG, "onSkipPressed()");

        sharedPreferences.edit().putBoolean(SettingsFragment.KEY_SHOW_INTRO, false).apply();

        Intent Activity = new Intent(this, Map.class);
        startActivity(Activity);
        finish();
    }

    @Override
    protected void onDonePressed(Fragment currentFragment) {

        super.onDonePressed(currentFragment);
        Log.i(TAG, "onDonePressed()");

        sharedPreferences.edit().putBoolean(SettingsFragment.KEY_SHOW_INTRO, false).apply();

        Intent Activity = new Intent(this, Map.class);
        startActivity(Activity);
        finish();
    }
}
