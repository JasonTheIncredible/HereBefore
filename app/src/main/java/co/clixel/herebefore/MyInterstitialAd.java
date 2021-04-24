package co.clixel.herebefore;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Objects;

public class MyInterstitialAd extends AppCompatActivity {

    private ArrayList<String> circleUUIDsAL = new ArrayList<>();
    private ArrayList<LatLng> circleCentersAL = new ArrayList<>();
    private boolean theme, fromDms = false, newShape, fromNavigation = false;
    private String shapeUUID, UUIDToHighlight, imageFile, videoFile, lastKnownKey;
    private Double shapeLat, shapeLon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        updatePreferences();

        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            fromDms = extras.getBoolean("fromDms");
            UUIDToHighlight = extras.getString("UUIDToHighlight");
            fromDms = extras.getBoolean("fromDms");
            newShape = extras.getBoolean("newShape");
            // No need to get these if it's a new shape, as the most up-to-date location information will be received here.
            if (!newShape) {
                shapeLat = extras.getDouble("shapeLat");
                shapeLon = extras.getDouble("shapeLon");
            } else {
                //noinspection unchecked
                circleUUIDsAL = (ArrayList<String>) extras.getSerializable("circleUUIDsAL");
                //noinspection unchecked
                circleCentersAL = (ArrayList<LatLng>) extras.getSerializable("circleCentersAL");
            }
            shapeUUID = extras.getString("shapeUUID");
            imageFile = extras.getString("imageFile");
            videoFile = extras.getString("videoFile");
            lastKnownKey = extras.getString("lastKnownKey");
            fromNavigation = extras.getBoolean("fromNavigation");
        }

        setContentView(R.layout.myinterstitialad);

        ImageView splashScreen = findViewById(R.id.splashScreen);
        // theme == true is light mode.
        if (theme) {

            splashScreen.setImageResource(R.color.gray);
        }

        showInterstitialAd();
    }

    protected void updatePreferences() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // theme == true is light mode.
        theme = sharedPreferences.getBoolean(getString(R.string.prefTheme), false);

        if (theme) {

            // Set to light mode.
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO);
        } else {

            // Set to dark mode.
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    protected void showInterstitialAd() {

        Objects.requireNonNull(getSupportActionBar()).hide();

        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest, new InterstitialAdLoadCallback() {

            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {

                interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {

                    @Override
                    public void onAdDismissedFullScreenContent() {

                        goToNextActivity();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {

                        goToNextActivity();
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                    }
                });

                interstitialAd.show(MyInterstitialAd.this);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {

                goToNextActivity();
            }
        });
    }

    private void goToNextActivity() {

        // If an interstitial ad is being loaded while user is in Navigation (eg they uploaded multiple photos), then just go back.
        if (fromNavigation) {

            onBackPressed();
            finish();
            return;
        }

        Intent Activity = new Intent(MyInterstitialAd.this, Navigation.class);
        Activity.putExtra("noChat", false);
        Activity.putExtra("UUIDToHighlight", UUIDToHighlight);
        Activity.putExtra("fromDms", fromDms);
        Activity.putExtra("newShape", newShape);
        if (!newShape) {

            Activity.putExtra("shapeLat", shapeLat);
            Activity.putExtra("shapeLon", shapeLon);
        } else {

            Activity.putExtra("circleCentersAL", circleCentersAL);
            Activity.putExtra("circleUUIDsAL", circleUUIDsAL);
        }

        // Pass this value to Chat.java to identify the shape.
        Activity.putExtra("shapeUUID", shapeUUID);
        Activity.putExtra("imageFile", imageFile);
        Activity.putExtra("videoFile", videoFile);
        Activity.putExtra("lastKnownKey", lastKnownKey);
        startActivity(Activity);
        finish();
    }
}
