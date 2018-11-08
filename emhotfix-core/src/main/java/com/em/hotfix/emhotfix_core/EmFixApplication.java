package com.em.hotfix.emhotfix_core;

import android.app.Application;

/**
 * Time ： 2018/11/8 .
 * Author ： JN Zhang .
 * Description ： .
 */
public class EmFixApplication extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        PatchManager patchManager = new PatchManager(getApplicationContext());
        //开始热修复
        patchManager.fixPatch();
    }

}
