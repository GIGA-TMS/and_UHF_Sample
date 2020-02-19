package com.gigatms.uhf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import com.crashlytics.android.Crashlytics;
import com.gigatms.BaseDevice;
import com.gigatms.tools.GLog;

import io.fabric.sdk.android.Fabric;

import static com.gigatms.uhf.BaseScanFragment.DEBUG;

public class MainActivity extends AppCompatActivity implements DebugFragment.DebugFragmentListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Button mBtnDebug;
    private boolean mDebugMode = false;
    private boolean mFirstClick = true;
    private int mClickDebugCount;
    long mTime, mTemp = 0;
    private TextView mTvLog;
    private TextView mTvInformation;
    private ScrollView mScrollView;
    private SharedPreferences mDebugSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DeviceScanFragment())
                    .commit();
        }
        getAppVersion();
        findViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        setDebugView();
    }

    private void getAppVersion() {
        try {
            PackageInfo packageInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = packageInfo.versionName;
            int verCode = packageInfo.versionCode;
            TextView txtVersion = findViewById(R.id.tv_version);
            version = "APP v" + version + "-b" + verCode
                    + ", SDK v" + BaseDevice.VERSION;
            txtVersion.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: ");
        DebugFragment.setDebugListener(null);
    }

    private void findViews() {
        mBtnDebug = findViewById(R.id.btn_debug);
        mScrollView = findViewById(R.id.scrollView);
        mTvLog = findViewById(R.id.tv_log);
        mTvInformation = findViewById(R.id.tv_information);
    }

    private void setDebugView() {
        mDebugSharedPreferences = getSharedPreferences(getString(R.string.debug_mode), Context.MODE_PRIVATE);
        setDebugButtonListener();
        mDebugSharedPreferences = this.getSharedPreferences(getString(R.string.debug_mode), Context.MODE_PRIVATE);
        mDebugMode = mDebugSharedPreferences.getBoolean(DEBUG, false);
        switchLogView(mDebugMode);
        setTbLogView();
        mTvInformation.setOnLongClickListener(v -> false);
        DebugFragment.setDebugListener(this);
    }

    @SuppressLint("ClickableViewAccessibility")
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
            mTime = System.currentTimeMillis();
            if (mFirstClick) {
                mClickDebugCount++;
                mTemp = System.currentTimeMillis();
                mFirstClick = false;
            } else {
                if (mTime - mTemp < 2000) {
                    mTemp = mTime;
                    mClickDebugCount++;
                    if (mClickDebugCount == 10) {
                        mDebugMode = !mDebugMode;
                        mDebugSharedPreferences.edit().putBoolean(DEBUG, mDebugMode).apply();
                        switchLogView(mDebugMode);
                        clearDebugCountData();

                    }
                } else {
                    clearDebugCountData();
                }
            }
        });
    }

    void switchLogView(boolean debugMode) {
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
