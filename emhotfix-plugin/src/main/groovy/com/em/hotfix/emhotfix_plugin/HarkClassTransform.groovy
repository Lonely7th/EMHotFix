package com.em.hotfix.emhotfix_plugin

import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 * Time ： 2018/11/9 .
 * Author ： JN Zhang .
 * Description ： .
 */
public class HarkClassTransform extends Transform {
    private Project mProject
    private File backupDir

    public HarkClassTransform(Project p) {
        this.mProject = p
    }

    @Override
    public String getName() {
        return getClass().getSimpleName()
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    public boolean isIncremental() {
        return false
    }

    /**
     * inputs中是传过来的输入流，其中有两种格式，一种是jar包格式一种是目录格式
     * outputProvider 获取到输出目录，最后将修改的文件复制到输出目录，这一步必须做不然编译会报错
     */
    @Override
    public void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs,
                          TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        // 生成备份文件,执行代码打桩
        creatBackupFile(inputs, outputProvider)
    }

    /**
     * 生成备份文件
     */
    def creatBackupFile(Collection<TransformInput> inputs, TransformOutputProvider outputProvider)
            throws IOException, TransformException, InterruptedException{
        System.out.println("------开始备份class文件------")
        backupDir = new File(mProject.buildDir,EmContentKey.backupDir)
        if(backupDir.exists()) {
            FileUtils.cleanDirectory(backupDir)
        }

        // 遍历transfrom的inputs, inputs有两种类型，一种是目录，一种是jar，需要分别遍历。
        inputs.each {TransformInput input ->
            input.directoryInputs.each {DirectoryInput directoryInput->

                // 这是transfrom的输出目录
                def dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                // 备份dir
                def dirBackup = dest.absolutePath.replace('intermediates',EmContentKey.backupDir)
                File dirBackupFile = new File(dirBackup)
                if(!dirBackupFile.exists()) {
                    dirBackupFile.mkdirs()
                }
                FileUtils.copyDirectory(directoryInput.file, dirBackupFile)

                // 注入代码
                HarkInjects.inject(directoryInput.file.absolutePath)
                // 将input的目录复制到output指定目录
                FileUtils.copyDirectory(directoryInput.file, dest)
            }

            //jar文件不注入代码，只输出源文件
            input.jarInputs.each {JarInput jarInput->

                //重命名输出文件（同目录copyFile会冲突）
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if(jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0,jarName.length()-4)
                }
                def dest = outputProvider.getContentLocation(jarName+md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)

                //备份jar
                def jarBackup = dest.absolutePath.replace('intermediates',EmContentKey.backupDir).replace(jarName,jarName+md5Name)
                File jarBackupFile = new File(jarBackup)
                FileUtils.copyFile(jarInput.file,jarBackupFile)
                //输出jar
                FileUtils.copyFile(jarInput.file, dest)
            }
        }
        System.out.println("------结束备份class文件------")
    }
}
