package co.clixel.herebefore;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.gauravk.bubblenavigation.BubbleNavigationConstraintView;
import com.gauravk.bubblenavigation.listener.BubbleNavigationChangeListener;

public class Navigation extends AppCompatActivity {

    static private boolean noChat = false, fromDMs = false;
    private ViewPager viewPager;
    private BubbleNavigationConstraintView bubbleNavigationConstraintView;
    private ViewPager.OnPageChangeListener pagerListener;
    private int currentItem = -1;
    private SharedPreferences sharedPreferences;
    private boolean theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Update to the user's preferences.
        loadPreferences();
        updatePreferences();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            // If noChat is true, hide Chat, as user entered this activity without clicking a shape.
            noChat = extras.getBoolean("noChat");
            fromDMs = extras.getBoolean("fromDMs");

            if (noChat) {

                setContentView(R.layout.navigationnochat);
            } else {

                setContentView(R.layout.navigation);
            }
        }
    }

    protected void loadPreferences() {

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        theme = sharedPreferences.getBoolean(SettingsFragment.KEY_THEME_SWITCH, false);
    }

    protected void updatePreferences() {

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
        sharedPreferences.edit().putBoolean(SettingsFragment.KEY_SIGN_OUT, true).apply();
    }

    @Override
    protected void onStart() {

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

        BubbleNavigationChangeListener bubbleListener = new BubbleNavigationChangeListener() {

            @Override
            public void onNavigationChanged(View view, int position) {

                viewPager.setCurrentItem(position, true);

                currentItem = viewPager.getCurrentItem();
            }
        };

        bubbleNavigationConstraintView.setNavigationChangeListener(bubbleListener);

        // The user has changed the selected item. Reload with this selected item.
        if (currentItem != -1) {

            viewPager.setCurrentItem(currentItem, false);
            bubbleNavigationConstraintView.setCurrentActiveItem(currentItem);
        }

        // The following is used if the user "reloads" the activity after clicking the toggle theme button in Settings.
        if (!fromDMs && !noChat && SettingsFragment.themeToggled) {

            viewPager.setCurrentItem(2, false);
            bubbleNavigationConstraintView.setCurrentActiveItem(2);
            currentItem = 2;
            SettingsFragment.themeToggled = false;
        } else if (noChat && SettingsFragment.themeToggled) {

            viewPager.setCurrentItem(1, false);
            bubbleNavigationConstraintView.setCurrentActiveItem(1);
            currentItem = 1;
            SettingsFragment.themeToggled = false;
        }

        super.onStart();
    }

    @Override
    public void onStop() {

        if (viewPager != null) {

            if (pagerListener != null) {

                viewPager.removeOnPageChangeListener(pagerListener);
                viewPager = null;
            }
        }

        if (bubbleNavigationConstraintView != null) {

            bubbleNavigationConstraintView.setNavigationChangeListener(null);
        }

        super.onStop();
    }

    @Override
    protected void onRestart() {

        super.onRestart();
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    public static class ScreenSlidePagerAdapter extends FragmentPagerAdapter {

        ScreenSlidePagerAdapter(FragmentManager fm) {

            super(fm);
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
                    default:
                        return null;
                }
            } else {

                switch (position) {
                    case 0:
                        return new Chat();
                    case 1:
                        return new DirectMentions();
                    case 2:
                        return new SettingsFragment();
                    default:
                        return null;
                }
            }
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
