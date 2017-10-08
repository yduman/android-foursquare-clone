package com.teammike.iptk.foursquare.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.DefaultSliderView;
import com.teammike.iptk.foursquare.R;
import com.teammike.iptk.foursquare.utils.Constants;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * This activity is responsible for displaying a photo album with swiping feature for the current venue.
 * @author Yadullah Duman
 */
public class VenuePhotoAlbumActivity extends AppCompatActivity {

    @BindView(R.id.slider) SliderLayout sliderShow;
    @BindView(R.id.toolbar) Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_venue_photo_album);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        ArrayList<String> imagePaths = getImagePaths();
        buildSlider(imagePaths);
        setTitle("Photo Album");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onStop() {
        sliderShow.stopAutoCycle();
        super.onStop();
    }

    /**
     * helper to build the slider for images
     * @param imagePaths - all image paths for the current venue
     */
    private void buildSlider(ArrayList<String> imagePaths) {
        for (String path : imagePaths) {
            DefaultSliderView defaultSliderView = new DefaultSliderView(this);
            defaultSliderView
                    .image(path)
                    .setScaleType(BaseSliderView.ScaleType.FitCenterCrop);

            sliderShow.addSlider(defaultSliderView);
        }
    }

    /**
     * get the image paths sent by @{@link VenueOverviewActivity}
     * @return list of all image paths
     */
    private ArrayList<String> getImagePaths() {
        Intent intent = getIntent();
        return intent.getStringArrayListExtra(Constants.INTENT_KEY);
    }
}
