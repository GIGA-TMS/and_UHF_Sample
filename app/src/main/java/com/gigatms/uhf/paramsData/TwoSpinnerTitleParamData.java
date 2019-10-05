package com.gigatms.uhf.paramsData;

import static com.gigatms.uhf.paramsData.ParamData.ViewType.TWO_SPINNER_WITH_TITLE;

public class TwoSpinnerTitleParamData<E extends Enum<E>, T extends Enum<T>> extends ParamData {
    String mTitle;
    private Enum<E>[] mFirstEnums;
    private Enum<T>[] mSecondEnums;
    private Enum mFirstSelected;
    private Enum mSecondSelected;
    private com.gigatms.uhf.paramsData.TwoSpinnerParamData.OnFirstItemSelected mOnFirstItemSelected;

    public interface OnFirstItemSelected {
        void onFirstItemSelected(Enum selected);
    }

    public TwoSpinnerTitleParamData(Enum<E>[] firstEnums, Enum<T>[] secondEnums) {
        super(TWO_SPINNER_WITH_TITLE);
        mFirstEnums = firstEnums;
        mSecondEnums = secondEnums;
        mTitle = (firstEnums.getClass().getSimpleName() + secondEnums.getClass().getSimpleName())
                .replace('[', ' ').replace(']', ' ');
    }


    public void setOnFirstItemSelected(com.gigatms.uhf.paramsData.TwoSpinnerParamData.OnFirstItemSelected onFirstItemSelected) {
        mOnFirstItemSelected = onFirstItemSelected;
    }

    public Enum<E>[] getFirstEnums() {
        return mFirstEnums;
    }

    public Enum<T>[] getSecondEnums() {
        return mSecondEnums;
    }

    public void setSecondEnums(Enum<T>[] secondEnums) {
        mSecondEnums = secondEnums;
    }

    public Enum getFirstSelected() {
        return mFirstSelected;
    }

    public Enum getSecondSelected() {
        return mSecondSelected;
    }

    public void setFirstSelected(Enum firstSelected) {
        mFirstSelected = firstSelected;
        if (mOnFirstItemSelected != null) {
            mOnFirstItemSelected.onFirstItemSelected(mFirstSelected);
        }
    }

    public void setSecondSelected(Enum secondSelected) {
        mSecondSelected = secondSelected;
    }

    public String getTitle() {
        return mTitle;
    }
}
