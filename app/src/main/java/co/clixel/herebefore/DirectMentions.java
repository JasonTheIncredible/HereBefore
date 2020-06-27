package co.clixel.herebefore;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static java.text.DateFormat.getDateTimeInstance;

public class DirectMentions extends AppCompatActivity {

    private static final String TAG = "DirectMentions";
    private String email;
    private ArrayList<String> mTime = new ArrayList<>(), mUser = new ArrayList<>(), mImage = new ArrayList<>(), mVideo = new ArrayList<>(), mText = new ArrayList<>(), mShapeUUID = new ArrayList<>();
    private ArrayList<Boolean> mUserIsWithinShape = new ArrayList<>();
    private RecyclerView directMentionsRecyclerView;
    private static int index = -1, top = -1, last, mentionCount = 0, mentionCount1 = 0;
    private DatabaseReference databaseReferenceOne, databaseReferenceTwo;
    private ValueEventListener eventListenerOne, eventListenerTwo;
    private LinearLayoutManager directMentionsRecyclerViewLinearLayoutManager = new LinearLayoutManager(this);
    private boolean theme, firstLoad;
    private View loadingIcon;
    private SharedPreferences sharedPreferences;
    private Toast longToast, noDMsToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        setContentView(R.layout.directmentions);

        directMentionsRecyclerView = findViewById(R.id.mentionsList);
        loadingIcon = findViewById(R.id.loadingIcon);

        // Set to true to scroll to the bottom of directMentionsRecyclerView.
        firstLoad = true;

        // Make the loadingIcon visible upon the first load, as it can sometimes take a while to show anything. It should be made invisible in initDirectMentionsAdapter().
        if (loadingIcon != null) {

            loadingIcon.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onStart() {

        super.onStart();
        Log.i(TAG, "onStart()");

        // Update to the user's preferences.
        loadPreferences();
        updatePreferences();

        mentionCount = 0;
        mentionCount1 = 0;

        // If user has a Google account, get email one way. Else, get email another way.
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getBaseContext());
        if (acct != null) {

            email = acct.getEmail();
        } else {

            email = sharedPreferences.getString("userToken", "null");
        }

        // Connect to Firebase.
        final DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        databaseReferenceOne = rootRef.child("MessageThreads");
        eventListenerOne = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                // Clear the RecyclerView before adding new entries to prevent duplicates.
                if (directMentionsRecyclerViewLinearLayoutManager != null) {

                    mTime.clear();
                    mUser.clear();
                    mImage.clear();
                    mVideo.clear();
                    mText.clear();
                    mShapeUUID.clear();
                    mUserIsWithinShape.clear();
                }

                // Read RecyclerView scroll position (for use in initDirectMentionsAdapter()).
                if (directMentionsRecyclerViewLinearLayoutManager != null) {

                    index = directMentionsRecyclerViewLinearLayoutManager.findFirstVisibleItemPosition();
                    last = directMentionsRecyclerViewLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                    View v = directMentionsRecyclerView.getChildAt(0);
                    top = (v == null) ? 0 : (v.getTop() - directMentionsRecyclerView.getPaddingTop());
                }

                for (final DataSnapshot ds : dataSnapshot.getChildren()) {

                    if (ds.child("removedMentionDuplicates").getValue() != null) {

                        for (final DataSnapshot mention : ds.child("removedMentionDuplicates").getChildren()) {

                            if (mention.getValue() != null) {

                                // If mentionCount == mentionCount1, initialize the adapter.
                                mentionCount++;
                                databaseReferenceTwo = rootRef.child("Users");
                                eventListenerTwo = new ValueEventListener() {

                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        for (DataSnapshot dss : dataSnapshot.getChildren()) {

                                            String userUUID = (String) dss.child("userUUID").getValue();
                                            if (mention.getValue().toString().equals(userUUID)) {

                                                // If mentionCount == mentionCount1, initialize the adapter.
                                                mentionCount1++;
                                                String userEmail = (String) dss.child("email").getValue();
                                                if (userEmail != null) {

                                                    if (userEmail.equals(email)) {

                                                        Long serverDate = (Long) ds.child("date").getValue();
                                                        String user = (String) ds.child("userUUID").getValue();
                                                        String imageURL = (String) ds.child("imageURL").getValue();
                                                        String videoURL = (String) ds.child("videoURL").getValue();
                                                        String messageText = (String) ds.child("message").getValue();
                                                        String shapeUUID = (String) ds.child("shapeUUID").getValue();
                                                        Boolean userIsWithinShape = (Boolean) ds.child("userIsWithinShape").getValue();
                                                        DateFormat dateFormat = getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
                                                        // Getting ServerValue.TIMESTAMP from Firebase will create two calls: one with an estimate and one with the actual value.
                                                        // This will cause onDataChange to fire twice; optimizations could be made in the future.
                                                        if (serverDate != null) {

                                                            Date netDate = (new Date(serverDate));
                                                            String messageTime = dateFormat.format(netDate);
                                                            mTime.add(messageTime);
                                                        } else {

                                                            Log.e(TAG, "onStart() -> serverDate == null");
                                                        }
                                                        mUser.add(user);
                                                        mImage.add(imageURL);
                                                        mVideo.add(videoURL);
                                                        mText.add(messageText);
                                                        mShapeUUID.add(shapeUUID);
                                                        mUserIsWithinShape.add(userIsWithinShape);
                                                    }
                                                }
                                            }
                                        }

                                        // Prevent recyclerView from getting initialized more than once,
                                        // as the loading icon / toast is dependant on this happening only once.
                                        if (mentionCount == mentionCount1) {

                                            initDirectMentionsAdapter();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                        loadingIcon.setVisibility(View.INVISIBLE);
                                        toastMessageLong(databaseError.getMessage());
                                    }
                                };

                                // Add the second Firebase listener.
                                databaseReferenceTwo.addListenerForSingleValueEvent(eventListenerTwo);
                            }
                        }
                    }
                }
            }

            public void onCancelled(@NonNull DatabaseError databaseError) {

                loadingIcon.setVisibility(View.INVISIBLE);
                toastMessageLong(databaseError.getMessage());
            }
        };

        // Add the first Firebase listener.
        databaseReferenceOne.addValueEventListener(eventListenerOne);
    }

    @Override
    public void onRestart() {

        super.onRestart();
        Log.i(TAG, "onRestart()");

        mentionCount = 0;
        mentionCount1 = 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        Log.i(TAG, "onCreateOptionsMenu()");

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chatsettings_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        if (id == R.id.settingsButton) {

            Log.i(TAG, "onOptionsItemSelected() -> settingsButton");

            cancelToasts();

            Intent Activity = new Intent(getBaseContext(), co.clixel.herebefore.Settings.class);

            startActivity(Activity);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {

        Log.i(TAG, "onStop()");

        if (databaseReferenceOne != null) {

            databaseReferenceOne.removeEventListener(eventListenerOne);
        }

        if (databaseReferenceTwo != null) {

            databaseReferenceTwo.removeEventListener(eventListenerTwo);
        }

        if (directMentionsRecyclerView != null) {

            directMentionsRecyclerView.clearOnScrollListeners();
        }

        if (eventListenerOne != null) {

            eventListenerOne = null;
        }

        if (eventListenerTwo != null) {

            eventListenerTwo = null;
        }

        cancelToasts();

        super.onStop();
    }

    private void cancelToasts() {

        if (longToast != null) {

            longToast.cancel();
        }

        if (noDMsToast != null) {

            noDMsToast.cancel();
        }
    }

    protected void loadPreferences() {

        Log.i(TAG, "loadPreferences()");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        theme = sharedPreferences.getBoolean(co.clixel.herebefore.Settings.KEY_THEME_SWITCH, false);
    }

    protected void updatePreferences() {

        Log.i(TAG, "updatePreferences()");

        if (theme) {

            // Set to light mode.
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO);
        } else {

            // Set to dark mode.
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES);
        }

        // This will allow the settings button to appear in Map.java.
        sharedPreferences.edit().putBoolean(Settings.KEY_SIGN_OUT, true).apply();
    }

    private void initDirectMentionsAdapter() {

        // Initialize the RecyclerView.
        Log.i(TAG, "initDirectMentionsAdapter()");

        DirectMentionsAdapter adapter = new DirectMentionsAdapter(this, mTime, mUser, mImage, mVideo, mText, mShapeUUID, mUserIsWithinShape);
        directMentionsRecyclerView.setAdapter(adapter);
        directMentionsRecyclerView.setLayoutManager(directMentionsRecyclerViewLinearLayoutManager);

        if (last == (mTime.size() - 2) || firstLoad) {

            // Scroll to bottom of recyclerviewlayout after first initialization and after sending a recyclerviewlayout.
            directMentionsRecyclerView.scrollToPosition(mTime.size() - 1);
            firstLoad = false;
        } else {

            // Set RecyclerView scroll position to prevent position change when Firebase gets updated and after screen orientation change.
            directMentionsRecyclerViewLinearLayoutManager.scrollToPositionWithOffset(index, top);
        }

        // After the initial load, make the loadingIcon invisible.
        if (loadingIcon != null) {

            loadingIcon.setVisibility(View.INVISIBLE);
        }

        if (mUser.size() == 0) {

            noDMsToast = Toast.makeText(getBaseContext(), "You have no direct mentions", Toast.LENGTH_LONG);
            noDMsToast.setGravity(Gravity.CENTER, 0, 0);
            noDMsToast.show();
        }

        mentionCount = 0;
        mentionCount1 = 0;
    }

    private void toastMessageLong(String message) {

        longToast = Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG);
        longToast.setGravity(Gravity.CENTER, 0, 0);
        longToast.show();
    }
}
