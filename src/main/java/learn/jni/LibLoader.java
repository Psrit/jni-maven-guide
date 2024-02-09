package learn.jni;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LibLoader {
    private static String getOsName() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("mac")) {
            return "darwin";
        } else if (osName.contains("win")) {
            return "windows";
        } else {
            return "linux";
        }
    }

    private static String getArchName() {
        String osArch = System.getProperty("os.arch");
        if (osArch.equals("amd64") || osArch.equals("x86_64")) {
            return "x64";
        } else if (osArch.equals("aarch64")) {
            return "arm64";
        } else {
            return "x86";
        }
    }

    private static String getLibSuffix() {
        if (getOsName() == "windows") {
            return ".dll";
        } else if (getOsName() == "darwin") {
            return ".dylib";
        } else {
            return ".so";
        }
    }

    public static void load(String libName) throws IOException {
        InputStream in = LibLoader.class.getResourceAsStream(
                String.format("/%s-%s/%s%s", getOsName(), getArchName(), libName, getLibSuffix()));
        File libTempFile = File.createTempFile(libName, getLibSuffix());
        libTempFile.deleteOnExit();

        try (OutputStream out = new FileOutputStream(libTempFile)) {
            byte[] buffer = new byte[2048];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }

        System.load(libTempFile.getCanonicalPath());
    }
}
