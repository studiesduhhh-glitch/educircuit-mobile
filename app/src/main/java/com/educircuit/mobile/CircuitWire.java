package com.educircuit.mobile;

final class CircuitWire {
    final String fromId;
    final int fromPort;
    final String toId;
    final int toPort;

    CircuitWire(String fromId, int fromPort, String toId, int toPort) {
        this.fromId = fromId;
        this.fromPort = fromPort;
        this.toId = toId;
        this.toPort = toPort;
    }

    CircuitWire copy() {
        return new CircuitWire(fromId, fromPort, toId, toPort);
    }
}
