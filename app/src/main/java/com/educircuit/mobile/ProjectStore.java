package com.educircuit.mobile;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class ProjectStore {
    static final class StoredProject {
        final String name;
        final CircuitSnapshot snapshot;
        final String teacherGrade;
        final String teacherFeedback;

        StoredProject(String name, CircuitSnapshot snapshot, String teacherGrade, String teacherFeedback) {
            this.name = name;
            this.snapshot = snapshot;
            this.teacherGrade = teacherGrade;
            this.teacherFeedback = teacherFeedback;
        }
    }

    private static final String PREFS = "educircuit_mobile_projects";
    private static final String KEY_PROJECT = "latest_project";

    private final SharedPreferences preferences;

    ProjectStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    void save(String name, CircuitSnapshot snapshot, String teacherGrade, String teacherFeedback) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("name", name);
        root.put("voltage", snapshot.batteryVoltage);
        root.put("teacherGrade", teacherGrade);
        root.put("teacherFeedback", teacherFeedback);

        JSONArray components = new JSONArray();
        for (CircuitComponent component : snapshot.components) {
            JSONObject item = new JSONObject();
            item.put("id", component.id);
            item.put("type", component.type);
            item.put("x", component.x);
            item.put("y", component.y);
            components.put(item);
        }
        root.put("components", components);

        JSONArray wires = new JSONArray();
        for (CircuitWire wire : snapshot.wires) {
            JSONObject item = new JSONObject();
            item.put("fromId", wire.fromId);
            item.put("fromPort", wire.fromPort);
            item.put("toId", wire.toId);
            item.put("toPort", wire.toPort);
            wires.put(item);
        }
        root.put("wires", wires);

        preferences.edit().putString(KEY_PROJECT, root.toString()).apply();
    }

    StoredProject load() throws JSONException {
        String raw = preferences.getString(KEY_PROJECT, null);
        if (raw == null) {
            return null;
        }

        JSONObject root = new JSONObject(raw);
        List<CircuitComponent> components = new ArrayList<>();
        JSONArray componentArray = root.optJSONArray("components");
        if (componentArray != null) {
            for (int index = 0; index < componentArray.length(); index += 1) {
                JSONObject item = componentArray.getJSONObject(index);
                components.add(new CircuitComponent(
                        item.getString("id"),
                        item.getString("type"),
                        (float) item.optDouble("x", 80),
                        (float) item.optDouble("y", 80)
                ));
            }
        }

        List<CircuitWire> wires = new ArrayList<>();
        JSONArray wireArray = root.optJSONArray("wires");
        if (wireArray != null) {
            for (int index = 0; index < wireArray.length(); index += 1) {
                JSONObject item = wireArray.getJSONObject(index);
                wires.add(new CircuitWire(
                        item.getString("fromId"),
                        item.optInt("fromPort", CircuitComponent.PORT_POSITIVE),
                        item.getString("toId"),
                        item.optInt("toPort", CircuitComponent.PORT_NEGATIVE)
                ));
            }
        }

        return new StoredProject(
                root.optString("name", "Untitled Circuit"),
                new CircuitSnapshot(components, wires, (float) root.optDouble("voltage", 5.0)),
                root.optString("teacherGrade", ""),
                root.optString("teacherFeedback", "")
        );
    }
}
