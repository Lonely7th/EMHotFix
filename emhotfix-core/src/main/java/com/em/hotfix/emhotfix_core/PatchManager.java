package com.em.hotfix.emhotfix_core;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * Time ： 2018/11/8 .
 * Author ： JN Zhang .
 * Description ：补丁管理类 .
 */
public class PatchManager {
    private static final String TAG = "PatchManager";
    private Context context;

    public PatchManager(Context context){
        this.context = context;
    }

    /**
     * 启动补丁修复功能
     */
    public void fixPatch() {
        File patchDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/patch/");
        //获取patch文件夹下所有的补丁文件
        File[] files = patchDir.listFiles();
        if(files != null && files.length > 0){
            //补丁按下载日期排序(最新补丁放前面)
            patchSort(files);
            for (File file : files) {
                //判断file是否为补丁
                if(file.isFile() && file.getAbsolutePath().endsWith(".jar")){
                    System.out.println("---:"+file.getName());
                    //开始加载补丁并修复
                    loadPatch(file);
                }
            }
            Log.d(TAG, "fiexd success...");
        }
    }

    /**
     * [按最后修改日期排序]，排序方式有很多：冒泡排序、快速排序等等，
     * 这个随便，只要排序后，保证最新的dex补丁在最前面就行
     */
    private void patchSort(File[] files){
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File file, File t1) {
                System.out.println(file.getName()+":"+file.lastModified());
                System.out.println(t1.getName()+":"+t1.lastModified());
                long d = t1.lastModified() - file.lastModified();
                //从大到小排序
                if(d>0){
                    return -1;
                }else if(d<0){
                    return 1;
                }else{
                    return 0;
                }
            }
            @Override
            public boolean equals(Object obj) {
                return true;
            }
        });
    }

    /**
     * 加载并安装补丁
     * @type {[type]}
     */
    private void loadPatch(File file){
        Log.d(TAG, file.getAbsolutePath());
        if(file.exists()){
            Log.d(TAG,"文件存在...");
        }else{
            Log.d(TAG, "文件不存在...");
        }
        //获取系统PathClassLoader
        PathClassLoader pLoader = (PathClassLoader) context.getClassLoader();
        //获取PathClassLoader中的PathList
        Object pPathList = getPathList(pLoader);
        if(pPathList == null){
            Log.d(TAG, "get PathClassLoader pathlist failed...");
            return;
        }
        //加载补丁
        DexClassLoader dLoader = new DexClassLoader(file.getAbsolutePath(),EmContentKey.getPatchPath(context),null, pLoader);
        //获取DexClassLoader的pathLit，即BaseDexClassLoader中的pathList
        Object dPathList = getPathList(dLoader);
        if(dPathList == null){
            Log.d(TAG, "get DexClassLoader pathList failed...");
            return;
        }
        //获取PathList和DexClassLoader的DexElements
        Object pElements = getElements(pPathList);
        Object dElements = getElements(dPathList);

        //将补丁dElements[]插入系统pElements[]的最前面
        Object newElements = insertElements(pElements, dElements);
        if(newElements == null){
            Log.d(TAG, "patch insert failed...");
            return;
        }
        //用插入补丁后的新Elements[]替换系统Elements[]
        try {
            Field fElements = pPathList.getClass().getDeclaredField("dexElements");
            fElements.setAccessible(true);
            fElements.set(pPathList, newElements);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "fixed failed....");
        }
    }

    /**
     * 将补丁插入系统DexElements[]最前端，生成一个新的DexElements[]
     * @param pElements
     * @param dElements
     * @return
     */
    private Object insertElements(Object pElements, Object dElements){
        //判断是否为数组
        if(pElements.getClass().isArray() && dElements.getClass().isArray()){
            //获取数组长度
            int pLen = Array.getLength(pElements);
            int dLen = Array.getLength(dElements);
            //创建新数组
            Object newElements = Array.newInstance(pElements.getClass().getComponentType(), pLen+dLen);
            //循环插入
            for(int i=0; i<pLen+dLen;i++){
                if(i<dLen){
                    Array.set(newElements, i, Array.get(dElements, i));
                }else{
                    Array.set(newElements, i, Array.get(pElements, i-dLen));
                }
            }
            return newElements;
        }
        return null;
    }

    /**
     *  获取DexElements
     * @param object
     * @return
     */
    private Object getElements(Object object){
        try {
            Class<?> c = object.getClass();
            Field fElements = c.getDeclaredField("dexElements");
            fElements.setAccessible(true);
            Object obj = fElements.get(object);
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 通过反射机制获取PathList
     * @param loader
     * @return
     */
    private Object getPathList(BaseDexClassLoader loader){
        try {
            Class<?> c = Class.forName("dalvik.system.BaseDexClassLoader");
            //获取成员变量pathList
            Field fPathList = c.getDeclaredField("pathList");
            //抑制jvm检测访问权限
            fPathList.setAccessible(true);
            //获取成员变量pathList的值
            Object obj = fPathList.get(loader);
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
