package com.em.hotfix.emhotfix_core;

import android.content.Context;
import android.os.Environment;

/**
 * Time ： 2018/11/8 .
 * Author ： JN Zhang .
 * Description ： .
 */
public class EmContentKey {

    /**
     * @return 补丁文件夹路径
     */
    public static String getPatchPath(Context context){
        return context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/patch/";
    }
}
