package com.gigatms.ts800;

import android.support.v4.app.Fragment;

import com.gigatms.tools.GLog;


public class DebugFragment extends Fragment {
    protected static DebugFragmentListener mDebugFragmentListener;

    public interface DebugFragmentListener {
        void onUpdateDebugLog(String message);
        void onUpdateLog(String message);
        void onUpdateDebugInformation(String message, int resColor);
    }

    public static void setDebugListener(DebugFragmentListener debugFragmentListener) {
        mDebugFragmentListener = debugFragmentListener;
    }

    protected void onUpdateLog(String tag,String message) {
        GLog.v(tag,message);
        if (mDebugFragmentListener != null) {
            mDebugFragmentListener.onUpdateLog(message);
        }
    }

    protected void onUpdateDebugLog(String tag,String message) {
        GLog.v(tag,message);
        if (mDebugFragmentListener != null) {
            mDebugFragmentListener.onUpdateDebugLog(message);
        }
    }
}
