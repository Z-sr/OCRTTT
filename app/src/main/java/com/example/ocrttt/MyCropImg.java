package com.example.ocrttt;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.isseiaoki.simplecropview.CropImageView;
import com.isseiaoki.simplecropview.callback.CropCallback;
import com.isseiaoki.simplecropview.callback.LoadCallback;
import com.isseiaoki.simplecropview.callback.SaveCallback;

import java.io.File;
import java.io.IOException;

public class MyCropImg extends AppCompatActivity implements View.OnClickListener {

    private CropImageView mCropImageView;
    public final static String IMG_URI = "imgUri";
    public final static String IMG_SAVE_URI = "imgSaveUri";
    public final static int REQUEST_CROP_CODE = 111;
    private View btn_save;
    private View btn_left_rotation;
    private View btn_right_rotation;
    private Bitmap mCropped;
    private Uri mImgSaveUri;
    private Intent mIntent;
    private Uri mImgUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_crop_img);
        mCropImageView = findViewById(R.id.civ_);
        btn_left_rotation = findViewById(R.id.btn_left_rotation);
        btn_right_rotation = findViewById(R.id.btn_right_rotation);
        btn_save = findViewById(R.id.btn_save);

        btn_left_rotation.setOnClickListener(this);
        btn_right_rotation.setOnClickListener(this);
        btn_save.setOnClickListener(this);


        mCropImageView.setCropMode(CropImageView.CropMode.FREE);
        mIntent = getIntent();
        mImgUri = mIntent.getParcelableExtra(IMG_URI);
        mImgSaveUri = mIntent.getParcelableExtra(IMG_SAVE_URI);
        if (mImgUri != null) {
            mCropImageView.load(mImgUri).execute(new LoadCallback() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onError(Throwable e) {
                    Toast.makeText(MyCropImg.this, "Load Image Failure", Toast.LENGTH_LONG).show();
                }
            });

        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_left_rotation:
                mCropImageView.rotateImage(CropImageView.RotateDegrees.ROTATE_M90D);
                break;
            case R.id.btn_right_rotation:
                mCropImageView.rotateImage(CropImageView.RotateDegrees.ROTATE_90D);
                break;
            case R.id.btn_save:
                saveImg();
                break;
        }
    }

    private void saveImg() {
        mCropImageView.crop(mImgUri)
                .execute(new CropCallback() {
                    @Override
                    public void onSuccess(Bitmap cropped) {
                        mCropImageView.save(cropped)
                                .execute(mImgSaveUri, new SaveCallback() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        setResult(REQUEST_CROP_CODE, mIntent);
                                        finish();
                                    }
                                    @Override
                                    public void onError(Throwable e) {
                                        Toast.makeText(MyCropImg.this, "Save Image Failure", Toast.LENGTH_LONG).show();
                                    }
                                });
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(MyCropImg.this, "crop Image Failure", Toast.LENGTH_LONG).show();
                    }
                });
    }
}
