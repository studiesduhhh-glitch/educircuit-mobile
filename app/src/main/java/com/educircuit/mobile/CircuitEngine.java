package com.educircuit.mobile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

final class CircuitEngine {
    static final class SimulationResult {
        final boolean ledOn;
        final boolean motorOn;
        final boolean buzzerOn;
        final boolean pumpOn;
        final boolean overload;
        final int score;
        final String grade;
        final String status;
        final String hint;
        final String fix;

        SimulationResult(
                boolean ledOn,
                boolean motorOn,
                boolean buzzerOn,
                boolean pumpOn,
                boolean overload,
                int score,
                String grade,
                String status,
                String hint,
                String fix
        ) {
            this.ledOn = ledOn;
            this.motorOn = motorOn;
            this.buzzerOn = buzzerOn;
            this.pumpOn = pumpOn;
            this.overload = overload;
            this.score = score;
            this.grade = grade;
            this.status = status;
            this.hint = hint;
            this.fix = fix;
        }
    }

    SimulationResult simulate(CircuitSnapshot snapshot) {
        CircuitComponent battery = findBattery(snapshot.components);
        if (battery == null) {
            return result(false, false, false, false, false, 20,
                    "Needs Power",
                    "Add a battery before running the lab.",
                    "Every working circuit needs a source of voltage.",
                    "Add Battery, then wire Battery + through your parts and return to Battery -.");
        }

        Map<String, CircuitComponent> byId = new HashMap<>();
        for (CircuitComponent component : snapshot.components) {
            byId.put(component.id, component);
        }

        Map<String, Set<String>> graph = buildGraph(snapshot, byId);
        String positive = nodeKey(battery.id, CircuitComponent.PORT_POSITIVE);
        String negative = nodeKey(battery.id, CircuitComponent.PORT_NEGATIVE);
        Set<String> poweredNodes = reachable(graph, positive);
        boolean closedLoop = poweredNodes.contains(negative);

        if (!closedLoop) {
            return result(false, false, false, false, false, 38,
                    "Open Loop",
                    "The circuit is not complete yet.",
                    "Power leaves Battery +, but it does not return to Battery -.",
                    "Connect the last component's - port back to Battery -.");
        }

        List<CircuitComponent> activeComponents = activeComponents(snapshot.components, poweredNodes);
        boolean hasLoad = false;
        boolean hasResistor = false;
        boolean hasSensor = false;
        float neededVoltage = 0f;
        for (CircuitComponent component : activeComponents) {
            hasLoad = hasLoad || CircuitComponent.isLoad(component.type);
            hasResistor = hasResistor || "Resistor".equals(component.type);
            hasSensor = hasSensor || CircuitComponent.isSensor(component.type);
            neededVoltage = Math.max(neededVoltage, CircuitComponent.minVoltageFor(component.type));
        }

        if (!hasLoad && !hasSensor) {
            return result(false, false, false, false, true, 42,
                    "Unsafe Loop",
                    "The loop has no useful output or sensor.",
                    "A battery loop without a load can behave like a short circuit.",
                    "Add an LED, buzzer, motor, pump, or sensor before closing the loop.");
        }

        boolean ledPresent = containsType(activeComponents, "LED");
        if (ledPresent && !hasResistor) {
            return result(false, false, false, false, true, 55,
                    "Protect LED",
                    "The LED path needs a resistor.",
                    "LEDs can burn out when current is not limited.",
                    "Place a Resistor in the loop before or after the LED.");
        }

        if (snapshot.batteryVoltage < neededVoltage) {
            return result(false, false, false, false, false, 64,
                    "Low Voltage",
                    "The loop is correct, but the battery voltage is too low.",
                    "Your strongest component needs about " + voltageLabel(neededVoltage) + ".",
                    "Raise the battery to " + voltageLabel(neededVoltage) + " or use a lower-voltage output.");
        }

        boolean ledOn = containsType(activeComponents, "LED");
        boolean motorOn = containsType(activeComponents, "Motor");
        boolean buzzerOn = containsType(activeComponents, "Buzzer");
        boolean pumpOn = containsType(activeComponents, "Pump");
        int score = hasSensor ? 94 : 88;

        return result(ledOn, motorOn, buzzerOn, pumpOn, false, score,
                "Working Circuit",
                "The circuit is complete and safe.",
                "Power flows through the loop and the active outputs can run.",
                hasSensor
                        ? "Nice: your sensor makes this feel like a real automation project."
                        : "Add a sensor to turn this into a smarter engineering build.");
    }

    private CircuitComponent findBattery(List<CircuitComponent> components) {
        for (CircuitComponent component : components) {
            if (CircuitComponent.isBattery(component.type)) {
                return component;
            }
        }
        return null;
    }

    private Map<String, Set<String>> buildGraph(CircuitSnapshot snapshot, Map<String, CircuitComponent> byId) {
        Map<String, Set<String>> graph = new HashMap<>();
        for (CircuitComponent component : snapshot.components) {
            ensureNode(graph, nodeKey(component.id, CircuitComponent.PORT_NEGATIVE));
            ensureNode(graph, nodeKey(component.id, CircuitComponent.PORT_POSITIVE));
            if (!CircuitComponent.isBattery(component.type)) {
                connect(graph,
                        nodeKey(component.id, CircuitComponent.PORT_NEGATIVE),
                        nodeKey(component.id, CircuitComponent.PORT_POSITIVE));
            }
        }

        for (CircuitWire wire : snapshot.wires) {
            if (byId.containsKey(wire.fromId) && byId.containsKey(wire.toId)) {
                connect(graph, nodeKey(wire.fromId, wire.fromPort), nodeKey(wire.toId, wire.toPort));
            }
        }

        return graph;
    }

    private List<CircuitComponent> activeComponents(List<CircuitComponent> components, Set<String> poweredNodes) {
        List<CircuitComponent> active = new ArrayList<>();
        for (CircuitComponent component : components) {
            if (CircuitComponent.isBattery(component.type)) {
                continue;
            }

            boolean positivePowered = poweredNodes.contains(nodeKey(component.id, CircuitComponent.PORT_POSITIVE));
            boolean negativePowered = poweredNodes.contains(nodeKey(component.id, CircuitComponent.PORT_NEGATIVE));
            if (positivePowered || negativePowered) {
                active.add(component);
            }
        }
        return active;
    }

    private boolean containsType(List<CircuitComponent> components, String type) {
        for (CircuitComponent component : components) {
            if (type.equals(component.type)) {
                return true;
            }
        }
        return false;
    }

    private void ensureNode(Map<String, Set<String>> graph, String key) {
        if (!graph.containsKey(key)) {
            graph.put(key, new HashSet<String>());
        }
    }

    private void connect(Map<String, Set<String>> graph, String first, String second) {
        ensureNode(graph, first);
        ensureNode(graph, second);
        graph.get(first).add(second);
        graph.get(second).add(first);
    }

    private Set<String> reachable(Map<String, Set<String>> graph, String start) {
        Set<String> seen = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            String current = queue.remove();
            Set<String> nextNodes = graph.get(current);
            if (nextNodes == null) {
                continue;
            }

            for (String next : nextNodes) {
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }

        return seen;
    }

    private String nodeKey(String componentId, int port) {
        return componentId + ":" + port;
    }

    private String voltageLabel(float voltage) {
        if (voltage == (int) voltage) {
            return ((int) voltage) + "V";
        }
        return String.format(java.util.Locale.US, "%.1fV", voltage);
    }

    private SimulationResult result(
            boolean ledOn,
            boolean motorOn,
            boolean buzzerOn,
            boolean pumpOn,
            boolean overload,
            int score,
            String status,
            String hint,
            String fix,
            String extra
    ) {
        String grade;
        if (score >= 90) {
            grade = "A+";
        } else if (score >= 80) {
            grade = "A";
        } else if (score >= 70) {
            grade = "B";
        } else if (score >= 55) {
            grade = "C";
        } else {
            grade = "Needs Work";
        }

        return new SimulationResult(
                ledOn,
                motorOn,
                buzzerOn,
                pumpOn,
                overload,
                score,
                grade,
                status,
                hint,
                fix + "\n" + extra
        );
    }
}
