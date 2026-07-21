package net.eqozqq.pocketminestudio;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import net.eqozqq.pocketminestudio.R;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class LogActivity extends BaseActivity {

	public static LogActivity logActivity;
	public static android.widget.ScrollView sv;
	public static SpannableStringBuilder currentLog = new SpannableStringBuilder();

	private LinearLayout macroContainer;
	private HorizontalScrollView macroScroll;
	private com.google.android.material.appbar.AppBarLayout appBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log);
		
		androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
		toolbar.getMenu().add(0, COPY_CODE, 0, getString(R.string.action_copy)).setIcon(R.drawable.ic_content_copy_24px)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		toolbar.getMenu().add(0, CLEAR_CODE, 0, getString(R.string.action_clear)).setIcon(R.drawable.ic_clear_all_24px)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		toolbar.setOnMenuItemClickListener(item -> onOptionsItemSelected(item));
		findViewById(R.id.nav_back).setOnClickListener(v -> finish());
		
		appBar = findViewById(R.id.app_bar);
		

		logActivity = this;
		android.widget.TextView logTV = (android.widget.TextView) findViewById(R.id.logTextView);
		logTV.setText(currentLog);
		logTV.setTextIsSelectable(true);

		sv = (android.widget.ScrollView) findViewById(R.id.logScrollView);

		Button btnCmd = (Button) findViewById(R.id.runCommand);
		btnCmd.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				EditText et = (EditText) findViewById(R.id.textCmd);
				if (et.getText().toString().trim().isEmpty()) return;
				log(">"
						+ et.getText().toString().replace("&", "&amp;")
								.replace("<", "&lt;").replace(">", "&gt;"));
				ServerUtils.executeCMD(et.getText().toString());
				et.setText("");
			}
		});

        macroContainer = findViewById(R.id.macroContainer);
        macroScroll = findViewById(R.id.macroScroll);
        loadMacrosUI();
	}

    

	private void loadMacrosUI() {
	    macroContainer.removeAllViews();
	    List<String> macros = MacroManager.getMacros(this);
	    for (String macro : macros) {
	        MaterialButton btn = new MaterialButton(this);
	        btn.setText(macro);
	        btn.setAllCaps(false);
	        btn.setInsetTop(0);
	        btn.setInsetBottom(0);
	        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
	            LinearLayout.LayoutParams.WRAP_CONTENT,
	            LinearLayout.LayoutParams.WRAP_CONTENT
	        );
	        params.setMarginEnd(8);
	        btn.setLayoutParams(params);
	        btn.setOnClickListener(v -> {
	            EditText et = findViewById(R.id.textCmd);
	            String current = et.getText().toString();
	            String space = (current.isEmpty() || current.endsWith(" ")) ? "" : " ";
	            et.append(space + macro + " ");
	            et.requestFocus();
	            et.setSelection(et.getText().length());
	        });
	        macroContainer.addView(btn);
	    }

	    ImageButton btnEdit = new ImageButton(this);
	    btnEdit.setImageResource(R.drawable.ic_edit_24px);
	    btnEdit.setBackgroundResource(R.drawable.bg_rounded); // Or ?attr/selectableItemBackground
	    btnEdit.setColorFilter(getResources().getColor(android.R.color.darker_gray));
	    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            (int)(48 * getResources().getDisplayMetrics().density),
            (int)(48 * getResources().getDisplayMetrics().density)
        );
        params.setMarginStart(8);
        btnEdit.setLayoutParams(params);
        btnEdit.setOnClickListener(v -> showMacroEditorDialog());
        macroContainer.addView(btnEdit);
	}

	private void showMacroEditorDialog() {
	    View view = LayoutInflater.from(this).inflate(R.layout.dialog_macros, null);
	    ListView listView = view.findViewById(R.id.listViewMacros);
	    Button btnAdd = view.findViewById(R.id.btnAddMacro);

	    List<String> macros = MacroManager.getMacros(this);
	    BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() { return macros.size(); }
            @Override
            public Object getItem(int position) { return macros.get(position); }
            @Override
            public long getItemId(int position) { return position; }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(LogActivity.this).inflate(R.layout.item_macro, parent, false);
                }
                TextView textName = convertView.findViewById(R.id.textMacroName);
                ImageButton btnEdit = convertView.findViewById(R.id.btnEditMacro);
                ImageButton btnDelete = convertView.findViewById(R.id.btnDeleteMacro);

                String macro = macros.get(position);
                textName.setText(macro);

                btnDelete.setOnClickListener(v -> {
                    macros.remove(position);
                    MacroManager.saveMacros(LogActivity.this, macros);
                    notifyDataSetChanged();
                    loadMacrosUI();
                });

                btnEdit.setOnClickListener(v -> {
                    showEditSingleMacroDialog(macros, position, this);
                });

                return convertView;
            }
        };
        listView.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> {
            showEditSingleMacroDialog(macros, -1, adapter);
        });

	    new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
	        .setTitle(R.string.title_macros)
	        .setView(view)
	        .setPositiveButton(android.R.string.ok, null)
	        .show();
	}

	private void showEditSingleMacroDialog(List<String> macros, int position, BaseAdapter adapter) {
	    android.view.View view = LayoutInflater.from(this).inflate(R.layout.dialog_input, null);
	    com.google.android.material.textfield.TextInputEditText input = view.findViewById(R.id.dialog_input);
	    input.setHint(R.string.hint_macro);
	    if (position >= 0) {
	        input.setText(macros.get(position));
	    }
	    
	    new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
	        .setTitle(position >= 0 ? R.string.btn_edit_macro : R.string.btn_add_macro)
	        .setView(view)
	        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
	            String text = input.getText().toString().trim();
	            if (text.isEmpty()) {
	                Toast.makeText(this, R.string.msg_macro_empty, Toast.LENGTH_SHORT).show();
	                return;
	            }
	            if (position >= 0) {
	                macros.set(position, text);
	            } else {
	                macros.add(text);
	            }
	            MacroManager.saveMacros(this, macros);
	            adapter.notifyDataSetChanged();
	            loadMacrosUI();
	        })
	        .setNegativeButton(R.string.btn_cancel, null)
	        .show();
	}

	final static int CLEAR_CODE = 143;
	final static int COPY_CODE = CLEAR_CODE + 1;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == CLEAR_CODE) {
			currentLog = new SpannableStringBuilder();
			android.widget.TextView logTV = (android.widget.TextView) logActivity
					.findViewById(R.id.logTextView);
			logTV.setText(currentLog);

			return true;
		} else if (item.getItemId() == COPY_CODE) {
			ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			clipboard.setText(currentLog);
			Toast.makeText(this,
					getString(R.string.msg_console_copied),
					Toast.LENGTH_SHORT).show();
		}
		return false;
	}

	public static void log(final String whatToLog) {
		String stripped = whatToLog.replaceAll("\u001B\\[[;\\d]*m", "").replaceAll("\u001B\\][^\u0007]*\u0007", "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		final Spanned result = Html.fromHtml(stripped + "<br/>");
		currentLog.append(result);
		if (logActivity != null) {
			logActivity.runOnUiThread(new Runnable() {
				public void run() {
					android.widget.TextView logTV = (android.widget.TextView) logActivity.findViewById(R.id.logTextView);
					if (logTV != null) logTV.append(result);
					if (sv != null) {
						sv.post(new Runnable() {
							@Override
							public void run() {
								sv.scrollTo(0, sv.getChildAt(0).getHeight());
							}
						});
					}
				}
			});
		}
	}
}
