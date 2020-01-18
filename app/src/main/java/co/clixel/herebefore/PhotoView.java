package co.clixel.herebefore;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

public class PhotoView extends AppCompatActivity {

    // https://github.com/chrisbanes/PhotoView
    com.github.chrisbanes.photoview.PhotoView myImage;
    String url = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.photoview);

        url = getIntent().getStringExtra("imgURL");

        myImage = findViewById(R.id.myImage);
        Picasso.get()
                .load(url)
                .into(myImage);
    }
}