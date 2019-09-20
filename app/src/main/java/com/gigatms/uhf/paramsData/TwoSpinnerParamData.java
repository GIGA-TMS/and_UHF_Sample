package com.gigatms.uhf.paramsData;


import static com.gigatms.uhf.paramsData.ParamData.ViewType.TWO_SPINNER;

public class TwoSpinnerParamData<E extends Enum<E>, T extends Enum<T>> extends ParamData {
    private Enum<E>[] mFirstEnums;
    private Enum<T>[] mSecondEnums;
    private Enum mFirstSelected;
    private Enum mSecondSelected;
    private OnFirstItemSelected mOnFirstItemSelected;

    public interface OnFirstItemSelected {
        void onFirstItemSelected(Enum selected);
    }

    public TwoSpinnerParamData(Enum<E>[] firstEnums, Enum<T>[] secondEnums) {
        super(TWO_SPINNER);
        mFirstEnums = firstEnums;
        mSecondEnums = secondEnums;
    }


    public void setOnFirstItemSelected(OnFirstItemSelected onFirstItemSelected) {
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

}
