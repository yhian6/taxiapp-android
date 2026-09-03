package com.yhian.taxiapp;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.yhian.taxiapp.utils.ImageLoader;

public class SliderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_DESCRIPTION = "extra_description";
    public static final String EXTRA_DETAIL = "extra_detail";
    public static final String EXTRA_IMAGE_URL = "extra_image_url";
    public static final String EXTRA_CATEGORY = "extra_category";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(getColor(R.color.green_dark));
        getWindow().setNavigationBarColor(getColor(R.color.background_light));

        setContentView(R.layout.activity_slider_detail);

        ImageButton backButton = findViewById(R.id.backButton);
        ImageView detailImage = findViewById(R.id.detailImage);
        TextView categoryText = findViewById(R.id.detailCategoryText);
        TextView titleText = findViewById(R.id.detailTitleText);
        TextView descriptionText = findViewById(R.id.detailDescriptionText);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String description = getIntent().getStringExtra(EXTRA_DESCRIPTION);
        String detail = getIntent().getStringExtra(EXTRA_DETAIL);
        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        String category = getIntent().getStringExtra(EXTRA_CATEGORY);

        categoryText.setText(category != null ? category : "TaxiApp");
        titleText.setText(title != null ? title : "Informacion");
        descriptionText.setText(detail != null && !detail.trim().isEmpty()
                ? detail
                : description != null ? description : "Contenido informativo de TaxiApp.");

        ImageLoader.load(imageUrl, detailImage, R.drawable.bg_points_hero);

        backButton.setOnClickListener(view -> finish());
    }
}
