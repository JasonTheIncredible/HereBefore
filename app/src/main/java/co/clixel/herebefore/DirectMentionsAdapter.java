package co.clixel.herebefore;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.ContextMenu;
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

public class DirectMentionsAdapter extends RecyclerView.Adapter<DirectMentionsAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<String> mMessageTime, mMessageUser, mMessageImage, mMessageImageVideo, mMessageText, mShapeUUID;
    private ArrayList<Boolean> mUserIsWithinShape;
    private ImageButton playButtonInside, playButtonOutside;
    private boolean theme;

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView messageTimeInside, messageTimeOutside, messageUserInside, messageUserOutside, messageTextInside, messageTextOutside;
        ImageView messageImageInside, messageImageOutside, messageImageVideoInside, messageImageVideoOutside;
        FrameLayout videoFrameInside, videoFrameOutside;
        RelativeLayout messageItem;

        ViewHolder(@NonNull View itemView) {

            super(itemView);
            messageTimeInside = itemView.findViewById(R.id.messageTimeInside);
            messageTimeOutside = itemView.findViewById(R.id.messageTimeOutside);
            messageUserInside = itemView.findViewById(R.id.messageUserInside);
            messageUserOutside = itemView.findViewById(R.id.messageUserOutside);
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

    DirectMentionsAdapter(Context context, ArrayList<String> mMessageTime, ArrayList<String> mMessageUser, ArrayList<String> mMessageImage, ArrayList<String> mMessageImageVideo, ArrayList<String> mMessageText, ArrayList<String> mShapeUUID, ArrayList<Boolean> mUserIsWithinShape) {

        this.mContext = context;
        this.mMessageTime = mMessageTime;
        this.mMessageUser = mMessageUser;
        this.mMessageImage = mMessageImage;
        this.mMessageImageVideo = mMessageImageVideo;
        this.mMessageText = mMessageText;
        this.mShapeUUID = mShapeUUID;
        this.mUserIsWithinShape = mUserIsWithinShape;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.directmentionsadapterlayout, parent, false);

        loadPreferences();

        final ViewHolder holder = new ViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {

        holder.itemView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {

            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

                menu.add(position, R.string.report_post, 0, R.string.report_post);
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent Activity = new Intent(mContext, Chat.class);
                Activity.putExtra("shapeUUID", mShapeUUID.get(position));
                mContext.startActivity(Activity);
                ((DirectMentions) mContext).finish();
            }
        });

        // Set the left side if the user sent the message from inside the shape.
        if (mUserIsWithinShape.get(position)) {

            holder.messageTimeInside.setText(mMessageTime.get(position));

            holder.messageUserInside.setText(mMessageUser.get(position));

            // Set messageImage, messageImageVideo, or messageText to gone if an image or text doesn't exist, for spacing consistency.
            if (mMessageImage.get(position) == null) {

                holder.messageImageInside.setVisibility(View.GONE);
            } else {

                Glide.with(mContext)
                        .load(mMessageImage.get(position))
                        .apply(new RequestOptions().override(5000, 640).placeholder(R.drawable.ic_recyclerview_image_placeholder))
                        .into(holder.messageImageInside);

                holder.messageImageInside.setVisibility(View.VISIBLE);
            }

            if (mMessageImageVideo.get(position) == null) {

                holder.videoFrameInside.setVisibility(View.GONE);
            } else {

                Glide.with(mContext)
                        .load(mMessageImageVideo.get(position))
                        .apply(new RequestOptions().override(5000, 640).placeholder(R.drawable.ic_recyclerview_image_placeholder))
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

            holder.messageUserOutside.setText(mMessageUser.get(position));

            // Set messageImage, messageImageVideo, or messageText to gone if an image or text doesn't exist, for spacing consistency.
            if (mMessageImage.get(position) == null) {

                holder.messageImageOutside.setVisibility(View.GONE);
            } else {

                Glide.with(mContext)
                        .load(mMessageImage.get(position))
                        .apply(new RequestOptions().override(5000, 640).placeholder(R.drawable.ic_recyclerview_image_placeholder).centerInside())
                        .into(holder.messageImageOutside);

                holder.messageImageOutside.setVisibility(View.VISIBLE);
            }

            if (mMessageImageVideo.get(position) == null) {

                holder.videoFrameOutside.setVisibility(View.GONE);
            } else {

                Glide.with(mContext)
                        .load(mMessageImageVideo.get(position))
                        .apply(new RequestOptions().override(5000, 640).placeholder(R.drawable.ic_recyclerview_image_placeholder).centerInside())
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
        if (!theme) {

            if (position % 2 == 0) {

                holder.itemView.setBackgroundColor(Color.parseColor("#222222"));
            } else {

                holder.itemView.setBackgroundColor(Color.parseColor("#292929"));
            }
        } else {

            if (position % 2 == 0) {

                holder.itemView.setBackgroundColor(Color.parseColor("#D9D9D9"));
            } else {

                holder.itemView.setBackgroundColor(Color.parseColor("#F2F2F2"));
            }
        }
    }

    @Override
    public int getItemCount() {

        return mMessageTime.size();
    }

    @Override
    public int getItemViewType(int position) {

        return position;
    }

    protected void loadPreferences() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        theme = sharedPreferences.getBoolean(co.clixel.herebefore.Settings.KEY_THEME_SWITCH, false);
    }
}