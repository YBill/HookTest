package com.bill.hooktest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import android.os.Bundle;
import android.text.TextUtils;

public class DetailActivity extends AppCompatActivity {

    private AppCompatTextView tagTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        tagTv = findViewById(R.id.tv_tag);

        String tag = getIntent().getStringExtra("tag");

        if (!TextUtils.isEmpty(tag)) {
            tagTv.setText(tag);
        }

    }
}