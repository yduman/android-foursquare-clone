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
 * @author Yadullah Duman
 */
public class VenuesAdapter extends BaseAdapter implements ListAdapter {

    private static final String TAG = "VenuesAdapter";
    private List<JSONObject> list = new ArrayList<>();
    private Context context;
    private String vName, vCategory, vLocation, vImagePath;

    public VenuesAdapter(List<JSONObject> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        View view = convertView;

        JSONObject venue = (JSONObject) getItem(pos);
        getVenueDataFrom(venue);

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.venue_list_item, null);
        }

        setUp(view);

        return view;
    }

    /**
     * helper to set venue results list item
     * @param view - the current view
     */
    private void setUp(View view) {
        ImageView venueImage = view.findViewById(R.id.venue_image);
        TextView venueName = view.findViewById(R.id.venue_name);
        TextView venueCategory = view.findViewById(R.id.venue_category);
        TextView venueLocation = view.findViewById(R.id.venue_location);

        Picasso.with(context).load(vImagePath).into(venueImage);
        venueName.setText(vName);
        venueCategory.setText(vCategory);
        venueLocation.setText(vLocation);
    }

    /**
     * helper to fetch data needed for the list item
     * @param venue - JSON of the venue
     */
    private void getVenueDataFrom(JSONObject venue) {
        try {
            String pathPrefix = venue.getJSONObject("photos").getJSONArray("groups").getJSONObject(0).getJSONArray("items").getJSONObject(0).getString("prefix");
            String pathInfix = "70x70";
            String pathSuffix = venue.getJSONObject("photos").getJSONArray("groups").getJSONObject(0).getJSONArray("items").getJSONObject(0).getString("suffix");
            vImagePath = pathPrefix + pathInfix + pathSuffix;
            vName = venue.getString("name");
            vCategory = venue.getJSONArray("categories").getJSONObject(0).getString("name");
            vLocation = venue.getJSONObject("location").getJSONArray("formattedAddress").getString(0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
