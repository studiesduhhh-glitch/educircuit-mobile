package com.educircuit.mobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class CircuitBoardView extends View {
    interface OnBoardChangedListener {
        void onBoardChanged();
    }

    private static final int BACKGROUND = Color.rgb(245, 248, 251);
    private static final int GRID = Color.rgb(224, 232, 238);
    private static final int TEXT = Color.rgb(18, 24, 31);
    private static final int MUTED = Color.rgb(103, 116, 132);
    private static final int WIRE = Color.rgb(0, 145, 170);
    private static final int ACTIVE = Color.rgb(9, 139, 119);
    private static final int WARNING = Color.rgb(232, 87, 82);
    private static final int AMBER = Color.rgb(235, 171, 48);

    private final List<CircuitComponent> components = new ArrayList<>();
    private final List<CircuitWire> wires = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path wirePath = new Path();
    private final PathMeasure pathMeasure = new PathMeasure();
    private final float[] pulsePoint = new float[2];
    private final RectF rect = new RectF();
    private final RectF scratch = new RectF();
    private final CircuitEngine engine = new CircuitEngine();
    private final Runnable animationTick = new Runnable() {
        @Override
        public void run() {
            animationPhase += 0.06f;
            invalidate();
            if (simulationResult != null) {
                postDelayed(this, 16);
            }
        }
    };

    private CircuitEngine.SimulationResult simulationResult;
    private OnBoardChangedListener listener;
    private CircuitComponent dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private PortHit selectedPort;
    private String selectedComponentId;
    private boolean movedDuringDrag;
    private float downX;
    private float downY;
    private float animationPhase;
    private int nextId = 1;
    private float batteryVoltage = 5.0f;

    public CircuitBoardView(Context context) {
        super(context);
        init();
    }

    public CircuitBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setFocusable(true);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        seedStarterLab();
    }

    void setOnBoardChangedListener(OnBoardChangedListener listener) {
        this.listener = listener;
    }

    void seedStarterLab() {
        components.clear();
        wires.clear();
        nextId = 1;
        selectedComponentId = null;
        addComponentInternal("Battery", dp(28), dp(72));
        addComponentInternal("Resistor", dp(178), dp(72));
        addComponentInternal("LED", dp(328), dp(72));
        autoWire();
    }

    void addComponent(String type) {
        float width = Math.max(getWidth(), dp(390));
        int index = components.size();
        float x = dp(24) + (index % 2) * Math.min(dp(170), width / 2f - dp(26));
        float y = dp(54) + (index / 2) * dp(96);
        addComponentInternal(type, x, y);
        selectedComponentId = components.get(components.size() - 1).id;
        notifyChanged();
    }

    void clearBoard() {
        components.clear();
        wires.clear();
        selectedPort = null;
        selectedComponentId = null;
        dragging = null;
        simulationResult = null;
        stopAnimation();
        notifyChanged();
    }

    void autoWire() {
        ensureBattery();
        ensureSafeDefaultLoad();
        wires.clear();

        CircuitComponent battery = findBattery();
        if (battery == null) {
            notifyChanged();
            return;
        }

        List<CircuitComponent> chain = new ArrayList<>();
        for (CircuitComponent component : components) {
            if (!CircuitComponent.isBattery(component.type)) {
                chain.add(component);
            }
        }

        Collections.sort(chain, new Comparator<CircuitComponent>() {
            @Override
            public int compare(CircuitComponent first, CircuitComponent second) {
                return first.id.compareTo(second.id);
            }
        });

        layoutChain(battery, chain);
        if (!chain.isEmpty()) {
            CircuitComponent first = chain.get(0);
            wires.add(new CircuitWire(battery.id, CircuitComponent.PORT_POSITIVE,
                    first.id, CircuitComponent.PORT_POSITIVE));
            for (int index = 0; index < chain.size() - 1; index += 1) {
                CircuitComponent current = chain.get(index);
                CircuitComponent next = chain.get(index + 1);
                wires.add(new CircuitWire(current.id, CircuitComponent.PORT_NEGATIVE,
                        next.id, CircuitComponent.PORT_POSITIVE));
            }
            CircuitComponent last = chain.get(chain.size() - 1);
            wires.add(new CircuitWire(last.id, CircuitComponent.PORT_NEGATIVE,
                    battery.id, CircuitComponent.PORT_NEGATIVE));
        }

        selectedPort = null;
        notifyChanged();
    }

    void loadTemplate(String template) {
        components.clear();
        wires.clear();
        selectedComponentId = null;
        selectedPort = null;
        dragging = null;
        simulationResult = null;
        stopAnimation();
        nextId = 1;

        if ("alarm".equals(template)) {
            addComponentInternal("Battery", dp(28), dp(56));
            addComponentInternal("Switch", dp(178), dp(56));
            addComponentInternal("Buzzer", dp(328), dp(56));
            batteryVoltage = 5f;
        } else if ("plant".equals(template)) {
            addComponentInternal("Battery", dp(28), dp(50));
            addComponentInternal("Soil Sensor", dp(178), dp(50));
            addComponentInternal("Pump", dp(328), dp(50));
            batteryVoltage = 6f;
        } else if ("fan".equals(template)) {
            addComponentInternal("Battery", dp(28), dp(56));
            addComponentInternal("Switch", dp(178), dp(56));
            addComponentInternal("Motor", dp(328), dp(56));
            batteryVoltage = 6f;
        } else {
            addComponentInternal("Battery", dp(28), dp(72));
            addComponentInternal("Resistor", dp(178), dp(72));
            addComponentInternal("LED", dp(328), dp(72));
            batteryVoltage = 5f;
        }

        autoWire();
    }

    boolean removeSelectedComponent() {
        if (selectedComponentId == null) {
            return false;
        }

        CircuitComponent selected = findById(selectedComponentId);
        if (selected == null || CircuitComponent.isBattery(selected.type)) {
            return false;
        }

        for (int index = wires.size() - 1; index >= 0; index -= 1) {
            CircuitWire wire = wires.get(index);
            if (wire.fromId.equals(selectedComponentId) || wire.toId.equals(selectedComponentId)) {
                wires.remove(index);
            }
        }

        components.remove(selected);
        selectedComponentId = null;
        selectedPort = null;
        simulationResult = null;
        stopAnimation();
        notifyChanged();
        return true;
    }

    boolean duplicateSelectedComponent() {
        CircuitComponent selected = findById(selectedComponentId);
        if (selected == null || CircuitComponent.isBattery(selected.type)) {
            return false;
        }

        CircuitComponent copy = addComponentInternal(
                selected.type,
                clamp(selected.x + dp(24), dp(8), Math.max(dp(8), getWidth() - componentWidth() - dp(8))),
                clamp(selected.y + dp(82), dp(8), Math.max(dp(8), getHeight() - componentHeight() - dp(8)))
        );
        selectedComponentId = copy.id;
        simulationResult = null;
        stopAnimation();
        notifyChanged();
        return true;
    }

    String selectedComponentSummary() {
        CircuitComponent selected = findById(selectedComponentId);
        if (selected == null) {
            return "Tap a component to inspect it. Drag parts to move them. Tap one port, then another, to wire.";
        }

        return selected.type + " selected\n" + detailFor(selected.type) + "\nPorts: tap + or - to connect wires.";
    }

    void setBatteryVoltage(float voltage) {
        batteryVoltage = voltage;
        notifyChanged();
    }

    float getBatteryVoltage() {
        return batteryVoltage;
    }

    int getComponentCount() {
        return components.size();
    }

    int getWireCount() {
        return wires.size();
    }

    CircuitSnapshot snapshot() {
        return new CircuitSnapshot(components, wires, batteryVoltage);
    }

    void loadSnapshot(CircuitSnapshot snapshot) {
        components.clear();
        wires.clear();
        int largestId = 0;

        for (CircuitComponent component : snapshot.components) {
            CircuitComponent copy = component.copy();
            components.add(copy);
            largestId = Math.max(largestId, numericId(copy.id));
        }

        for (CircuitWire wire : snapshot.wires) {
            wires.add(wire.copy());
        }

        nextId = largestId + 1;
        batteryVoltage = snapshot.batteryVoltage;
        selectedPort = null;
        dragging = null;
        simulationResult = null;
        stopAnimation();
        notifyChanged();
    }

    CircuitEngine.SimulationResult runSimulation() {
        simulationResult = engine.simulate(snapshot());
        startAnimation();
        invalidate();
        return simulationResult;
    }

    void setSimulationResult(CircuitEngine.SimulationResult result) {
        simulationResult = result;
        startAnimation();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawPremiumBackground(canvas);
        drawGrid(canvas);
        drawWires(canvas);
        drawHint(canvas);
        drawComponents(canvas);
        drawBoardHud(canvas);
    }

    private void drawPremiumBackground(Canvas canvas) {
        LinearGradient gradient = new LinearGradient(
                0,
                0,
                getWidth(),
                getHeight(),
                new int[] {
                        Color.WHITE,
                        Color.rgb(246, 251, 249),
                        Color.rgb(242, 247, 250)
                },
                new float[] { 0f, 0.58f, 1f },
                Shader.TileMode.CLAMP
        );
        paint.setShader(gradient);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(130, 255, 255, 255));
        canvas.drawRect(0, 0, getWidth(), dp(74), paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(Color.argb(120, 220, 229, 236));
        canvas.drawLine(dp(18), dp(74), getWidth() - dp(18), dp(74), paint);
    }

    private void drawGrid(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(Color.argb(150, Color.red(GRID), Color.green(GRID), Color.blue(GRID)));
        float step = dp(32);
        for (float x = 0; x <= getWidth(); x += step) {
            canvas.drawLine(x, 0, x, getHeight(), paint);
        }
        for (float y = 0; y <= getHeight(); y += step) {
            canvas.drawLine(0, y, getWidth(), y, paint);
        }
    }

    private void drawHint(Canvas canvas) {
        if (!components.isEmpty()) {
            return;
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(MUTED);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(16));
        canvas.drawText("Add parts, wire ports, then run the lab", getWidth() / 2f, getHeight() / 2f, paint);
    }

    private void drawWires(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        boolean animated = simulationResult != null && !simulationResult.overload && "Working Circuit".equals(simulationResult.status);

        for (CircuitWire wire : wires) {
            CircuitComponent from = findById(wire.fromId);
            CircuitComponent to = findById(wire.toId);
            if (from == null || to == null) {
                continue;
            }

            float startX = portX(from, wire.fromPort);
            float startY = portY(from, wire.fromPort);
            float endX = portX(to, wire.toPort);
            float endY = portY(to, wire.toPort);
            buildWirePath(startX, startY, endX, endY);

            paint.setStrokeWidth(dp(8));
            paint.setColor(Color.argb(42, 18, 24, 31));
            canvas.drawPath(wirePath, paint);

            paint.setStrokeWidth(dp(5));
            paint.setColor(Color.WHITE);
            canvas.drawPath(wirePath, paint);

            paint.setStrokeWidth(dp(3));
            paint.setColor(animated ? ACTIVE : WIRE);
            canvas.drawPath(wirePath, paint);

            if (animated) {
                drawWirePulse(canvas);
            }
        }
    }

    private void buildWirePath(float startX, float startY, float endX, float endY) {
        wirePath.reset();
        wirePath.moveTo(startX, startY);
        float curve = Math.max(dp(46), Math.abs(endX - startX) * 0.42f);
        float direction = endX >= startX ? 1f : -1f;
        wirePath.cubicTo(
                startX + curve * direction,
                startY,
                endX - curve * direction,
                endY,
                endX,
                endY
        );
    }

    private void drawWirePulse(Canvas canvas) {
        float position = (animationPhase % 1f + 1f) % 1f;
        pathMeasure.setPath(wirePath, false);
        pathMeasure.getPosTan(pathMeasure.getLength() * position, pulsePoint, null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(pulsePoint[0], pulsePoint[1], dp(6), paint);
        paint.setColor(ACTIVE);
        canvas.drawCircle(pulsePoint[0], pulsePoint[1], dp(3), paint);
        paint.setStyle(Paint.Style.STROKE);
    }

    private void drawComponents(Canvas canvas) {
        paint.setTextAlign(Paint.Align.LEFT);
        for (CircuitComponent component : components) {
            drawComponent(canvas, component);
        }

        if (selectedPort != null) {
            CircuitComponent component = findById(selectedPort.componentId);
            if (component != null) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(3));
                paint.setColor(ACTIVE);
                canvas.drawCircle(portX(component, selectedPort.port), portY(component, selectedPort.port), dp(15), paint);
            }
        }
    }

    private void drawComponent(Canvas canvas, CircuitComponent component) {
        float width = componentWidth();
        float height = componentHeight();
        rect.set(component.x, component.y, component.x + width, component.y + height);

        boolean activeOutput = isActiveOutput(component.type);
        boolean selected = component.id.equals(selectedComponentId);
        if (activeOutput) {
            paint.setStyle(Paint.Style.FILL);
            int glow = 32 + (int) (Math.abs(Math.sin(animationPhase * 4f)) * 32);
            paint.setColor(Color.argb(glow, 9, 139, 119));
            canvas.drawRoundRect(expanded(rect, dp(8)), dp(8), dp(8), paint);
        }

        if (selected) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(44, 0, 145, 170));
            canvas.drawRoundRect(expanded(rect, dp(7)), dp(8), dp(8), paint);
        }

        paint.setShadowLayer(dp(8), 0, dp(4), Color.argb(28, 18, 24, 31));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(rect, dp(8), dp(8), paint);
        paint.clearShadowLayer();

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(selected ? dp(3) : dp(2));
        paint.setColor(selected ? WIRE : activeOutput ? ACTIVE : Color.rgb(212, 219, 230));
        canvas.drawRoundRect(rect, dp(8), dp(8), paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(CircuitComponent.colorFor(component.type));
        RectF colorBar = new RectF(rect.left, rect.top, rect.left + dp(7), rect.bottom);
        canvas.drawRoundRect(colorBar, dp(7), dp(7), paint);

        paint.setColor(TEXT);
        paint.setTextSize(dp(15));
        paint.setFakeBoldText(true);
        canvas.drawText(component.type, rect.left + dp(19), rect.top + dp(27), paint);

        paint.setFakeBoldText(false);
        paint.setTextSize(dp(11));
        paint.setColor(MUTED);
        String details = detailFor(component.type);
        canvas.drawText(details, rect.left + dp(19), rect.top + dp(49), paint);
        drawComponentBadge(canvas, component);

        drawPort(canvas, component, CircuitComponent.PORT_NEGATIVE);
        drawPort(canvas, component, CircuitComponent.PORT_POSITIVE);
    }

    private void drawComponentBadge(Canvas canvas, CircuitComponent component) {
        String badge = badgeFor(component.type);
        float right = rect.right - dp(14);
        float centerY = rect.top + dp(21);
        paint.setStyle(Paint.Style.FILL);
        scratch.set(right - dp(16), centerY - dp(12), right + dp(16), centerY + dp(12));
        paint.setColor(Color.rgb(246, 249, 251));
        canvas.drawRoundRect(scratch, dp(8), dp(8), paint);
        canvas.drawCircle(right, centerY, dp(12), paint);
        paint.setColor(CircuitComponent.colorFor(component.type));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(10));
        paint.setFakeBoldText(true);
        canvas.drawText(badge, right, centerY + dp(4), paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private RectF expanded(RectF source, float amount) {
        return new RectF(source.left - amount, source.top - amount, source.right + amount, source.bottom + amount);
    }

    private void drawPort(Canvas canvas, CircuitComponent component, int port) {
        float x = portX(component, port);
        float y = portY(component, port);
        boolean positive = port == CircuitComponent.PORT_POSITIVE;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(positive ? WARNING : Color.rgb(40, 48, 58));
        canvas.drawCircle(x, y, dp(11), paint);

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(12));
        paint.setFakeBoldText(true);
        canvas.drawText(positive ? "+" : "-", x, y + dp(4), paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawBoardHud(Canvas canvas) {
        if (simulationResult == null) {
            return;
        }

        String status = simulationResult.status + "  " + simulationResult.score + "/100";
        float padding = dp(10);
        paint.setTextSize(dp(12));
        paint.setFakeBoldText(true);
        float textWidth = paint.measureText(status);
        scratch.set(dp(14), dp(14), dp(14) + textWidth + padding * 2 + dp(12), dp(48));

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(simulationResult.overload ? Color.rgb(255, 240, 238) : Color.rgb(232, 249, 244));
        canvas.drawRoundRect(scratch, dp(8), dp(8), paint);
        paint.setColor(simulationResult.overload ? WARNING : ACTIVE);
        canvas.drawCircle(scratch.left + dp(12), scratch.centerY(), dp(4), paint);
        paint.setColor(TEXT);
        canvas.drawText(status, scratch.left + dp(24), scratch.top + dp(22), paint);
        paint.setFakeBoldText(false);
    }

    private boolean isActiveOutput(String type) {
        if (simulationResult == null) {
            return false;
        }
        return ("LED".equals(type) && simulationResult.ledOn)
                || ("Motor".equals(type) && simulationResult.motorOn)
                || ("Buzzer".equals(type) && simulationResult.buzzerOn)
                || ("Pump".equals(type) && simulationResult.pumpOn);
    }

    private String detailFor(String type) {
        if (CircuitComponent.isBattery(type)) {
            return String.format(Locale.US, "%.1fV source", batteryVoltage);
        }

        float minVoltage = CircuitComponent.minVoltageFor(type);
        if (minVoltage > 0f) {
            return String.format(Locale.US, "Needs %.1fV", minVoltage);
        }

        if ("Resistor".equals(type)) {
            return "Current limiter";
        }

        if ("Switch".equals(type)) {
            return "Closed switch";
        }

        return "Circuit part";
    }

    private String badgeFor(String type) {
        if ("Battery".equals(type)) return "P";
        if ("Resistor".equals(type)) return "R";
        if ("LED".equals(type)) return "L";
        if ("Switch".equals(type)) return "S";
        if ("Motor".equals(type)) return "M";
        if ("Buzzer".equals(type)) return "B";
        if ("Soil Sensor".equals(type)) return "SS";
        if ("Pump".equals(type)) return "P";
        return "?";
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                downY = y;
                movedDuringDrag = false;
                PortHit port = findPort(x, y);
                if (port != null) {
                    handlePortTap(port);
                    return true;
                }

                dragging = findComponentAt(x, y);
                if (dragging != null) {
                    selectedComponentId = dragging.id;
                    dragOffsetX = x - dragging.x;
                    dragOffsetY = y - dragging.y;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    notifyChanged();
                    return true;
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (dragging != null) {
                    if (Math.abs(x - downX) > dp(4) || Math.abs(y - downY) > dp(4)) {
                        movedDuringDrag = true;
                    }
                    dragging.x = clamp(x - dragOffsetX, dp(8), Math.max(dp(8), getWidth() - componentWidth() - dp(8)));
                    dragging.y = clamp(y - dragOffsetY, dp(8), Math.max(dp(8), getHeight() - componentHeight() - dp(8)));
                    simulationResult = null;
                    stopAnimation();
                    notifyChanged();
                    return true;
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (event.getActionMasked() == MotionEvent.ACTION_UP && dragging == null) {
                    performClick();
                }
                if (event.getActionMasked() == MotionEvent.ACTION_UP && dragging != null && !movedDuringDrag) {
                    selectedComponentId = dragging.id;
                    notifyChanged();
                }
                dragging = null;
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;

            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void handlePortTap(PortHit port) {
        if (selectedPort == null) {
            selectedPort = port;
            invalidate();
            return;
        }

        if (selectedPort.componentId.equals(port.componentId) && selectedPort.port == port.port) {
            selectedPort = null;
            invalidate();
            return;
        }

        if (!hasWire(selectedPort.componentId, selectedPort.port, port.componentId, port.port)) {
            wires.add(new CircuitWire(selectedPort.componentId, selectedPort.port, port.componentId, port.port));
        }
        selectedComponentId = port.componentId;
        selectedPort = null;
        simulationResult = null;
        stopAnimation();
        notifyChanged();
    }

    private boolean hasWire(String firstId, int firstPort, String secondId, int secondPort) {
        for (CircuitWire wire : wires) {
            boolean same = wire.fromId.equals(firstId)
                    && wire.fromPort == firstPort
                    && wire.toId.equals(secondId)
                    && wire.toPort == secondPort;
            boolean reversed = wire.fromId.equals(secondId)
                    && wire.fromPort == secondPort
                    && wire.toId.equals(firstId)
                    && wire.toPort == firstPort;
            if (same || reversed) {
                return true;
            }
        }
        return false;
    }

    private PortHit findPort(float x, float y) {
        for (int index = components.size() - 1; index >= 0; index -= 1) {
            CircuitComponent component = components.get(index);
            PortHit negative = hitPort(component, CircuitComponent.PORT_NEGATIVE, x, y);
            if (negative != null) {
                return negative;
            }
            PortHit positive = hitPort(component, CircuitComponent.PORT_POSITIVE, x, y);
            if (positive != null) {
                return positive;
            }
        }
        return null;
    }

    private PortHit hitPort(CircuitComponent component, int port, float x, float y) {
        float dx = x - portX(component, port);
        float dy = y - portY(component, port);
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance <= dp(22)) {
            return new PortHit(component.id, port);
        }
        return null;
    }

    private CircuitComponent findComponentAt(float x, float y) {
        for (int index = components.size() - 1; index >= 0; index -= 1) {
            CircuitComponent component = components.get(index);
            rect.set(component.x, component.y, component.x + componentWidth(), component.y + componentHeight());
            if (rect.contains(x, y)) {
                return component;
            }
        }
        return null;
    }

    private CircuitComponent addComponentInternal(String type, float x, float y) {
        CircuitComponent component = new CircuitComponent("c" + nextId, type, x, y);
        nextId += 1;
        components.add(component);
        return component;
    }

    private void ensureBattery() {
        if (findBattery() == null) {
            addComponentInternal("Battery", dp(28), dp(72));
        }
    }

    private void ensureSafeDefaultLoad() {
        boolean hasNonBattery = false;
        boolean hasLed = false;
        boolean hasResistor = false;
        for (CircuitComponent component : components) {
            hasNonBattery = hasNonBattery || !CircuitComponent.isBattery(component.type);
            hasLed = hasLed || "LED".equals(component.type);
            hasResistor = hasResistor || "Resistor".equals(component.type);
        }

        if (!hasNonBattery) {
            addComponentInternal("Resistor", dp(178), dp(72));
            addComponentInternal("LED", dp(328), dp(72));
            return;
        }

        if (hasLed && !hasResistor) {
            addComponentInternal("Resistor", dp(178), dp(170));
        }
    }

    private void layoutChain(CircuitComponent battery, List<CircuitComponent> chain) {
        float margin = dp(20);
        float width = Math.max(getWidth(), dp(390));
        float stepY = dp(92);
        battery.x = margin;
        battery.y = dp(52);

        for (int index = 0; index < chain.size(); index += 1) {
            CircuitComponent component = chain.get(index);
            int row = index + 1;
            component.x = Math.min(width - componentWidth() - margin, margin + (row % 2) * dp(150));
            component.y = dp(52) + row * stepY;
        }
    }

    private CircuitComponent findBattery() {
        for (CircuitComponent component : components) {
            if (CircuitComponent.isBattery(component.type)) {
                return component;
            }
        }
        return null;
    }

    private CircuitComponent findById(String id) {
        for (CircuitComponent component : components) {
            if (component.id.equals(id)) {
                return component;
            }
        }
        return null;
    }

    private int numericId(String id) {
        if (id == null || id.length() < 2) {
            return 0;
        }
        try {
            return Integer.parseInt(id.substring(1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private float portX(CircuitComponent component, int port) {
        if (port == CircuitComponent.PORT_POSITIVE) {
            return component.x + componentWidth();
        }
        return component.x;
    }

    private float portY(CircuitComponent component, int port) {
        return component.y + componentHeight() / 2f;
    }

    private float componentWidth() {
        if (getWidth() <= 0) {
            return dp(132);
        }
        return Math.min(dp(136), Math.max(dp(112), getWidth() - dp(54)));
    }

    private float componentHeight() {
        return dp(70);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void notifyChanged() {
        invalidate();
        if (listener != null) {
            listener.onBoardChanged();
        }
    }

    private void startAnimation() {
        removeCallbacks(animationTick);
        if (simulationResult != null) {
            post(animationTick);
        }
    }

    private void stopAnimation() {
        removeCallbacks(animationTick);
    }

    private static final class PortHit {
        final String componentId;
        final int port;

        PortHit(String componentId, int port) {
            this.componentId = componentId;
            this.port = port;
        }
    }
}
