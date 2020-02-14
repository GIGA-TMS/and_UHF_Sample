package com.gigatms.uhf.paramsData;

import static com.gigatms.uhf.paramsData.ParamData.ViewType.ASCII_EDIT_TEXT;

public class ASCIIEditTextParamData extends ParamData {
    private static final String TAG = ASCIIEditTextParamData.class.getSimpleName();
    private String mHint;
    private byte[] mSelected;

    public ASCIIEditTextParamData(String hint) {
        super(ASCII_EDIT_TEXT);
        mHint = hint;
        mSelected = new byte[0];
    }

    public ASCIIEditTextParamData(String hint, byte[] selected) {
        super(ASCII_EDIT_TEXT);
        mHint = hint;
        mSelected = selected;
    }

    public String getHint() {
        return mHint;
    }

    public byte[] getSelected() {
        return mSelected;
    }

    public void setSelected(String selected) {
        mSelected = toBytes(selected);
    }

    public void setSelected(byte[] selected) {
        mSelected = selected;
    }

    private byte[] toBytes(String data) {
        if (data == null) {
            return new byte[0];
        } else {
            return data.replaceAll("<NUL>", new String(new char[]{(char) 0x00}))
                    .replaceAll("<SOH>", new String(new char[]{(char) 0x01}))
                    .replaceAll("<STX>", new String(new char[]{(char) 0x02}))
                    .replaceAll("<ETX>", new String(new char[]{(char) 0x03}))
                    .replaceAll("<EOT>", new String(new char[]{(char) 0x04}))
                    .replaceAll("<ENQ>", new String(new char[]{(char) 0x05}))
                    .replaceAll("<ACK>", new String(new char[]{(char) 0x06}))
                    .replaceAll("<BEL>", new String(new char[]{(char) 0x07}))
                    .replaceAll("<BS>", new String(new char[]{(char) 0x08}))
                    .replaceAll("<TAB>", new String(new char[]{(char) 0x09}))
                    .replaceAll("<LF>", new String(new char[]{(char) 0x0A}))
                    .replaceAll("<VT>", new String(new char[]{(char) 0x0B}))
                    .replaceAll("<FF>", new String(new char[]{(char) 0x0C}))
                    .replaceAll("<CR>", new String(new char[]{(char) 0x0D}))
                    .replaceAll("<SO>", new String(new char[]{(char) 0x0E}))
                    .replaceAll("<SI>", new String(new char[]{(char) 0x0F}))
                    .replaceAll("<DLE>", new String(new char[]{(char) 0x10}))
                    .replaceAll("<DC1>", new String(new char[]{(char) 0x11}))
                    .replaceAll("<DC2>", new String(new char[]{(char) 0x12}))
                    .replaceAll("<DC3>", new String(new char[]{(char) 0x13}))
                    .replaceAll("<DC4>", new String(new char[]{(char) 0x14}))
                    .replaceAll("<NAK>", new String(new char[]{(char) 0x15}))
                    .replaceAll("<SYN>", new String(new char[]{(char) 0x16}))
                    .replaceAll("<ETB>", new String(new char[]{(char) 0x17}))
                    .replaceAll("<CAN>", new String(new char[]{(char) 0x18}))
                    .replaceAll("<EM>", new String(new char[]{(char) 0x19}))
                    .replaceAll("<SUB>", new String(new char[]{(char) 0x1A}))
                    .replaceAll("<ESC>", new String(new char[]{(char) 0x1B}))
                    .replaceAll("<FS>", new String(new char[]{(char) 0x1C}))
                    .replaceAll("<GS>", new String(new char[]{(char) 0x1D}))
                    .replaceAll("<RS>", new String(new char[]{(char) 0x1E}))
                    .replaceAll("<US>", new String(new char[]{(char) 0x1F}))
                    .replaceAll("<DEL>", new String(new char[]{(char) 0x7F}))
                    .getBytes();

        }
    }
}
