package com.teammike.iptk.foursquare.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This adapter is responsible for the list view of {@link com.teammike.iptk.foursquare.fragments.FriendsListFragment}
 * @author Yadullah Duman
 */
public class FriendsAdapter extends BaseAdapter implements ListAdapter {

    private static final String TAG = "FriendsAdapter";
    private Context context;
    private int resource;
    private JSONArray jsonArray;

    public FriendsAdapter(@NonNull Context context, int resource, JSONArray jsonArray) {
        this.resource = resource;
        this.context = context;
        this.jsonArray = jsonArray;
    }

    @Override
    public int getCount() {
        return jsonArray.length();
    }

    @Override
    public JSONObject getItem(int i) {
        try {
            return jsonArray.getJSONObject(i);
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LinearLayout friendsView;
        String username = null;
        String imagePath = null;
        try {
            username = getItem(position).getString("username");
            imagePath = getItem(position).getString("imagePath");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (convertView == null) {
            friendsView = new LinearLayout(context);
            String inflater = Context.LAYOUT_INFLATER_SERVICE;
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(inflater);
            layoutInflater.inflate(resource, friendsView, true);
        } else {
            friendsView = (LinearLayout) convertView;
        }

        TextView usernameView = friendsView.findViewById(R.id.friend_username);
        ImageView friendImageView = friendsView.findViewById(R.id.friend_image);

        usernameView.setText(username);
        if (imagePath != null && !imagePath.equals("no image"))
            setImageWithPicasso(imagePath, friendImageView);
        else
            setDefaultImage(friendImageView);

        return friendsView;
    }

    /**
     * helper to set image with Picasso
     * @param imagePath - the image path
     * @param imageView - the view it will be displayed
     */
    private void setImageWithPicasso(String imagePath, ImageView imageView) {
        Picasso.with(context).load(Constants.BASE_URL_STATIC_DEV + imagePath).into(imageView);
    }

    /**
     * helper to set default image on failure events
     */
    private void setDefaultImage(ImageView imageView) {
        imageView.setImageResource(R.drawable.ic_sentiment_satisfied_black_24dp);
    }
}
