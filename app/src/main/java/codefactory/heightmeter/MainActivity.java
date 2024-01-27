package codefactory.heightmeter;

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
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.preference.PreferenceManager;
import static android.content.ContentValues.TAG;
import com.codefactory.heightmeter.R;


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

    private final AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.F);
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

        buttonEnterDist = findViewById(R.id.enterDistance);
        editDistance = findViewById(R.id.editDistance);

        buttonEnterLensH = findViewById(R.id.enterLensHeight);
        editLensH = findViewById(R.id.editLensHeight);

        buttonEnterDist.setOnClickListener(
            v -> {
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

                SharedPreferences preference1 = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                if (!preference1.contains("heightStored")) {
                    editLensH.requestFocus();
                } else {
                    hideSoftKeyboard(MainActivity.this);
                    editDistance.clearFocus();
                }
            }
        );

        distHint = findViewById(R.id.distHint);
        buttonMeasureDist = findViewById(R.id.measureDistance);
        buttonMeasureDist.setOnClickListener(
            v -> {
                v.startAnimation(buttonClick);
                if(!(isEmpty(editLensH)) && (lensHeight != 0)) {
                    measureDist = true;
                    distHint.setText(getResources().getString(R.string.aimAtBase));
                }
                else {
                    editLensH.requestFocus();
                    openSoftKeyboard(editLensH);
                }
            }
        );

        buttonFullscreen = findViewById(R.id.buttonFullscreen);
        buttonFullscreen.setOnClickListener(
            v -> {
                v.startAnimation(buttonClick);

                measureDist = false;
                if (distance == 0) {
                    return;
                }

                //get the value from distance variable and show it in editText editDistance
                float floatDist = (float) (Math.round(distance * 10.0) * 0.1);
                String setDistance = Float.toString(floatDist);
                editDistance.setText(setDistance, TextView.BufferType.EDITABLE);

                if (isEmpty(editDistance) || isEmpty(editLensH)) {
                    return;
                }

                distHint.setText(getResources().getString(R.string.aimAtTop));
                SharedPreferences preference12 = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                if (preference12.contains("heightStored")) {
                    return;
                }

                editLensH.requestFocus();
                openSoftKeyboard(editLensH);
            }
        );

        buttonEnterLensH.setOnClickListener(
            view -> {
                if (!((editLensH.getText().toString().equals(null)) || editLensH.getText().toString().equals(""))) {
                    lensHeight = Float.parseFloat(editLensH.getText().toString());
                }
                else {
                    String height = Float.toString(lensHeight);
                    editLensH.setText(height, TextView.BufferType.EDITABLE);
                }

                SharedPreferences preference13 = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                SharedPreferences.Editor editor = preference13.edit();
                editor.putFloat("heightStored", lensHeight); // value to store
                editor.apply();
                heightStored = true;

                view.startAnimation(buttonClick);
                hideSoftKeyboard(MainActivity.this);
                editLensH.clearFocus();

                if (measureDist || isEmpty(editDistance) || isEmpty(editLensH)) {
                    return;
                }
                distHint.setText(getResources().getString(R.string.aimAtTop));
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
        if (mCamera == null) {
            return;
        }
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
            FrameLayout preview = findViewById(R.id.camera_preview);
            preview.removeView(mPreview);
            mPreview = null;
            mSensorManager.unregisterListener(this);
        } catch (Exception e){
            Log.d(TAG, "onPause() error");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        //If type is accelerometer only assign values to global property mGravity
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values;
        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
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

        TextView resultView = findViewById(R.id.height);
        TextView textView = findViewById(R.id.heightText);

        Context context = this;
        String strHeight = context.getString(R.string.strHeight);
        String strDist = context.getString(R.string.strDist);
        String min = context.getString(R.string.min);
        String max = context.getString(R.string.max);

        if (mGravity == null || mGeomagnetic == null) {
            return;
        }

        float[] R = new float[9];
        float[] I = new float[9];

        boolean gotRotationMatrix = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
        if (!gotRotationMatrix) {
            return;
        }

        float[] orientation = new float[3];
        SensorManager.getOrientation(R, orientation);

        inclineGravity = mGravity.clone();
        float norm_Of_g = (float) Math.sqrt(inclineGravity[0] * inclineGravity[0] + inclineGravity[1] * inclineGravity[1] + inclineGravity[2] * inclineGravity[2]);

        // Normalize the accelerometer vector
        inclineGravity[2] = (inclineGravity[2] / norm_Of_g);

        float angle = (float) Math.toDegrees(Math.acos(inclineGravity[2]));
        angle = angle - 90.0f;
        angle = (float) Math.toRadians(angle);

        if (measureDist) {
            distance = lensHeight / (-(float) (Math.tan(angle)));
            float floatDist = (float) (Math.round(distance * 10.0) * 0.1);

            textView.setText(strDist);

            String displayDist = Float.toString(floatDist);
            resultView.setText(displayDist);
        }
        else {
            float height;
            height = (float) (Math.tan(angle) * distance) + lensHeight;
            height = (float) (Math.round(height * 10.0) * 0.1);

            if (height > 999999.9f) {
                maxFlag = true;
            }
            else if (height < -99999.9f) {
                minFlag = true;
            }

            textView.setText(strHeight);
            if (minFlag) {
                resultView.setText(min);
            }
            else if (maxFlag) {
                resultView.setText(max);
            }
            else {
                String displayHeight = Float.toString(height);
                resultView.setText(displayHeight);
            }
        }
    }
}

