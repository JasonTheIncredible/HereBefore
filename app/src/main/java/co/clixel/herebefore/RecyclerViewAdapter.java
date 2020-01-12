package co.clixel.herebefore;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "RecyclerViewAdapter";

    private ArrayList<String> mMessageUser;
    private ArrayList<String> mMessageTime;
    private ArrayList<String> mMessageImage;
    private ArrayList<String> mMessageText;
    private Context mContext;

    RecyclerViewAdapter(Context context, ArrayList<String> mMessageUser, ArrayList<String> mMessageTime, ArrayList<String> mMessageImage, ArrayList<String> mMessageText) {

        this.mContext = context;
        this.mMessageUser = mMessageUser;
        this.mMessageTime = mMessageTime;
        this.mMessageImage = mMessageImage;
        this.mMessageText = mMessageText;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.recyclerviewlayout, parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Log.d(TAG, "onBindViewHolder: called.");
        //holder.messageUser.setText(mMessageUser.get(position));
        holder.messageTime.setText(mMessageTime.get(position));
        Picasso.get()
                .load(mMessageImage.get(position))
                .into(holder.messageImage);
        holder.messageText.setText(mMessageText.get(position));
        // Set messageImage to gone if an image doesn't exist for spacing consistency.
        if (mMessageImage.get(position) == null) {

            holder.messageImage.setVisibility(View.GONE);
        }
        holder.setIsRecyclable(true);

        // Change the color of every other row for visual purposes.
        if (position %2 == 0) {

            holder.itemView.setBackgroundColor(Color.parseColor("#222222"));
        } else {

            holder.itemView.setBackgroundColor(Color.parseColor("#292929"));
        }
    }

    @Override
    public int getItemCount() {

        return mMessageTime.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        TextView messageUser, messageTime, messageText;
        ImageView messageImage;
        RelativeLayout messageItem;
        
        ViewHolder(@NonNull View itemView) {

            super(itemView);
            messageUser = itemView.findViewById(R.id.messageUser);
            messageTime = itemView.findViewById(R.id.messageTime);
            messageImage = itemView.findViewById(R.id.messageImage);
            messageText = itemView.findViewById(R.id.messageText);
            messageItem = itemView.findViewById(R.id.message);
        }
    }
}
