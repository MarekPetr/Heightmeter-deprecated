package codefactory.heightmeter;

/**
 * Created by Petr Marek on 6/19/17.
 */

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.preference.PreferenceManager;

import static codefactory.heightmeter.R.id.editLensHeight;
import static android.content.ContentValues.TAG;


public class MainActivity extends Activity implements SensorEventListener {

    private Camera mCamera;
    private CameraPreview mPreview;

    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    float lensHeight;
    float distance;
    float[] inclineGravity = new float[3];
    float[] mGravity;
    float[] mGeomagnetic;

    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.F);
    Button buttonEnterLensH;
    EditText editLensH;
    boolean heightStored = false;

    Button buttonEnterDist;
    EditText editDistance;

    Button buttonFullscreen;

    Button buttonMeasureDist;
    boolean measureDist = false;

    TextView distHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);

        setSensors();
        initListeners();
        initializeCamera();

        buttonEnterDist = (Button)findViewById(R.id.enterDistance);
        editDistance = (EditText)findViewById(R.id.editDistance);

        buttonEnterLensH = (Button)findViewById(R.id.enterLensHeight);
        editLensH = (EditText)findViewById(editLensHeight);

        buttonEnterDist.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!((editDistance.getText().toString().equals(null)) || (editDistance.getText().toString().equals("")))) {
                        distance = Float.parseFloat(editDistance.getText().toString());
                    }
                    v.startAnimation(buttonClick);

                    if ((!measureDist) && !(isEmpty(editDistance)) && !(isEmpty(editLensH)))
                        distHint.setText(getResources().getString(R.string.aimAtTop));

                    if(isEmpty(editDistance)){
                        String setDistance = Float.toString(distance);
                        editDistance.setText(setDistance, TextView.BufferType.EDITABLE);
                    }

                    SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    if (!preference.contains("heightStored")) {
                        editLensH.requestFocus();
                    } else {
                        hideSoftKeyboard(MainActivity.this);
                        editDistance.clearFocus();
                    }
                }
            });

        distHint = (TextView) findViewById(R.id.distHint);
        buttonMeasureDist = (Button)findViewById(R.id.measureDistance);
        buttonMeasureDist.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.startAnimation(buttonClick);

                    if(!(isEmpty(editLensH)) && (lensHeight != 0)) {
                        measureDist = true;
                        distHint.setText(getResources().getString(R.string.aimAtBase));
                    }
                    else{
                        editLensH.requestFocus();
                        openSoftKeyboard(editLensH);
                    }
                }
            }
        );

        buttonFullscreen = (Button)findViewById(R.id.buttonFullscreen);
        buttonFullscreen.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.startAnimation(buttonClick);
                    measureDist = false;

                    if(distance != 0.0) {
                        //get the value from distance variable and show it in editText editDistance
                        String setDistance = Float.toString(distance);
                        editDistance.setText(setDistance, TextView.BufferType.EDITABLE);

                        if(!(isEmpty(editDistance)) && !(isEmpty(editLensH)))
                        {
                            distHint.setText(getResources().getString(R.string.aimAtTop));
                            SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                            if(!preference.contains("heightStored")) {
                                editLensH.requestFocus();
                                openSoftKeyboard(editLensH);
                            }
                        }
                    }
                }
            }
        );

        buttonEnterLensH.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!((editLensH.getText().toString().equals(null)) || (editLensH.getText().toString().equals("")))) {
                        lensHeight = Float.parseFloat(editLensH.getText().toString());
                    }
                    else {
                        String height = Float.toString(lensHeight);
                        editLensH.setText(height, TextView.BufferType.EDITABLE);
                    }

                    SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    SharedPreferences.Editor editor = preference.edit();
                    editor.putFloat("heightStored", lensHeight); // value to store
                    editor.apply();
                    heightStored = true;

                    view.startAnimation(buttonClick);
                    hideSoftKeyboard(MainActivity.this);
                    editLensH.clearFocus();

                    if ((!measureDist) && !(isEmpty(editDistance)) && !(isEmpty(editLensH)))
                        distHint.setText(getResources().getString(R.string.aimAtTop));
                }
            }
        );

        if (preference.contains("heightStored")) {
            lensHeight = preference.getFloat("heightStored", 0);
            String height = Float.toString(lensHeight);
            editLensH.setText(height, TextView.BufferType.EDITABLE);
        }
    }

    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    protected void initializeCamera(){
        // Create an instance of Camera
        mCamera = getCameraInstance();
        mCamera.setDisplayOrientation(90);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    public static void hideSoftKeyboard(Activity activity) {
        if (activity == null) return;
        if (activity.getCurrentFocus() == null) return;

        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    public void openSoftKeyboard(EditText etText){
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(etText, InputMethodManager.SHOW_IMPLICIT);
    }

    private boolean isEmpty(EditText etText) {
        return etText.getText().toString().trim().length() == 0;
    }

    public void setSensors()
    {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void initListeners()
    {
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        initListeners();
        // Get the Camera instance as the activity achieves full user focus
        if (mCamera == null) {
            initializeCamera();
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        try {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mPreview.getHolder().removeCallback(mPreview);
            releaseCamera();
            mCamera = null;
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.removeView(mPreview);
            mPreview = null;
            mSensorManager.unregisterListener(this);
        }catch (Exception e){
            Log.d(TAG, "onPause() error");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        //If type is accelerometer only assign values to global property mGravity
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            mGravity = event.values;
        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
        {
            mGeomagnetic = event.values;
            getHeight();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

    public void getHeight()
    {
        boolean minFlag = false;
        boolean maxFlag = false;

        TextView resultView = (TextView) findViewById(R.id.height);
        TextView textView = (TextView) findViewById(R.id.heightText);

        Context context = this;
        String strHeight = context.getString(R.string.strHeight);
        String strDist = context.getString(R.string.strDist);
        String min = context.getString(R.string.min);
        String max = context.getString(R.string.max);

        float height;
        if (mGravity != null && mGeomagnetic != null)
        {
            float R[] = new float[9];
            float I[] = new float[9];

            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);

            if (success)
            {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                inclineGravity = mGravity.clone();
                float norm_Of_g = (float) Math.sqrt(inclineGravity[0] * inclineGravity[0] + inclineGravity[1] * inclineGravity[1] + inclineGravity[2] * inclineGravity[2]);

                // Normalize the accelerometer vector
                inclineGravity[2] = (inclineGravity[2] / norm_Of_g);

                float angle = (float) Math.toDegrees(Math.acos(inclineGravity[2]));
                angle = angle - 90.0f;
                angle = (float) Math.toRadians(angle);

                if(measureDist) {
                    distance = lensHeight / (-(float) (Math.tan(angle)));
                    float floatDist = (float) (Math.round(distance * 10.0) * 0.1);

                    textView.setText(strDist);

                    String displayDist = Float.toString(floatDist);
                    resultView.setText(displayDist);
                }
                else {
                    height = (float) (Math.tan(angle) * distance) + lensHeight;
                    height = (float) (Math.round(height * 10.0) * 0.1);

                    if (height > 999999.9f)
                        maxFlag = true;
                    else if (height < -99999.9f)
                        minFlag = true;

                    textView.setText(strHeight);
                    if (minFlag)
                        resultView.setText(min);
                    else if (maxFlag)
                        resultView.setText(max);
                    else {
                        String displayHeight = Float.toString(height);
                        resultView.setText(displayHeight);
                    }
                }
            }
        }
    }
}

