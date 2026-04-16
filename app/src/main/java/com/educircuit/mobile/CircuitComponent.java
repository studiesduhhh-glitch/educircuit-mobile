package com.educircuit.mobile;

import android.graphics.Color;

final class CircuitComponent {
    static final int PORT_NEGATIVE = 0;
    static final int PORT_POSITIVE = 1;

    final String id;
    final String type;
    float x;
    float y;

    CircuitComponent(String id, String type, float x, float y) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
    }

    CircuitComponent copy() {
        return new CircuitComponent(id, type, x, y);
    }

    static String[] catalog() {
        return new String[] {
                "Battery",
                "Resistor",
                "LED",
                "Switch",
                "Motor",
                "Buzzer",
                "Soil Sensor",
                "Pump"
        };
    }

    static int colorFor(String type) {
        switch (type) {
            case "Battery":
                return Color.rgb(22, 93, 255);
            case "LED":
                return Color.rgb(255, 77, 79);
            case "Motor":
                return Color.rgb(21, 184, 129);
            case "Switch":
                return Color.rgb(255, 176, 32);
            case "Buzzer":
                return Color.rgb(126, 87, 194);
            case "Resistor":
                return Color.rgb(95, 112, 133);
            case "Soil Sensor":
                return Color.rgb(62, 166, 91);
            case "Pump":
                return Color.rgb(0, 150, 199);
            default:
                return Color.rgb(60, 72, 88);
        }
    }

    static float minVoltageFor(String type) {
        switch (type) {
            case "LED":
                return 2.0f;
            case "Buzzer":
                return 3.0f;
            case "Motor":
            case "Pump":
                return 6.0f;
            case "Soil Sensor":
                return 3.3f;
            default:
                return 0.0f;
        }
    }

    static boolean isLoad(String type) {
        return "LED".equals(type)
                || "Motor".equals(type)
                || "Buzzer".equals(type)
                || "Pump".equals(type);
    }

    static boolean isSensor(String type) {
        return "Soil Sensor".equals(type);
    }

    static boolean isBattery(String type) {
        return "Battery".equals(type);
    }
}
