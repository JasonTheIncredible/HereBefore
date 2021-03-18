package co.clixel.herebefore;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class Navigation extends AppCompatActivity {

    private ViewPager viewPager;
    private BubbleNavigationConstraintView bubbleNavigationConstraintView;
    private ViewPager.OnPageChangeListener pagerListener;
    private int currentItem = -1, dmCounter = 0;
    private String firebaseUid;
    private Query query;
    private ChildEventListener childEventListener;
    private boolean firstLoad, noChat = false, fromDms = false, needToLoadCorrectTab = true, dmExists = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Update to the user's preferences.
        updatePreferences();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            // If noChat is true, hide Chat, as user entered this activity without clicking a shape.
            noChat = extras.getBoolean("noChat");
            fromDms = extras.getBoolean("fromDms");

            if (noChat) {

                setContentView(R.layout.navigationnochat);
            } else {

                setContentView(R.layout.navigation);
            }
        }
    }

    protected void updatePreferences() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // theme == true is light mode.
        boolean theme = sharedPreferences.getBoolean(getString(R.string.prefTheme), false);

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
        sharedPreferences.edit().putBoolean(getString(R.string.prefSignOut), true).apply();
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
        firstLoad = true;

        ScreenSlidePagerAdapter pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());

        bubbleNavigationConstraintView = findViewById(R.id.bottom_navigation_constraint);

        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(pagerAdapter);
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
        if (query != null) {

            query.removeEventListener(childEventListener);
        }

        query = FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUid).child("ReceivedDms").limitToLast(1);
        childEventListener = new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                // If this is the first time calling this eventListener, prevent double posts (as onStart() already added the last item).
                if (firstLoad && dmExists) {

                    firstLoad = false;
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

        query.addChildEventListener(childEventListener);
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

        if (query != null) {

            query.removeEventListener(childEventListener);
        }

        super.onStop();
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    public class ScreenSlidePagerAdapter extends FragmentPagerAdapter {

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
