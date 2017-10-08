package com.teammike.iptk.foursquare.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.fragments.FriendRequestsListFragment;
import com.teammike.iptk.foursquare.fragments.FriendSearchFragment;
import com.teammike.iptk.foursquare.fragments.FriendsListFragment;
import com.teammike.iptk.foursquare.utils.FragmentHandler;

import butterknife.BindView;

/**
 * inside of this activity the user can deal with all friends related use cases
 * @author Yadullah Duman
 */
public class FriendsActivity extends BaseActivity {

    private static final String TAG = "FriendsActivity";
    private int fragmentContainer = R.id.activity_friends;
    private FragmentManager fragmentManager = getFragmentManager();
    private FriendsListFragment friendsListFragment;
    private FriendRequestsListFragment friendRequestsListFragment;
    private FriendSearchFragment friendSearchFragment;

    @BindView(R.id.toolbar) Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        initFragements();
        FragmentHandler.addFragment(fragmentContainer, friendsListFragment, fragmentManager);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        int count = getFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            finish();
            startActivity(new Intent(this, HomeActivity.class));
        } else
            getFragmentManager().popBackStack();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.friends_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settings_friend_requests)
            FragmentHandler.replaceFragment(fragmentContainer, friendRequestsListFragment, fragmentManager);
        else if (item.getItemId() == R.id.settings_search_friends)
            FragmentHandler.replaceFragment(fragmentContainer, friendSearchFragment, fragmentManager);

        return super.onOptionsItemSelected(item);
    }

    /**
     * initialize the fragments used by this activity
     * @see FriendsListFragment
     * @see FriendRequestsListFragment
     */
    private void initFragements() {
        friendsListFragment = (FriendsListFragment) Fragment.instantiate(this, FriendsListFragment.class.getName(), getIntentExtra());
        friendRequestsListFragment = (FriendRequestsListFragment) Fragment.instantiate(this, FriendRequestsListFragment.class.getName(), getIntentExtra());
        friendSearchFragment = (FriendSearchFragment) Fragment.instantiate(this, FriendSearchFragment.class.getName());
    }

    /**
     * helper method to get the response from the server and pass them to the fragments
     * @return String extra, which is the JSON returned from the server
     */
    private Bundle getIntentExtra() {
        Intent intent = getIntent();
        return intent.getExtras();
    }

    @Override
    int getContentViewId() {
        return R.layout.activity_friends;
    }

    @Override
    int getNavigationMenuItem() {
        return R.id.navigation_friends;
    }
}
