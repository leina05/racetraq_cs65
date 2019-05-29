package edu.dartmouth.cs.racetraq.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import edu.dartmouth.cs.racetraq.Models.DriveEntry;
import edu.dartmouth.cs.racetraq.R;

public class HistoryViewAdapter extends RecyclerView.Adapter<HistoryViewAdapter.DriveViewHolder>{

    private List<DriveEntry> entryList;
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm", Locale.US);
    private Context context;
    private String unitPref;

    private static final double MILES_TO_KM = 1.60934;
    private static final double KM_TO_MILES = 0.621371;


    public class DriveViewHolder extends RecyclerView.ViewHolder {
        public TextView name, dateTime, duration, distance, top_speed;
        public ImageView map_thumbnail;

        public DriveViewHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.history_drive_name);
            dateTime = itemView.findViewById(R.id.history_drive_date);
            distance = itemView.findViewById(R.id.history_drive_distance);
            duration = itemView.findViewById(R.id.history_drive_duration);
            top_speed = itemView.findViewById(R.id.history_drive_top_speed);
            map_thumbnail = itemView.findViewById(R.id.map_thumbnail);
        }
    }

    /**
     * Constructor
     */
    public HistoryViewAdapter(List<DriveEntry> entryList, Context context) {
        this.entryList = entryList;
        this.context = context;
    }

    @NonNull
    @Override
    public DriveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_layout, parent, false);
        return new DriveViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull DriveViewHolder holder, int position) {
        DriveEntry entry = entryList.get(position);

        // Set image
        holder.map_thumbnail.setImageBitmap(entry.getMap_thumbnail());

        // Set drive name
        holder.name.setText(entry.getName());

        // set dateTime
        holder.dateTime.setText(entry.getDateTime());

        // set distance
        holder.distance.setText("Distance: "+String.format("%.2f", entry.getDistance())+" miles");

        // set duration
        holder.duration.setText("Duration: " +entry.getDuration());

        // set top speed
        holder.top_speed.setText("Top Speed: "+String.format("%.2f", entry.getTopSpeed())+" mph");
    }

    @Override
    public int getItemCount() {
        return entryList.size();
    }

}
