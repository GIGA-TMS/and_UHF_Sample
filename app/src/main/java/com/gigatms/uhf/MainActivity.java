package com.gigatms.uhf;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.Guideline;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.gigatms.BaseDevice;
import com.gigatms.tools.GLog;

import io.fabric.sdk.android.Fabric;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;
import static com.gigatms.uhf.BaseScanFragment.DEBUG;

public class MainActivity extends AppCompatActivity implements DebugFragment.DebugFragmentListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final float HIDE_LOG_RATIO = 0f;
    public static final float SHOW_LOG_RATIO = 0.25f;
    private final int REQUEST_COARSE_LOCATION = 99;

    private Button mBtnDebug;
    private boolean mDebugMode = false;
    private boolean mFirstClick = true;
    private int mClickDebugCount;
    long mTime, mTemp = 0;
    private TextView mTvLog;
    private TextView mTvInformation;
    private ScrollView mScrollView;
    private SharedPreferences mDebugSharedPreferences;
    private Guideline mGuideline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new DeviceScanFragment())
                .commit();
        getAppVersion();
        findViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestNeededPermissions();
        setDebugView();
        DebugFragment.setDebugListener(this);
    }

    private void getAppVersion() {
        try {
            PackageInfo packageInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = packageInfo.versionName;
            int verCode = packageInfo.versionCode;
            TextView txtVersion = findViewById(R.id.tv_version);
            version = "APP v" + version + " " + verCode
                    + ", SDK v" + BaseDevice.VERSION;
            txtVersion.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        DebugFragment.setDebugListener(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.fragment_device_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_item_hide_log) {
            if (item.getTitle().equals(getString(R.string.hide_log_view))) {
                mGuideline.setGuidelinePercent(HIDE_LOG_RATIO);
                item.setTitle(getString(R.string.show_log_view));
            } else {
                mGuideline.setGuidelinePercent(SHOW_LOG_RATIO);
                item.setTitle(getString(R.string.hide_log_view));
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void requestNeededPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{ACCESS_COARSE_LOCATION, WRITE_EXTERNAL_STORAGE}, REQUEST_COARSE_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_COARSE_LOCATION) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void findViews() {
        mBtnDebug = findViewById(R.id.btn_debug);
        mScrollView = findViewById(R.id.scrollView);
        mTvLog = findViewById(R.id.tv_log);
        mTvInformation = findViewById(R.id.tv_information);
        mGuideline = findViewById(R.id.guideline);
    }

    private void setDebugView() {
        mDebugSharedPreferences = getSharedPreferences(getString(R.string.debug_mode), Context.MODE_PRIVATE);
        setDebugButtonListener();
        mDebugSharedPreferences = this.getSharedPreferences(getString(R.string.debug_mode), Context.MODE_PRIVATE);
        mDebugMode = mDebugSharedPreferences.getBoolean(DEBUG, false);
        switchDebugView(mDebugMode);
        setTbLogView();
        mTvInformation.setOnLongClickListener(v -> false);
    }

    private void setTbLogView() {
        final GestureDetectorCompat gestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                mTvLog.setText("");
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

        mTvLog.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });
    }

    private void setDebugButtonListener() {
        mBtnDebug.setOnClickListener(v -> {
            //Debug mode
            mTime = System.currentTimeMillis();
            if (mFirstClick) {
                mClickDebugCount++;
                mTemp = System.currentTimeMillis();
                mFirstClick = false;
            } else {
                Log.d(TAG, "onClick: mTime - mTemp: " + (mTime - mTemp));
                if (mTime - mTemp < 2000) {
                    mTemp = mTime;
                    mClickDebugCount++;
                    if (mClickDebugCount == 10) {
                        mDebugMode = !mDebugMode;
                        mDebugSharedPreferences.edit().putBoolean(DEBUG, mDebugMode).apply();
                        switchDebugView(mDebugMode);
                        clearDebugCountData();

                    }
                } else {
                    clearDebugCountData();
                }
            }
            Log.d(TAG, "onClick: " +
                    "\nmTime:" + mTime
                    + "\nmTemp:" + mTemp
                    + "\nclick debug count: " + mClickDebugCount
                    + "\nfirst click: " + mFirstClick);
        });
    }

    void switchDebugView(boolean debugMode) {
        mTvLog.setText("");
        if (debugMode) {
            mTvInformation.setText("");
            mTvInformation.setBackgroundColor(getResources().getColor(R.color.device_operation_background));
            mTvInformation.setVisibility(View.VISIBLE);
        } else {
            mTvInformation.setVisibility(View.GONE);
        }
    }

    private void clearDebugCountData() {
        mFirstClick = true;
        mClickDebugCount = 0;
        mTime = 0;
        mTemp = 0;
    }

    private void updateLog(@NonNull final String message) {
        runOnUiThread(() -> {
            if (mTvLog.getText().toString().length() > 30000) {
                mTvLog.setText("");
            }
            mTvLog.append(message + "\n");
            mScrollView.fullScroll(View.FOCUS_DOWN);
        });
    }

    @Override
    public void onUpdateDebugLog(final String message) {
        if (mDebugMode) {
            updateLog(message);
        }
    }

    @Override
    public void onUpdateLog(String message) {
        if (!mDebugMode) {
            updateLog(message);
        }
    }

    @Override
    public void onUpdateDebugInformation(final String message, final int resColor) {
        if (mDebugMode) {
            GLog.d(TAG, message);
            runOnUiThread(() -> {
                mTvInformation.setText(message);
                mTvInformation.setBackgroundColor(getResources().getColor(resColor));
            });
        }
    }
}
