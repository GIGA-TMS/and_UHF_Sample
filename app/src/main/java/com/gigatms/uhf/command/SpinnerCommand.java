package com.gigatms.uhf.command;

import static com.gigatms.uhf.CommandRecyclerViewAdapter.SPINNER;

public class SpinnerCommand<E extends Enum<E>> extends Command {
    private E[] mEnum;
    private Enum mSelected;

    public SpinnerCommand(String title, Class<E> enumData) {
        super(title);
        mEnum = enumData.getEnumConstants();
    }

    public SpinnerCommand(String title, String leftBtnName, String rightBtnName, Class<E> enumData) {
        super(title, leftBtnName, rightBtnName);
        mEnum = enumData.getEnumConstants();
    }

    @Override
    public int getViewType() {
        return SPINNER;
    }

    public E[] getEnum() {
        return mEnum;
    }

    public void setSelected(Enum selected) {
        mSelected = selected;
    }

    public Enum getSelected() {
        return mSelected;
    }

}
