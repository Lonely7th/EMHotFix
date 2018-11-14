package com.em.hotfix.emhotfix_core;

import android.content.Context;
import android.os.Environment;

import java.io.File;

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
        String patchDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/patch/";
        File file = new File(patchDir);
        if(!file.exists()){
            file.mkdirs();
        }
        return patchDir;
    }
}
