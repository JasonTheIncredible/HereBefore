package co.clixel.herebefore;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MentionsAdapter extends RecyclerView.Adapter<MentionsAdapter.ViewHolder> {

    private List<String> mSuggestions;
    private Context mContext;
    private boolean theme;

    MentionsAdapter(Context mContext, List<String> mSuggestions) {

        this.mContext = mContext;
        this.mSuggestions = mSuggestions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.mentionsadapterlayout, parent, false);

        loadPreferences();

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {

        holder.suggestion.setText(mSuggestions.get(position));

        //holder.itemView.setOnClickListener(new View.OnClickListener() {

        //    @Override
        //    public void onClick(View v) {
        //    }
        //});

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

        return mSuggestions.size();
    }

    protected void loadPreferences() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        theme = sharedPreferences.getBoolean(co.clixel.herebefore.Settings.KEY_THEME_SWITCH, false);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView suggestion;

        ViewHolder(@NonNull View itemView) {

            super(itemView);
            suggestion = itemView.findViewById(R.id.suggestion);
        }
    }
}
