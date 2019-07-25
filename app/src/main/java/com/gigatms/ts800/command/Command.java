package com.gigatms.ts800.command;

import android.view.View;

import static com.gigatms.ts800.CommandRecyclerViewAdapter.BASE;

public class Command {
    private static final String TAG = Command.class.getSimpleName();
    private String mTitle;
    private String mRightBtnName;
    private String mLeftBtnName;
    private boolean mHasRightBtn;
    private boolean mHasLeftBtn;
    private View.OnClickListener mRightOnClickListener;
    private View.OnClickListener mWriteOnClickListener;

    public Command(String title) {
        mTitle = title;
        mRightBtnName = "Write";
        mLeftBtnName = "Read";
        mHasLeftBtn = true;
        mHasRightBtn = true;
    }

    public Command(String title, String leftBtnName, String rightBtnName) {
        mTitle = title;
        if (leftBtnName != null) {
            mLeftBtnName = leftBtnName;
            mHasLeftBtn = true;
        }

        if (rightBtnName != null) {
            mRightBtnName = rightBtnName;
            mHasRightBtn = true;
        }

    }

    public int getViewType() {
        return BASE;
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
}
