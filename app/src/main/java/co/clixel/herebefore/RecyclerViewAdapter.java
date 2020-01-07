package co.clixel.herebefore;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "RecyclerViewAdapter";

    private ArrayList<String> mMessageUser;
    private ArrayList<String> mMessageTime;
    private ArrayList<String> mMessageText;
    private Context mContext;

    RecyclerViewAdapter(Context context, ArrayList<String> mMessageUser, ArrayList<String> mMessageTime, ArrayList<String> mMessageText) {

        this.mContext = context;
        this.mMessageUser = mMessageUser;
        this.mMessageTime = mMessageTime;
        this.mMessageText = mMessageText;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerviewlayout, parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Log.d(TAG, "onBindViewHolder: called.");
        //holder.messageUser.setText(mMessageUser.get(position));
        holder.messageTime.setText(mMessageTime.get(position));
        holder.messageText.setText(mMessageText.get(position));
        holder.setIsRecyclable(true);

        // Change the color of every other row for visual purposes.
        if (position %2 == 0) {

            holder.itemView.setBackgroundColor(Color.parseColor("#222222")); //121212 for darker
        } else {

            holder.itemView.setBackgroundColor(Color.parseColor("#303030")); //222222 for darker
        }
    }

    @Override
    public int getItemCount() {

        return mMessageText.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        TextView messageUser, messageTime, messageText;
        RelativeLayout messageItem;
        
        ViewHolder(@NonNull View itemView) {

            super(itemView);
            messageUser = itemView.findViewById(R.id.messageUser);
            messageTime = itemView.findViewById(R.id.messageTime);
            messageText = itemView.findViewById(R.id.messageText);
            messageItem = itemView.findViewById(R.id.message);
        }
    }
}
