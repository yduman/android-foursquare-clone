package com.teammike.iptk.foursquare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.teammike.iptk.foursquare.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This adapter is responsible for the list view of @{@link com.teammike.iptk.foursquare.activities.CheckInsActivity}
 * @author Yadullah Duman
 */
public class CheckInsAdapter extends BaseAdapter implements ListAdapter {

    public static final String TAG = "CheckInsAdapter";
    private List<JSONObject> list = new ArrayList<>();
    private Context context;
    private String venueName, venueCategory, venueLocation, venueImage;

    public CheckInsAdapter(List<JSONObject> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public JSONObject getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        try {
            View view = convertView;
            JSONObject venue = getItem(pos).getJSONObject("venue");
            String timestamp = getItem(pos).getString("timestamp");

            getVenueDataFrom(venue);

            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.item_check_in, null);
            }

            setUp(view, timestamp);

            return view;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * helper to get venue data needed for the list item
     * @param venue - JSON of the venue
     */
    private void getVenueDataFrom(JSONObject venue) {
        try {
            String imgPrefix = venue.getJSONArray("photos").getJSONObject(0).getString("prefix");
            String imgInfix = "70x70";
            String imgSuffix = venue.getJSONArray("photos").getJSONObject(0).getString("suffix");

            venueImage = imgPrefix + imgInfix + imgSuffix;
            venueName = venue.getString("name");
            venueCategory = venue.getJSONArray("categories").getJSONObject(0).getString("name");
            venueLocation = venue.getJSONArray("formattedAddress").getString(0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * set the view for list item
     * @param view - the current view
     */
    private void setUp(View view, String timestamp) {
        ImageView imageView = view.findViewById(R.id.venue_image);
        TextView nameView = view.findViewById(R.id.venue_name);
        TextView categoryView = view.findViewById(R.id.venue_category);
        TextView locationView = view.findViewById(R.id.venue_location);
        TextView timestampView = view.findViewById(R.id.venue_timestamp);

        Picasso.with(context).load(venueImage).into(imageView);
        nameView.setText(venueName);
        categoryView.setText(venueCategory);
        locationView.setText(venueLocation);
        timestampView.setText(timestamp);
    }
}
