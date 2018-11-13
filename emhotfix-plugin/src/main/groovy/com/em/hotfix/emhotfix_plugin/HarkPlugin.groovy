package com.em.hotfix.emhotfix_plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.apache.commons.io.FileUtils

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
        HarkClassTransform classTransform = new HarkClassTransform(project)
        android.registerTransform(classTransform)

        /**
         * 我们是在混淆之前就完成注入代码的，这会出现问题，找不到AntilazyLoad这个类
         *
         * 在PreDexTransform注入代码之前，先将原来没有注入的代码保存了一份到 buildDir/backup
         * 如果开启了混淆，则在混淆之前将代码覆盖回来
         */
        project.afterEvaluate {
            project.android.applicationVariants.each {variant->
                def proguardTask = project.getTasks().findByName("transformClassesAndResourcesWithProguardFor${variant.name.capitalize()}")
                if(proguardTask) {
                    System.out.println("------开启混淆------")
                    // 如果有混淆，执行之前将备份的文件覆盖原来的文件
                    proguardTask.doFirst {
                        System.out.println("------doFirst------")
                        File backupDir = new File(project.buildDir,"backup\\transforms\\$classTransform.name\\$variant.name")
                        if(backupDir.exists()) {
                            def srcDirPath = backupDir.getAbsolutePath().replace('backup','intermediates')
                            File srcDir = new File(srcDirPath)
                            FileUtils.cleanDirectory(srcDir)
                            FileUtils.copyDirectory(backupDir,srcDir)
                        }
                    }

                    proguardTask.doLast {
                        System.out.println("------doLast------")
                        // 如果是开启混淆的release，混淆注入代码，并且将mapping复制到patch目录
                        if(proguardTask.name.endsWith('ForRelease')) {
                            System.out.println("------ForRelease------")
                            // 遍历proguard文件夹,注入代码
                            File proguardDir = new File("$project.buildDir\\intermediates\\transforms\\proguard\\release")
//                            proguardDir.eachFileRecurse { File file ->
//                                if(file.name.endsWith('jar')) {
//                                    Inject.injectJar(file.absolutePath)
//                                }
//                            }

                            File mapping = new File("$project.buildDir\\outputs\\mapping\\release\\mapping.txt")
                            File mappingCopy = new File("$project.projectDir\\patch\\mapping.txt")
                            FileUtils.copyFile(mapping, mappingCopy)
                        }

                        // 自动打补丁
                        if(proguardTask.name.endsWith('ForDoDebug')) {
                            System.out.println("------ForDoDebug------")
                            // 解析mapping文件
                            File mapping = new File("$project.projectDir\\patch\\mapping.txt")
                            def reader = mapping.newReader()
                            Map<String, String> map = new HashMap<>()
                            reader.eachLine {String line->
                                if(line.endsWith(':')) {
                                    String[] strings = line.replace(':','').split(' -> ')
                                    if(strings.length == 2) {
                                        map.put(strings[0],strings[1])
                                    }
                                }
                            }
                            reader.close()
                            println "map= $map"

                            // 在Transfrom中已经将需要打补丁的类复制到了指定目录, 我们需要遍历这个目录获取类名
                            List<String> patchList = new ArrayList<>()
                            File patchCacheDir = new File("$project.projectDir.absolutePath\\patch\\class")
                            patchCacheDir.eachFileRecurse { File file->
                                String filePath = file.absolutePath

                                if(filePath.endsWith('.class')) {
                                    // 获取类名
                                    int beginIndex = filePath.lastIndexOf(patchCacheDir.name)+patchCacheDir.name.length()+1
                                    String className = filePath.substring(beginIndex, filePath.length()-6).replace('\\','.').replace('/','.')
                                    System.out.println("className = " + className)
                                    // 获取混淆后类名
                                    String proguardName = map.get(className)
                                    System.out.println("proguardName = " + proguardName)
                                    patchList.add(proguardName)
                                }
                            }

                            println "list= $patchList"
                            // patchList保存的是需要打补丁的类名(混淆后)
                            // 1. 清除原类文件夹
                            FileUtils.cleanDirectory(patchCacheDir)

                            // 2. 将混淆的后jar包解压到当前目录
                            File proguardDir = new File("$project.buildDir\\intermediates\\transforms\\proguard")
                            proguardDir.eachFileRecurse {File file->
                                if(file.name.endsWith('.jar')) {
                                    File destDir = new File(file.parent,file.getName().replace('.jar',''))
                                    //JarZipUtil.unzipJar(file.absolutePath,destDir.absolutePath)
                                    // 3. 遍历destDir, 将需要打补丁的类复制到cache目录
                                    destDir.eachFileRecurse {File f->
                                        String fPath = f.absolutePath
                                        if(fPath.endsWith('.class')) {
                                            // 获取类名
                                            int beginIndex = fPath.lastIndexOf(destDir.name) + destDir.name.length() + 1
                                            String className = fPath.substring(beginIndex, fPath.length() - 6).replace('\\', '.').replace('/', '.')

                                            project.logger.info "class=======================$className"
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
}
