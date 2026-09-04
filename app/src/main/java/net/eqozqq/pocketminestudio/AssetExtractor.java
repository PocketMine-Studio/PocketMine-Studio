package net.eqozqq.pocketminestudio;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class AssetExtractor {
    public static void extractAssets(Context context) {
        AssetManager assetManager = context.getAssets();
        String[] files = null;
        try {
            files = assetManager.list("php");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (files != null && files.length > 0) {
            File destDir = new File(context.getFilesDir(), "php");
            if (!destDir.exists()) {
                destDir.mkdirs();
            }

            for (String filename : files) {
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = assetManager.open("php/" + filename);
                    File outFile = new File(destDir, filename);
                    out = new FileOutputStream(outFile);
                    copyFile(in, out);
                    if (filename.equals("php")) {
                        outFile.setExecutable(true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (in != null) in.close();
                        if (out != null) out.close();
                    } catch (Exception e) {}
                }
            }
            File iniFile = new File(destDir, "php.ini");
            File bootstrapFile = new File(destDir, "bootstrap.php");
            if (iniFile.exists() && bootstrapFile.exists()) {
                try {
                    String content = new String(java.nio.file.Files.readAllBytes(iniFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                    content = content.replace("auto_prepend_file=bootstrap.php", "auto_prepend_file=" + bootstrapFile.getAbsolutePath());
                    if (!content.contains("include_path")) {
                        content += "\ninclude_path=\".:" + destDir.getAbsolutePath() + "\"\n";
                    }
                    java.nio.file.Files.write(iniFile.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } catch (Exception e) {}
            }
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}
