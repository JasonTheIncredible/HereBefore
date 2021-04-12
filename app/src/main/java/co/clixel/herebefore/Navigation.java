package co.clixel.herebefore;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.gauravk.bubblenavigation.BubbleNavigationConstraintView;
import com.gauravk.bubblenavigation.listener.BubbleNavigationChangeListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Collections;
import java.util.Objects;

public class Navigation extends AppCompatActivity {

    private ViewPager viewPager;
    private BubbleNavigationConstraintView bubbleNavigationConstraintView;
    private ViewPager.OnPageChangeListener pagerListener;
    private int currentItem = -1, dmCounter = 0;
    private String firebaseUid;
    private Query mQuery;
    private ChildEventListener childEventListener;
    private boolean onStartJustCalled, noChat = false, fromDms = false, needToLoadCorrectTab = true, dmExists = false;
    private InterstitialAd mInterstitialAd;
    private ImageView splashScreen;
    private ProgressBar progressIconIndeterminate;
    private AdView bannerAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            // If noChat is true, hide Chat, as user entered this activity without clicking a shape.
            noChat = extras.getBoolean("noChat");
            fromDms = extras.getBoolean("fromDms");

            // The order is a bit awkward, but in order to change the interstitial ad splash screen color depending on the theme,
            // setContentView needs to be called, then findViewById, then updatePreferences.
            if (noChat) {

                setContentView(R.layout.navigationnochat);

                splashScreen = findViewById(R.id.splashScreen);
                progressIconIndeterminate = findViewById(R.id.progressIconIndeterminate);

                // Update to the user's preferences.
                updatePreferences();
            } else {

                setContentView(R.layout.navigation);

                splashScreen = findViewById(R.id.splashScreen);
                progressIconIndeterminate = findViewById(R.id.progressIconIndeterminate);

                // Update to the user's preferences.
                updatePreferences();

                showInterstitialAd();
            }
        }

        RequestConfiguration configuration = new RequestConfiguration.Builder().setTestDeviceIds(Collections.singletonList("814BF63877CBD71E91F9D7241907F4FF")).build();
        MobileAds.setRequestConfiguration(configuration);

        FrameLayout bannerAdFrameLayout = findViewById(R.id.bannerAdFrameLayout);
        bannerAdView = new AdView(this);
        bannerAdView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
        bannerAdFrameLayout.addView(bannerAdView);
        loadBanner();

        viewPager = findViewById(R.id.view_pager);
        bubbleNavigationConstraintView = findViewById(R.id.bottom_navigation_constraint);
    }

    protected void updatePreferences() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // theme == true is light mode.
        boolean theme = sharedPreferences.getBoolean(getString(R.string.prefTheme), false);

        if (theme) {

            // Set to light mode.
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO);

            splashScreen.setImageResource(R.color.gray);
        } else {

            // Set to dark mode.
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES);
        }

        // This will allow the settings button to appear in Map.java.
        sharedPreferences.edit().putBoolean(getString(R.string.prefSignOut), true).apply();
    }

    protected void showInterstitialAd() {

        // Hide the top bar while the ad loads. Remember to show the top bar after the ad goes away.
        Objects.requireNonNull(getSupportActionBar()).hide();

        splashScreen.setVisibility(View.VISIBLE);
        progressIconIndeterminate.setVisibility(View.VISIBLE);
        splashScreen.setZ(1000);
        progressIconIndeterminate.setZ(1000);

        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest, new InterstitialAdLoadCallback() {

            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {

                // The mInterstitialAd reference will be null until an ad is loaded.
                mInterstitialAd = interstitialAd;

                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {

                    @Override
                    public void onAdDismissedFullScreenContent() {

                        splashScreen.setZ(0);
                        progressIconIndeterminate.setZ(0);
                        splashScreen.setVisibility(View.GONE);
                        progressIconIndeterminate.setVisibility(View.GONE);
                        Objects.requireNonNull(getSupportActionBar()).show();
                        // Make sure to set your reference to null so you don't show it a second time.
                        mInterstitialAd = null;
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {

                        splashScreen.setZ(0);
                        progressIconIndeterminate.setZ(0);
                        splashScreen.setVisibility(View.GONE);
                        progressIconIndeterminate.setVisibility(View.GONE);
                        Objects.requireNonNull(getSupportActionBar()).show();
                        // Make sure to set your reference to null so you don't show it a second time.
                        mInterstitialAd = null;
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {

                        // Make sure to set your reference to null so you don't show it a second time.
                        mInterstitialAd = null;
                    }
                });

                mInterstitialAd.show(Navigation.this);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {

                // Make sure to set your reference to null so you don't show it a second time.
                mInterstitialAd = null;
                splashScreen.setZ(0);
                progressIconIndeterminate.setZ(0);
                splashScreen.setVisibility(View.GONE);
                progressIconIndeterminate.setVisibility(View.GONE);
                Objects.requireNonNull(getSupportActionBar()).show();
            }
        });
    }

    private void loadBanner() {

        AdRequest adRequest = new AdRequest.Builder().build();

        AdSize adSize = getAdSize();

        // Step 4 - Set the adaptive ad size on the ad view.
        bannerAdView.setAdSize(adSize);

        // Step 5 - Start loading the ad in the background.
        bannerAdView.loadAd(adRequest);
    }

    private AdSize getAdSize() {

        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        Display display = this.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);

        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }

    @Override
    protected void onRestart() {

        super.onRestart();

        dmCounter = 0;
    }

    @Override
    protected void onStart() {

        super.onStart();

        // Prevents duplicates in addQuery.
        onStartJustCalled = true;

        ScreenSlidePagerAdapter pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(pagerAdapter);

        if (pagerListener == null) {

            pagerListener = new ViewPager.OnPageChangeListener() {

                @Override
                public void onPageScrolled(int i, float v, int i1) {
                }

                @Override
                public void onPageSelected(int i) {

                    // Close the keyboard when switching fragments.
                    InputMethodManager imm = (InputMethodManager) getApplication().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {

                        imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
                    }

                    bubbleNavigationConstraintView.setCurrentActiveItem(i);
                }

                @Override
                public void onPageScrollStateChanged(int i) {
                }
            };
        }

        viewPager.addOnPageChangeListener(pagerListener);
        viewPager.setOffscreenPageLimit(2);

        BubbleNavigationChangeListener bubbleNavigationChangeListener = (view, position) -> {

            viewPager.setCurrentItem(position, true);

            currentItem = viewPager.getCurrentItem();
        };

        bubbleNavigationConstraintView.setNavigationChangeListener(bubbleNavigationChangeListener);

        firebaseUid = FirebaseAuth.getInstance().getUid();

        DatabaseReference Dms = FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUid).child("ReceivedDms");
        Dms.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot ds : snapshot.getChildren()) {

                    // Used in addQuery to allow the child to be called the first time if no child exists and prevent double posts if a child exists.
                    dmExists = true;

                    Boolean seenByUser = (Boolean) ds.child("seenByUser").getValue();
                    if (seenByUser != null && !seenByUser) {

                        dmCounter++;
                    }
                }

                // Set the DM tab's badge value.
                if (dmCounter == 0) {
                    // Do nothing.
                } else if (!fromDms && !noChat) {

                    bubbleNavigationConstraintView.setBadgeValue(1, String.valueOf(dmCounter));
                } else {

                    bubbleNavigationConstraintView.setBadgeValue(0, String.valueOf(dmCounter));
                }

                addQuery();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        // The user has changed the selected item. Reload with this selected item.
        if (currentItem != -1) {

            viewPager.setCurrentItem(currentItem, false);
            bubbleNavigationConstraintView.setCurrentActiveItem(currentItem);
        }

        // Load the correct tab.
        if (needToLoadCorrectTab) {

            if (fromDms || !noChat) {

                viewPager.setCurrentItem(0, false);
                bubbleNavigationConstraintView.setCurrentActiveItem(0);
                currentItem = 0;
            } else {

                viewPager.setCurrentItem(1, false);
                bubbleNavigationConstraintView.setCurrentActiveItem(1);
                currentItem = 1;
            }

            needToLoadCorrectTab = false;
        }
    }

    // Change to .limitToLast(1) to cut down on data usage. Otherwise, EVERY child at this node will be downloaded every time the child is updated.
    private void addQuery() {

        // This prevents duplicates when loading into Settings fragment then switched back into Chat (as onStop is never called but onStart is called).
        if (mQuery != null) {

            if (childEventListener != null) {

                mQuery.removeEventListener(childEventListener);
            }
        }

        if (mQuery == null) {

            mQuery = FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUid).child("ReceivedDms").limitToLast(1);
        }

        if (childEventListener == null) {

            childEventListener = new ChildEventListener() {

                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    // If this is the first time calling this eventListener, prevent double posts (as onStart() already added the last item).
                    if (onStartJustCalled && dmExists) {

                        onStartJustCalled = false;
                        return;
                    }

                    dmCounter++;

                    // Set the DM tab's badge value.
                    if (!fromDms && !noChat) {

                        bubbleNavigationConstraintView.setBadgeValue(1, String.valueOf(dmCounter));
                    } else {

                        bubbleNavigationConstraintView.setBadgeValue(0, String.valueOf(dmCounter));
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            };
        }

        mQuery.addChildEventListener(childEventListener);
    }

    @Override
    public void onStop() {

        if (viewPager != null) {

            if (pagerListener != null) {

                viewPager.removeOnPageChangeListener(pagerListener);
            }
        }

        if (bubbleNavigationConstraintView != null) {

            bubbleNavigationConstraintView.setNavigationChangeListener(null);
        }

        if (mQuery != null) {

            if (childEventListener != null) {

                mQuery.removeEventListener(childEventListener);
            }
        }

        super.onStop();
    }

    @Override
    public void onDestroy() {

        if (bannerAdView != null) {

            bannerAdView.removeAllViews();
            bannerAdView.destroy();
            bannerAdView = null;
        }

        super.onDestroy();
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    protected class ScreenSlidePagerAdapter extends FragmentPagerAdapter {

        ScreenSlidePagerAdapter(FragmentManager fm) {

            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {

            if (noChat) {

                switch (position) {
                    case 0:
                        return new DirectMentions();
                    case 1:
                        return new SettingsFragment();
                }
            } else {

                switch (position) {
                    case 0:
                        return new Chat();
                    case 1:
                        return new DirectMentions();
                    case 2:
                        return new SettingsFragment();
                }
            }

            return new SettingsFragment();
        }

        @Override
        public int getCount() {

            if (noChat) {

                return 2;
            } else {

                return 3;
            }
        }
    }
}
