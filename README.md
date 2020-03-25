## Android内网开发将gradle缓存上传到maven私服的方法
内网开发情况下，开发机无法访问外网。但我们需要从外网下载一些第三方库供项目开发使用。所以要先使用一台可以连外网的电脑先将第三方代码同步下来，
然后将本地的.gradle缓存目录拷贝到内网的数据交换的电脑上。最后使用我这个工具将gradle缓存的第三方库上传到内网的私有maven仓库中。

## 使用方法
```shell script
java -jar Deploy.jar ~/.gradle/caches/modules-2/files-2.1/ http://127.0.0.1:8081/repository/android/ android
```
其中，
Deploy.jar 在out/artifacts/目录下，
~/.gradle/caches/modules-2/files-2.1/为存放gradle缓存的路径，
http://127.0.0.1:8081/repository/android/为私有maven仓库地址，
android为仓库id。

最后将私有maven仓库添加到项目的build.gradle配置中就可以在内网开发过程中使用第三方库了。

```groovy
 repositories {
//        google()
//        jcenter()
        maven {
            url 'http://127.0.0.1:8081/repository/android/'
            credentials {
                username = 'admin'
                password = 'admin123'
            }
        }
    }
```
