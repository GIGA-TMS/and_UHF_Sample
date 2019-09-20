package com.gigatms.uhf.paramsData;


import static com.gigatms.uhf.paramsData.ParamData.ViewType.SPINNER;

public class SpinnerParamData<E extends Enum<E>> extends ParamData {
    private E[] mDataArray;
    private Enum mSelected;

    public SpinnerParamData(Class<E> enumData) {
        super(SPINNER);
        mDataArray = enumData.getEnumConstants();
    }

    public SpinnerParamData(E[] enumData) {
        super(SPINNER);
        mDataArray = enumData;
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

}
