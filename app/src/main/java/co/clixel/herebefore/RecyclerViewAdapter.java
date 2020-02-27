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
    private ImageButton playButtonInside;
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

        if (playButtonInside != null) {

            playButtonInside.setOnClickListener(new View.OnClickListener() {

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

        holder.messageTimeInside.setText(mMessageTime.get(position));

        // Set messageImage, messageImageVideo, or messageText to gone if an image or text doesn't exist, for spacing consistency.
        if (mMessageImage.get(position) == null) {

            holder.messageImageInside.setVisibility(View.GONE);
        } else {

            holder.messageImageInside.setVisibility(View.VISIBLE);
            Glide.with(mContext)
                    .load(mMessageImage.get(position))
                    .apply(new RequestOptions().override(usableWidth, 5000).placeholder(R.drawable.ic_recyclerview_image_placeholder).centerInside())
                    .into(holder.messageImageInside);
        }

        if (mMessageImageVideo.get(position) == null) {

            holder.videoFrameInside.setVisibility(View.GONE);
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

                    holder.videoFrameInside.getLayoutParams().width = (int) ((usableWidth / 2) * scale + 0.5f); // Convert dp to px.
                }
            } else {

                if (bmpHeight > usableHeight) {

                    holder.videoFrameInside.getLayoutParams().height = (int) ((usableHeight / 2) * scale + 0.5f); // Convert dp to px.
                }
            }

            holder.messageImageVideoInside.setImageBitmap(bmp);
            holder.videoFrameInside.setVisibility(View.VISIBLE);
        }

        if (mMessageText.get(position) == null) {

            holder.messageTextInside.setVisibility(View.GONE);
        } else {

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.messageTextInside.getLayoutParams();

            if (holder.messageImageInside.getVisibility() == View.VISIBLE) {

                params.addRule(RelativeLayout.BELOW, R.id.messageImageInside);
                holder.messageTextInside.setLayoutParams(params);
            } else if (holder.messageImageVideoInside.getVisibility() == View.VISIBLE) {

                params.addRule(RelativeLayout.BELOW, R.id.videoFrameInside);
                holder.messageTextInside.setLayoutParams(params);
            }

            holder.messageTextInside.setText(mMessageText.get(position));
            holder.messageTextInside.setVisibility(View.VISIBLE);
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

        TextView messageTimeInside, messageTimeOutSide, messageTextInside, messageTextOutside;
        ImageView messageImageInside, messageImageOutside, messageImageVideoInside, messageImageVideoOutside;
        FrameLayout videoFrameInside;
        RelativeLayout messageItem;

        ViewHolder(@NonNull View itemView) {

            super(itemView);
            messageTimeInside = itemView.findViewById(R.id.messageTimeInside);
            messageImageInside = itemView.findViewById(R.id.messageImageInside);
            videoFrameInside = itemView.findViewById(R.id.videoFrameInside);
            messageImageVideoInside = itemView.findViewById(R.id.messageImageVideoInside);
            playButtonInside = itemView.findViewById(R.id.playButtonInside);
            messageTextInside = itemView.findViewById(R.id.messageTextInside);
            messageItem = itemView.findViewById(R.id.message);
        }
    }
}
