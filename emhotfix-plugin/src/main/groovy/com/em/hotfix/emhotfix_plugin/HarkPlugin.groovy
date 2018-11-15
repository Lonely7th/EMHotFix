package com.em.hotfix.emhotfix_plugin

import com.android.build.gradle.AppExtension
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.apache.commons.io.FileUtils

/**
 * Time ： 2018/11/9 .
 * Author ： JN Zhang .
 * Description ： .
 */
public class HarkPlugin implements Plugin<Project>{
    private File backupDir
    Map<String, String> map = new HashMap<>()

    @Override
    public void apply(Project project) {
        backupDir = new File(project.buildDir,EmContentKey.backupDir)

        //AppExtension就是build.gradle中android{...}
        def android = project.extensions.getByType(AppExtension)

        //注册一个Transform
        HarkClassTransform classTransform = new HarkClassTransform(project)
        android.registerTransform(classTransform)

        //在PreDexTransform注入代码之前，先将原来没有注入的代码保存了一份到 buildDir/backup,如果开启了混淆，则在混淆之前将代码覆盖回来
        project.afterEvaluate {
            project.android.applicationVariants.each {variant->
                def proguardTask = project.getTasks().findByName("transformClassesAndResourcesWithProguardFor${variant.name.capitalize()}")
                if(proguardTask) {
                    System.out.println("------开启混淆------")
                    // 如果有混淆，执行之前将备份的文件覆盖原来的文件
                    proguardTask.doFirst {
                        File backupDir = new File(project.buildDir,EmContentKey.backupDir+"\\transforms\\$classTransform.name\\$variant.name")
                        if(backupDir.exists()) {
                            def srcDirPath = backupDir.getAbsolutePath().replace(EmContentKey.backupDir,'intermediates')
                            File srcDir = new File(srcDirPath)
                            FileUtils.cleanDirectory(srcDir)
                            FileUtils.copyDirectory(backupDir,srcDir)
                        }
                    }

                    proguardTask.doLast {
                        // 如果是开启混淆的release，混淆注入代码，并且将mapping复制到patch目录
                        if(proguardTask.name.endsWith('ForRelease')) {
                            // 遍历proguard文件夹,注入代码
//                            File proguardDir = new File("$project.buildDir\\intermediates\\transforms\\proguard\\release")
//                            proguardDir.eachFileRecurse { File file ->
//                                if(file.name.endsWith('jar')) {
//                                    Inject.injectJar(file.absolutePath)
//                                }
//                            }

                            File mapping = new File("$project.buildDir\\outputs\\mapping\\release\\mapping.txt")
                            File mappingCopy = new File("$project.projectDir\\patch\\mapping.txt")
                            FileUtils.copyFile(mapping, mappingCopy)
                            // 生成release class对应的md5值
                            creatMd5FileByClass(project, classTransform)
                        }

                        // 每次运行debug模式时，自动生成补丁文件
                        if(proguardTask.name.endsWith('ForDebug')) {
                            // 匹配release的md5文件，筛选出需要被打补丁的文件
                            creatPatchFile(project, classTransform)
                            // 解析mapping文件
                            File mapping = new File("$project.projectDir\\patch\\mapping.txt")
                            def reader = mapping.newReader()
                            reader.eachLine {String line->
                                if(line.endsWith(':')) {
                                    String[] strings = line.replace(':','').split(' -> ')
                                    if(strings.length == 2) {
                                        map.put(strings[0],strings[1])
                                    }
                                }
                            }
                            reader.close()
                            System.out.println("map= $map")

                            // 在Transfrom中已经将需要打补丁的类复制到了指定目录, 我们需要遍历这个目录获取类名
                            List<String> patchList = new ArrayList<>()
                            File patchCacheDir = new File("$project.projectDir.absolutePath" + EmContentKey.patchClassDir)
                            patchCacheDir.eachFileRecurse { File file->
                                String filePath = file.absolutePath

                                if(filePath.endsWith('.class')) {
                                    // 获取类名
                                    int beginIndex = filePath.lastIndexOf(patchCacheDir.path)+patchCacheDir.path.length()+1
                                    String className = filePath.substring(beginIndex, filePath.length()-6).replace('\\','.').replace('/','.')
                                    // 获取混淆后类名
                                    String proguardName = getClassNameByMapping(className)
                                    patchList.add(proguardName)
                                }
                            }

                            // patchList保存的是需要打补丁的类名(混淆后)
                            System.out.println("list= $patchList")
                            // 1. 清除原类文件夹
                            FileUtils.cleanDirectory(patchCacheDir)

                            // 2. 将混淆的后jar包解压到当前目录
                            File proguardDir = new File("$project.buildDir\\intermediates\\transforms\\proguard\\debug")
                            proguardDir.eachFileRecurse {File file->
                                if(file.name.endsWith('.jar')) {
                                    File destDir = new File(file.parent,file.getName().replace('.jar',''))
                                    JarZipUtil.unzipJar(file.absolutePath,destDir.absolutePath)
                                    // 3. 遍历destDir, 将需要打补丁的类复制到cache目录
                                    destDir.eachFileRecurse {File f->
                                        String fPath = f.absolutePath
                                        if(fPath.endsWith('.class')) {
                                            // 获取类名
                                            int beginIndex = fPath.lastIndexOf(destDir.name) + destDir.name.length() + 1
                                            String className = fPath.substring(beginIndex, fPath.length() - 6).replace('\\', '.').replace('/', '.')

                                            // 是否是补丁，复制到cache目录
                                            if(patchList.contains(className)) {
                                                String destPath = className.replace(".","\\").concat('.class')
                                                File destFile = new File(patchCacheDir,destPath)
                                                FileUtils.copyFile(f, destFile)
                                            }
                                        }
                                    }
                                    FileUtils.deleteDirectory(destDir)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * relaease后生成md5文件，md5文件用于匹配补丁文件
     */
    def creatMd5FileByClass(Project project,HarkClassTransform classTransform){
        // 首先需要判断是否是release版本，只有在release版本的时候需要生成md5
        File releaseDir = new File(backupDir,"transforms\\${classTransform.getName()}\\release")
        if(releaseDir.exists()) {
            System.out.println("------开始创建md5文件------")
            // 创建patch目录, 用来保存MD5文件
            File patchDir = new File("$project.projectDir.absolutePath\\patch")
            if (!patchDir.exists()) {
                patchDir.mkdirs()
            }

            // 创建md5文件
            File md5File = new File(patchDir, EmContentKey.class_md5File)
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
            }
            pw.close()
            System.out.println("------结束创建md5文件------")
        }
    }

    /**
     * 生成patch文件
     */
    def creatPatchFile(Project project,HarkClassTransform classTransform){
        // 每次运行debug的时候，生成补丁文件
        File dopatchDir = new File(backupDir,"transforms\\${classTransform.getName()}\\debug")
        // 这个是我们release版本打包时保存的md5文件
        File md5File = new File("$project.projectDir\\patch\\" + EmContentKey.class_md5File)
        if(dopatchDir.exists() && md5File.exists()) {
            // 这个是保存补丁的目录
            File patchCacheDir = new File("$project.projectDir.absolutePath" + EmContentKey.patchClassDir)
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

            }
        }
    }

    /**
     * 获取混淆后的类名
     */
    private String getClassNameByMapping(String className){
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if(className.contains(entry.key)){
                return entry.value
            }
        }
        return ""
    }
}
