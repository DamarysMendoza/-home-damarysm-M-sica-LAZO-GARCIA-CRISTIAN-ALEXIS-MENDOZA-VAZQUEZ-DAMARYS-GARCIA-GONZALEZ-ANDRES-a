package com.example.proyectofinal;

import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.rajawali3d.Object3D;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.loader.ParsingException;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.renderer.RajawaliRenderer;
import org.rajawali3d.surface.IRajawaliSurface;
import org.rajawali3d.surface.RajawaliSurfaceView;

import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.SurfaceView;
import android.widget.Toast;


import androidx.core.app.ActivityCompat;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.HybridBinarizer;


public class MainActivity extends Activity implements  OnTouchListener, CvCameraViewListener2 {

    // Cualquier pinche VALOR!!!
    private static final int REQUEST_ID_CAMERA_PERMISSION = 100;

    private static final String  TAG = "Advertencia: ";

    private Mat mRgba;
    private Mat mRgbaF;
    private Mat mRgbaT;

    private CameraBridgeViewBase mOpenCvCameraView;

    public ResultPoint[] points;

    int evaluar = 1;

    public boolean mostrarOBJ = false;
    public boolean reloadAcitivity = false;

    Renderer renderer;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        //Mas que un error, esto marca una advertencia
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Esto manda a llamar a una función que le pide confirmación al usuario
        // acerca del acceso a los archivos por parte de la aplicación
        // No es necesario en versiones anteriores a la 6.0, pero se incluye para
        // que sea compatible con todo dispositivo viejo o nuevo...

        // Recomiendo llamara este permiso antes de SetContent, si se pone despues "truena"
        askPermissionOnly();

        setContentView(R.layout.color_blob_detection_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        //Hacer full screen la pantalla
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        final RajawaliSurfaceView surface = new RajawaliSurfaceView(this);
        surface.setFrameRate(60.0);
        surface.setRenderMode(IRajawaliSurface.RENDERMODE_WHEN_DIRTY);

        // Add mSurface to your root view
        addContentView(surface, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT));

        renderer = new Renderer(this, mostrarOBJ);
        surface.setSurfaceRenderer(renderer);

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        // Rotate mRgba 90 degrees
        Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, mRgba, 1 );

        Bitmap bMapR = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bMapR);
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        //En caso de que no haya cargado de forma correcta la aplicacion
        if(evaluar == 0){
            return mRgba;
        }

        try {

            String correctQR = decodeQRCode(bMapR);

            //Si detecto un QR, dibuja el modelo 3D
            if(correctQR.equals("1")){

                mostrarOBJ = true;
                renderer.setmostrarOBJ(mostrarOBJ);
                reloadAcitivity = true;

            } else {

                mostrarOBJ = false;
                renderer.setmostrarOBJ(mostrarOBJ);

                if (reloadAcitivity == true) {
                    //startActivity(new Intent(this,MainActivity.class));
                    //this.finish();
                    reloadAcitivity = false;
                }
            }


        } catch (Exception e) {
            Log.d("Ayuda","Error1" + e.getMessage() + e.getCause() + e.getLocalizedMessage());
        }

        return mRgba;
    }

    // Funcionaes de Permisos
    private void askPermissionOnly() {
        this.askPermission(REQUEST_ID_CAMERA_PERMISSION, Manifest.permission.CAMERA);
    }

    // With Android Level >= 23, you have to ask the user
    // for permission with device (For example read/write data on the device).
    private boolean askPermission(int requestId, String permissionName) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            // Check if we have permission
            int permission = ActivityCompat.checkSelfPermission(this, permissionName);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // If don't have permission so prompt the user.
                this.requestPermissions(
                        new String[]{permissionName},
                        requestId
                );
                return false;
            }
        }
        return true;
    }

    public String decodeQRCode(Bitmap bMap) {

        int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());
        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        try {

            Result result = new MultiFormatReader().decode(bitmap);
            points = result.getResultPoints();

            return "1";

        } catch (Exception e) {

            return "0";
        }
    }

    // When you have the request results
    @Override
    public void onRequestPermissionsResult(int requestCode,  String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //
        // Note: If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0) {
            switch (requestCode) {
                case REQUEST_ID_CAMERA_PERMISSION: {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(getApplicationContext(), "Permission Lectura Concedido!", Toast.LENGTH_SHORT).show();

                    }
                }
            }
        } else {
            Toast.makeText(getApplicationContext(), "Permission Cancelled!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }
}

class Renderer extends RajawaliRenderer {

    public Context context;
    private Object3D obj;
    boolean mostrarOBJ;

    public Renderer(Context context, boolean mostrarOBJ) {
        super(context);
        this.context = context;
        this.mostrarOBJ = mostrarOBJ;
        setFrameRate(60);
    }

    public void setmostrarOBJ(boolean mostrarOBJ) {
        this.mostrarOBJ = mostrarOBJ;
    }

    @Override
    protected void initScene() {

        DirectionalLight key = new DirectionalLight(-3,-4,-5);
        key.setPower(2);
        getCurrentScene().addLight(key);

        LoaderOBJ loader = new LoaderOBJ(getContext().getResources(), getTextureManager(), R.raw.jaguar_obj);

        try {

            loader.parse();
            obj = loader.getParsedObject();
            obj.setScale(5.5f);
            getCurrentScene().addChild(obj);
            //getCurrentCamera().setZ(3.2f);
            //getCurrentCamera().setY(10.5f);
            //obj.rotate(Vector3.Axis.Y, 30.0);

        } catch (ParsingException e) {
            e.printStackTrace();
        }

        getCurrentCamera().setPosition(6,20,6);
        getCurrentCamera().setLookAt(obj.getPosition());
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }

    @Override
    public void onRender(final long elapsedTime, final double deltaTime) {
        if (mostrarOBJ == true) {
            super.onRender(elapsedTime, deltaTime);
            obj.rotate(Vector3.Axis.Y, 1.0);
        }


    }


}