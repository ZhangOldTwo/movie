package com.hw.movie.tools;

import android.content.res.Resources;

import java.util.List;

public class SystemUtils {


    public static boolean eqList(List<String> list1, List<String> list2) {
        boolean bl = true;
        if (list1.size() == list2.size()) {
            for (int i = 0; i < list1.size(); i++) {
                if ((list1.get(i)).equals(list2.get(i))) {
                    continue;
                } else {
                    bl = false;
                    break;
                }
            }
        } else {
            bl = false;
        }
        return bl;
    }

    public static int dp2px(final float dpValue) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
