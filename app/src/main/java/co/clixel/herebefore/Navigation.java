package co.clixel.herebefore;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.gauravk.bubblenavigation.BubbleNavigationConstraintView;
import com.gauravk.bubblenavigation.listener.BubbleNavigationChangeListener;

public class Navigation extends AppCompatActivity {

    static private boolean noChat = false, fromDMs = false, firstLoad = true;
    private ViewPager viewPager;
    private BubbleNavigationConstraintView bubbleNavigationConstraintView;
    private ViewPager.OnPageChangeListener pagerListener;
    private int currentItem = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

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
                InputMethodManager imm = (InputMethodManager)getApplication().getSystemService(Context.INPUT_METHOD_SERVICE);
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

        // If user didn't enter this activity from the DMs button,
        // the layout doesn't include chat and this is the first time loading,
        // set the current item as 1 (the settings tab).
        if (!fromDMs && noChat && firstLoad) {

            viewPager.setCurrentItem(1, false);
            bubbleNavigationConstraintView.setCurrentActiveItem(1);
            currentItem = 1;
        }

        super.onStart();
    }

    @Override
    protected void onRestart() {

        super.onRestart();

        firstLoad = false;
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

        super.onStop();
    }
}
