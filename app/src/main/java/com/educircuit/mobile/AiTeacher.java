package com.educircuit.mobile;

import java.util.Locale;

final class AiTeacher {
    String reply(String prompt, CircuitSnapshot snapshot, CircuitEngine.SimulationResult result) {
        String text = prompt == null ? "" : prompt.toLowerCase(Locale.US);

        if (text.contains("quiz")) {
            return "Quick quiz: why should an LED usually have a resistor in series?\n\nA) To store energy\nB) To limit current\nC) To make voltage disappear";
        }

        if (text.contains("resistor")) {
            return "A resistor limits current. In this lab it is especially important for LEDs, because an LED can glow with low voltage but can still be damaged by too much current.";
        }

        if (text.contains("voltage") || text.contains("battery")) {
            return "Voltage is the push that moves charge around the loop. Your current battery is set to "
                    + String.format(Locale.US, "%.1fV", snapshot.batteryVoltage)
                    + ". The coach says: " + result.hint;
        }

        if (text.contains("short")) {
            return "A short circuit is a path where power can rush from Battery + to Battery - without a useful load. Add a load, and for LEDs add a resistor, so the energy does useful work safely.";
        }

        if (text.contains("why") || text.contains("not") || text.contains("fix")) {
            return "First fix: " + result.fix + "\n\nStatus right now: " + result.status + ".";
        }

        if (text.contains("explain") || text.contains("current circuit")) {
            return "Your project has " + snapshot.components.size() + " components and "
                    + snapshot.wires.size() + " wires. The simulator score is "
                    + result.score + "/100. Coach note: " + result.hint;
        }

        return "I can help debug the circuit, explain voltage and current, quiz you, or suggest the first exact fix. Current status: "
                + result.status + ".";
    }
}
