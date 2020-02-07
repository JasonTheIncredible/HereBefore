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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private ArrayList<String> mMessageUser;
    private ArrayList<String> mMessageTime;
    private ArrayList<String> mMessageImage;
    private ArrayList<String> mMessageVideo;
    private ArrayList<String> mMessageText;
    private Context mContext;
    private int usableWidth;

    RecyclerViewAdapter(Context context, ArrayList<String> mMessageUser, ArrayList<String> mMessageTime, ArrayList<String> mMessageImage, ArrayList<String> mMessageVideo, ArrayList<String> mMessageText) {

        this.mContext = context;
        this.mMessageUser = mMessageUser;
        this.mMessageTime = mMessageTime;
        this.mMessageImage = mMessageImage;
        this.mMessageVideo = mMessageVideo;
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

                    Intent Activity = new Intent(mContext, co.clixel.herebefore.PhotoView.class);
                    Activity.putExtra("imgURL", mMessageImage.get(holder.getAdapterPosition()));
                    mContext.startActivity(Activity);
                }
            }
        });

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

        //holder.messageUser.setText(mMessageUser.get(position));
        holder.messageTime.setText(mMessageTime.get(position));

        // Set messageImage, messageVideo, or messageText to gone if an image or text doesn't exist, for spacing consistency.
        if (mMessageImage.get(position) == null) {

            holder.messageImage.setVisibility(View.GONE);
        } else {

            holder.messageImage.setVisibility(View.VISIBLE);
            Glide.with(mContext)
                    .load(mMessageImage.get(position))
                    .apply(new RequestOptions().override(usableWidth, 5000).placeholder(R.drawable.ic_recyclerview_image_placeholder).centerInside())
                    .into(holder.messageImage);
        }

        if (mMessageVideo.get(position) == null) {

            holder.messageVideo.setVisibility(View.GONE);
        } else {

            holder.messageVideo.setVisibility(View.VISIBLE);
            holder.messageVideo.setVideoPath(mMessageVideo.get(position));
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

        TextView messageUser, messageTime, messageText;
        ImageView messageImage;
        VideoView messageVideo;
        RelativeLayout messageItem;

        ViewHolder(@NonNull View itemView) {

            super(itemView);
            messageUser = itemView.findViewById(R.id.messageUser);
            messageTime = itemView.findViewById(R.id.messageTime);
            messageImage = itemView.findViewById(R.id.messageImage);
            messageVideo = itemView.findViewById(R.id.messageVideo);
            messageText = itemView.findViewById(R.id.messageText);
            messageItem = itemView.findViewById(R.id.message);
        }
    }
}
