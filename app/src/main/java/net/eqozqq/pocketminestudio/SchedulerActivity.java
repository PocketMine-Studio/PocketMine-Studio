package net.eqozqq.pocketminestudio;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SchedulerActivity extends BaseActivity {

    private ListView listEvents;
    private EventAdapter adapter;
    private List<ScheduledEvent> events;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scheduler);

        android.widget.ImageButton navBack = findViewById(R.id.nav_back);
        if (navBack != null) {
            navBack.setOnClickListener(v -> finish());
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        prefs = getSharedPreferences("EventScheduler", MODE_PRIVATE);
        events = loadEvents();

        listEvents = findViewById(R.id.list_events);
        adapter = new EventAdapter(this, events);
        listEvents.setAdapter(adapter);

        FloatingActionButton fabAdd = findViewById(R.id.fab_add_event);
        fabAdd.setOnClickListener(v -> showEditDialog(null));

        listEvents.setOnItemLongClickListener(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showContextMenu(ScheduledEvent event) {
        String[] options = {"Edit", "Delete"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(event.name)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditDialog(event);
                    } else if (which == 1) {
                        events.remove(event);
                        saveEvents();
                        adapter.notifyDataSetChanged();
                    }
                })
                .show();
    }

    private void showEditDialog(ScheduledEvent existingEvent) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_event, null);
        EditText editName = view.findViewById(R.id.edit_event_name);
        AutoCompleteTextView spinnerAction = view.findViewById(R.id.spinner_event_action);
        View layoutCommand = view.findViewById(R.id.layout_event_command);
        EditText editCommand = view.findViewById(R.id.edit_event_command);
        EditText editInterval = view.findViewById(R.id.edit_event_interval);
        AutoCompleteTextView spinnerUnit = view.findViewById(R.id.spinner_event_unit);
        MaterialSwitch switchRetry = view.findViewById(R.id.switch_retry_fail);

        String[] actions = {"Restart Server", "Stop Server", "Send Command"};
        ArrayAdapter<String> actionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, actions);
        spinnerAction.setAdapter(actionAdapter);

        String[] units = {"Seconds", "Minutes", "Hours"};
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, units);
        spinnerUnit.setAdapter(unitAdapter);

        spinnerAction.setOnItemClickListener((parent, v, position, id) -> {
            if (position == 2) {
                layoutCommand.setVisibility(View.VISIBLE);
            } else {
                layoutCommand.setVisibility(View.GONE);
            }
        });

        spinnerAction.setText(actions[0], false);
        layoutCommand.setVisibility(View.GONE);
        spinnerUnit.setText(units[1], false);
        
        int currentActionPos = 0;
        int currentUnitPos = 1;

        if (existingEvent != null) {
            editName.setText(existingEvent.name);
            if (existingEvent.action.equals("restart")) {
                currentActionPos = 0;
            } else if (existingEvent.action.equals("stop")) {
                currentActionPos = 1;
            } else {
                currentActionPos = 2;
                editCommand.setText(existingEvent.actionData);
                layoutCommand.setVisibility(View.VISIBLE);
            }
            spinnerAction.setText(actions[currentActionPos], false);
            
            long ms = existingEvent.intervalMs;
            if (ms % (60 * 60 * 1000) == 0 && ms >= (60 * 60 * 1000)) {
                editInterval.setText(String.valueOf(ms / (60 * 60 * 1000)));
                currentUnitPos = 2;
            } else if (ms % (60 * 1000) == 0 && ms >= (60 * 1000)) {
                editInterval.setText(String.valueOf(ms / (60 * 1000)));
                currentUnitPos = 1;
            } else {
                editInterval.setText(String.valueOf(ms / 1000));
                currentUnitPos = 0;
            }
            spinnerUnit.setText(units[currentUnitPos], false);
            switchRetry.setChecked(existingEvent.retryOnFail);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(existingEvent == null ? "Create Event" : "Edit Event")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = editName.getText().toString().trim();
                    String intervalStr = editInterval.getText().toString().trim();
                    if (name.isEmpty() || intervalStr.isEmpty()) {
                        Toast.makeText(this, getString(R.string.auto_java_please_fill_all_fields), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    long intervalVal = 0;
                    try {
                        intervalVal = Long.parseLong(intervalStr);
                    } catch (Exception e) {}
                    
                    if (intervalVal <= 0) {
                        Toast.makeText(this, getString(R.string.auto_java_interval_must_be_0), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    String unitStr = spinnerUnit.getText().toString();
                    long multiplier = 1000; // seconds
                    if (unitStr.equals("Minutes")) multiplier = 60 * 1000;
                    if (unitStr.equals("Hours")) multiplier = 60 * 60 * 1000;
                    
                    long finalIntervalMs = intervalVal * multiplier;

                    String actionStr = spinnerAction.getText().toString();
                    String action = "restart";
                    if (actionStr.equals("Stop Server")) action = "stop";
                    else if (actionStr.equals("Send Command")) action = "command";
                    
                    String actionData = action.equals("command") ? editCommand.getText().toString().trim() : "";

                    if (existingEvent == null) {
                        ScheduledEvent newEvent = new ScheduledEvent(UUID.randomUUID().toString(), name, action, actionData, finalIntervalMs, switchRetry.isChecked(), true, System.currentTimeMillis());
                        events.add(newEvent);
                    } else {
                        existingEvent.name = name;
                        existingEvent.action = action;
                        existingEvent.actionData = actionData;
                        existingEvent.intervalMs = finalIntervalMs;
                        existingEvent.retryOnFail = switchRetry.isChecked();
                    }

                    saveEvents();
                    adapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private List<ScheduledEvent> loadEvents() {
        List<ScheduledEvent> list = new ArrayList<>();
        String json = prefs.getString("events", "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                
                long intervalMs = obj.optLong("intervalMs", 0);
                if (intervalMs == 0) {
                    intervalMs = obj.optInt("intervalMinutes", 20) * 60 * 1000L;
                }
                
                ScheduledEvent e = new ScheduledEvent(
                        obj.getString("id"),
                        obj.getString("name"),
                        obj.getString("action"),
                        obj.optString("actionData", ""),
                        intervalMs,
                        obj.optBoolean("retryOnFail", false),
                        obj.getBoolean("isEnabled"),
                        obj.optLong("lastRunTime", System.currentTimeMillis())
                );
                list.add(e);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void saveEvents() {
        JSONArray arr = new JSONArray();
        for (ScheduledEvent e : events) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", e.id);
                obj.put("name", e.name);
                obj.put("action", e.action);
                obj.put("actionData", e.actionData);
                obj.put("intervalMs", e.intervalMs);
                obj.put("retryOnFail", e.retryOnFail);
                obj.put("isEnabled", e.isEnabled);
                obj.put("lastRunTime", e.lastRunTime);
                arr.put(obj);
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }
        prefs.edit().putString("events", arr.toString()).apply();
    }

    private class EventAdapter extends ArrayAdapter<ScheduledEvent> {

        public EventAdapter(Context context, List<ScheduledEvent> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_scheduler_event, parent, false);
            }

            ScheduledEvent event = getItem(position);

            TextView textName = convertView.findViewById(R.id.text_event_name);
            TextView textAction = convertView.findViewById(R.id.text_event_action);
            TextView textInterval = convertView.findViewById(R.id.text_event_interval);
            TextView textRetry = convertView.findViewById(R.id.text_event_retry);
            MaterialSwitch switchEnabled = convertView.findViewById(R.id.switch_event_enabled);

            textName.setText(event.name);
            if (event.action.equals("restart")) {
                textAction.setText(getString(R.string.auto_java_action_restart_server));
            } else if (event.action.equals("stop")) {
                textAction.setText(getString(R.string.auto_java_action_stop_server));
            } else {
                textAction.setText("Action: Send Command (" + event.actionData + ")");
            }
            
            long ms = event.intervalMs;
            if (ms % (60 * 60 * 1000) == 0 && ms >= (60 * 60 * 1000)) {
                textInterval.setText("Every " + (ms / (60 * 60 * 1000)) + " hour(s)");
            } else if (ms % (60 * 1000) == 0 && ms >= (60 * 1000)) {
                textInterval.setText("Every " + (ms / (60 * 1000)) + " minute(s)");
            } else {
                textInterval.setText("Every " + (ms / 1000) + " second(s)");
            }
            
            if (event.retryOnFail) {
                textRetry.setText(getString(R.string.auto_java_retries_if_server_offline));
                textRetry.setVisibility(View.VISIBLE);
            } else {
                textRetry.setVisibility(View.GONE);
            }

            switchEnabled.setOnCheckedChangeListener(null);
            switchEnabled.setChecked(event.isEnabled);
            switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                event.isEnabled = isChecked;
                if (isChecked) {
                    event.lastRunTime = System.currentTimeMillis();
                }
                saveEvents();
            });

            convertView.setOnLongClickListener(v -> {
                showContextMenu(event);
                return true;
            });

            return convertView;
        }
    }

    public static class ScheduledEvent {
        public String id;
        public String name;
        public String action;
        public String actionData;
        public long intervalMs;
        public boolean retryOnFail;
        public boolean isEnabled;
        public long lastRunTime;

        public ScheduledEvent(String id, String name, String action, String actionData, long intervalMs, boolean retryOnFail, boolean isEnabled, long lastRunTime) {
            this.id = id;
            this.name = name;
            this.action = action;
            this.actionData = actionData;
            this.intervalMs = intervalMs;
            this.retryOnFail = retryOnFail;
            this.isEnabled = isEnabled;
            this.lastRunTime = lastRunTime;
        }
    }
}
