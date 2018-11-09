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

        //执行Transform
        project.task('HarkTask') {
            doLast {
                System.out.println('start hark task')
            }
        }
    }
}
