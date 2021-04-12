package co.clixel.herebefore;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.mikepenz.aboutlibraries.LibsBuilder;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements
        PreferenceManager.OnPreferenceTreeClickListener {

    private SwitchPreferenceCompat toggleTheme, toggleNotifications;
    private Preference mapTypePreference, progressIconIndeterminate, rewardAd;
    private RewardedAd mRewardAd;
    private Toast longToast;
    // "FIREBASE_TOKEN" to find Firebase token for messaging.

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Check if the account is a Google account. If not, hide "Reset Password".
        if (GoogleSignIn.getLastSignedInAccount(requireContext()) == null) {

            setPreferencesFromResource(R.xml.preferences_herebefore, rootKey);
        } else {

            setPreferencesFromResource(R.xml.preferences_google, rootKey);
        }

        mapTypePreference = findPreference("mapTypePreference");
        toggleNotifications = findPreference("toggleNotifications");
        toggleTheme = findPreference("toggleTheme");
        progressIconIndeterminate = findPreference("progressIconIndeterminateLayout");
        rewardAd = findPreference("rewardAd");

        if (mapTypePreference != null) {

            // Sets the default value according to the user's current preference.
            ListPreference mapTypeListPreference = findPreference("mapTypePreference");
            if (mapTypeListPreference != null) {

                String preferredMapType = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(getString(R.string.prefMapType), getResources().getString(R.string.use_hybrid_view));
                if (preferredMapType != null) {

                    if (preferredMapType.equals(getString(R.string.use_road_map_view))) {

                        mapTypeListPreference.setValueIndex(0);
                    } else if (preferredMapType.equals(getString(R.string.use_satellite_view))) {

                        mapTypeListPreference.setValueIndex(1);
                    } else if (preferredMapType.equals(getString(R.string.use_hybrid_view))) {

                        mapTypeListPreference.setValueIndex(2);
                    } else if (preferredMapType.equals(getString(R.string.use_terrain_view))) {

                        mapTypeListPreference.setValueIndex(3);
                    }
                } else {

                    // Make "Hybrid view" the default.
                    mapTypeListPreference.setValueIndex(2);
                }
            }

            mapTypePreference.setOnPreferenceChangeListener((preference, newValue) -> {

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext());
                SharedPreferences.Editor editor = sharedPref.edit();

                switch (newValue.toString()) {

                    case "Road map view":

                        editor.putString(getString(R.string.prefMapType), getString(R.string.use_road_map_view));
                        editor.apply();
                        break;

                    case "Satellite view":

                        editor.putString(getString(R.string.prefMapType), getString(R.string.use_satellite_view));
                        editor.apply();
                        break;

                    case "Hybrid view":

                        editor.putString(getString(R.string.prefMapType), getString(R.string.use_hybrid_view));
                        editor.apply();
                        break;

                    case "Terrain view":

                        editor.putString(getString(R.string.prefMapType), getString(R.string.use_terrain_view));
                        editor.apply();
                        break;
                }

                return true;
            });
        }

        if (toggleNotifications != null) {

            toggleNotifications.setOnPreferenceClickListener((Preference pref) -> {

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(getString(R.string.prefNotifications), toggleNotifications.isChecked());
                editor.apply();

                return true;
            });
        }

        if (toggleTheme != null) {

            toggleTheme.setOnPreferenceClickListener((Preference pref) -> {

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(getString(R.string.prefTheme), toggleTheme.isChecked());
                editor.apply();

                if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {

                    Snackbar snackBar = Snackbar.make(requireView(), "Theme will change on activity reload.", Snackbar.LENGTH_LONG);
                    snackBar.setAnchorView(requireActivity().findViewById(R.id.bottom_navigation_constraint));
                    View snackBarView = snackBar.getView();
                    TextView snackTextView = snackBarView.findViewById(com.google.android.material.R.id.snackbar_text);
                    snackTextView.setMaxLines(10);
                    snackBar.show();
                } else {

                    Toast longToast = Toast.makeText(getContext(), "Theme will change on activity reload.", Toast.LENGTH_LONG);
                    longToast.setGravity(Gravity.CENTER, 0, 0);
                    longToast.show();
                }

                return true;
            });
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {

        String key = preference.getKey();

        if (getActivity() != null) {

            switch (key) {

                case "introduction": {

                    Intent Activity = new Intent(getActivity(), MyAppIntro.class);

                    Activity.putExtra("fromSettings", true);

                    startActivity(Activity);

                    break;
                }

                case "resetPassword": {

                    Intent Activity = new Intent(getActivity(), ResetPassword.class);

                    startActivity(Activity);

                    break;
                }

                case "feedback": {

                    Intent Activity = new Intent(getActivity(), Feedback.class);

                    startActivity(Activity);

                    break;
                }

                case "signOut": {

                    AuthUI.getInstance().signOut(requireActivity());
                    PreferenceManager.getDefaultSharedPreferences(requireActivity()).edit().clear().apply();

                    // Remove the token so user will not get notifications while they are not logged into their account.
                    String firebaseUid = FirebaseAuth.getInstance().getUid();
                    if (firebaseUid == null) {

                        break;
                    }

                    FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUid).child("Token").removeValue();

                    Toast signedOutToast = Toast.makeText(getActivity(), "Signed Out", Toast.LENGTH_SHORT);
                    signedOutToast.setGravity(Gravity.CENTER, 0, 250);
                    signedOutToast.show();

                    Intent Activity = new Intent(getActivity(), Map.class);

                    requireActivity().finishAffinity();

                    startActivity(Activity);

                    break;
                }

                case "deleteAccount": {

                    Intent Activity = new Intent(getActivity(), DeleteAccount.class);

                    startActivity(Activity);

                    break;
                }

                case "rewardAd": {

                    // Show loading icon and get rid of rewardAd.
                    progressIconIndeterminate.setVisible(true);
                    rewardAd.setVisible(false);

                    AdRequest adRequest = new AdRequest.Builder().build();

                    RewardedAd.load(requireContext(), "ca-app-pub-3940256099942544/5224354917", adRequest, new RewardedAdLoadCallback() {

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {

                            progressIconIndeterminate.setVisible(false);
                            rewardAd.setVisible(true);

                            // Make sure to set your reference to null so you don't show it a second time.
                            mRewardAd = null;
                        }

                        @Override
                        public void onAdLoaded(@NonNull RewardedAd rewardedAd) {

                            // The mRewardAd reference will be null until an ad is loaded.
                            mRewardAd = rewardedAd;

                            mRewardAd.setFullScreenContentCallback(new FullScreenContentCallback() {

                                @Override
                                public void onAdShowedFullScreenContent() {

                                    // For interstitial ads in Navigation, the visible logic shouldn't appear in onAdShowedFullScreenContent because the user can see it.
                                    // However, since the visible logic takes a while to occur in preferences, it can happen here so the user doesn't see it occur after the ad is dismissed.
                                    progressIconIndeterminate.setVisible(false);
                                    rewardAd.setVisible(true);

                                    // Make sure to set your reference to null so you don't show it a second time.
                                    mRewardAd = null;
                                }

                                @Override
                                public void onAdFailedToShowFullScreenContent(AdError adError) {

                                    showMessageLong("An error occurred: " + adError);

                                    progressIconIndeterminate.setVisible(false);
                                    rewardAd.setVisible(true);

                                    // Make sure to set your reference to null so you don't show it a second time.
                                    mRewardAd = null;
                                }

                                @Override
                                public void onAdDismissedFullScreenContent() {

                                    progressIconIndeterminate.setVisible(false);
                                    rewardAd.setVisible(true);

                                    // Make sure to set your reference to null so you don't show it a second time.
                                    mRewardAd = null;
                                }
                            });

                            if (mRewardAd != null) {

                                mRewardAd.show(requireActivity(), rewardItem -> {

                                    int rewardAmount = rewardItem.getAmount();
                                    String rewardType = rewardItem.getType();
                                });
                            }
                        }
                    });

                    break;
                }

                case "about": {

                    new LibsBuilder()
                            .withAboutVersionShown(true)
                            .withAboutIconShown(true)
                            .withLicenseShown(true)
                            .withVersionShown(true)
                            .start(requireActivity());

                    break;
                }
            }
        }

        return true;
    }

    @Override
    public void onDestroy() {

        if (mapTypePreference != null) {

            mapTypePreference.setOnPreferenceChangeListener(null);
        }

        if (toggleNotifications != null) {

            toggleNotifications.setOnPreferenceClickListener(null);
        }

        if (toggleTheme != null) {

            toggleTheme.setOnPreferenceClickListener(null);
        }

        super.onDestroy();
    }

    private void showMessageLong(String message) {

        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {

            Snackbar snackBar = Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG);
            View snackBarView = snackBar.getView();
            TextView snackTextView = snackBarView.findViewById(com.google.android.material.R.id.snackbar_text);
            snackTextView.setMaxLines(10);
            snackBar.show();
        } else {

            cancelToasts();
            longToast = Toast.makeText(requireContext(), message, Toast.LENGTH_LONG);
            longToast.setGravity(Gravity.CENTER, 0, 0);
            longToast.show();
        }
    }

    private void cancelToasts() {

        if (longToast != null) {

            longToast.cancel();
        }
    }
}
