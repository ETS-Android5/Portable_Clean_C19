/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.detection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Trace;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;


import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.examples.detection.Controler.CameraController;
import org.tensorflow.lite.examples.detection.Controler.Sensor;
import org.tensorflow.lite.examples.detection.Server.Koneksi_RMQ;
import org.tensorflow.lite.examples.detection.Server.MyRmq;
import org.tensorflow.lite.examples.detection.Session.SharedPrefManager;
import org.tensorflow.lite.examples.detection.View.Mysensor;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.utils.Screenshot;
import org.tensorflow.lite.examples.detection.utils.Utils;

import static org.tensorflow.lite.examples.detection.Session.SharedPrefManager.Sp_gambar;
import static org.tensorflow.lite.examples.detection.Session.SharedPrefManager.Sp_mac;
import static org.tensorflow.lite.examples.detection.Session.SharedPrefManager.Sp_suhu;

public abstract class CameraActivity extends AppCompatActivity
        implements OnImageAvailableListener,
        Camera.PreviewCallback,
        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener, MyRmq, Mysensor {
    private static final Logger LOGGER = new Logger();

    private static final int PERMISSIONS_REQUEST = 1;
    SharedPrefManager sharedPrefManager;
    Sensor sensor;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    protected int previewWidth = 0;
    protected int previewHeight = 0;
    private boolean debug = false;
    private Handler handler;
    private HandlerThread handlerThread;
    private boolean useCamera2API;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;

    private LinearLayout bottomSheetLayout;
    private LinearLayout gestureLayout;
    private BottomSheetBehavior<LinearLayout> sheetBehavior;

    protected TextView frameValueTextView, cropValueTextView, inferenceTimeTextView, valueSuhu;
    protected ImageView bottomSheetArrowImageView;
    private ImageView plusImageView, minusImageView;
    private SwitchCompat apiSwitchCompat;
    private TextView threadsTextView;

    private FloatingActionButton btnSwitchCam;
    TextView Keterangan;
    private static final String KEY_USE_FACING = "use_facing";
    private Integer useFacing = null;
    private String cameraId = null;
    private Camera camera;
    private boolean hasCamera;
    private Context context;
    private int cameraId1;
    CameraController cameraController;

    protected Integer getCameraFacing() {
        return useFacing;
    }


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        LOGGER.d("onCreate " + this);
        super.onCreate(null);
        sensor=new Sensor(CameraActivity.this);
//         cameraController=new CameraController(context);

        Intent intent = getIntent();
        //useFacing = intent.getIntExtra(KEY_USE_FACING, CameraCharacteristics.LENS_FACING_FRONT);
        useFacing = intent.getIntExtra(KEY_USE_FACING, CameraCharacteristics.LENS_FACING_FRONT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sharedPrefManager = new SharedPrefManager(this);
        setContentView(R.layout.tfe_od_activity_camera);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }

//        threadsTextView = findViewById(R.id.threads);
        valueSuhu = findViewById(R.id.valuesuhu);
        Keterangan=findViewById(R.id.keterangan);
//        plusImageView = findViewById(R.id.plus);
//        minusImageView = findViewById(R.id.minus);
//        apiSwitchCompat = findViewById(R.id.api_info_switch);
//        bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
//        gestureLayout = findViewById(R.id.gesture_layout);
//        sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
//        bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);
//
        btnSwitchCam = findViewById(R.id.fab);

//        ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
//        vto.addOnGlobalLayoutListener(
//                new ViewTreeObserver.OnGlobalLayoutListener() {
//                    @Override
//                    public void onGlobalLayout() {
//                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
//                            gestureLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
//                        } else {
//                            gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                        }
//                        //                int width = bottomSheetLayout.getMeasuredWidth();
//                        int height = gestureLayout.getMeasuredHeight();
//                        sheetBehavior.setPeekHeight(height);
//                    }
//                });
//        sheetBehavior.setHideable(false);
//
//        sheetBehavior.setBottomSheetCallback(
//                new BottomSheetBehavior.BottomSheetCallback() {
//                    @Override
//                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
//                        switch (newState) {
//                            case BottomSheetBehavior.STATE_HIDDEN:
//                                break;
//                            case BottomSheetBehavior.STATE_EXPANDED: {
//                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
//                            }
//                            break;
//                            case BottomSheetBehavior.STATE_COLLAPSED: {
//                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
//                            }
//                            break;
//                            case BottomSheetBehavior.STATE_DRAGGING:
//                                break;
//                            case BottomSheetBehavior.STATE_SETTLING:
//                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
//                                break;
//                        }
//                    }
//
//                    @Override
//                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
//                    }
//                });

//        frameValueTextView = findViewById(R.id.frame_info);
//        cropValueTextView = findViewById(R.id.crop_info);
//        inferenceTimeTextView = findViewById(R.id.inference_info);
//        apiSwitchCompat.setOnCheckedChangeListener(this);
//        plusImageView.setOnClickListener(this);
//        minusImageView.setOnClickListener(this);
        getsuhu();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getsuhu();
            }
            //disini maksud 3000 nya itu adalah lama screen ini terdelay 3 detik,dalam satuan mili second
        }, 3000);

        btnSwitchCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSwitchCamClick();
            }
        });

    }


    private void getsuhu() {
        Koneksi_RMQ rmq = new Koneksi_RMQ(this);
        rmq.setupConnectionFactory();
        final Handler incomingMessageHandler = new Handler() {
            @SuppressLint("HandlerLeak")
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void handleMessage(Message msg) {
                String message = msg.getData().getString("msg");
                Log.d("RMQMessage", message);
                String s = message.toString();
                try {
                    JSONObject jsonRESULTS = new JSONObject(s);
                    String mac = jsonRESULTS.getString("mac");
                    String suhu = jsonRESULTS.getString("suhu");
                    String suhuruangan = jsonRESULTS.getString("Ruangan");
                    String suhuhasil = jsonRESULTS.getString("Hasil");
                    float suhudata=Float.parseFloat(suhuhasil);
                    if (suhudata <= 33.00){
                        Keterangan.setText("Suhu Belum Terbaca");
                        valueSuhu.setText("");
                    }else if (suhudata > 33.00) {
                        Keterangan.setText("");
                        String datasuhu=suhuhasil.substring(0,5);
                        valueSuhu.setText(datasuhu + " °C");
                        sharedPrefManager.saveSPString(Sp_mac, mac);
                        sharedPrefManager.saveSPString(Sp_suhu, suhu);
                        String gambar=sharedPrefManager.getGambar();
                        String Mac=sharedPrefManager.getMac();
                        String Suhu=sharedPrefManager.getSuhu();
                        String keterangan=sharedPrefManager.getSp_keterangan();
                        String Suhu_ruangan=suhuruangan;
                        String Suhu_hasil=suhuhasil.substring(0,5);
                        sensor.Simpan(Mac,Suhu,Suhu_ruangan,Suhu_hasil,keterangan,gambar);
                        Log.d("ruangan",Suhu_ruangan);
                        Log.d("Hasil",Suhu_hasil);
                        Log.d("suhu Asli",Suhu);

//                        Toast.makeText(CameraActivity.this, "suhu Ruangan:", Toast.LENGTH_SHORT).show();
                        Perintah_palang();

                    }else if (suhudata > 37.00) {
                        String datasuhu=suhuhasil.substring(0,5);
                        valueSuhu.setText(datasuhu + " °C");
                        Keterangan.setText("Suhu Badan Anda Tinggi");
                        sharedPrefManager.saveSPString(Sp_mac, mac);
                        sharedPrefManager.saveSPString(Sp_suhu, suhu);
                        String gambar=sharedPrefManager.getGambar();
                        String Mac=sharedPrefManager.getMac();
                        String Suhu=sharedPrefManager.getSuhu();
                        String keterangan=sharedPrefManager.getSp_keterangan();
                        String Suhu_ruangan=suhuruangan;
                        String Suhu_hasil=suhuhasil.substring(0,5);
                        sensor.Simpan(Mac,Suhu,Suhu_ruangan,Suhu_hasil,keterangan,gambar);
                        Log.d("ruangan",Suhu_ruangan);
                        Log.d("Hasil",Suhu_hasil);
                        Log.d("suhu Asli",Suhu);
                        Perintah_palang();

                    }else if (suhudata > 38.00) {
                        String datasuhu=suhuhasil.substring(0,5);
                        valueSuhu.setText(datasuhu + " °C");
                        Keterangan.setText("Suhu Badan Anda Tinggi");
                        sharedPrefManager.saveSPString(Sp_mac, mac);
                        sharedPrefManager.saveSPString(Sp_suhu, suhu);
                        String gambar=sharedPrefManager.getGambar();
                        String Mac=sharedPrefManager.getMac();
                        String Suhu=sharedPrefManager.getSuhu();
                        String keterangan=sharedPrefManager.getSp_keterangan();
                        String Suhu_ruangan=suhuruangan;
                        String Suhu_hasil=suhuhasil.substring(0,5);
                        sensor.Simpan(Mac,Suhu,Suhu_ruangan,Suhu_hasil,keterangan,gambar);
                        Log.d("ruangan",Suhu_ruangan);
                        Log.d("Hasil",Suhu_hasil);
                        Log.d("suhu Asli",Suhu);
                        Perintah_palang();

                    }else if (suhudata > 40.00) {
                        String datasuhu=suhuhasil.substring(0,5);
                        valueSuhu.setText(datasuhu + " °C");
                        Keterangan.setText("Suhu Badan Anda Tinggi");
                        sharedPrefManager.saveSPString(Sp_mac, mac);
                        sharedPrefManager.saveSPString(Sp_suhu, suhu);
                        String gambar=sharedPrefManager.getGambar();
                        String Mac=sharedPrefManager.getMac();
                        String Suhu=sharedPrefManager.getSuhu();
                        String keterangan=sharedPrefManager.getSp_keterangan();
                        String Suhu_ruangan=suhuruangan;
                        String Suhu_hasil=suhuhasil.substring(0,5);
                        sensor.Simpan(Mac,Suhu,Suhu_ruangan,Suhu_hasil,keterangan,gambar);
                        Log.d("ruangan",Suhu_ruangan);
                        Log.d("Hasil",Suhu_hasil);
                        Log.d("suhu Asli",Suhu);
                        Perintah_palang();

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String[] tokens = s.split("");
//                valueSuhu.setText("");
            }
        };

        Thread subscribeThread = new Thread();
        String data = "deteksimasker";
        rmq.subscribe(incomingMessageHandler, subscribeThread, data, data);

    }

    public  void Perintah_palang(){
        Koneksi_RMQ rmq = new Koneksi_RMQ(this);
        String Sn="fc:f5:c4:97:07:ec";
        String Queue="mqtt-subscription-"+Sn+"qos0";
//        String Pesan="1";

        String masker=sharedPrefManager.getSp_keterangan();
        String suhuhasil=sharedPrefManager.getSuhu();
        float suhudata=Float.parseFloat(suhuhasil);
        if(masker.equals("Menggunakan Masker")&&suhudata <=37.50){ //suhu normal
            rmq.setupConnectionFactory();
            rmq.publish("1",Queue);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getsuhu();
                }
                //disini maksud 3000 nya itu adalah lama screen ini terdelay 3 detik,dalam satuan mili second
            }, 3000);
        }else if (masker.equals("Menggunakan Masker")&&suhudata >37.50){ // suhu tinggi
            rmq.setupConnectionFactory();
            rmq.publish("0",Queue);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getsuhu();
                }
                //disini maksud 3000 nya itu adalah lama screen ini terdelay 3 detik,dalam satuan mili second
            }, 3000);
        }else if (masker.equals("Tidak Menggunakan Masker")&&suhudata <=37.50) { //suhu normal
            rmq.setupConnectionFactory();
            rmq.publish("0",Queue);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getsuhu();
                }
                //disini maksud 3000 nya itu adalah lama screen ini terdelay 3 detik,dalam satuan mili second
            }, 3000);
        }else if (masker.equals("Tidak Menggunakan Masker")||suhudata >37.50){ // suhu tinggi
            rmq.setupConnectionFactory();
            rmq.publish("0",Queue);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getsuhu();
                }
                //disini maksud 3000 nya itu adalah lama screen ini terdelay 3 detik,dalam satuan mili second
            }, 3000);
        }else{
            Toast.makeText(context, "Kondisi Belum Di temukan", Toast.LENGTH_SHORT).show();
        }


    }


    private void onSwitchCamClick() {

        switchCamera();

    }

    public void switchCamera() {
        Intent intent = getIntent();
        if (useFacing == CameraCharacteristics.LENS_FACING_FRONT) {
            useFacing = CameraCharacteristics.LENS_FACING_BACK;
        } else {
            useFacing = CameraCharacteristics.LENS_FACING_FRONT;
        }
        intent.putExtra(KEY_USE_FACING, useFacing);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        restartWith(intent);

    }

    private void restartWith(Intent intent) {
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    protected int getLuminanceStride() {
        return yRowStride;
    }

    protected byte[] getLuminance() {
        return yuvBytes[0];
    }

    /**
     * Callback for android.hardware.Camera API
     */
    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        if (isProcessingFrame) {
            LOGGER.w("Dropping frame!");
            return;
        }

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size previewSize = parameters.getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                int rotation = 90;
                if (useFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                    rotation = 270;
                }
                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), rotation);
            }
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            return;
        }

        isProcessingFrame = true;
        yuvBytes[0] = bytes;
        yRowStride = previewWidth;

        imageConverter =
                new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
                    }
                };

        postInferenceCallback =
                new Runnable() {
                    @Override
                    public void run() {
                        camera.addCallbackBuffer(bytes);
                        isProcessingFrame = false;
                    }
                };
        processImage();
    }

    /**
     * Callback for Camera2 API
     */
    @Override
    public void onImageAvailable(final ImageReader reader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (isProcessingFrame) {
                image.close();
                return;
            }
            isProcessingFrame = true;
            Trace.beginSection("imageAvailable");
            final Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                    new Runnable() {
                        @Override
                        public void run() {
                            ImageUtils.convertYUV420ToARGB8888(
                                    yuvBytes[0],
                                    yuvBytes[1],
                                    yuvBytes[2],
                                    previewWidth,
                                    previewHeight,
                                    yRowStride,
                                    uvRowStride,
                                    uvPixelStride,
                                    rgbBytes);
                        }
                    };

            postInferenceCallback =
                    new Runnable() {
                        @Override
                        public void run() {
                            image.close();
                            isProcessingFrame = false;
                        }
                    };

            processImage();
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            Trace.endSection();
            return;
        }
        Trace.endSection();
    }

    @Override
    public synchronized void onStart() {
        LOGGER.d("onStart " + this);
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }

        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        LOGGER.d("onDestroy " + this);
        super.onDestroy();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (allPermissionsGranted(grantResults)) {
                setFragment();
            } else {
                requestPermission();
            }
        }
    }

    private static boolean allPermissionsGranted(final int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(
                        CameraActivity.this,
                        "Camera permission is required for this demo",
                        Toast.LENGTH_LONG)
                        .show();
            }
            requestPermissions(new String[]{PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
        }
    }

    // Returns true if the device supports the required hardware level, or better.
    private boolean isHardwareLevelSupported(
            CameraCharacteristics characteristics, int requiredLevel) {
        int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            return requiredLevel == deviceLevel;
        }
        // deviceLevel is not LEGACY, can use numerical sort
        return requiredLevel <= deviceLevel;
    }

    private String chooseCamera() {

        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {


            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);


                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null) {
                    continue;
                }

                // Fallback to camera1 API for internal cameras that don't have full support.
                // This should help with legacy situations where using the camera2 API causes
                // distorted or otherwise broken previews.
                //final int facing =
                //(facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
//        if (!facing.equals(useFacing)) {
//          continue;
//        }

                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (useFacing != null &&
                        facing != null &&
                        !facing.equals(useFacing)
                ) {
                    continue;
                }


                useCamera2API = (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                        || isHardwareLevelSupported(
                        characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);


                LOGGER.i("Camera API lv2?: %s", useCamera2API);
                return cameraId;
            }
        } catch (CameraAccessException e) {
            LOGGER.e(e, "Not allowed to access camera");
        }

        return null;
    }


    protected void setFragment() {
        this.cameraId = chooseCamera();
        Fragment fragment;
        if (useCamera2API) {
            CameraConnectionFragment camera2Fragment =
                    CameraConnectionFragment.newInstance(
                            new CameraConnectionFragment.ConnectionCallback() {
                                @Override
                                public void onPreviewSizeChosen(final Size size, final int rotation) {
                                    previewHeight = size.getHeight();
                                    previewWidth = size.getWidth();
                                    CameraActivity.this.onPreviewSizeChosen(size, rotation);
                                }
                            },
                            this,
                            getLayoutId(),
                            getDesiredPreviewFrameSize());

            camera2Fragment.setCamera(cameraId);
            fragment = camera2Fragment;

        } else {

            int facing = (useFacing == CameraCharacteristics.LENS_FACING_BACK) ?
                    Camera.CameraInfo.CAMERA_FACING_BACK :
                    Camera.CameraInfo.CAMERA_FACING_FRONT;
            LegacyCameraConnectionFragment frag = new LegacyCameraConnectionFragment(this,
                    getLayoutId(),
                    getDesiredPreviewFrameSize(), facing);
            fragment = frag;

        }
        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    public boolean isDebug() {
        return debug;
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setUseNNAPI(isChecked);
        if (isChecked) apiSwitchCompat.setText("NNAPI");
        else apiSwitchCompat.setText("TFLITE");
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.plus) {
            String threads = threadsTextView.getText().toString().trim();
            int numThreads = Integer.parseInt(threads);
            if (numThreads >= 9) return;
            numThreads++;
            threadsTextView.setText(String.valueOf(numThreads));
            setNumThreads(numThreads);
        } else if (v.getId() == R.id.minus) {
            String threads = threadsTextView.getText().toString().trim();
            int numThreads = Integer.parseInt(threads);
            if (numThreads == 1) {
                return;
            }
            numThreads--;
            threadsTextView.setText(String.valueOf(numThreads));
            setNumThreads(numThreads);
        }
    }

    protected void showFrameInfo(String frameInfo) {
//        frameValueTextView.setText(frameInfo);
    }

    protected void showCropInfo(String cropInfo) {
//        cropValueTextView.setText(cropInfo);
    }

    protected void showInference(String inferenceTime) {
//        inferenceTimeTextView.setText(inferenceTime);
    }

    protected abstract void processImage();

    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

    protected abstract int getLayoutId();

    protected abstract Size getDesiredPreviewFrameSize();

    protected abstract void setNumThreads(int numThreads);

    protected abstract void setUseNNAPI(boolean isChecked);


}
