package com.teammike.iptk.foursquare.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.activities.LoginActivity;
import com.teammike.iptk.foursquare.activities.ProfileActivity;
import com.teammike.iptk.foursquare.utils.AppHandler;
import com.teammike.iptk.foursquare.utils.ClientErrorHandler;
import com.teammike.iptk.foursquare.utils.Constants;
import com.teammike.iptk.foursquare.utils.RestClient;
import com.teammike.iptk.foursquare.utils.Api;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This fragment displays the view for deleting a profile
 * @author Yadullah Duman
 */
public class DeleteProfileFragment extends Fragment {

    private static final String TAG = "DeleteProfileFragment";
    private String bundleExtra;

    @BindView(R.id.delete_profile_button_no) Button noButton;
    @BindView(R.id.delete_profile_button_yes) Button yesButton;
    @BindString(R.string.error_invalid_user) String invalidUser;
    @BindString(R.string.error_invalid_profile_deletion) String invalidDeletion;
    @BindString(R.string.error_internal_server_error) String internalServerErrorMsg;
    @BindString(R.string.delete_account) String fragmentTitle;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delete_profile, container, false);
        ButterKnife.bind(this, view);

        setListeners();
        getActivity().setTitle(fragmentTitle);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        bundleExtra = getArguments().getString(Constants.INTENT_KEY);
    }

    /**
     * helper method to set required listeners
     */
    private void setListeners() {
        noButton.setOnClickListener(v -> startActivity(new Intent(getContext(), ProfileActivity.class).putExtra(Constants.INTENT_KEY, bundleExtra)));
        yesButton.setOnClickListener(v -> deleteProfile());
    }

    /**
     * delete profile request to server
     */
    private void deleteProfile() {
        RestClient client = Api.getInstance().getClient();

        Call<ResponseBody> call = client.deleteUserById(AppHandler.getId());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200)
                    startActivity(new Intent(getActivity(), LoginActivity.class).putExtra(Constants.ACTION_KEY, Constants.ACTION_DELETE_PROFILE_VALUE));
                else if (response.code() == 400)
                    ClientErrorHandler.showMessage(getContext(), invalidDeletion);
                else if (response.code() == 404)
                    ClientErrorHandler.showMessage(getContext(), invalidUser);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppHandler.logError(getContext(), internalServerErrorMsg, t);
            }
        });
    }

}
