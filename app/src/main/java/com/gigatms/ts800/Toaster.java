package com.gigatms.ts800;

import android.content.Context;
import android.widget.Toast;

class Toaster {
    private static Toast toast;

    static  void showToast(Context context, String text, int duration) {
        if (toast != null) {
            toast.cancel();
        }
        if (context != null) {
            toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }
}
