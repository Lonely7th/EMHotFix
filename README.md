### 1.引入EMHotFix
在project下的build.gradle文件中添加依赖：
```
maven {url 'https://dl.bintray.com/jnzhang/EMHotFix'}

classpath 'com.emhotfix.core:emhotfix-plugin:1.0.0'
```
在module下的build.gradle文件中添加引用和插件：
```
apply plugin: 'com.em.hotfix.plugin'

implementation 'com.emhotfix.core:emhotfix-core:1.0.0'
```
### 2.初始化EMHotFix
2.1在应用程序的入口继承EmFixApplication：
```
public class MainApplication extends EmFixApplication{

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
```
2.2自定义补丁分发机制
EMHotFix将补丁分发机制交由开发者自行处理，开发者可以根据项目的需求制定补丁分发规则，只需要将下发的补丁保存在应用目录下的patch文件夹中即可，保存补丁文件的路径：
```
String patchDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/patch/";
```
### 3.热修复的相关操作
* 每次发布release版本后，开发者需要对工程进行备份，并做好版本管理；
* 如果某版本的安装包发现bug需要进行热修复，则运行对应的备份工程进行修改和测试；
* 修复完成后，在debug模式下运行工程，此时EMHotFix会筛选出此次修复改动的文件并保存至工程目录下的/patch/class文件夹中；
* 开发者进入patch/class文件夹并执行打包命令即可生成补丁文件：
```
jar -cvf patch.jar com;

dx --dex --output=patch_dex.jar patch.jar
```
***注意：***
* EMHotFix默认以时间先后顺序来加载补丁文件，既最新下发的文件最先加载，建议开发者定时清理弃用的补丁；
* 正常情况下release版本的安装包都是开启混淆的，这就需要开发者在生成补丁文件时对debug模式开启混淆，否则将会导致生成补丁失败：
```
debug {
    minifyEnabled true
    proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
}
```
