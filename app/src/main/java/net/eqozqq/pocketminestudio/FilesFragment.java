package net.eqozqq.pocketminestudio;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import java.io.FileOutputStream;
import java.io.InputStream;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.appcompat.widget.Toolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FilesFragment extends Fragment {
    private static final int REQUEST_UPLOAD = 101;


    private LinearLayout filesList;
    private Toolbar toolbar;
    private File currentDir;
    private File rootDir;
    private java.util.Set<File> clipboardFiles = new java.util.HashSet<>();
    private java.util.Set<File> selectedFiles = new java.util.HashSet<>();
    private View view;
    private LinearLayout selectionBar;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!selectedFiles.isEmpty()) {
                    selectedFiles.clear();
                    loadDirectory();
                } else if (!currentDir.equals(rootDir) && currentDir.getParentFile() != null) {
                    currentDir = currentDir.getParentFile();
                    loadDirectory();
                } else {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_files, container, false);
        filesList = view.findViewById(R.id.files_list);
        toolbar = view.findViewById(R.id.files_toolbar);
        selectionBar = view.findViewById(R.id.selection_bar);

        view.findViewById(R.id.btn_bulk_delete).setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(getString(R.string.dialog_delete_confirmation))
                    .setMessage(getString(R.string.dialog_delete_items_message, selectedFiles.size()))
                    .setPositiveButton(getString(R.string.auto_text_delete), (d, w) -> {
                        for (File f : selectedFiles) {
                            deleteRecursive(f);
                        }
                        selectedFiles.clear();
                        loadDirectory();
                    })
                    .setNegativeButton(getString(R.string.config_cancel), null)
                    .show();
        });

        view.findViewById(R.id.btn_bulk_archive).setOnClickListener(v -> {
            showBulkArchiveDialog();
        });

        view.findViewById(R.id.btn_bulk_copy).setOnClickListener(v -> {
            clipboardFiles.clear();
            clipboardFiles.addAll(selectedFiles);
            selectedFiles.clear();
            Toast.makeText(requireActivity(), getString(R.string.msg_copied_items, clipboardFiles.size()), Toast.LENGTH_SHORT).show();
            loadDirectory();
        });

        view.findViewById(R.id.btn_clear_selection).setOnClickListener(v -> {
            selectedFiles.clear();
            loadDirectory();
        });

        rootDir = new File(ServerUtils.getDataDirectory());
        currentDir = rootDir;

        view.findViewById(R.id.nav_back).setOnClickListener(v -> {
            if (!selectedFiles.isEmpty()) {
                selectedFiles.clear();
                loadDirectory();
            } else if (!currentDir.equals(rootDir) && currentDir.getParentFile() != null) {
                currentDir = currentDir.getParentFile();
                loadDirectory();
            }
        });

        
        view.findViewById(R.id.addButton).setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_file, null);
            androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(getString(R.string.action_add))
                .setView(dialogView)
                .create();

            dialogView.findViewById(R.id.btn_add_upload).setOnClickListener(btnView -> {
                dialog.dismiss();
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, REQUEST_UPLOAD);
            });
            dialogView.findViewById(R.id.btn_add_folder).setOnClickListener(btnView -> {
                dialog.dismiss();
                showCreateDialog(true);
            });
            dialogView.findViewById(R.id.btn_add_file).setOnClickListener(btnView -> {
                dialog.dismiss();
                showCreateDialog(false);
            });

            dialog.show();
        });

        loadDirectory();
        return view;
    }

    
    private void showCreateDialog(boolean isFolder) {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_input, null);
        com.google.android.material.textfield.TextInputLayout til = dialogView.findViewById(R.id.dialog_til);
        til.setHint(getString(R.string.input_name_hint));
        com.google.android.material.textfield.TextInputEditText input = dialogView.findViewById(R.id.dialog_input);
        
        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(isFolder ? getString(R.string.action_create_folder) : getString(R.string.action_create_file))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.action_add), (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        File newFile = new File(currentDir, name);
                        try {
                            if (isFolder) {
                                newFile.mkdirs();
                            } else {
                                newFile.createNewFile();
                            }
                            Toast.makeText(requireActivity(), getString(R.string.msg_file_created), Toast.LENGTH_SHORT).show();
                            loadDirectory();
                        } catch (Exception e) {
                            Toast.makeText(requireActivity(), getString(R.string.msg_file_creation_failed), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_UPLOAD && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    String fileName = "uploaded_file";
                    android.database.Cursor cursor = requireActivity().getContentResolver().query(uri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                        if (index != -1) {
                            fileName = cursor.getString(index);
                        }
                        cursor.close();
                    }
                    File dest = new File(currentDir, fileName);
                    InputStream in = requireActivity().getContentResolver().openInputStream(uri);
                    FileOutputStream out = new FileOutputStream(dest);
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    in.close();
                    out.close();
                    Toast.makeText(requireActivity(), getString(R.string.msg_file_created), Toast.LENGTH_SHORT).show();
                    loadDirectory();
                } catch (Exception e) {
                    Toast.makeText(requireActivity(), getString(R.string.msg_file_creation_failed), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void loadDirectory() {
        if (!currentDir.exists())
            currentDir.mkdirs();

        selectionBar.setVisibility(selectedFiles.isEmpty() ? View.GONE : View.VISIBLE);

        if (currentDir.equals(rootDir)) {
            ((android.widget.TextView) view.findViewById(R.id.toolbar_title)).setText(getString(R.string.auto_text_files));
            view.findViewById(R.id.nav_back).setVisibility(android.view.View.GONE);
        } else {
            ((android.widget.TextView) view.findViewById(R.id.toolbar_title)).setText(currentDir.getName());
            view.findViewById(R.id.nav_back).setVisibility(android.view.View.VISIBLE);
        }

        filesList.removeAllViews();

        File[] files = currentDir.listFiles();
        List<File> fileList = new ArrayList<>();
        if (files != null) {
            Collections.addAll(fileList, files);
        }

        Collections.sort(fileList, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory())
                return -1;
            if (!f1.isDirectory() && f2.isDirectory())
                return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        for (File file : fileList) {
            LinearLayout item = new LinearLayout(requireActivity());
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setPadding(30, 40, 30, 40);
            item.setBackgroundResource(R.drawable.bg_rounded);
            item.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(10, 10, 10, 10);
            item.setLayoutParams(params);
            item.setClickable(true);

            android.widget.ImageView icon = new android.widget.ImageView(requireActivity());
            if (selectedFiles.contains(file)) {
                icon.setImageResource(R.drawable.ic_check_small_24px);
                item.setBackgroundResource(R.drawable.bg_rounded_selected); // Use rounded background for selected items
            } else {
                icon.setImageResource(file.isDirectory() ? R.drawable.ic_folder_24px : R.drawable.ic_draft_24px);
                item.setBackgroundResource(R.drawable.bg_rounded);
            }
            android.widget.LinearLayout.LayoutParams iconParams = new android.widget.LinearLayout.LayoutParams(
                    (int) (24 * getResources().getDisplayMetrics().density),
                    (int) (24 * getResources().getDisplayMetrics().density));
            iconParams.setMargins(0, 0, 30, 0);
            icon.setLayoutParams(iconParams);
            item.addView(icon);

            LinearLayout textLayout = new LinearLayout(requireActivity());
            textLayout.setOrientation(LinearLayout.VERTICAL);
            textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            TextView name = new TextView(requireActivity());
            name.setText(file.getName());
            name.setTextSize(16);
            android.content.res.TypedArray a = requireActivity()
                    .obtainStyledAttributes(new int[] { android.R.attr.textColorPrimary });
            name.setTextColor(a.getColor(0, android.graphics.Color.BLACK));
            a.recycle();
            textLayout.addView(name);

            TextView size = new TextView(requireActivity());
            size.setText(formatFileSize(file.length()));
            size.setTextSize(12);
            size.setTextColor(android.graphics.Color.GRAY);
            size.setVisibility(file.isDirectory() ? View.GONE : View.VISIBLE);
            textLayout.addView(size);

            item.addView(textLayout);

            item.setOnClickListener(v -> {
                if (!selectedFiles.isEmpty()) {
                    if (selectedFiles.contains(file))
                        selectedFiles.remove(file);
                    else
                        selectedFiles.add(file);
                    loadDirectory();
                } else {
                    if (file.isDirectory()) {
                        currentDir = file;
                        loadDirectory();
                    } else {
                        openFileEditor(file);
                    }
                }
            });

            item.setOnLongClickListener(v -> {
                showFileMenu(file);
                return true;
            });

            filesList.addView(item);
        }

        if (!clipboardFiles.isEmpty()) {
            com.google.android.material.button.MaterialButton pasteBtn = new com.google.android.material.button.MaterialButton(
                    requireActivity());
            pasteBtn.setText(getString(R.string.action_paste_items, clipboardFiles.size()));
            pasteBtn.setOnClickListener(v -> {
                try {
                    for (File cFile : clipboardFiles) {
                        File dest = new File(currentDir, cFile.getName());
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            Files.copy(cFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
                        Toast.makeText(requireActivity(), getString(R.string.msg_copying_requires_android), Toast.LENGTH_SHORT).show();
                    }
                    clipboardFiles.clear();
                    loadDirectory();
                } catch (Exception e) {
                    Toast.makeText(requireActivity(), getString(R.string.msg_failed_to_paste), Toast.LENGTH_SHORT).show();
                }
            });
            filesList.addView(pasteBtn);
        }
    }

    private void showFileMenu(File file) {
        View dialogView = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_file_edit, null);
        TextView tvFileName = dialogView.findViewById(R.id.toolbar_file_name);
        if (tvFileName != null) {
            tvFileName.setText(file.getName());
        }

        android.app.Dialog dialog = new android.app.Dialog(requireActivity());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.y = (int) (80 * requireActivity().getResources().getDisplayMetrics().density);
            dialog.getWindow().setAttributes(params);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setWindowAnimations(android.R.style.Animation_Dialog);
        }

        View.OnLongClickListener tooltipListener = v -> {
            CharSequence desc = v.getContentDescription();
            if (desc != null && desc.length() > 0) {
                Toast.makeText(requireActivity(), desc, Toast.LENGTH_SHORT).show();
            }
            return true;
        };

        View btnCopy = dialogView.findViewById(R.id.btn_action_copy);
        View btnRename = dialogView.findViewById(R.id.btn_action_rename);
        View btnPermissions = dialogView.findViewById(R.id.btn_action_permissions);
        View btnArchive = dialogView.findViewById(R.id.btn_action_archive);
        View btnDownload = dialogView.findViewById(R.id.btn_action_download);
        View btnSelect = dialogView.findViewById(R.id.btn_action_select);
        View btnDelete = dialogView.findViewById(R.id.btn_action_delete);

        View[] buttons = {btnCopy, btnRename, btnPermissions, btnArchive, btnDownload, btnSelect, btnDelete};
        for (View btn : buttons) {
            if (btn != null) btn.setOnLongClickListener(tooltipListener);
        }

        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> {
                dialog.dismiss();
                clipboardFiles.clear();
                clipboardFiles.add(file);
                Toast.makeText(requireActivity(), getString(R.string.msg_file_copied), Toast.LENGTH_SHORT).show();
                loadDirectory();
            });
        }
        if (btnRename != null) {
            btnRename.setOnClickListener(v -> {
                dialog.dismiss();
                showRenameDialog(file);
            });
        }
        if (btnPermissions != null) {
            btnPermissions.setOnClickListener(v -> {
                dialog.dismiss();
                showPermissionsDialog(file);
            });
        }
        if (btnArchive != null) {
            btnArchive.setOnClickListener(v -> {
                dialog.dismiss();
                showArchiveDialog(file);
            });
        }
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> {
                dialog.dismiss();
                saveFileToDownloads(file);
            });
        }
        if (btnSelect != null) {
            btnSelect.setOnClickListener(v -> {
                dialog.dismiss();
                if (selectedFiles.contains(file)) {
                    selectedFiles.remove(file);
                } else {
                    selectedFiles.add(file);
                }
                loadDirectory();
            });
        }
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                dialog.dismiss();
                new MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(getString(R.string.dialog_delete_confirmation))
                        .setMessage(getString(R.string.dialog_delete_item_message, file.getName()))
                        .setPositiveButton(getString(R.string.auto_text_delete), (d, w) -> {
                            deleteRecursive(file);
                            selectedFiles.remove(file);
                            loadDirectory();
                        })
                        .setNegativeButton(getString(R.string.config_cancel), null)
                        .show();
            });
        }

        dialog.show();
    }

    private void saveFileToDownloads(File file) {
        try {
            File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            File dest = new File(downloadsDir, file.getName());
            FileInputStream is = new FileInputStream(file);
            FileOutputStream os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
            os.close();
            is.close();
            Toast.makeText(requireActivity(), String.format(getString(R.string.msg_file_downloaded), file.getName()), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireActivity(), getString(R.string.msg_file_download_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void showArchiveDialog(File file) {
        String[] formats = { ".zip", ".tar.gz", ".tar" };
        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(getString(R.string.dialog_archive_format))
                .setItems(formats, (dialog, which) -> {
                    String format = formats[which];
                    new Thread(() -> {
                        try {
                            if (which == 0)
                                archiveZip(file);
                            else if (which == 1)
                                archiveTarGz(file);
                            else
                                archiveTar(file);

                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireActivity(), getString(R.string.msg_archived_success), Toast.LENGTH_SHORT).show();
                                loadDirectory();
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireActivity(), getString(R.string.msg_failed_to_archive), Toast.LENGTH_SHORT).show();
                            });
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
        if (fileToZip.isHidden())
            return;
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/"))
                zos.putNextEntry(new java.util.zip.ZipEntry(fileName));
            else
                zos.putNextEntry(new java.util.zip.ZipEntry(fileName + "/"));
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
        org.apache.commons.compress.archivers.tar.TarArchiveOutputStream taos = new org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(
                fos);
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
        org.apache.commons.compress.archivers.tar.TarArchiveOutputStream taos = new org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(
                gos);
        taos.setLongFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU);
        addFileToTar(file, file.getName(), taos);
        taos.finish();
        taos.close();
        gos.close();
        fos.close();
    }

    private void addFileToTar(File fileToTar, String fileName,
            org.apache.commons.compress.archivers.tar.TarArchiveOutputStream taos) throws Exception {
        if (fileToTar.isHidden())
            return;
        org.apache.commons.compress.archivers.tar.TarArchiveEntry entry = new org.apache.commons.compress.archivers.tar.TarArchiveEntry(
                fileToTar, fileName);
        taos.putArchiveEntry(entry);
        if (fileToTar.isFile()) {
            java.io.FileInputStream fis = new java.io.FileInputStream(fileToTar);
            org.apache.commons.compress.utils.IOUtils.copy(fis, taos);
            fis.close();
            taos.closeArchiveEntry();
        } else if (fileToTar.isDirectory()) {
            taos.closeArchiveEntry();
            File[] children = fileToTar.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    addFileToTar(childFile, fileName + "/" + childFile.getName(), taos);
                }
            }
        }
    }

    private void showRenameDialog(File file) {
        android.view.View view = android.view.LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_input, null);
        com.google.android.material.textfield.TextInputEditText input = view.findViewById(R.id.dialog_input);
        input.setText(file.getName());
        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(getString(R.string.action_rename))
                .setView(view)
                .setPositiveButton(getString(R.string.action_rename), (d, w) -> {
                    file.renameTo(new File(currentDir, input.getText().toString()));
                    loadDirectory();
                }).show();
    }

    private void showPermissionsDialog(File file) {
        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(getString(R.string.action_permissions))
                .setMessage(getString(R.string.dialog_make_executable))
                .setPositiveButton("Yes", (d, w) -> {
                    file.setExecutable(true);
                    Toast.makeText(requireActivity(), getString(R.string.msg_permission_granted), Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    private void openFileEditor(File file) {
        String mimeType = java.net.URLConnection.guessContentTypeFromName(file.getName());
        boolean isText = file.getName().endsWith(".php") || file.getName().endsWith(".json") || file.getName().endsWith(".yml") 
                         || file.getName().endsWith(".yaml") || file.getName().endsWith(".properties") || file.getName().endsWith(".md")
                         || file.getName().endsWith(".log") || file.getName().endsWith(".ini");
                         
        if (mimeType != null && mimeType.startsWith("text/")) isText = true;

        if (!isText || file.getName().endsWith(".phar") || file.getName().endsWith(".gz") || file.getName().endsWith(".tar")
                || file.getName().endsWith(".zip") || file.getName().endsWith(".so")) {
                
            try {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(requireActivity(), requireActivity().getPackageName() + ".provider", file);
                
                if (mimeType == null) mimeType = "*/*";
                if (file.getName().endsWith(".zip")) mimeType = "application/zip";
                else if (file.getName().endsWith(".tar") || file.getName().endsWith(".gz")) mimeType = "application/x-tar";
                else if (file.getName().endsWith(".phar")) mimeType = "application/octet-stream";
                
                intent.setDataAndType(uri, mimeType);
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(android.content.Intent.createChooser(intent, getString(R.string.action_open_with)));
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireActivity(), getString(R.string.msg_cannot_open_file), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        try {
            android.content.Intent intent = new android.content.Intent(requireActivity(), CodeEditorActivity.class);
            intent.putExtra("filePath", file.getAbsolutePath());
            startActivity(intent);
        } catch (Exception e) {
            try {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(requireActivity(), requireActivity().getPackageName() + ".provider", file);
                intent.setDataAndType(uri, "*/*");
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(android.content.Intent.createChooser(intent, getString(R.string.action_open_with)));
            } catch (Exception ex) {
                Toast.makeText(requireActivity(), getString(R.string.auto_java_cannot_open_file), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showBulkArchiveDialog() {
        String[] formats = { ".zip", ".tar.gz", ".tar" };
        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle("Archive " + selectedFiles.size() + " items as...")
                .setItems(formats, (dialog, which) -> {
                    new Thread(() -> {
                        try {
                            if (which == 0)
                                archiveBulkZip(selectedFiles);
                            else if (which == 1)
                                archiveBulkTarGz(selectedFiles);
                            else
                                archiveBulkTar(selectedFiles);

                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireActivity(), getString(R.string.msg_archived_success), Toast.LENGTH_SHORT).show();
                                selectedFiles.clear();
                                loadDirectory();
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireActivity(), getString(R.string.msg_failed_to_archive), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                }).show();
    }

    private void archiveBulkZip(java.util.Set<File> files) throws Exception {
        File zipFile = new File(currentDir, "archive_" + System.currentTimeMillis() + ".zip");
        java.io.FileOutputStream fos = new java.io.FileOutputStream(zipFile);
        java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos);
        for (File f : files) {
            addFileToZip(f, f.getName(), zos);
        }
        zos.close();
        fos.close();
    }

    private void archiveBulkTar(java.util.Set<File> files) throws Exception {
        File tarFile = new File(currentDir, "archive_" + System.currentTimeMillis() + ".tar");
        java.io.FileOutputStream fos = new java.io.FileOutputStream(tarFile);
        org.apache.commons.compress.archivers.tar.TarArchiveOutputStream taos = new org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(
                fos);
        taos.setLongFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU);
        for (File f : files) {
            addFileToTar(f, f.getName(), taos);
        }
        taos.finish();
        taos.close();
        fos.close();
    }

    private void archiveBulkTarGz(java.util.Set<File> files) throws Exception {
        File tgzFile = new File(currentDir, "archive_" + System.currentTimeMillis() + ".tar.gz");
        java.io.FileOutputStream fos = new java.io.FileOutputStream(tgzFile);
        java.util.zip.GZIPOutputStream gos = new java.util.zip.GZIPOutputStream(fos);
        org.apache.commons.compress.archivers.tar.TarArchiveOutputStream taos = new org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(
                gos);
        taos.setLongFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU);
        for (File f : files) {
            addFileToTar(f, f.getName(), taos);
        }
        taos.finish();
        taos.close();
        gos.close();
        fos.close();
    }

    private String formatFileSize(long size) {
        if (size <= 0)
            return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " "
                + units[digitGroups];
    }
}
