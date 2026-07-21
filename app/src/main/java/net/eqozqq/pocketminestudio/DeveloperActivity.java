package net.eqozqq.pocketminestudio;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class DeveloperActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_developer);

		Button localPluginsEditor = (Button) findViewById(R.id.developer_plugin_edit);
		localPluginsEditor.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(
						DeveloperActivity.this);
				b.setTitle(getString(R.string.auto_java_edit_local_plugin_list_depreca));
				b.setMessage(getString(R.string.auto_java_this_feature_has_been_removed));
				b.show();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.developer, menu);
		return true;
	}
}
