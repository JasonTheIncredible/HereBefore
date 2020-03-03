package co.clixel.herebefore;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private ArrayList<String> mMessageTime, mMessageImage, mMessageImageVideo, mMessageText;
    private ArrayList<Boolean> mUserIsWithinShape;
    private Context mContext;
    private ImageButton playButtonInside, playButtonOutside;
    private int usableWidth;

    RecyclerViewAdapter(Context context, ArrayList<String> mMessageTime, ArrayList<String> mMessageImage, ArrayList<String> mMessageImageVideo, ArrayList<String> mMessageText, ArrayList<Boolean> mUserIsWithinShape) {

        this.mContext = context;
        this.mMessageTime = mMessageTime;
        this.mMessageImage = mMessageImage;
        this.mMessageImageVideo = mMessageImageVideo;
        this.mMessageText = mMessageText;
        this.mUserIsWithinShape = mUserIsWithinShape;
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

        if (playButtonOutside != null) {

            playButtonOutside.setOnClickListener(new View.OnClickListener() {

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
        measuredWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        if (measuredWidth > 0) {

            if (measuredWidth >= 400) {

                usableWidth = measuredWidth / 2;
            } else {

                usableWidth = measuredWidth;
            }
        } else {

            usableWidth = 400;
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {

        // Set the left side if the user sent the message from inside the shape.
        if (mUserIsWithinShape.get(position)) {

            holder.messageTimeInside.setText(mMessageTime.get(position));

            // Set messageImage, messageImageVideo, or messageText to gone if an image or text doesn't exist, for spacing consistency.
            if (mMessageImage.get(position) == null) {

                holder.messageImageInside.setVisibility(View.GONE);
            } else {

                Glide.with(mContext)
                        .load(mMessageImage.get(position))
                        .apply(new RequestOptions().override(usableWidth, 5000).placeholder(R.drawable.ic_recyclerview_image_placeholder).centerInside())
                        .into(holder.messageImageInside);

                holder.messageImageInside.setVisibility(View.VISIBLE);
            }

            if (mMessageImageVideo.get(position) == null) {

                holder.videoFrameInside.setVisibility(View.GONE);
            } else {

                Glide.with(mContext)
                        .load(mMessageImageVideo.get(position))
                        .apply(new RequestOptions().override(usableWidth, 5000).placeholder(R.drawable.ic_recyclerview_image_placeholder).centerInside())
                        .into(holder.messageImageVideoInside);

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
        } else {

            // User sent the message from outside the shape. Setup the right side.
            holder.messageTimeOutside.setText(mMessageTime.get(position));

            // Set messageImage, messageImageVideo, or messageText to gone if an image or text doesn't exist, for spacing consistency.
            if (mMessageImage.get(position) == null) {

                holder.messageImageOutside.setVisibility(View.GONE);
            } else {

                Glide.with(mContext)
                        .load(mMessageImage.get(position))
                        .apply(new RequestOptions().override(usableWidth, 5000).placeholder(R.drawable.ic_recyclerview_image_placeholder).centerInside())
                        .into(holder.messageImageOutside);

                holder.messageImageOutside.setVisibility(View.VISIBLE);
            }

            if (mMessageImageVideo.get(position) == null) {

                holder.videoFrameOutside.setVisibility(View.GONE);
            } else {

                Glide.with(mContext)
                        .load(mMessageImageVideo.get(position))
                        .apply(new RequestOptions().override(usableWidth, 5000).placeholder(R.drawable.ic_recyclerview_image_placeholder).centerInside())
                        .into(holder.messageImageVideoOutside);

                holder.videoFrameOutside.setVisibility(View.VISIBLE);
            }

            if (mMessageText.get(position) == null) {

                holder.messageTextOutside.setVisibility(View.GONE);
            } else {

                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.messageTextOutside.getLayoutParams();

                if (holder.messageImageOutside.getVisibility() == View.VISIBLE) {

                    params.addRule(RelativeLayout.BELOW, R.id.messageImageOutside);
                    holder.messageTextOutside.setLayoutParams(params);
                } else if (holder.messageImageVideoOutside.getVisibility() == View.VISIBLE) {

                    params.addRule(RelativeLayout.BELOW, R.id.videoFrameOutside);
                    holder.messageTextOutside.setLayoutParams(params);
                }

                holder.messageTextOutside.setText(mMessageText.get(position));
                holder.messageTextOutside.setVisibility(View.VISIBLE);
            }
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

        TextView messageTimeInside, messageTimeOutside, messageTextInside, messageTextOutside;
        ImageView messageImageInside, messageImageOutside, messageImageVideoInside, messageImageVideoOutside;
        FrameLayout videoFrameInside, videoFrameOutside;
        RelativeLayout messageItem;

        ViewHolder(@NonNull View itemView) {

            super(itemView);
            messageTimeInside = itemView.findViewById(R.id.messageTimeInside);
            messageTimeOutside = itemView.findViewById(R.id.messageTimeOutside);
            messageImageInside = itemView.findViewById(R.id.messageImageInside);
            messageImageOutside = itemView.findViewById(R.id.messageImageOutside);
            videoFrameInside = itemView.findViewById(R.id.videoFrameInside);
            videoFrameOutside = itemView.findViewById(R.id.videoFrameOutside);
            messageImageVideoInside = itemView.findViewById(R.id.messageImageVideoInside);
            messageImageVideoOutside = itemView.findViewById(R.id.messageImageVideoOutside);
            playButtonInside = itemView.findViewById(R.id.playButtonInside);
            playButtonOutside = itemView.findViewById(R.id.playButtonOutside);
            messageTextInside = itemView.findViewById(R.id.messageTextInside);
            messageTextOutside = itemView.findViewById(R.id.messageTextOutside);
            messageItem = itemView.findViewById(R.id.message);
        }
    }
}
