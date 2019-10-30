package com.gigatms.uhf.paramsData;

import static com.gigatms.uhf.paramsData.ParamData.ViewType.SPINNER_WITH_TITLE;

public class SpinnerTitleParamData<E extends Enum<E>> extends ParamData {
    private String mTitle;
    private E[] mDataArray;
    private Enum mSelected;

    public SpinnerTitleParamData(Class<E> enumData) {
        super(SPINNER_WITH_TITLE);
        mDataArray = enumData.getEnumConstants();
        mTitle = enumData.getSimpleName();
        mSelected = mDataArray[0];
    }

    public SpinnerTitleParamData(E[] enumData) {
        super(SPINNER_WITH_TITLE);
        mDataArray = enumData;
        mTitle = enumData.getClass().getSimpleName().replace('[', '\0').replace(']', '\0');
        mSelected = mDataArray[0];
    }

    public E[] getDataArray() {
        return mDataArray;
    }

    public void setSelected(Enum selected) {
        mSelected = selected;
    }

    public Enum getSelected() {
        return mSelected;
    }

    public String getTitle() {
        return mTitle;
    }
}
