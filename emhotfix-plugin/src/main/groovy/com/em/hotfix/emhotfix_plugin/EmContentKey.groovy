package com.em.hotfix.emhotfix_plugin;

/**
 * Time ： 2018/11/13 .
 * Author ： JN Zhang .
 * Description ： .
 */
public class EmContentKey {
    public final static String hackInjectCode = "System.out.println();"

    public final static List<String> noInjectKeyWord = ["\$","R.class","BuildConfig.class"]

    public final static String patchCacheDir = ""

}
