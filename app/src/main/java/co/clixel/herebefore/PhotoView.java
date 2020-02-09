package co.clixel.herebefore;

import android.content.res.Resources;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

public class PhotoView extends AppCompatActivity {

    // https://github.com/chrisbanes/PhotoView
    com.github.chrisbanes.photoview.PhotoView myImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.photoview);

        String url = getIntent().getStringExtra("imgURL");

        // Sets each picture's size relative to the screen (used in onBindViewHolder().
        int measuredWidth;
        measuredWidth = Resources.getSystem().getDisplayMetrics().widthPixels;

        int usableWidth;
        if (measuredWidth > 0) {

            usableWidth = measuredWidth;

        } else {

            // If measured width does not get measured for some reason (and is 0), set usableWidth to a number to prevent crashes.
            usableWidth = 400;
        }

        myImage = findViewById(R.id.myImage);
        Glide.with(this)
                .load(url)
                .apply(new RequestOptions().override(usableWidth, 0))
                .into(myImage);
    }
}