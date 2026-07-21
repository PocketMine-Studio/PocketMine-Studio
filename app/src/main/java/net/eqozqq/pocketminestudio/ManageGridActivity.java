package net.eqozqq.pocketminestudio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.widget.Toolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ManageGridActivity extends BaseActivity {

    private File targetDir;
    private String initialPath;
    private RecyclerView recyclerView;
    private GridAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_grid);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        android.widget.ImageButton navBack = findViewById(R.id.nav_back);
        android.widget.TextView toolbarTitle = findViewById(R.id.toolbar_title);
        
        String path = getIntent().getStringExtra("path");
        String title = getIntent().getStringExtra("title");
        if (path == null) {
            finish();
            return;
        }

        toolbarTitle.setText(title != null ? title : "Manager");
        initialPath = path;
        targetDir = new File(path);
        
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!targetDir.getAbsolutePath().equals(initialPath) && targetDir.getParentFile() != null) {
                    targetDir = targetDir.getParentFile();
                    String t = getIntent().getStringExtra("title");
                    toolbarTitle.setText(targetDir.getAbsolutePath().equals(initialPath) ? (t != null ? t : "Manager") : targetDir.getName());
                    loadFiles();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
        
        navBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        if (!targetDir.exists()) targetDir.mkdirs();

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new GridAdapter();
        recyclerView.setAdapter(adapter);

        loadFiles();
    }

    private void loadFiles() {
        File[] files = targetDir.listFiles();
        List<File> fileList = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                fileList.add(f);
            }
        }
        Collections.sort(fileList, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });
        adapter.setFiles(fileList);
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private long getFolderSize(File file) {
        long size = 0;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    size += getFolderSize(child);
                }
            }
        } else {
            size = file.length();
        }
        return size;
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    private void showItemMenu(File file) {
        String[] actions = {getString(R.string.action_rename), getString(R.string.action_delete), getString(R.string.action_archive)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(file.getName())
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        showRenameDialog(file);
                    } else if (which == 1) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(getString(R.string.auto_text_delete))
                                .setMessage(String.format(getString(R.string.msg_delete_confirm), file.getName()))
                                .setPositiveButton(getString(R.string.btn_delete), (d, w) -> {
                                    deleteRecursive(file);
                                    loadFiles();
                                })
                                .setNegativeButton(getString(R.string.btn_cancel), null)
                                .show();
                    } else if (which == 2) {
                        showArchiveDialog(file);
                    }
                }).show();
    }

    private void showRenameDialog(File file) {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_input, null);
        com.google.android.material.textfield.TextInputLayout til = dialogView.findViewById(R.id.dialog_til);
        til.setHint(getString(R.string.input_name_hint));
        com.google.android.material.textfield.TextInputEditText input = dialogView.findViewById(R.id.dialog_input);
        input.setText(file.getName());
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.action_rename))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.btn_rename), (d, w) -> {
                    file.renameTo(new File(targetDir, input.getText().toString()));
                    loadFiles();
                }).show();
    }

    private void showArchiveDialog(File file) {
        String[] formats = {".zip", ".tar.gz", ".tar"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.dialog_archive_format))
                .setItems(formats, (dialog, which) -> {
                    new Thread(() -> {
                        try {
                            if (which == 0) archiveZip(file);
                            else if (which == 1) archiveTarGz(file);
                            else archiveTar(file);
                            
                            runOnUiThread(() -> {
                                Toast.makeText(this, getString(R.string.msg_archived_success), Toast.LENGTH_SHORT).show();
                                loadFiles();
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> Toast.makeText(this, getString(R.string.msg_failed_to_archive), Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                }).show();
    }

    private void archiveZip(File file) throws Exception {
        File zipFile = new File(file.getParent(), file.getName() + ".zip");
        java.io.FileOutputStream fos = new java.io.FileOutputStream(zipFile);
        java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos);
        addFileToZip(file, file.getName(), zos);
        zos.close();
        fos.close();
    }

    private void addFileToZip(File fileToZip, String fileName, java.util.zip.ZipOutputStream zos) throws Exception {
        if (fileToZip.isHidden()) return;
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) zos.putNextEntry(new java.util.zip.ZipEntry(fileName));
            else zos.putNextEntry(new java.util.zip.ZipEntry(fileName + "/"));
            zos.closeEntry();
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    addFileToZip(childFile, fileName + "/" + childFile.getName(), zos);
                }
            }
            return;
        }
        java.io.FileInputStream fis = new java.io.FileInputStream(fileToZip);
        java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(fileName);
        zos.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }
        fis.close();
    }

    private void archiveTar(File file) throws Exception {
        File tarFile = new File(file.getParent(), file.getName() + ".tar");
        java.io.FileOutputStream fos = new java.io.FileOutputStream(tarFile);
        org.apache.commons.compress.archivers.tar.TarArchiveOutputStream taos = new org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(fos);
        taos.setLongFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU);
        addFileToTar(file, file.getName(), taos);
        taos.finish();
        taos.close();
        fos.close();
    }

    private void archiveTarGz(File file) throws Exception {
        File tgzFile = new File(file.getParent(), file.getName() + ".tar.gz");
        java.io.FileOutputStream fos = new java.io.FileOutputStream(tgzFile);
        java.util.zip.GZIPOutputStream gos = new java.util.zip.GZIPOutputStream(fos);
        org.apache.commons.compress.archivers.tar.TarArchiveOutputStream taos = new org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(gos);
        taos.setLongFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU);
        addFileToTar(file, file.getName(), taos);
        taos.finish();
        taos.close();
        gos.close();
        fos.close();
    }

    private void addFileToTar(File fileToTar, String fileName, org.apache.commons.compress.archivers.tar.TarArchiveOutputStream taos) throws Exception {
        if (fileToTar.isHidden()) return;
        org.apache.commons.compress.archivers.tar.TarArchiveEntry entry = new org.apache.commons.compress.archivers.tar.TarArchiveEntry(fileToTar, fileName);
        taos.putArchiveEntry(entry);
        if (fileToTar.isFile()) {
            java.io.FileInputStream fis = new java.io.FileInputStream(fileToTar);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                taos.write(bytes, 0, length);
            }
            fis.close();
        }
        taos.closeArchiveEntry();
        if (fileToTar.isDirectory()) {
            File[] children = fileToTar.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    addFileToTar(childFile, fileName + "/" + childFile.getName(), taos);
                }
            }
        }
    }

    private class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> {
        private List<File> files = new ArrayList<>();

        public void setFiles(List<File> files) {
            this.files = files;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grid_file, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            File file = files.get(position);
            holder.name.setText(file.getName());
            if (file.isDirectory()) {
                holder.icon.setImageResource(R.drawable.ic_folder_24px);
                holder.size.setText(getString(R.string.auto_java_));
                new Thread(() -> {
                    long size = getFolderSize(file);
                    holder.itemView.post(() -> {
                        holder.size.setText(formatFileSize(size));
                    });
                }).start();
            } else {
                holder.icon.setImageResource(R.drawable.ic_description_24px);
                holder.size.setText(formatFileSize(file.length()));
            }

            holder.itemView.setOnLongClickListener(v -> {
                showItemMenu(file);
                return true;
            });
            
            holder.itemView.setOnClickListener(v -> {
                if (file.isDirectory()) {
                    targetDir = file;
                    android.widget.TextView tbTitle = ((android.app.Activity) v.getContext()).findViewById(R.id.toolbar_title);
                    if (tbTitle != null) {
                        tbTitle.setText(file.getName());
                    }
                    loadFiles();
                } else {
                    if (file.getName().endsWith(".phar") || file.getName().endsWith(".gz") || file.getName().endsWith(".tar")
                            || file.getName().endsWith(".zip") || file.getName().endsWith(".so")) {
                        try {
                            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(ManageGridActivity.this, getPackageName() + ".provider", file);
                            String mimeType = "*/*";
                            if (file.getName().endsWith(".zip")) mimeType = "application/zip";
                            else if (file.getName().endsWith(".tar") || file.getName().endsWith(".gz")) mimeType = "application/x-tar";
                            else if (file.getName().endsWith(".phar")) mimeType = "application/octet-stream";
                            intent.setDataAndType(uri, mimeType);
                            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(android.content.Intent.createChooser(intent, "Open file"));
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(ManageGridActivity.this, getString(R.string.msg_cannot_open_file), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        android.content.Intent intent = new android.content.Intent(ManageGridActivity.this, CodeEditorActivity.class);
                        intent.putExtra("filePath", file.getAbsolutePath());
                        startActivity(intent);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return files.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView name;
            TextView size;

            ViewHolder(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.icon);
                name = itemView.findViewById(R.id.name);
                size = itemView.findViewById(R.id.size);
            }
        }
    }
}
