package com.gigatms.uhf.paramsData;

import java.util.List;

import static com.gigatms.uhf.paramsData.ParamData.ViewType.SPINNER;

public class InterchangeableParamData<E extends Enum<E>> extends ParamData {
    private String mTitle;
    private E[] mDataArray;
    private Enum<E> mSelected;
    private List<ParamData> mParamData;
    private OnFirstItemSelectedListener mOnFirstItemSelectedListener;

    public interface OnFirstItemSelectedListener {
        void onFirstItemSelected(Enum selected);
    }

    public InterchangeableParamData(String title, E[] dataArray, List<ParamData> paramData) {
        super(ViewType.INTERCHANGEABLE_VIEW);
        mDataArray = dataArray;
        mSelected = mDataArray[0];
        mParamData = paramData;
        mTitle = title;
    }

    public InterchangeableParamData(String title, Class<E> enumData, List<ParamData> paramData) {
        super(SPINNER);
        mDataArray = enumData.getEnumConstants();
        assert mDataArray != null;
        mSelected = mDataArray[0];
        mParamData = paramData;
        mTitle = title;
    }

    public void setSelected(Enum<E> selected) {
        mSelected = selected;
        if (mOnFirstItemSelectedListener != null) {
            mOnFirstItemSelectedListener.onFirstItemSelected(selected);
        }
    }

    public String getTitle() {
        return mTitle;
    }

    public Enum<E> getSelected() {
        return mSelected;
    }

    public E[] getDataArray() {
        return mDataArray;
    }

    public List<ParamData> getParamData() {
        return mParamData;
    }

    public void setOnFirstItemSelectedListener(OnFirstItemSelectedListener onFirstItemSelectedListener) {
        mOnFirstItemSelectedListener = onFirstItemSelectedListener;
    }
}