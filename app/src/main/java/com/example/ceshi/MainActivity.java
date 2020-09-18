package com.example.ceshi;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ceshi.PeterTimeCountRefresh;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import android.provider.DocumentsContract;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView timerView;
    private long baseTimer;
    private ImageButton imageButton;
    private static final int SELECT_PICTURE = 1;
    private static final int SELECT_CAMER= 1;
    private ImageView imageView;
    private LinearLayout contactInfo;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor editor;
    Uri contentUri;
    private String icon = "上传地址";

    //相册请求码
    private static final int CONSTANTS_SELECT_PHOTO_CODE = 1;
    //相机请求码
    private static final int CAMERA_REQUEST_CODE = 2;
    //剪裁请求码
    private static final int CROP_REQUEST_CODE = 3;

    //调用照相机返回图片文件
    private File tempFile;

    boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    private static boolean checkCameraFacing(final int facing) {
        if (getSdkVersion() < Build.VERSION_CODES.GINGERBREAD) {
            return false;
        }
        final int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, info);
            if (facing == info.facing) {
                return true;
            }
        }
        return false;
    }
    public static boolean hasBackFacingCamera() {
        final int CAMERA_FACING_BACK = 0;
        return checkCameraFacing(CAMERA_FACING_BACK);
    }
    public static boolean hasFrontFacingCamera() {
        final int CAMERA_FACING_BACK = 1;
        return checkCameraFacing(CAMERA_FACING_BACK);
    }
    public static int getSdkVersion() {
        return android.os.Build.VERSION.SDK_INT;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);

        MainActivity.this.baseTimer = SystemClock.elapsedRealtime();
        timerView = (TextView) this.findViewById(R.id.timerView);
        imageButton = (ImageButton) this.findViewById(R.id.photo);
        imageView = (ImageView) this.findViewById(R.id.back_ground);
        contactInfo = (LinearLayout)findViewById(R.id.activity_main);
//        mSharedPreferences=getSharedPreferences("ThumbLock", Activity.MODE_PRIVATE);
//        editor=mSharedPreferences.edit();
        getBitmapFromSharedPreferences();

        imageButton.setOnClickListener(this);

        final Handler startTimehandler = new Handler(){
            public void handleMessage(android.os.Message msg) {
                if (null != timerView) {
                    timerView.setText((String) msg.obj);
                }
            }
        };
        new Timer("开机计时器").scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int time = (int)((SystemClock.elapsedRealtime() - getGapMinutes(startTime,endTime)) / 1000);
                String hh = new DecimalFormat("00").format(time / 3600);
                String mm = new DecimalFormat("00").format(time % 3600 / 60);
                String ss = new DecimalFormat("00").format(time % 60);
                String timeFormat = new String(hh + ":" + mm + ":" + ss);
                Message msg = new Message();
                msg.obj = timeFormat;
                startTimehandler.sendMessage(msg);
            }

        }, 0, 1000L);
        super.onCreate(savedInstanceState);
    }
    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.photo:
                showChoosePhotoDialog();
                break;

            default:
                break;
        }
    }
    private void showChoosePhotoDialog() {
        CharSequence[] items = { "相册", "相机" };
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("选择图片来源")
                .setItems(items, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        if (which == SELECT_PICTURE) {
                            //用于保存调用相机拍照后所生成的文件
                            tempFile = new File(Environment.getExternalStorageDirectory().getPath(), System.currentTimeMillis() + ".jpg");
                            //跳转到调用系统相机
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            //判断版本
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {   //如果在Android7.0以上,使用FileProvider获取Uri
                                intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                Uri contentUri = FileProvider.getUriForFile(MainActivity.this,  BuildConfig.APPLICATION_ID + ".myprovider", tempFile);
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
                            } else {    //否则使用Uri.fromFile(file)方法获取Uri
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));
                            }
                            startActivityForResult(intent, CAMERA_REQUEST_CODE);
                        } else {
                            final Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);    // 选择数据
                            photoPickerIntent.setType("image/*");                               // 获取所有本地图片
                            startActivityForResult(photoPickerIntent, CONSTANTS_SELECT_PHOTO_CODE);
                        }
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        dialog.cancel();
                    }
                }).create();
        dialog.show();
    }

//    //选择图片或拍完照片之后触发
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        // TODO Auto-generated method stub
//        super.onActivityResult(requestCode, resultCode, data);
//        if (resultCode == RESULT_OK) {
//            View view = this.getWindow().getDecorView();
//            Uri uri = data.getData();
//            Cursor cursor = getContentResolver().query(uri, null, null, null, null);//用ContentProvider查找选中的图片
//            cursor.moveToFirst();
//////            String path=cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));//获取图片的绝对路径
//            String path=getPath(this,uri);
//            Bitmap bitmap = BitmapFactory.decodeFile(path);
//            Drawable drawable = new BitmapDrawable(bitmap);
////            Drawable drawable= Drawable.createFromPath(path);
//            // 获取手机屏幕的像素
//            DisplayMetrics dm = new DisplayMetrics();
//            getWindowManager().getDefaultDisplay().getMetrics(dm);
//            Resources s = getResources();
//            view.setBackgroundDrawable(drawable);
////            setBackGround(path,dm,s);
////            contactInfo.setBackground(drawable);
//            cursor.close();
//
//
//
//        } else {
//            Toast.makeText(this, "选择图片失败,请重新选择", Toast.LENGTH_SHORT)
//                    .show();
//        }
//    }
    public  void saveBitmapToSharedPreferences(Bitmap bitmap){
        ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
        //第二步:利用Base64将字节数组输出流中的数据转换成字符串String
        byte[] byteArray=byteArrayOutputStream.toByteArray();
        String imageString=new String(Base64.encodeToString(byteArray, Base64.DEFAULT));
        //第三步:将String保持至SharedPreferences
        SharedPreferences sharedPreferences=getSharedPreferences("testSP", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        editor.putString("image", imageString);
        editor.commit();

    }
    private void getBitmapFromSharedPreferences()
    {
        SharedPreferences sharedPreferences=getSharedPreferences("testSP", Context.MODE_PRIVATE);
        //第一步:取出字符串形式的Bitmap
        String imageString=sharedPreferences.getString("image", "");
        //第二步:利用Base64将字符串转换为ByteArrayInputStream
        byte[] byteArray=Base64.decode(imageString, Base64.DEFAULT);
        ByteArrayInputStream byteArrayInputStream=new ByteArrayInputStream(byteArray);
        //第三步:利用ByteArrayInputStream生成Bitmap
        Bitmap bitmap=BitmapFactory.decodeStream(byteArrayInputStream);
        imageView.setImageBitmap(bitmap);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
            super.onActivityResult(requestCode, resultCode, intent);
            Bitmap bitmap = null;
            switch (requestCode) {
                case CONSTANTS_SELECT_PHOTO_CODE:
                    if (resultCode == RESULT_OK) {
                        try {
                            getBitmapFromSharedPreferences();
                            final Uri imageUri = intent.getData();
                            final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                            bitmap = BitmapFactory.decodeStream(imageStream);
                            View view = this.getWindow().getDecorView();
                            WindowManager wm = (WindowManager) this.getSystemService(
                                    Context.WINDOW_SERVICE);
                            int w = wm.getDefaultDisplay().getWidth();
                            int h = wm.getDefaultDisplay().getHeight();
                            BitmapFactory.Options factory = new BitmapFactory.Options();
                            // 如果设置为true,允许查询图片不是按照像素分配内存
                            factory.inJustDecodeBounds = true;
                            int wRatio = (int) Math.ceil(factory.outWidth / (float) w);
                            // 高度之比
                            int hRatio = (int) Math.ceil(factory.outHeight / (float) h);
                            if (wRatio > 1 || hRatio > 1) {
                                // inSampleSize>1则返回比原图更小的图片
                                if (hRatio > wRatio) {
                                    factory.inSampleSize = hRatio;
                                } else {
                                    factory.inSampleSize = wRatio;
                                }
                            }
                            // 该属性为false则允许调用者查询图片无需为像素分配内存
                            factory.inJustDecodeBounds = false;
                            // 再次使用BitmapFactory对象图像进行适屏操作
                            Bitmap bmp = BitmapFactory.decodeStream(getContentResolver()
                                    .openInputStream(imageUri), null, factory);
//                            int width = bitmap.getWidth();
//                            int height = bitmap.getHeight();
//                            int newWidth = w;
//                            int newHeight = h;
//                            float scaleWight = ((float)newWidth)/width;
//                            float scaleHeight = ((float)newHeight)/height;
//                            Matrix matrix = new Matrix();
//                            matrix.postScale(scaleWight, scaleHeight);
//                            Bitmap res = Bitmap.createBitmap(bitmap, 0,0,width, height, matrix, true);

                            Drawable drawable = new BitmapDrawable(bmp);
//                            view.setBackgroundDrawable(drawable);
                            saveBitmapToSharedPreferences(bmp);
                            imageView.setImageDrawable(drawable);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }break;
                default:
                    break;
    }

    }
    /**
     * 裁剪图片
     */
    private void cropPhoto(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        intent.putExtra("return-data", true);
        startActivityForResult(intent, CROP_REQUEST_CODE);
    }

    /**
     * 保存图片到本地
     *
     * @param name
     * @param bmp
     * @return
     */
    public String saveImage(String name, Bitmap bmp) {
        File appDir = new File(Environment.getExternalStorageDirectory().getPath());
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = name + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }







    /** 将格林威治时间格式转为指定的时间格式 */
    public static String GTMToLocal(String GTMDate) {

        int tIndex = GTMDate.indexOf("T");
        String dateTemp = GTMDate.substring(0, tIndex);
        String timeTemp = GTMDate.substring(tIndex + 1, GTMDate.length() - 6);
        String convertString = dateTemp + " " + timeTemp;

        SimpleDateFormat format;
        format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        Date result_date;
        long result_time = 0;

        if (null == GTMDate) {
            return GTMDate;
        } else {
            try {
                format.setTimeZone(TimeZone.getTimeZone("GMT00:00"));
                result_date = format.parse(convertString);
                result_time = result_date.getTime();
                format.setTimeZone(TimeZone.getDefault());
                return format.format(result_time);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return GTMDate;

    }
    String startTime = GTMToLocal("1970-01-01T00:00:00.45425+00:00");
    String endTime = GTMToLocal("2020-03-04T00:17:13.45425+00:00");
    private static int getGapMinutes(String startDate, String endDate) {
        long start = 0;
        long end = 0;
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            start = df.parse(startDate).getTime();
            end = df.parse(endDate).getTime();
        } catch (Exception e) {
            // TODO: handle exception

        }

        int minutes = (int) ((end - start) / (1000));

        return minutes;
    }
}
