package com.em.hotfix.emhotfix_plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Time ： 2018/11/9 .
 * Author ： JN Zhang .
 * Description ： .
 */
public class HarkPlugin implements Plugin<Project>{
    @Override
    public void apply(Project project) {
        //AppExtension就是build.gradle中android{...}这一块
        def android = project.extensions.getByType(AppExtension)

        //注册一个Transform
        def classTransform = new HarkClassTransform(project)
        android.registerTransform(classTransform)

        /**
         * 我们是在混淆之前就完成注入代码的，这会出现问题，找不到AntilazyLoad这个类
         *
         * 我的解决方式：
         * 在PreDexTransform注入代码之前，先将原来没有注入的代码保存了一份到 buildDir/backup
         * 如果开启了混淆，则在混淆之前将代码覆盖回来
         */
//        project.afterEvaluate {
//            project.android.applicationVariants.each {variant->
//                def proguardTask = project.getTasks().findByName("transformClassesAndResourcesWithProguardFor${variant.name.capitalize()}")
//                if(proguardTask) {
//
//                    // 如果有混淆，执行之前将备份的文件覆盖原来的文件(变相的清除已注入代码)
//                    proguardTask.doFirst {
//                        File backupDir = new File(project.buildDir,"backup\\transforms\\$preDexTransform.name\\$variant.name")
//                        if(backupDir.exists()) {
//                            def srcDirPath = backupDir.getAbsolutePath().replace('backup','intermediates')
//                            File srcDir = new File(srcDirPath)
//                            FileUtils.cleanDirectory(srcDir)
//                            FileUtils.copyDirectory(backupDir,srcDir)
//                        }
//                    }
//
//                    proguardTask.doLast {
//                        //TODO 开启混淆后在这里注入代码
//                    }
//                }
//            }
//        }
    }
}
