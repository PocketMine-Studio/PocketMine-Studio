package net.eqozqq.pocketminestudio;

import android.content.Intent;
import android.os.Bundle;

public class ManageWhitelistActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, ManagePlayersActivity.class);
        intent.putExtra("selected_tab", 1);
        startActivity(intent);
        finish();
    }
}
