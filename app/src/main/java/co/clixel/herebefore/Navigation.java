package co.clixel.herebefore;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.gauravk.bubblenavigation.BubbleNavigationConstraintView;
import com.gauravk.bubblenavigation.listener.BubbleNavigationChangeListener;

public class Navigation extends AppCompatActivity {

    static private boolean fromSettings = false, fromDMs = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            // If fromSettings or fromDMs is true, hide Chat, as user entered this activity without clicking a shape.
            fromSettings = extras.getBoolean("fromSettings");
            fromDMs = extras.getBoolean("fromDMs");

            if (fromSettings) {

                setContentView(R.layout.navigationsettings);
            } else if (fromDMs) {

                setContentView(R.layout.navigationdms);
            } else {

                setContentView(R.layout.navigation);
            }
        }

        ScreenSlidePagerAdapter pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());

        final BubbleNavigationConstraintView bubbleNavigationConstraintView = findViewById(R.id.bottom_navigation_constraint);

        final ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageSelected(int i) {

                bubbleNavigationConstraintView.setCurrentActiveItem(i);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

        bubbleNavigationConstraintView.setNavigationChangeListener(new BubbleNavigationChangeListener() {

            @Override
            public void onNavigationChanged(View view, int position) {

                viewPager.setCurrentItem(position, true);
            }
        });

        if (fromSettings) {

            viewPager.setCurrentItem(1, true);
        }
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    public static class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        ScreenSlidePagerAdapter(FragmentManager fm) {

            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {

            if (fromSettings || fromDMs) {

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

            if (fromSettings || fromDMs) {

                return 2;
            } else {

                return 3;
            }
        }
    }
}
