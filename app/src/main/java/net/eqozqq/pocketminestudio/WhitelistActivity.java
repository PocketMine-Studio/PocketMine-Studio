package net.eqozqq.pocketminestudio;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import net.eqozqq.pocketminestudio.R;

import androidx.appcompat.app.AppCompatActivity;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

public class WhitelistActivity extends BaseActivity {

	ActionMode actionMode = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_whitelist);

		final ListView list = (ListView) findViewById(R.id.whitelist_list);

		load();

		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				SparseBooleanArray arr = list.getCheckedItemPositions();
				boolean checked = false;
				for (int i = 0; i < arr.size() && !checked; i++) {
					checked = arr.valueAt(i);
				}

				if (actionMode == null && checked) {
					actionMode = WhitelistActivity.this.startActionMode(new ActionMode.Callback() {

						@Override
						public boolean onPrepareActionMode(ActionMode mode,
								Menu menu) {
							return false;
						}

						@Override
						public void onDestroyActionMode(ActionMode mode) {
							list.clearChoices();
							for (int i = 0; i < list.getChildCount(); i++)
								list.setItemChecked(i, false);
							actionMode = null;
						}

						@Override
						public boolean onCreateActionMode(ActionMode mode,
								Menu menu) {
							menu.add(0, 1, 0, "Remove")
									.setIcon(R.drawable.ic_action_remove)
									.setShowAsAction(
											MenuItem.SHOW_AS_ACTION_ALWAYS);
							return true;
						}

						@Override
						public boolean onActionItemClicked(ActionMode mode,
								MenuItem item) {
							if (item.getItemId() == 1) {
								SparseBooleanArray arr = list
										.getCheckedItemPositions();
								Boolean needsSave = false;
								for (int i = arr.size() - 1; i >= 0; i--) {
									if (arr.valueAt(i)) {
										entries.remove(arr.keyAt(i));
										needsSave = true;
									}
								}

								if (needsSave) {
									save();
									load();
								}

								actionMode.finish();
								return true;
							}

							return false;
						}
					});
				} else if (actionMode != null && !checked) {
					actionMode.finish();
					actionMode = null;
				}
			}
		});
		list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	}

	ArrayList<String> entries;
	ArrayAdapter<String> adapter;

	public void save() {
		try {
			PrintWriter writer = new PrintWriter(ServerUtils.getDataDirectory()
					+ "/white-list.txt");
			for (String entry : entries) {
				writer.println(entry);
			}
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void load() {
		ListView list = (ListView) findViewById(R.id.whitelist_list);

		entries = new ArrayList<String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					ServerUtils.getDataDirectory() + "/white-list.txt"));
			try {
				String line;

				while ((line = reader.readLine()) != null) {
					if (line.length() > 0) {
						entries.add(line);
					}
				}
			} finally {
				reader.close();
			}
		} catch (FileNotFoundException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_multiple_choice,
				entries.toArray(new String[entries.size()]));
		list.setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.whitelist, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.whitelist_add) {
			android.view.View view = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_input, null);
			final com.google.android.material.textfield.TextInputEditText input = view.findViewById(R.id.dialog_input);
			input.setHint(R.string.auto_java_whitelist_player);
			new MaterialAlertDialogBuilder(this)
					.setTitle(getString(R.string.auto_java_whitelist_player))
					.setMessage(getString(R.string.auto_java_write_the_player_name_which_yo))
					.setView(view)
					.setPositiveButton("Whitelist",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									entries.add(input.getText().toString()
											.toLowerCase());
									save();
									load();
								}
							}).setNegativeButton("Cancel", null).show();
			return true;
		}

		return false;
	}

}
