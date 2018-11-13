package com.em.hotfix.emhotfix_plugin

import javassist.ClassPool
import javassist.CtClass
import javassist.CtConstructor

/**
 * Time ： 2018/11/9 .
 * Author ： JN Zhang .
 * Description ： .
 */
public class HarkInjects {
    //初始化类池
    private final static ClassPool pool = ClassPool.getDefault()

    //引入关联的jar包
    public static void setJarPath(String path) {
        pool.appendClassPath(path)
    }

    public static void inject(String path) {
        //将当前路径加入类池
        pool.appendClassPath(path)

        File dir = new File(path)
        if (dir.isDirectory()) {
            //遍历文件夹
            dir.eachFileRecurse { File file ->
                String filePath = file.absolutePath
                println("filePath = " + filePath)
                if (filePath.endsWith(".class") && !filePath.contains('R$') && !filePath.contains('R.class')
                        && !filePath.contains("BuildConfig.class") && !getPackagebyFilePath(filePath).equals("")){
                    //获取.class文件
                    println("class = " + getPackagebyFilePath(filePath))
                    CtClass ctClass = pool.getCtClass(getPackagebyFilePath(filePath))
                    if (ctClass.isFrozen()){
                        //解冻
                        ctClass.defrost()
                    }
                    CtConstructor[] cts = ctClass.getDeclaredConstructors()

                    if (cts == null || cts.length == 0) {
                        insertNewConstructor(ctClass)
                    } else {
                        cts[0].insertBeforeBody(EmContentKey.hackInjectCode)
                    }
                    ctClass.writeFile(path)
                    //释放
                    ctClass.detach()
                }
            }
        }

    }

    //创建构造方法
    private static void insertNewConstructor(CtClass c) {
        CtConstructor constructor = new CtConstructor(new CtClass[0], c)
        constructor.insertBeforeBody(EmContentKey.hackInjectCode)
        c.addConstructor(constructor)
    }

    //获取当前类的绝对路径
    private static String getPackagebyFilePath(String filePath){
        //排除空路径
        if(filePath == null || filePath.equals("") || filePath.contains("\$")){
            return ""
        }
        //排除文件夹路径
        if(!filePath.contains(".class")){
            return ""
        }
        String strPackage
        if(filePath.contains("classes\\debug")){
            strPackage = filePath.substring(filePath.indexOf("classes\\debug")+"classes\\debug".length()+1)
        }else if(filePath.contains("classes\\release")){
            strPackage = filePath.substring(filePath.indexOf("classes\\release")+"classes\\release".length()+1)
        } else{
            strPackage = filePath.substring(filePath.indexOf("classes")+"classes".length()+1)
        }
        strPackage = strPackage.replace(".class","")
        strPackage = strPackage.replace("\\",".")
        return strPackage
    }

    /**
     * 获取需要被处理的class
     */
    public static boolean needInject(String filePath){
        for(String key : EmContentKey.noInjectKeyWord){
            if(filePath.contains(key)){
                return false
            }
        }
        return true
    }

}
