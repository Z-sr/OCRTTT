package com.example.ocrttt;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Color;
import android.graphics.Picture;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.DetectTextRequest;
import com.amazonaws.services.rekognition.model.DetectTextResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.TextDetection;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
//import com.yanzhenjie.durban.Controller;
//import com.yanzhenjie.durban.Durban;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.ocrttt.MyCropImg.IMG_SAVE_URI;
import static com.example.ocrttt.MyCropImg.IMG_URI;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView img_;
    private View btn_1;
    private View btn_2;
    private View btn_3;
    private EditText et_url;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};
    private static String[] PERMISSIONS_CAMERA = {"android.permission.CAMERA"};
    private String mKey = "public/img1.jpg";
    private String IN_IMG_PATH = "inImg";
    private String OUT_IMG_PATH = "outImg";
    private TextView tv_;
    private AmazonS3Client mAmazonS3Client;
    private AmazonRekognitionClient mAmazonRekognitionClient;
    private File mFilePath = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "VVVV");
    private long startTime;
    private long endTime;
    private RecyclerView rv_;
    private Map<String, List<String>> mDatas;
    private MyAdapterO mMyAdapterO;
    private AlertDialog mDialog;
    private TextView tv_dialog;
    private int REQUEST_CROP_CODE = 111;
    private ArrayList<String> mDateDatas;
    private ArrayList<String> mTimeDatas;
    private ArrayList<String> mPriceDatas;
    private ArrayList<String> mTotalDatas;
    private Uri mSelectImageUri;
    private File mImgOutFile;
    private long mUploadStartTime;
    private long mUploadEndTime;
    private long mRekognitionStartTime;
    private long mRekognitionEndTime;
    private long mCheckDataStartTime;
    private long mCheckDataEndTime;
    private Bitmap mCropBitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        img_ = findViewById(R.id.img_);
        rv_ = findViewById(R.id.rv_);
        btn_1 = findViewById(R.id.btn_1);
        btn_2 = findViewById(R.id.btn_2);
        btn_3 = findViewById(R.id.btn_3);
        et_url = findViewById(R.id.et_url);
        tv_ = findViewById(R.id.tv_);
        img_.setOnClickListener(this);
        btn_1.setOnClickListener(this);
        btn_2.setOnClickListener(this);
        btn_3.setOnClickListener(this);
        int checkSelfPermission = ActivityCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE");
        if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, 456);
        }
        initData();

        Log.e("-----", mFilePath.getAbsolutePath());
    }

    private void initData() {
        CognitoCachingCredentialsProvider credentialsProvider =
                new CognitoCachingCredentialsProvider(this,
                        "us-east-1:9e1d24c2-d3db-4cd3-a49c-1b7f7aa18c7d", // Identity pool ID
                        Regions.US_EAST_1 // Region
                );
        mAmazonRekognitionClient = new AmazonRekognitionClient(credentialsProvider);
        mAmazonS3Client = new AmazonS3Client(credentialsProvider);
        mDatas = new HashMap<>();
        mDateDatas = new ArrayList<>();
        mTimeDatas = new ArrayList<>();
        mPriceDatas = new ArrayList<>();
        mTotalDatas = new ArrayList<>();
        mDatas.put("Date: ", mDateDatas);
        mDatas.put("Time: ", mTimeDatas);
        mDatas.put("Price: ", mPriceDatas);
        mDatas.put("Total: ", mTotalDatas);
        mMyAdapterO = new MyAdapterO(mDatas, this);
        rv_.setLayoutManager(new LinearLayoutManager(this));
        rv_.setAdapter(mMyAdapterO);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        int[] location = new int[2];
        int[] location1 = new int[2];
        btn_1.getLocationOnScreen(location);
        btn_1.getLocationInWindow(location1);
        Log.e("--Changed---btn_1", "==" + Arrays.toString(location) + "==" + Arrays.toString(location1) + "==" + btn_1.getTop());
        btn_2.getLocationOnScreen(location);
        btn_2.getLocationInWindow(location1);
        Log.e("--Changed---btn_2", "==" + Arrays.toString(location) + "==" + Arrays.toString(location1) + "==" + btn_2.getTop());

    }

    @Override
    protected void onStart() {
        super.onStart();
        int[] location = new int[2];
        int[] location1 = new int[2];
        btn_1.getLocationOnScreen(location);
        btn_1.getLocationInWindow(location1);
        Log.e("--onStart---btn_1", "==" + Arrays.toString(location) + "==" + Arrays.toString(location1) + "==" + btn_1.getTop());
        btn_2.getLocationOnScreen(location);
        btn_2.getLocationInWindow(location1);
        Log.e("--onStart---btn_2", "==" + Arrays.toString(location) + "==" + Arrays.toString(location1) + "==" + btn_2.getTop());
    }

    @Override
    protected void onResume() {
        super.onResume();
        int[] location = new int[2];
        int[] location1 = new int[2];
        btn_1.getLocationOnScreen(location);
        btn_1.getLocationInWindow(location1);
        Log.e("--onResume---btn_1", "==" + Arrays.toString(location) + "==" + Arrays.toString(location1) + "==" + btn_1.getTop());
        btn_2.getLocationOnScreen(location);
        btn_2.getLocationInWindow(location1);
        Log.e("--onResume---btn_2", "==" + Arrays.toString(location) + "==" + Arrays.toString(location1) + "==" + btn_2.getTop());
    }

    /**
     * 开始识别
     */
    private void rekognition(String url) {
        clearData();
        showProgress(true, "Identify image");
//        https://triplog-ocr.s3.amazonaws.com/images/1.jpg
//        https://triplog-oregon.s3.amazonaws.com/receipts/cf57e00f-c7f1-4d06-963f-811dd87b5162.jpg
        String bucket = url.substring(8, url.indexOf("."));
        Uri uri = Uri.parse(url);
        String path = uri.getPath();
        if (path==null){
            Toast.makeText(MainActivity.this,"Url is wrong",Toast.LENGTH_LONG).show();
            return;
        }
        String photo = path.substring(1);
        Log.e("------", "---="+bucket+"=--" + url + "===" + photo);
        S3Object s3Object = new S3Object()
                .withName(photo)
//                .withBucket("triplog-ocr");
                .withBucket(bucket);
        Image image = new Image().withS3Object(s3Object);
        final DetectTextRequest request = new DetectTextRequest().withImage(image);

        new Thread(new Runnable() {
            @Override
            public void run() {
                mRekognitionStartTime = System.currentTimeMillis();
                DetectTextResult textResult = null;
                try {
                    textResult = mAmazonRekognitionClient.detectText(request);
                }catch (final Exception e){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();
                            showProgress(false,"");
                        }
                    });
                }
                if (textResult==null){
                    return;
                }
                endTime = System.currentTimeMillis();
                Log.e("------耗时", (endTime - startTime) + "");
                List<TextDetection> textDetections = textResult.getTextDetections();
                mRekognitionEndTime = System.currentTimeMillis();

                long l = System.currentTimeMillis();
                mCheckDataStartTime = System.currentTimeMillis();
                processText(textDetections);
                Log.e("----hahahhahahah", "---" + (System.currentTimeMillis() - l));

                Collections.sort(textDetections, new Comparator<TextDetection>() {
                    @Override
                    public int compare(TextDetection o1, TextDetection o2) {
                        return o2.getId() > o1.getId() ? 1 : o1.getId().equals(o2.getId()) ? 0 : -1;
                    }
                });
//                Collections.sort(textDetections, new Comparator<TextDetection>() {
//                    @Override
//                    public int compare(TextDetection o1, TextDetection o2) {
//                        return o2.getId() > o1.getId() ? 1 : o1.getId().equals(o2.getId()) ? 0 : -1;
//                    }
//                });

                Integer idLine = 0;
                final StringBuffer reStr = new StringBuffer();
                for (int i = 0; i < textDetections.size(); i++) {
                    TextDetection textDetection = textDetections.get(i);
                    Integer id = textDetection.getId();
                    if (!idLine.equals(id)) {
                        reStr.append("\n");
                        idLine = id;
                    }
                    reStr.append("  ")
                            .append(textDetection.getDetectedText());
                }
                Log.e("-----------", textDetections.size() + "\n=======\n" + reStr);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        tv_.setText("耗时---"+(endTime - startTime)+"ms\n\n"+reStr);
//                    }
//                });

//                for (Label label : labels) {
//                    Log.e("----------", label.getName() + ": " + label.getConfidence().toString());
//                }
            }
        }).start();

    }

    private void clearData() {
        mDateDatas.clear();
        mTimeDatas.clear();
        mPriceDatas.clear();
        mTotalDatas.clear();
        mMyAdapterO.notifyDataSetChanged();
    }

    private void processText(List<TextDetection> textDetections) {
        for (int i = 0; i < textDetections.size(); i++) {
            TextDetection textDetection = textDetections.get(i);
            Log.e("-------", textDetection.getType() + "===" + textDetection.getDetectedText() + "\n");
            String text = textDetection.getDetectedText();
            if (checkAll(textDetection)) {
                continue;
            }
            if (checkDate(text)) {
                addData(mDateDatas, text);
                continue;
            }
            if (checkTimeDate(text)) {
                Log.e("------Time","---------"+text);
                if (i + 1 < textDetections.size()
                        && checkTimeAPM(textDetections.get(i + 1).getDetectedText())) {
                    addData(mTimeDatas, text + " " + textDetections.get(i + 1).getDetectedText());
                    continue;
                }
                addData(mTimeDatas, text);
                continue;
            }
            if (checkPrice(text)) {
                addData(mPriceDatas, text);
                continue;
            }
            if (checkTotal(text)) {
                addData(mTotalDatas, text);
            }
        }
        mCheckDataEndTime = System.currentTimeMillis();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showProgress(false, "");
                mMyAdapterO.notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "Identify image success", Toast.LENGTH_LONG).show();
                tv_.setText("上传耗时==" + (mUploadEndTime - mUploadStartTime)
                        + "==在线解析耗时==" + (mRekognitionEndTime - mRekognitionStartTime)
                        + "==本地解析耗时==" + (mCheckDataEndTime - mCheckDataStartTime));
            }
        });
    }

    private void addData(ArrayList<String> datas, String text) {
        if (!datas.contains(text)) {
            datas.add(text);
        }
    }

    private boolean checkAll(TextDetection textDetection) {
        String detectedText = textDetection.getDetectedText();
        return Pattern.compile("([a-zA-Z]+){4}").matcher(detectedText).matches();
    }

    private boolean checkTimeDate(String text) {
        return Pattern.compile("^([0-2])?\\d:([0-5])?\\d(:([0-5])?\\d)?([APap][Mm])?").matcher(text).matches();
    }

    private boolean checkTimeAPM(String text) {
        return Pattern.compile("^[APap]([Mm])?").matcher(text).matches();
    }

    private boolean checkPrice(String text) {
        return Pattern.compile("^([$@])?[1-6]\\.\\d\\d\\d(/[a-zA-Z]+)?").matcher(text).matches()
                &&checkAmount(text);
    }

    private boolean checkTotal(String text) {
        return Pattern.compile("^(\\$)?\\d+\\.\\d\\d").matcher(text).matches()
                &&checkAmount(text);
    }

    private boolean checkDate(String text) {
        return Pattern.compile("(20\\d\\d[/\\-])?([0-3])?\\d[/\\-]([0-3])?\\d([/\\-]\\d\\d)?([/\\-]20\\d\\d)?").matcher(text).matches();
    }

    private boolean checkAmount(String text) {
        String amountStr = text.replaceAll("[a-zA-Z/$@]", "");
        try{
            return Float.parseFloat(amountStr)>0f;
        }catch (Exception e){
            return true;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 321) {//相册选择图片
            if (data == null) {
                return;
            }
            Uri uri = data.getData();
            if (uri != null) {
                mSelectImageUri = uri;
                String filePath = getRealPathFromUriAboveApi19(this, uri);
                startTime = System.currentTimeMillis();
                Log.e("--------", "开始");
                //开始调用系统剪裁
                croppingImg();
            }
        } else if (requestCode == 456) {//获取到权限
            int checkSelfPermission = ActivityCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE");

        } else if (requestCode == 789) {//拍照的返回
            if (resultCode == RESULT_OK) {
                croppingImg();
            }
        } else if (requestCode == 963) {
            int checkSelfPermission = ActivityCompat.checkSelfPermission(this, "android.permission.CAMERA");
            if (checkSelfPermission == PackageManager.PERMISSION_GRANTED) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, 789);
                }
            }
        } else if (requestCode == REQUEST_CROP_CODE) {//剪裁图片后的返回

//            Bundle bundle = data.getExtras();
//            Bitmap bitmap = bundle.getParcelable("data");
//            img_.setImageBitmap(bitmap);

//            // accept the cropping results
            String imgPath = mImgOutFile.getAbsolutePath();
            mCropBitmap = BitmapFactory.decodeFile(imgPath);
            img_.setImageBitmap(mCropBitmap);
            mUploadStartTime = System.currentTimeMillis();
            upload(imgPath);
        }
    }

    //    private void croppingImg(String filePath) {
//        if (!mFilePath.exists()) {
//            mFilePath.mkdirs();
//        }
//        Durban.with(this)
//                .title("Crop Image")
//                .statusBarColor(ContextCompat.getColor(this, R.color.color_000000))
//                .toolBarColor(ContextCompat.getColor(this, R.color.color_1b6dcc))
//                .navigationBarColor(ContextCompat.getColor(this, R.color.color_1b6dcc))
//                // Image path list/array.
//                .inputImagePaths(filePath)
//                // Image output directory.
//                .outputDirectory(mFilePath.getAbsolutePath())
//                // Image size limit.
//                .maxWidthHeight(1000, 1000)
//                // Aspect ratio.
//                .aspectRatio(1f, 1f)
//                // Output format: JPEG, PNG.
//                .compressFormat(Durban.COMPRESS_JPEG)
//                // Compress quality, see Bitmap#compress(Bitmap.CompressFormat, int, OutputStream)
//                .compressQuality(90)
//                // Gesture: ROTATE, SCALE, ALL, NONE.
//                .gesture(Durban.GESTURE_ALL)
//                .controller(Controller.newBuilder()
//                        .enable(true)
//                        .rotation(true)
//                        .rotationTitle(true)
//                        .scale(true)
//                        .scaleTitle(true)
//                        .build())
//                .requestCode(REQUEST_CROP_CODE)
//                .start();
//    }


    private void croppingImg() {
        Intent intent = new Intent(this,MyCropImg.class);
        intent.putExtra(IMG_URI,mSelectImageUri);
        mImgOutFile = getImgFile(OUT_IMG_PATH);
        if (mImgOutFile == null) {
            return;
        }
        Uri uri = Uri.fromFile(mImgOutFile);
        intent.putExtra(IMG_SAVE_URI,uri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent,REQUEST_CROP_CODE);
    }

//    /**
//     * 裁剪图片
//     */
//    private void croppingImg(Uri uri, File file) {
//        Uri inUri;
//        Uri outUri;
//        if (uri == null) {//拍照来的
//            inUri = FileProvider.getUriForFile(this, "com.example.ocrttt.fileprovider", file);
//            outUri = inUri;
//            outFile = file;
//        } else {//相册选择的
//            inUri = uri;
//            outFile = getImgFile(OUT_IMG_PATH);
//            if (outFile == null) {
//                return;
//            }
//            outUri = FileProvider.getUriForFile(this, "com.example.ocrttt.fileprovider", outFile);
//        }
//
//        Intent intent = new Intent("com.android.camera.action.CROP");
//        intent.setDataAndType(inUri, "image/*");
////        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
////        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
//        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
//                | Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
//        // 下面这个crop=true是设置在开启的Intent中设置显示的VIEW可裁剪
//        intent.putExtra("crop", "true");
//        intent.putExtra("scale", false);
//        // aspectX aspectY 是宽高的比例
//        intent.putExtra("aspectX", 1);
//        intent.putExtra("aspectY", 1);
//        // outputX,outputY 是剪裁图片的宽高
//        intent.putExtra("outputX", 720);
//        intent.putExtra("outputY", 720);
//        //设置了true的话直接返回bitmap，可能会很占内存
//        intent.putExtra("return-data", false);
//        //设置输出的格式
//        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
//        //设置输出的地址
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, outUri);
//        //不启用人脸识别
//        intent.putExtra("noFaceDetection", true);
//
//        startActivityForResult(intent, REQUEST_CROP_CODE);//打开剪裁Activity
//    }

    public File saveBitmap(Bitmap bm, String picName) {
        Log.e("", "保存图片---" + mFilePath);
        startTime = System.currentTimeMillis();
        try {
            File file = getImgFile(picName);
            FileOutputStream out = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private File getImgFile(String picName) {
        if (!mFilePath.exists()) {
            mFilePath.mkdirs();
        }
        try {
            File file = new File(mFilePath + File.separator, picName + ".jpg");
            if (!file.exists()) {
                file.createNewFile();
            }
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 适配api19及以上,根据uri获取图片的绝对路径
     *
     * @param context 上下文对象
     * @param uri     图片的Uri
     * @return 如果Uri对应的图片存在, 那么返回该图片的绝对路径, 否则返回null
     */
    private static String getRealPathFromUriAboveApi19(Context context, Uri uri) {
        String filePath = null;
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // 如果是document类型的 uri, 则通过document id来进行处理
            String documentId = DocumentsContract.getDocumentId(uri);
            if (isMediaDocument(uri)) { // MediaProvider
                // 使用':'分割
                String type = documentId.split(":")[0];
                String id = documentId.split(":")[1];

                String selection = MediaStore.Images.Media._ID + "=?";
                String[] selectionArgs = {id};

                //
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                filePath = getDataColumn(context, contentUri, selection, selectionArgs);
            } else if (isDownloadsDocument(uri)) { // DownloadsProvider
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(documentId));
                filePath = getDataColumn(context, contentUri, null, null);
            } else if (isExternalStorageDocument(uri)) {
                // ExternalStorageProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    filePath = Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else {
                //Log.e("路径错误");
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // 如果是 content 类型的 Uri
            filePath = getDataColumn(context, uri, null, null);
        } else if ("file".equals(uri.getScheme())) {
            // 如果是 file 类型的 Uri,直接获取图片对应的路径
            filePath = uri.getPath();
        }
        return filePath;
    }

    /**
     * 获取数据库表中的 _data 列，即返回Uri对应的文件路径
     *
     * @return
     */
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        String path = null;

        String[] projection = new String[]{MediaStore.Images.Media.DATA};
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(projection[0]);
                path = cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            if (cursor != null) {
                cursor.close();
            }
        }
        return path;
    }

    /**
     * @param uri the Uri to check
     * @return Whether the Uri authority is MediaProvider
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri the Uri to check
     * @return Whether the Uri authority is DownloadsProvider
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * 上传
     *
     * @param file
     */
    public void uploadWithTransferUtility(File file) {

        TransferUtility transferUtility = TransferUtility.builder()
                .context(getApplicationContext())
                .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                .s3Client(mAmazonS3Client)
                .build();
        TransferObserver uploadObserver = transferUtility.upload(mKey, file);

        // Attach a listener to the observer to get state update and progress notifications
        uploadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    // Handle a completed upload.
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int) percentDonef;

                Log.d("YourActivity", "ID:" + id + " bytesCurrent: " + bytesCurrent
                        + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                // Handle errors
            }

        });

        // If you prefer to poll for the data, instead of attaching a
        // listener, check for the state and progress in the observer.
        if (TransferState.COMPLETED == uploadObserver.getState()) {
            // Handle a completed upload.
        }

        Log.d("YourActivity", "Bytes Transferred: " + uploadObserver.getBytesTransferred());
        Log.d("YourActivity", "Bytes Total: " + uploadObserver.getBytesTotal());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_1:
                click1();
                break;
            case R.id.btn_2:
                click2();
                break;
            case R.id.btn_3:
                mCropBitmap=null;
                click3();
                break;
            case R.id.img_:
                clickImg();
                break;
        }
    }

    private void clickImg() {
        String enterUrl = et_url.getText().toString();
        if (mCropBitmap==null&&TextUtils.isEmpty(enterUrl)){
            return;
        }
        showImg(mCropBitmap,enterUrl);
    }

    private void showImg(Bitmap bitmap,String url) {
        final android.app.AlertDialog dialog = new android.app.AlertDialog
                .Builder(this, R.style.showImgDialog)
                .create();
        if (dialog != null) {
            dialog.show();
            Window window = dialog.getWindow();
            if (window == null) return;
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            window.setLayout(displayMetrics.widthPixels * 4 / 5,
                    displayMetrics.heightPixels * 4 / 5);
            Log.e("-----大小-","-----"+(displayMetrics.widthPixels * 4 / 5)+"---"+(displayMetrics.heightPixels * 4 / 5));
            ImageView imageView = new TouchImageView(this);
            window.setContentView(imageView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                    , ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            Glide.with(MainActivity.this)
                    .load(bitmap != null ? bitmap : url)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.loading)
                            .error(R.drawable.load_failed))
                    .into(imageView);
        }
    }
    /**
     * enter Url
     */
    private void click3() {
        InputMethodManager inputMethodManager = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(et_url.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        String enterUrl = et_url.getText().toString();
        if (TextUtils.isEmpty(enterUrl)) {
            Toast.makeText(this, "Url cannot be empty", Toast.LENGTH_LONG).show();
            return;
        }
        if (enterUrl.length() < 8) {
            Toast.makeText(this, "Url is wrong", Toast.LENGTH_LONG).show();
            return;
        }
        if (!enterUrl.contains("s3.amazonaws")) {
            Toast.makeText(this, "Url Not AWS", Toast.LENGTH_LONG).show();
        } else {
            Glide.with(this)
                    .load(enterUrl)
                    .into(img_);
            rekognition(enterUrl);
        }
    }

    /**
     * select img
     */
    private void click2() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, 321);
    }

    /**
     * photograph
     */
    private void click1() {
        int checkSelfPermission = ActivityCompat.checkSelfPermission(this, "android.permission.CAMERA");
        if (checkSelfPermission == PackageManager.PERMISSION_GRANTED) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File file = getImgFile(IN_IMG_PATH);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //表示對目標應用臨時授權該Uri所代表的文件
                    mSelectImageUri = FileProvider.getUriForFile(this, "com.example.ocrttt.fileprovider", file);
                } else {
                    //7.0以下，如果直接拿到相機返回的intent值，拿到的則是拍照的原圖大小，很容易發生OOM，所以我們同樣將返回的地址，保存到指定路徑，返回到Activity時，去指定路徑獲取，壓縮圖片
                    mSelectImageUri = Uri.fromFile(file);
                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mSelectImageUri);
                startActivityForResult(takePictureIntent, 789);
            }
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS_CAMERA, 963);
        }
    }


    private void upload(String filePath) {
        showProgress(true, "uploading image");
        //https://triplog-ocr.s3.amazonaws.com/images/1.jpg
        String[] split = filePath.split("\\.");
        final String key = "images" + File.separator + System.currentTimeMillis() + "." + split[split.length - 1];

        TransferUtility transferUtility = TransferUtility.builder()
                .context(getApplicationContext())
                .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                .s3Client(mAmazonS3Client)
                .defaultBucket("triplog-ocr")
                .build();
        TransferObserver uploadObserver = transferUtility.upload(key, new File(filePath));

        // Attach a listener to the observer to get state update and progress notifications
        uploadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    // Handle a completed upload.
                    mUploadEndTime = System.currentTimeMillis();
                    rekognition("https://triplog-ocr.s3.amazonaws.com/" + key);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int) percentDonef;
                showProgress(true, "uploading image -- " + percentDone + "%");

                Log.e("YourActivity", "ID:" + id + " bytesCurrent: " + bytesCurrent
                        + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                // Handle errors
                Log.e("-----upload失败--", ex.toString());
                showProgress(false, "");
                Toast.makeText(MainActivity.this, "uploading images failure", Toast.LENGTH_LONG).show();
            }

        });
    }

    private void showProgress(boolean isShow, String msg) {
        if (mDialog == null) {
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_v, null, false);
            tv_dialog = view.findViewById(R.id.tv_dialog);
            mDialog = new AlertDialog
                    .Builder(this)
                    .setView(view)
                    .setCancelable(false)
                    .create();
        }
        if (!TextUtils.isEmpty(msg)) {
            tv_dialog.setText(msg);
        }

        if (isShow && !mDialog.isShowing()) {
            mDialog.show();
        } else if (!isShow && mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }


    class MyAdapterO extends RecyclerView.Adapter<MyAdapterO.MyViewHolderO> {
        private Map<String, List<String>> datas;
        private Context mContext;

        public MyAdapterO(Map<String, List<String>> datas, Context context) {
            this.datas = datas;
            mContext = context;
        }

        @NonNull
        @Override
        public MyViewHolderO onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_rv_o, viewGroup, false);
            return new MyViewHolderO(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolderO myViewHolderO, int i) {
            String key = i == 0 ? "Date: " : i == 1 ? "Time: " : i == 2 ? "Price: " : "Total: ";
            List<String> valueList = datas.get(key);
            myViewHolderO.tv_item_key.setText(key);
            if (valueList == null || valueList.isEmpty()) {
                myViewHolderO.et_item_value.setHint(key);
            } else {
                myViewHolderO.et_item_value.setText(valueList.get(0));
            }
            //有多条匹配数据
            if (valueList != null && valueList.size() > 1) {
                myViewHolderO.rv_item_rv.setVisibility(View.VISIBLE);
                MyAdapterT myAdapterT = new MyAdapterT(valueList, mContext, myViewHolderO.et_item_value);
                LinearLayoutManager layoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
                myViewHolderO.rv_item_rv.setLayoutManager(layoutManager);
                myViewHolderO.rv_item_rv.setAdapter(myAdapterT);
            } else {
                myViewHolderO.rv_item_rv.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return datas.size();
        }

        class MyViewHolderO extends RecyclerView.ViewHolder {
            private final TextView tv_item_key;
            private final EditText et_item_value;
            private final RecyclerView rv_item_rv;

            public MyViewHolderO(@NonNull View itemView) {
                super(itemView);
                tv_item_key = itemView.findViewById(R.id.tv_item_key);
                et_item_value = itemView.findViewById(R.id.et_item_value);
                rv_item_rv = itemView.findViewById(R.id.rv_item_rv);

            }
        }
    }

    class MyAdapterT extends RecyclerView.Adapter<MyAdapterT.MyViewHolderT> {
        private final EditText et_item_value;
        private List<String> datas;
        private Context mContext;

        public MyAdapterT(List<String> datas, Context context, EditText et_item_value) {
            this.datas = datas;
            mContext = context;
            this.et_item_value = et_item_value;
        }

        @NonNull
        @Override
        public MyViewHolderT onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            TextView textView = new TextView(mContext);
//            GradientDrawable drawable = new GradientDrawable();
//            drawable.setCornerRadius(0.5f);
//            drawable.setStroke(dip2px(mContext,1),Color.parseColor("#1b6dcc"));
//            textView.setBackground(drawable);
            textView.setBackgroundResource(R.drawable.item_rv_value);
            textView.setTextSize(dip2px(mContext, 4));
            textView.setTextColor(Color.parseColor("#000000"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, dip2px(mContext, 10), 0);
            textView.setLayoutParams(params);
            int i1 = dip2px(mContext, 5);
            int i2 = dip2px(mContext, 2);
            textView.setPadding(i1, i2, i1, i2);
            textView.setGravity(Gravity.CENTER);
            return new MyViewHolderT(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolderT myViewHolderT, int i) {
            final String text = datas.get(i);
            myViewHolderT.mTextView.setText(text);
            myViewHolderT.mTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    et_item_value.setText(text);

                }
            });
        }

        @Override
        public int getItemCount() {
            return datas.size();
        }

        class MyViewHolderT extends RecyclerView.ViewHolder {
            private final TextView mTextView;

            public MyViewHolderT(@NonNull View itemView) {
                super(itemView);
                mTextView = (TextView) itemView;
            }
        }
    }


    /*** dp--->px*/
    public static int dip2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /*** sp--->px*/
    public static int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    /*** px---->dp*/
    public static int px2dip(Context context, float pxValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /*** px--->sp*/
    public static int px2sp(Context context, float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }
}
