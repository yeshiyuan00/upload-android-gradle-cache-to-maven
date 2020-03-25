import java.io.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * 上传依赖到 Maven 私服
 *
 * @author liuzenghui
 * @since 2017/7/31.
 */
public class Deploy {
    /**
     * mvn -s F:\.m2\settings.xml
     * org.apache.maven.plugins:maven-deploy-plugin:2.8.2:deploy-file
     * -Durl=http://IP:PORT/nexus/content/repositories/thirdpart
     * -DrepositoryId=thirdpart
     * -Dfile=antlr-2.7.2.jar
     * -DpomFile=antlr-2.7.2.pom
     * -Dpackaging=jar
     * -DgeneratePom=false
     * -Dsources=./path/to/artifact-name-1.0-sources.jar
     * -Djavadoc=./path/to/artifact-name-1.0-javadoc.jar
     */
//    public static final String BASE_CMD = "cmd /c mvn " +
//            "-s F:\\.m2\\settings.xml " +
//            "deploy:deploy-file " +
//            "-Durl=http://127.0.0.1:8081/repository/android/ " +
//            "-DrepositoryId=android " +
//            "-DgeneratePom=true";

    public static final String BASE_CMD = "mvn " +
            "deploy:deploy-file "
//            +
//            "-Durl=http://127.0.0.1:8081/repository/test/ " +
//            "-DrepositoryId=test " +
//            "-DgeneratePom=true "
            ;

    public static String DUrl = "http://127.0.0.1:8081/repository/test/";
    public static String DrepositoryId = "test";

    public static final Pattern DATE_PATTERN = Pattern.compile("-[\\d]{8}\\.[\\d]{6}-");

    public static final Runtime CMD = Runtime.getRuntime();

    public static final Writer ERROR;

    public static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(10);

    static {
        Writer err = null;
        try {
            err = new OutputStreamWriter(new FileOutputStream("deploy-error.log"), "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        ERROR = err;
    }

    public static void main(String[] args) {
        MyFilter myFilter = new MyFilter();
////        deploy(new File("/Users/yeshiyuan/.gradle/caches/modules-2/files-2.1/").listFiles(myFilter));
//        deploy(new File("/Users/yeshiyuan/.gradle/caches/modules-2/files-2.1/com.android.tools.build/gradle/").listFiles(myFilter));
        if (checkArgs(args)) {
            File file = new File(args[0]);
            deploy(file.listFiles(myFilter));
        }
        EXECUTOR_SERVICE.shutdown();
        try {
            ERROR.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void error(String error) {
        try {
            System.err.println(error);
            ERROR.write(error + "\n");
            ERROR.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean checkArgs(String[] args) {
        if (args.length != 3) {
            System.out.println("用法如： java -jar Deploy D:\\some\\path\\ http://127.0.0.1:8081/repository/test/ test");
            return false;
        }
        File file = new File(args[0]);
        if (!file.exists()) {
            System.out.println(args[0] + " 目录不存在!");
            return false;
        }
        if (!file.isDirectory()) {
            System.out.println("必须指定为目录!");
            return false;
        }
        DUrl = args[1];
        DrepositoryId = args[2];
        return true;
    }


//    public static void deploy(File[] files) {
//        if (files.length == 0) {
//            //ignore
//        } else if (files[0].isDirectory()) {
//            for (File file : files) {
//                if (file.isDirectory()) {
//                    deploy(file.listFiles());
//                }
//            }
//        } else if (files[0].isFile()) {
//            File pom = null;
//            File jar = null;
//            File source = null;
//            File javadoc = null;
//            //忽略日期快照版本，如 xxx-mySql-2.2.6-20170714.095105-1.jar
//            for (File file : files) {
//                String name = file.getName();
//                if (DATE_PATTERN.matcher(name).find()) {
//                    //skip
//                } else if (name.endsWith(".pom")) {
//                    pom = file;
//                } else if (name.endsWith("-javadoc.jar")) {
//                    javadoc = file;
//                } else if (name.endsWith("-sources.jar")) {
//                    source = file;
//                } else if (name.endsWith(".jar")) {
//                    jar = file;
//                }
//            }
//            if (pom != null) {
//                if (jar != null) {
//                    deploy(pom, jar, source, javadoc);
//                } else if (packingIsPom(pom)) {
//                    deployPom(pom);
//                }
//            }
//        }
//    }


    public static void deploy(File[] files) {
        MyFilter myFilter = new MyFilter();
        if (files.length == 0) {
            //ignore
        } else if (files[0].isDirectory() && files[0].listFiles() != null && files[0].listFiles(myFilter).length > 0 && files[0].listFiles(myFilter)[0].isFile()) {
            String groupId = files[0].getParentFile().getParentFile().getParentFile().getName();
            String artifactId = files[0].getParentFile().getParentFile().getName();
            String version = files[0].getParentFile().getName();
            File aar = null;
            File pom = null;
            File jar = null;
            File source = null;
            File javadoc = null;
            //忽略日期快照版本，如 xxx-mySql-2.2.6-20170714.095105-1.jar

            for (File listFile : files) {
                if (listFile != null && listFile.isDirectory() && listFile.listFiles(myFilter) != null) {
                    for (File file : listFile.listFiles(myFilter)) {
                        String name = file.getName();
                        if (DATE_PATTERN.matcher(name).find()) {
                            //skip
                        } else if (name.endsWith(".aar")) {
                            aar = file;
                        } else if (name.endsWith(".pom")) {
                            pom = file;
                        } else if (name.endsWith("-javadoc.jar")) {
                            javadoc = file;
                        } else if (name.endsWith("-sources.jar")) {
                            source = file;
                        } else if (name.endsWith(".jar")) {
                            jar = file;
                        }
                    }
                }
            }


            if (aar != null) {
                deploy(aar, jar, source, pom, javadoc, groupId, artifactId, version);
            } else if (jar != null) {
                deploy(aar, jar, source, pom, javadoc, groupId, artifactId, version);
            }
        } else if (files[0].isDirectory()) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deploy(file.listFiles(myFilter));
                }
            }
        } /*else if (files[0].isFile()) {
            String groupId = files[0].getParentFile().getParentFile().getParentFile().getParentFile().getName();
            String artifactId = files[0].getParentFile().getParentFile().getParentFile().getName();
            String version = files[0].getParentFile().getParentFile().getName();
            File aar = null;
            File jar = null;
            File source = null;
            File javadoc = null;
            //忽略日期快照版本，如 xxx-mySql-2.2.6-20170714.095105-1.jar
            File[] listFiles = files[0].getParentFile().getParentFile().listFiles();
            if (listFiles != null && listFiles.length > 0) {
                for (File listFile : listFiles) {
                    for (File file : listFile.listFiles()) {
                        String name = file.getName();
                        if (DATE_PATTERN.matcher(name).find()) {
                            //skip
                        } else if (name.endsWith(".aar")) {
                            aar = file;
                        } else if (name.endsWith("-javadoc.jar")) {
                            javadoc = file;
                        } else if (name.endsWith("-sources.jar")) {
                            source = file;
                        } else if (name.endsWith(".jar")) {
                            jar = file;
                        }
                    }
                }
            }

            if (aar != null) {
                deploy(aar, jar, source, javadoc, groupId, artifactId, version);
            } else if (jar != null) {
                deploy(aar, jar, source, javadoc, groupId, artifactId, version);
            }
        }*/
    }


    /**
     * 过滤隐藏文件
     */
    public static class MyFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            if (file.isDirectory())
                return true;
            else {
                String name = file.getName();
                if (!name.startsWith("."))
                    return true;
                else
                    return false;
            }

        }
    }

    public static boolean packingIsPom(File pom) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(pom)));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().indexOf("<packaging>pom</packaging>") != -1) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
            }
        }
        return false;
    }

    public static void deployPom(final File pom) {
        EXECUTOR_SERVICE.execute(new Runnable() {
            @Override
            public void run() {
                StringBuffer cmd = new StringBuffer(BASE_CMD);
                cmd.append(" -DpomFile=").append(pom.getName());
                cmd.append(" -Dfile=").append(pom.getName());
                System.out.println("pomPath=" + pom.getAbsolutePath());
                try {
                    final Process proc = CMD.exec(cmd.toString(), null, pom.getParentFile());
                    InputStream inputStream = proc.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader reader = new BufferedReader(inputStreamReader);
                    String line;
                    StringBuffer logBuffer = new StringBuffer();
                    logBuffer.append("\n\n\n==================================\n");
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("[INFO]") || line.startsWith("Upload")) {
                            logBuffer.append(Thread.currentThread().getName() + " : " + line + "\n");
                        }
                    }
                    System.out.println(logBuffer);
                    int result = proc.waitFor();
                    if (result != 0) {
                        error("上传失败：" + pom.getAbsolutePath());
                    }
                } catch (IOException e) {
                    error("上传失败：" + pom.getAbsolutePath());
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    error("上传失败：" + pom.getAbsolutePath());
                    e.printStackTrace();
                }
            }
        });
    }

    public static void deploy(final File pom, final File jar, final File source, final File javadoc) {
        EXECUTOR_SERVICE.execute(new Runnable() {
            @Override
            public void run() {
                StringBuffer cmd = new StringBuffer(BASE_CMD);
                cmd.append(" -DpomFile=").append(pom.getName());
                System.out.println("pomPath=" + pom.getAbsolutePath());
                if (jar != null) {
                    //当有bundle类型时，下面的配置可以保证上传的jar包后缀为.jar
                    cmd.append(" -Dpackaging=jar -Dfile=").append(jar.getName());
                } else {
                    cmd.append(" -Dfile=").append(pom.getName());
                }
                if (source != null) {
                    cmd.append(" -Dsources=").append(source.getName());
                }
                if (javadoc != null) {
                    cmd.append(" -Djavadoc=").append(javadoc.getName());
                }

                try {
                    final Process proc = CMD.exec(cmd.toString(), null, pom.getParentFile());
                    InputStream inputStream = proc.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader reader = new BufferedReader(inputStreamReader);
                    String line;
                    StringBuffer logBuffer = new StringBuffer();
                    logBuffer.append("\n\n\n=======================================\n");
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("[INFO]") || line.startsWith("Upload")) {
                            logBuffer.append(Thread.currentThread().getName() + " : " + line + "\n");
                        }
                    }
                    System.out.println(logBuffer);
                    int result = proc.waitFor();
                    if (result != 0) {
                        error("上传失败：" + pom.getAbsolutePath());
                    }
                } catch (IOException e) {
                    error("上传失败：" + pom.getAbsolutePath());
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    error("上传失败：" + pom.getAbsolutePath());
                    e.printStackTrace();
                }
            }
        });
    }


    public static void deploy(final File aar, final File jar, final File source, final File pom, final File javadoc,
                              String groupId, String artifactId, String version) {
        EXECUTOR_SERVICE.execute(new Runnable() {
            @Override
            public void run() {
                StringBuffer cmd = new StringBuffer(BASE_CMD);
//                cmd.append(" -DpomFile=").append(pom.getName());
                cmd.append(" -DgroupId=").append(groupId);
                cmd.append(" -DartifactId=").append(artifactId);
                cmd.append(" -Dversion=").append(version);
                cmd.append(" -Durl=").append(DUrl);
                cmd.append(" -DrepositoryId=").append(DrepositoryId);
                if (aar != null) {
                    cmd.append(" -Dpackaging=aar -Dfile=").append(aar.getAbsolutePath());
                } else if (jar != null) {
                    //当有bundle类型时，下面的配置可以保证上传的jar包后缀为.jar
                    cmd.append(" -Dpackaging=jar -Dfile=").append(jar.getAbsolutePath());
                } else {
//                    cmd.append(" -Dfile=").append(pom.getName());
                }
                if (source != null) {
                    cmd.append(" -Dsources=").append(source.getAbsolutePath());
                }
                if (javadoc != null) {
                    cmd.append(" -Djavadoc=").append(javadoc.getAbsolutePath());
                }

                if (pom != null) {
                    cmd.append(" -DpomFile=").append(pom.getAbsolutePath());
                    cmd.append(" -DgeneratePom=false ");
                } else {
                    cmd.append(" -DgeneratePom=true ");
                }

                try {
                    final Process proc = CMD.exec(cmd.toString(), null, aar != null ? aar.getParentFile() : jar != null ? jar.getParentFile() : new File("~"));
                    InputStream inputStream = proc.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader reader = new BufferedReader(inputStreamReader);
                    String line;
                    StringBuffer logBuffer = new StringBuffer();
                    logBuffer.append("\n\n\n=======================================\n");
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("[INFO]") || line.startsWith("Upload")) {
                            logBuffer.append(Thread.currentThread().getName() + " : " + line + "\n");
                        }
                    }
                    System.out.println(logBuffer);
                    int result = proc.waitFor();
                    if (result != 0) {
                        error("上传失败：" + (aar != null ? aar.getAbsolutePath() : jar != null ? jar.getAbsolutePath() : ""));
                    }
                } catch (IOException e) {
                    error("上传失败：" + (aar != null ? aar.getAbsolutePath() : jar != null ? jar.getAbsolutePath() : ""));
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    error("上传失败：" + (aar != null ? aar.getAbsolutePath() : jar != null ? jar.getAbsolutePath() : ""));
                    e.printStackTrace();
                }
            }
        });
    }
}