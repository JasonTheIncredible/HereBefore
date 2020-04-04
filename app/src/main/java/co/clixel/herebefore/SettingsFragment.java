package co.clixel.herebefore;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements
        PreferenceManager.OnPreferenceTreeClickListener {

    private DialogInterface.OnClickListener dialogClickListener;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {

        String key = preference.getKey();

        if (key.equals("signOut")) {

            if (getActivity() != null) {

                boolean signOut = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("signOut", false);

                if (signOut) {

                    AuthUI.getInstance().signOut(getActivity());
                    PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().clear().commit();

                    Toast.makeText(getActivity(), "Signed out", Toast.LENGTH_SHORT).show();

                    Intent Activity = new Intent(getActivity(), Map.class);

                    startActivity(Activity);
                } else {

                    Toast.makeText(getActivity(), "No user signed in", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (key.equals("deleteAccount")) {

            if (getActivity() != null) {

                dialogClickListener = new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        switch (which) {

                            case DialogInterface.BUTTON_POSITIVE:

                                final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                                // Get auth credentials from the user for re-authentication. The example below shows
                                // email and password credentials but there are multiple possible providers,
                                // such as GoogleAuthProvider or FacebookAuthProvider.
                                AuthCredential credential = null;

                                if (getActivity() != null) {

                                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

                                    String googleIdToken = sharedPreferences.getString("googleIdToken", "");

                                    // Check if the account is a Google account.
                                    if (!googleIdToken.equals("")) {

                                        credential = GoogleAuthProvider.getCredential(googleIdToken, null);
                                    } else {

                                        String userToken = sharedPreferences.getString("userToken", "");
                                        String passToken = sharedPreferences.getString("passToken", "");

                                        credential = EmailAuthProvider.getCredential(userToken, passToken);
                                    }
                                }

                                // Prompt the user to re-provide their sign-in credentials
                                if (credential != null) {

                                    if (user != null) {

                                        user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {

                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {

                                                user.delete()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {

                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {

                                                                if (task.isSuccessful()) {

                                                                    PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().clear().commit();

                                                                    Toast.makeText(getActivity(), "Account deleted", Toast.LENGTH_LONG).show();

                                                                    Intent Activity = new Intent(getActivity(), Map.class);

                                                                    startActivity(Activity);
                                                                }
                                                            }
                                                        });

                                            }
                                        });
                                    }
                                }

                                break;

                            case DialogInterface.BUTTON_NEGATIVE:

                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage("Are you sure? This will permanently delete everything associated with your account.").setPositiveButton("Yes, delete my account!", dialogClickListener)
                        .setNegativeButton("No, take me back!", dialogClickListener).show();
            }
        }

        return true;
    }

    @Override
    public void onStop() {

        if (dialogClickListener != null) {

            dialogClickListener = null;
        }

        super.onStop();
    }
}
