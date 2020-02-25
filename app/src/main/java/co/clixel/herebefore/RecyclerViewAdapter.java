package co.clixel.herebefore;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.HashMap;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private ArrayList<String> mMessageTime;
    private ArrayList<String> mMessageImage;
    private ArrayList<String> mMessageImageVideo;
    private ArrayList<String> mMessageText;
    private ImageButton playButton;
    private Context mContext;
    private int usableWidth;
    private int usableHeight;

    RecyclerViewAdapter(Context context, ArrayList<String> mMessageTime, ArrayList<String> mMessageImage, ArrayList<String> mMessageImageVideo, ArrayList<String> mMessageText) {

        this.mContext = context;
        this.mMessageTime = mMessageTime;
        this.mMessageImage = mMessageImage;
        this.mMessageImageVideo = mMessageImageVideo;
        this.mMessageText = mMessageText;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.recyclerviewlayout, parent, false);

        final ViewHolder holder = new ViewHolder(view);

        view.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (mMessageImage.get(holder.getAdapterPosition()) != null) {

                    Intent Activity = new Intent(mContext, PhotoView.class);
                    Activity.putExtra("imgURL", mMessageImage.get(holder.getAdapterPosition()));
                    mContext.startActivity(Activity);
                }

                if (mMessageImageVideo.get(holder.getAdapterPosition()) != null) {

                    Intent Activity = new Intent(mContext, co.clixel.herebefore.VideoView.class);
                    Activity.putExtra("videoURL", mMessageImageVideo.get(holder.getAdapterPosition()));
                    mContext.startActivity(Activity);
                }
            }
        });

        if (playButton != null) {

            playButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    Intent Activity = new Intent(mContext, co.clixel.herebefore.VideoView.class);
                    Activity.putExtra("videoURL", mMessageImageVideo.get(holder.getAdapterPosition()));
                    mContext.startActivity(Activity);
                }
            });
        }

        // Sets each picture's size relative to the screen (used in onBindViewHolder().
        int measuredWidth;
        int measuredHeight;
        measuredWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        measuredHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        if (measuredWidth > 0) {

            if (measuredWidth >= 400) {

                usableWidth = measuredWidth / 2;
            } else {

                usableWidth = measuredWidth;
            }
        } else {

            usableWidth = 400;
        }

        if (measuredHeight > 0) {

            usableHeight = measuredHeight;
        } else {

            usableHeight = 400;
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {

        holder.messageTime.setText(mMessageTime.get(position));

        // Set messageImage, messageImageVideo, or messageText to gone if an image or text doesn't exist, for spacing consistency.
        if (mMessageImage.get(position) == null) {

            holder.messageImage.setVisibility(View.GONE);
        } else {

            holder.messageImage.setVisibility(View.VISIBLE);
            Glide.with(mContext)
                    .load(mMessageImage.get(position))
                    .apply(new RequestOptions().override(usableWidth, 5000).placeholder(R.drawable.ic_recyclerview_image_placeholder).centerInside())
                    .into(holder.messageImage);
        }

        if (mMessageImageVideo.get(position) == null) {

            holder.videoFrame.setVisibility(View.GONE);
        } else {

            // Change the videoView's orientation depending on the orientation of the video.
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            // Set the video Uri as data source for MediaMetadataRetriever
            retriever.setDataSource(mMessageImageVideo.get(position), new HashMap<String, String>());
            // Get one "frame"/bitmap - * NOTE - no time was set, so the first available frame will be used
            Bitmap bmp = retriever.getFrameAtTime(1);
            // Get the bitmap width and height
            int bmpWidth = bmp.getWidth();
            int bmpHeight = bmp.getHeight();

            final float scale = mContext.getResources().getDisplayMetrics().density;
            // Adjust the size so the frame doesn't expand past the screen.
            if (bmpWidth > bmpHeight) {

                if (bmpWidth > usableWidth) {

                    holder.videoFrame.getLayoutParams().width = (int) ((usableWidth / 2) * scale + 0.5f); // Convert dp to px.
                }
            } else {

                if (bmpHeight > usableHeight) {

                    holder.videoFrame.getLayoutParams().height = (int) ((usableHeight / 2) * scale + 0.5f); // Convert dp to px.
                }
            }

            holder.messageImageVideo.setImageBitmap(bmp);
            holder.videoFrame.setVisibility(View.VISIBLE);
        }

        if (mMessageText.get(position) == null) {

            holder.messageText.setVisibility(View.GONE);
        } else {

            holder.messageText.setVisibility(View.VISIBLE);
            holder.messageText.setText(mMessageText.get(position));
        }

        // Change the color of every other row for visual purposes.
        if (position % 2 == 0) {

            holder.itemView.setBackgroundColor(Color.parseColor("#222222"));
        } else {

            holder.itemView.setBackgroundColor(Color.parseColor("#292929"));
        }

        holder.setIsRecyclable(true);
    }

    @Override
    public int getItemCount() {

        return mMessageTime.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView messageTime, messageText;
        ImageView messageImage, messageImageVideo;
        FrameLayout videoFrame;
        RelativeLayout messageItem;

        ViewHolder(@NonNull View itemView) {

            super(itemView);
            messageTime = itemView.findViewById(R.id.messageTime);
            messageImage = itemView.findViewById(R.id.messageImage);
            videoFrame = itemView.findViewById(R.id.video_frame);
            messageImageVideo = itemView.findViewById(R.id.messageImageVideo);
            playButton = itemView.findViewById(R.id.play_button);
            messageText = itemView.findViewById(R.id.messageText);
            messageItem = itemView.findViewById(R.id.message);
        }
    }
}
