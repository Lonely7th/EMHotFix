package com.em.hotfix;

/**
 * Time ： 2018/11/9 .
 * Author ： JN Zhang .
 * Description ： .
 */
public class BugUtils {

    public String getValue(){
        int a = 1 / 0;
        return "" + a;
    }
}
