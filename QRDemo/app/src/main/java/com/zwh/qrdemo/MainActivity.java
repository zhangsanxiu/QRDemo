package com.zwh.qrdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ImageView resultView;
    private EditText rawText;
    private Button generateButton;
    private Button clearButton;
    private Button scanButton;
    private Button picButton;
    private TextView resultText;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;
    private static final String defaultString = "青山横北郭，白水绕东城。\n此地一为别，孤蓬万里征。\n浮云游子意，落日故人情。\n挥手自兹去，萧萧班马鸣。";
    private String picPath;
    private Bitmap demoBitmap;
    private Uri user_selected_uri;

    private String[] PERMISSION_REQUIRED = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initComponents();
        verifyStoragePermissions();
    }
    public boolean verifyStoragePermissions() {
        int permission_result = 0;
        for (String permission:PERMISSION_REQUIRED) {
            permission_result +=  ActivityCompat.checkSelfPermission(getApplicationContext(), permission);
        }
        if (permission_result != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSION_REQUIRED, 100);

        }
        return permission_result == 0 ? true:false;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        int result = 0;
        String perssionNotice = "";
        int i = 0;
        for(int grantResult : grantResults){
            result +=  grantResult;
            if (grantResult == -1){
                perssionNotice += permissions[i] + "  ";
            }
            i++;
        }
        if (result != 0){
            Toast.makeText(this,"Required perssions:\n " + perssionNotice,Toast.LENGTH_SHORT).show();
            finish();
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    private void initComponents(){
        resultView = (ImageView) findViewById(R.id.resultView);
        rawText = (EditText) findViewById(R.id.rawText);

        generateButton = (Button) findViewById(R.id.generateButton);
        generateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("zhangwenhao","onClick " + rawText.getText().toString().length());

                if (rawText != null){
                    String rawString = rawText.getText().toString();
                    if(rawString.length() == 0){
                        rawString = defaultString;
                    }
                    MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
                    try {
                        Map<EncodeHintType, ErrorCorrectionLevel> hints = new HashMap<>();
                        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);

                        BitMatrix bitMatrix = multiFormatWriter.encode(new String(rawString.getBytes("UTF-8"), "ISO-8859-1"), BarcodeFormat.QR_CODE,200,200,hints);


                        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                        Bitmap bitmap = createBitmap(bitMatrix);
                        Bitmap overLay = scaleBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.sagereal),50,50);
                        Log.e("zhangwenhao","scale logo");

                        Bitmap combined = mergeBitmaps(bitmap,overLay);//= Bitmap.createBitmap(bitmap.getHeight(), bitmap.getWidth(), Bitmap.Config.ARGB_8888);


                        storeImage(bitmap);
                        resultView.setImageBitmap(bitmap);
                        resultView.setWillNotDraw(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        clearButton = (Button) findViewById(R.id.clearResult);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (resultView != null && rawText != null){
                    resultView.setWillNotDraw(true);
                    rawText.setText("");
                }
            }
        });
        scanButton = (Button) findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(MainActivity.this, ScanActivity.class), 0);
            }
        });
        resultText = (TextView) findViewById(R.id.resultText);
        picButton = (Button) findViewById(R.id.selectPic);
        picButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chooseFile;
                Intent intent;
                chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
                chooseFile.setType("image/*");
                intent = Intent.createChooser(chooseFile, "Choose a file");
                startActivityForResult(intent, 10);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10){
            if (data == null){
                Toast.makeText(this,"No picture selected!",Toast.LENGTH_SHORT).show();
                demoBitmap = null;
                return;
            }
            user_selected_uri = data.getData();
            Log.e("zhangwenhao","selected file uri: " + user_selected_uri);
            picPath = getPath(user_selected_uri); // should the path be here in this string
            Log.e("zhangwenhao","selected file path: " + picPath);
            Message message = new Message();
            message.what = 10;
            handler.sendMessage(message);
            return;
        }
        if (data != null) {
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString("SCAN_RESULT");
            resultText.setText(scanResult);
            rawText.setText(scanResult);
        }
    }
    public static Bitmap mergeBitmaps(Bitmap bitmap, Bitmap overLay) {
        int deltaHeight = bitmap.getHeight() - overLay.getHeight();
        int deltaWidth = bitmap.getWidth() - overLay.getWidth();
        Bitmap bmOverlay = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bitmap, new Matrix(), null);
        canvas.drawBitmap(overLay, deltaWidth/2, deltaHeight/2, null);
        return bmOverlay;
    }
    public static Bitmap scaleBitmap(Bitmap logo, int width, int hight){
        int deltaHeight = logo.getHeight();
        int deltaWidth = logo.getWidth();
        float delta = logo.getWidth() / (float)logo.getHeight();
        Log.e("zhangwenhao","hight = " + Math.round(hight / delta) + " width = " + width);
        return Bitmap.createScaledBitmap(logo,width,Math.round(hight / delta),false);
    }
    public Bitmap createBitmap(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int[] pixels = new int[width * height];
        Bitmap demo = BitmapFactory.decodeResource(getResources(),R.drawable.demo);
        if (demoBitmap != null){
            demo = demoBitmap;
        }
        demo = scaleBitmap(demo,200,200);
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                int pixcel;
                if (x < demo.getWidth() && y < demo.getHeight()) {
                    pixcel = demo.getPixel(x, y);
                }else {
                    pixcel = BLACK;
                }
                pixels[offset + x] = matrix.get(x, y) ? pixcel : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private void storeImage(Bitmap image) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d("zhangwenhao",
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d("zhangwenhao", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("zhangwenhao", "Error accessing file: " + e.getMessage());
        }
    }
    private  File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + getApplicationContext().getPackageName()
                + "/Files");

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        Log.e("zhangwenhao","store picture to phone");
        // Create a media file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        File mediaFile;
        String mImageName="MI_"+ timeStamp +".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case 10:
                    //Bitmap bitmap = getBitmapFromPath(picPath);
                    try{
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(user_selected_uri));
                    if (bitmap != null){
                        demoBitmap = bitmap;
                        resultView.setWillNotDraw(false);
                        resultView.setImageBitmap(demoBitmap);
                    }
                    }catch (Exception e){

                    }

                    break;
            }
            return false;
        }
    });
    private Bitmap getBitmapFromPath(String path){
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        if (bitmap != null){
            Log.e("zhangwenhao","pic success");
        }
        return bitmap;
    }
    public String getPath(Uri uri) {

        String path = null;
        String[] projection = { MediaStore.Files.FileColumns.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if(cursor == null){
            path = uri.getPath();
        }
        else{
            cursor.moveToFirst();
            int column_index = cursor.getColumnIndexOrThrow(projection[0]);
            path = cursor.getString(column_index);
            cursor.close();
        }

        return ((path == null || path.isEmpty()) ? (uri.getPath()) : path);
    }
}
