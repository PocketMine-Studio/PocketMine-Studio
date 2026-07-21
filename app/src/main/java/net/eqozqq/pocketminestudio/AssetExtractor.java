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
