package com.gigatms.uhf;

import androidx.annotation.Nullable;
import android.view.View;

import com.gigatms.uhf.paramsData.ParamData;

import java.util.Arrays;

public class GeneralCommandItem {
    private String mTitle;
    private String mRightBtnName;
    private String mLeftBtnName;
    private boolean mHasRightBtn;
    private boolean mHasLeftBtn;
    private View.OnClickListener mRightOnClickListener;
    private View.OnClickListener mWriteOnClickListener;
    private int position;

    private ParamData[] mViewDataArray;

    public GeneralCommandItem(String title, ParamData... viewData) {
        mTitle = title;
        mRightBtnName = "Write";
        mLeftBtnName = "Read";
        mHasLeftBtn = true;
        mHasRightBtn = true;

        mViewDataArray = viewData;
    }

    public GeneralCommandItem(String title, String leftBtnName, String rightBtnName, @Nullable ParamData... viewData) {
        mTitle = title;
        if (leftBtnName != null) {
            mLeftBtnName = leftBtnName;
            mHasLeftBtn = true;
        }

        if (rightBtnName != null) {
            mRightBtnName = rightBtnName;
            mHasRightBtn = true;
        }
        mViewDataArray = viewData;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getRightBtnName() {
        return mRightBtnName;
    }

    public String getLeftBtnName() {
        return mLeftBtnName;
    }

    public boolean hasRightBtn() {
        return mHasRightBtn;
    }

    public boolean hasLeftBtn() {
        return mHasLeftBtn;
    }

    public View.OnClickListener getRightOnClickListener() {
        return mRightOnClickListener;
    }

    public void setRightOnClickListener(View.OnClickListener rightOnClickListener) {
        mRightOnClickListener = rightOnClickListener;
    }

    public View.OnClickListener getLeftOnClickListener() {
        return mWriteOnClickListener;
    }

    public void setLeftOnClickListener(View.OnClickListener writeOnClickListener) {
        mWriteOnClickListener = writeOnClickListener;
    }

    public ParamData[] getViewDataArray() {
        return Arrays.copyOf(mViewDataArray, mViewDataArray.length);
    }
}
