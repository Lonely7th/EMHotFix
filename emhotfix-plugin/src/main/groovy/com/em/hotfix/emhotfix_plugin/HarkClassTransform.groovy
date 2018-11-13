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

    //Transform中的核心方法，
    //inputs中是传过来的输入流，其中有两种格式，一种是jar包格式一种是目录格式。
    //outputProvider 获取到输出目录，最后将修改的文件复制到输出目录，这一步必须做不然编译会报错
    @Override
    public void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs,
                          TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        //备份class文件
        creatBackupFile(inputs, outputProvider)
        //生成md5文件
        creatMd5FileByClass()
        //生成补丁文件
        creatPatchFile()
    }

    /**
     * 生成备份文件
     */
    def creatBackupFile(Collection<TransformInput> inputs, TransformOutputProvider outputProvider)
            throws IOException, TransformException, InterruptedException{
        System.out.println("------开始备份class文件------")
        backupDir = new File(mProject.buildDir,"backup")
        if(backupDir.exists()) {
            FileUtils.cleanDirectory(backupDir)
        }

        // 遍历transfrom的inputs, inputs有两种类型，一种是目录，一种是jar，需要分别遍历。
        inputs.each {TransformInput input ->
            input.directoryInputs.each {DirectoryInput directoryInput->

                // 这是transfrom的输出目录
                def dest = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                // 备份dir
                def dirBackup = dest.absolutePath.replace('intermediates','backup')
                File dirBackupFile = new File(dirBackup)
                if(!dirBackupFile.exists()) {
                    dirBackupFile.mkdirs()
                }
                FileUtils.copyDirectory(directoryInput.file, dirBackupFile)

                //注入代码
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
                def jarBackup = dest.absolutePath.replace('intermediates','backup').replace(jarName,jarName+md5Name)
                File jarBackupFile = new File(jarBackup)
                FileUtils.copyFile(jarInput.file,jarBackupFile)
                //输出jar
                FileUtils.copyFile(jarInput.file, dest)
            }
        }
        System.out.println("------结束备份class文件------")
    }

    /**
     * 生成md5文件，md5文件用于匹配补丁文件
     */
    def creatMd5FileByClass(){
        // 首先需要判断是否是release版本，只有在release版本的时候需要生成md5
        File releaseDir = new File(backupDir,"transforms\\${getName()}\\release")
        if(releaseDir.exists()) {
            System.out.println("------开始创建md5文件------")
            // 创建patch目录, 用来保存MD5文件
            File patchDir = new File("$mProject.projectDir.absolutePath\\patch")
            if (!patchDir.exists()) {
                patchDir.mkdirs()
            }

            // 创建md5文件
            File md5File = new File(patchDir, "classesMD5.txt")
            if (md5File.exists()) {
                md5File.delete()
            }

            def pw = md5File.newPrintWriter()

            // 遍历所有class，获取md5，获取完整类名，写入到classesMd5文件中
            releaseDir.eachFileRecurse { File file ->
                String filePath = file.getAbsolutePath()

                if (filePath.endsWith('.class') && HarkInjects.needInject(filePath)) {
                    int beginIndex = filePath.lastIndexOf('release') + 8
                    String className = filePath.substring(beginIndex, filePath.length() - 6).replace('\\', '.').replace('/', '.')
                    InputStream inputStream = new FileInputStream(file)
                    String md5 = DigestUtils.md5Hex(inputStream)
                    inputStream.close()
                    pw.println("$className-$md5")
                }

//                if (filePath.endsWith('.jar')) {
//                    File destDir = new File(file.parent, file.getName().replace('.jar', ''))
//                    JarZipUtil.unzipJar(filePath, destDir.absolutePath)
//                    destDir.eachFileRecurse { File f ->
//                        String fPath = f.absolutePath
//                        if (fPath.endsWith('.class') && Inject.needInject(fPath)) {
//                            int beginIndex = fPath.indexOf(destDir.name) + destDir.name.length() + 1
//                            String className = fPath.substring(beginIndex, fPath.length() - 6).replace('\\', '.').replace('/', '.')
//                            InputStream inputStream = new FileInputStream(f)
//                            String md5 = DigestUtils.md5Hex(inputStream)
//                            inputStream.close()
//                            pw.println("$className-$md5")
//                        }
//                    }
//                    FileUtils.deleteDirectory(destDir)
//                }
            }
            pw.close()
            System.out.println("------结束创建md5文件------")
        }
    }

    /**
     * 生成patch文件
     */
    def creatPatchFile(){
        //每次运行debug的时候，生成补丁文件
        File dopatchDir = new File(backupDir,"transforms\\${getName()}\\debug")
        // 这个是我们release版本打包时保存的md5文件
        File md5File = new File("$mProject.projectDir\\patch\\classesMD5.txt")
        if(dopatchDir.exists() && md5File.exists()) {
            // 这个是保存补丁的目录
            File patchCacheDir = new File("$mProject.projectDir.absolutePath\\patch\\class")
            if(patchCacheDir.exists()) {
                FileUtils.cleanDirectory(patchCacheDir)
            } else {
                patchCacheDir.mkdirs()
            }

            // 使用reader读取md5文件，将每一行保存到集合中
            def reader = md5File.newReader()
            List<String> list = reader.readLines()
            reader.close()

            // 遍历当前的所有class文件，再次生成md5
            dopatchDir.eachFileRecurse {File file->
                String filePath = file.getAbsolutePath()
                if(filePath.endsWith('.class') && HarkInjects.needInject(filePath)) {
                    int beginIndex = filePath.lastIndexOf('debug')+"debug".length()+1
                    String className = filePath.substring(beginIndex, filePath.length()-6).replace('\\','.').replace('/','.')
                    InputStream inputStream = new FileInputStream(file)
                    String md5 = DigestUtils.md5Hex(inputStream)
                    inputStream.close()
                    String str = className +"-"+md5

                    // 然后和release中的md5进行对比，如果不一致，代表这个类已经修改，复制到补丁文件夹中
                    if(!list.contains(str)) {
                        String classFilePath = className.replace('.','\\').concat('.class')
                        File classFile = new File(patchCacheDir,classFilePath)
                        FileUtils.copyFile(file,classFile)
                    }
                }

//                if(filePath.endsWith('.jar')) {
//                    File destDir = new File(file.parent,file.getName().replace('.jar',''))
//                    JarZipUtil.unzipJar(filePath,destDir.absolutePath)
//                    destDir.eachFileRecurse {File f->
//                        String fPath =  f.absolutePath
//                        if(fPath.endsWith('.class') && Inject.needInject(fPath)) {
//                            int beginIndex = fPath.indexOf(destDir.name)+ destDir.name.length()+1
//                            String className = fPath.substring(beginIndex, fPath.length()-6).replace('\\','.').replace('/','.')
//                            InputStream inputStream= new FileInputStream(f)
//                            String md5 = DigestUtils.md5Hex(inputStream)
//                            inputStream.close()
//                            String str = className+"-"+md5
//                            if(!list.contains(str)) {
//                                String classFilePath = className.replace('.','\\').concat('.class')
//                                File classFile = new File(patchCacheDir,classFilePath)
//                                FileUtils.copyFile(file,classFile)
//                            }
//                        }
//                    }
//                    FileUtils.deleteDirectory(destDir)
//                }
            }
        }
    }
}
