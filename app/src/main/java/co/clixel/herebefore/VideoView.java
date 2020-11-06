package co.clixel.herebefore;

import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class VideoView extends AppCompatActivity {

    private PlayerView playerView;
    private SimpleExoPlayer player;
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.videoview);

        playerView = findViewById(R.id.playerView);

        url = getIntent().getStringExtra("videoUrl");
    }

    @Override
    protected void onStart() {

        super.onStart();

        player = new SimpleExoPlayer.Builder(this).build();

        // Bind the player to the view.
        playerView.setPlayer(player);

        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "Here Before"));
        // This is the MediaSource representing the media to be played.
        MediaSource videoSource =
                new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(Uri.parse(url));
        // Prepare the player with the source.
        player.prepare(videoSource);
    }

    @Override
    protected void onStop() {

        releasePlayer();

        super.onStop();
    }

    @Override
    protected void onDestroy() {

        player = null;
        playerView = null;

        super.onDestroy();
    }

    private void releasePlayer() {

        if (player != null) {

            player.setPlayWhenReady(true);
            player.stop();
            player.release();
            playerView.setPlayer(null);
        }
    }
}
