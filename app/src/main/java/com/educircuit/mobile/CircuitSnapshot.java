package com.educircuit.mobile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class CircuitSnapshot {
    final List<CircuitComponent> components;
    final List<CircuitWire> wires;
    final float batteryVoltage;

    CircuitSnapshot(List<CircuitComponent> components, List<CircuitWire> wires, float batteryVoltage) {
        List<CircuitComponent> componentCopies = new ArrayList<>();
        for (CircuitComponent component : components) {
            componentCopies.add(component.copy());
        }

        List<CircuitWire> wireCopies = new ArrayList<>();
        for (CircuitWire wire : wires) {
            wireCopies.add(wire.copy());
        }

        this.components = Collections.unmodifiableList(componentCopies);
        this.wires = Collections.unmodifiableList(wireCopies);
        this.batteryVoltage = batteryVoltage;
    }
}
