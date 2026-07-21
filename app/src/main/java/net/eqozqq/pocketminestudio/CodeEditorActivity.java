package net.eqozqq.pocketminestudio;

import io.github.rosemoe.sora.widget.CodeEditor;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.widget.Toolbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;



public class CodeEditorActivity extends BaseActivity {

    private CodeEditor codeView;
    private File currentFile;
    private LinearLayout searchBar;
    private EditText searchInput;
    private EditText replaceInput;
    private boolean hasUnsavedChanges = false;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_editor);

        codeView = findViewById(R.id.code_view);
        searchBar = findViewById(R.id.search_bar);
        searchInput = findViewById(R.id.search_input);
        replaceInput = findViewById(R.id.replace_input);
        Toolbar toolbar = findViewById(R.id.editor_toolbar);

        findViewById(R.id.nav_back).setOnClickListener(v -> finish());

        String path = getIntent().getStringExtra("filePath");
        if (path == null) {
            Toast.makeText(this, getString(R.string.msg_no_file_path), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentFile = new File(path);
        
        android.widget.TextView titleView = findViewById(R.id.toolbar_title);
        titleView.setText(currentFile.getName());
        
        toolbar.inflateMenu(R.menu.editor_menu);
        this.menu = toolbar.getMenu();
        toolbar.setOnMenuItemClickListener(item -> onOptionsItemSelected(item));

        setupCodeView();
        loadFile();

        findViewById(R.id.btn_close_search).setOnClickListener(v -> {
            searchBar.setVisibility(View.GONE);
            codeView.getSearcher().stopSearch();
        });

        findViewById(R.id.btn_replace).setOnClickListener(v -> {
            String search = searchInput.getText().toString();
            String replace = replaceInput.getText().toString();
            if (!search.isEmpty()) {
                String text = codeView.getText().toString();
                codeView.setText(text.replace(search, replace));
                Toast.makeText(this, getString(R.string.msg_replaced_instance), Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_next).setOnClickListener(v -> {
            codeView.getSearcher().gotoNext();
        });

        findViewById(R.id.btn_prev).setOnClickListener(v -> {
            codeView.getSearcher().gotoPrevious();
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions opt = new io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions(io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions.TYPE_NORMAL, true);
                    codeView.getSearcher().search(s.toString(), opt);
                } else {
                    codeView.getSearcher().stopSearch();
                }
            }
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getAction() == android.view.KeyEvent.ACTION_DOWN && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                
                String s = searchInput.getText().toString();
                if (!s.isEmpty()) {
                    codeView.getSearcher().gotoNext();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });

        codeView.subscribeEvent(io.github.rosemoe.sora.event.ContentChangeEvent.class, (event, unsubscribe) -> {
            if (!hasUnsavedChanges) {
                hasUnsavedChanges = true;
                if (menu != null) {
                    MenuItem saveItem = menu.findItem(R.id.action_save);
                    if (saveItem != null) saveItem.setIcon(R.drawable.ic_save_clock_24px);
                }
            }
        });
    }

    private void setupCodeView() {
        codeView.setLineNumberEnabled(true);
        codeView.setTextSize(14f);
        try {
            codeView.setColorScheme(new io.github.rosemoe.sora.widget.schemes.SchemeDarcula());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFile() {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(currentFile)));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            codeView.setText(sb.toString());
            hasUnsavedChanges = false;
            if (menu != null) {
                MenuItem saveItem = menu.findItem(R.id.action_save);
                if (saveItem != null) saveItem.setIcon(R.drawable.ic_save_24px);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.msg_cannot_load_file), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void saveFile() {
        try {
            FileOutputStream fos = new FileOutputStream(currentFile);
            fos.write(codeView.getText().toString().getBytes());
            fos.close();
            hasUnsavedChanges = false;
            if (menu != null) {
                MenuItem saveItem = menu.findItem(R.id.action_save);
                if (saveItem != null) saveItem.setIcon(R.drawable.ic_save_24px);
            }
            Toast.makeText(this, getString(R.string.msg_saved_successfully), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.msg_failed_to_save), Toast.LENGTH_SHORT).show();
        }
    }

    public void updateSaveIcon(boolean showClock) {
        if (this.menu != null) {
            MenuItem saveItem = this.menu.findItem(R.id.action_save);
            if (saveItem != null) {
                saveItem.setIcon(showClock ? R.drawable.ic_save_clock_24px : R.drawable.ic_save_24px);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveFile();
            return true;
        } else if (item.getItemId() == R.id.action_search) {
            searchBar.setVisibility(View.VISIBLE);
            findViewById(R.id.replace_input_layout).setVisibility(View.GONE);
            findViewById(R.id.btn_replace).setVisibility(View.GONE);
            searchInput.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            return true;
        } else if (item.getItemId() == R.id.action_replace) {
            searchBar.setVisibility(View.VISIBLE);
            findViewById(R.id.replace_input_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.btn_replace).setVisibility(View.VISIBLE);
            searchInput.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
