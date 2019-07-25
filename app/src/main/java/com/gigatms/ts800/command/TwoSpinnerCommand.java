package com.gigatms.ts800.command;

import static com.gigatms.ts800.CommandRecyclerViewAdapter.TWO_SPINNER;

public class TwoSpinnerCommand<E extends Enum<E>, T extends Enum<T>> extends Command {
    private OnGetValues mOnGetValues;
    private Enum<E>[] mFirstEnums;
    private Enum<T>[] mSecondEnums;
    private Enum mFirstSelected;
    private Enum mSecondSelected;
    private OnFirstItemSelected mOnFirstItemSelected;

    public interface OnFirstItemSelected {
        void onFirstItemSelected(Enum selected);
    }

    public TwoSpinnerCommand(String title, Enum<E>[] firstEnums, Enum<T>[] secondEnums) {
        super(title);
        mFirstEnums = firstEnums;
        mSecondEnums = secondEnums;
    }

/*    public TwoSpinnerCommand(String title, String rightBtnName, String leftBtnName, Class<E> firstEnum, Class<T> secondEnum) {
        super(title, rightBtnName, leftBtnName);
        mFirstEnums = firstEnum;
        mSecondEnums = secondEnum;
    }*/

    public interface OnGetValues {
        void onGetValues(Enum first, Enum second);
    }

    public void setOnFirstItemSelected(OnFirstItemSelected onFirstItemSelected) {
        mOnFirstItemSelected = onFirstItemSelected;
    }

    @Override
    public int getViewType() {
        return TWO_SPINNER;
    }

    public void setOnGetValues(OnGetValues onGetValues) {
        mOnGetValues = onGetValues;
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

    public void didGetValue(Enum first, Enum second) {
        mOnGetValues.onGetValues(first, second);
    }
}
