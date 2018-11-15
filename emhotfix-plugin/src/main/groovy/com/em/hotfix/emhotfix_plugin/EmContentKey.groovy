package com.em.hotfix.emhotfix_plugin;

/**
 * Time ： 2018/11/13 .
 * Author ： JN Zhang .
 * Description ： .
 */
public class EmContentKey {
    //打桩待插入的代码
    public final static String hackInjectCode = "System.out.println();"
    //备份文件夹
    public final static String backupDir = "backup"
    //不需要处理的关键字
    public final static List<String> noInjectKeyWord = ["\$","R.class","BuildConfig.class"]
    //保存补丁class文件的文件夹
    public final static String patchClassDir = "\\patch\\class"
    //保存补丁的文件夹
    public final static String patchCacheDir = ""
    //保存class md5值的文件
    public final static String class_md5File = "classesMD5.txt"
}
