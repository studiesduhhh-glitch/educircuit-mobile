package com.educircuit.mobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Insets;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(246, 248, 252);
    private static final int SURFACE = Color.WHITE;
    private static final int PRIMARY = Color.rgb(22, 93, 255);
    private static final int PRIMARY_DARK = Color.rgb(18, 67, 173);
    private static final int TEXT = Color.rgb(18, 28, 45);
    private static final int MUTED = Color.rgb(101, 116, 139);
    private static final int LINE = Color.rgb(221, 228, 238);
    private static final int GREEN = Color.rgb(21, 184, 129);
    private static final int RED = Color.rgb(255, 77, 79);
    private static final int AMBER = Color.rgb(255, 176, 32);
    private static final int CYAN = Color.rgb(0, 150, 199);
    private static final int SOFT_BLUE = Color.rgb(233, 239, 252);
    private static final int SOFT_GREEN = Color.rgb(232, 249, 241);
    private static final int SOFT_AMBER = Color.rgb(255, 247, 231);

    private final CircuitEngine engine = new CircuitEngine();
    private final AiTeacher aiTeacher = new AiTeacher();
    private final StringBuilder chatHistory = new StringBuilder();

    private ProjectStore projectStore;
    private CircuitBoardView boardView;
    private LinearLayout panelContent;
    private String activePanel = "student";
    private String projectName = "Smart Circuit Lab";
    private String teacherGrade = "";
    private String teacherFeedback = "";
    private CircuitEngine.SimulationResult latestResult;

    private TextView statusValue;
    private TextView scoreValue;
    private TextView gradeValue;
    private TextView outputsValue;
    private TextView coachValue;
    private TextView countsValue;
    private TextView voltageValue;
    private TextView selectedValue;
    private TextView challengeValue;
    private TextView teacherMetricsValue;
    private TextView chatLog;
    private EditText projectNameInput;
    private EditText teacherGradeInput;
    private EditText teacherFeedbackInput;
    private EditText chatInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        projectStore = new ProjectStore(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        applySystemBarInsets(root);

        root.addView(buildHeader());

        boardView = new CircuitBoardView(this);
        boardView.setOnBoardChangedListener(new CircuitBoardView.OnBoardChangedListener() {
            @Override
            public void onBoardChanged() {
                latestResult = null;
                refreshStats();
            }
        });
        root.addView(boardView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        ScrollView panelScroll = new ScrollView(this);
        panelScroll.setFillViewport(false);
        panelScroll.setBackgroundColor(BG);
        panelContent = new LinearLayout(this);
        panelContent.setOrientation(LinearLayout.VERTICAL);
        panelContent.setPadding(dp(14), dp(12), dp(14), dp(16));
        panelScroll.addView(panelContent);
        root.addView(panelScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(340)
        ));

        setContentView(root);
        latestResult = engine.simulate(boardView.snapshot());
        boardView.setSimulationResult(latestResult);
        renderPanel("student");
    }

    private void applySystemBarInsets(final View root) {
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                int topInset;
                int bottomInset;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Insets systemBars = insets.getInsets(WindowInsets.Type.systemBars());
                    topInset = systemBars.top;
                    bottomInset = systemBars.bottom;
                } else {
                    topInset = legacyTopInset(insets);
                    bottomInset = legacyBottomInset(insets);
                }

                view.setPadding(
                        0,
                        topInset,
                        0,
                        bottomInset
                );
                return insets;
            }
        });
    }

    @SuppressWarnings("deprecation")
    private int legacyTopInset(WindowInsets insets) {
        return insets.getSystemWindowInsetTop();
    }

    @SuppressWarnings("deprecation")
    private int legacyBottomInset(WindowInsets insets) {
        return insets.getSystemWindowInsetBottom();
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(14), dp(10), dp(14), dp(8));
        header.setBackground(premiumPanelBackground());
        header.setElevation(dp(4));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleStack = new LinearLayout(this);
        titleStack.setOrientation(LinearLayout.VERTICAL);
        titleStack.addView(text("EduCircuit Mobile", 25, TEXT, true));
        titleStack.addView(text("Build, simulate, debug, and grade circuits.", 13, MUTED, false));
        titleRow.addView(titleStack, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView livePill = pill("Live Lab", GREEN, Color.WHITE);
        titleRow.addView(livePill);
        header.addView(titleRow);

        LinearLayout metricRow = new LinearLayout(this);
        metricRow.setOrientation(LinearLayout.HORIZONTAL);
        metricRow.setPadding(0, dp(10), 0, 0);
        metricRow.addView(metricChip("Mode", "Student", SOFT_BLUE, PRIMARY));
        metricRow.addView(metricChip("Coach", "Ready", SOFT_GREEN, GREEN));
        metricRow.addView(metricChip("Storage", "Device", SOFT_AMBER, AMBER));
        header.addView(metricRow);

        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(12), 0, 0);
        scroller.addView(row);

        row.addView(button("Run Lab", PRIMARY, Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runLab();
            }
        }));
        row.addView(button("Auto Wire", GREEN, Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boardView.autoWire();
                runLab();
            }
        }));
        row.addView(button("Starter", CYAN, Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boardView.seedStarterLab();
                runLab();
            }
        }));
        row.addView(button("Clear", RED, Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boardView.clearBoard();
                latestResult = null;
                refreshStats();
                toast("Board cleared");
            }
        }));
        row.addView(button("Save", AMBER, TEXT, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveProject();
            }
        }));
        row.addView(button("Load", SOFT_BLUE, PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadProject();
            }
        }));
        row.addView(button("Student", SOFT_BLUE, PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                renderPanel("student");
            }
        }));
        row.addView(button("Teacher", SOFT_BLUE, PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                renderPanel("teacher");
            }
        }));
        row.addView(button("AI Coach", SOFT_BLUE, PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                renderPanel("ai");
            }
        }));

        header.addView(scroller);
        return header;
    }

    private void renderPanel(String panel) {
        activePanel = panel;
        clearPanelRefs();
        panelContent.removeAllViews();

        if ("teacher".equals(panel)) {
            renderTeacherPanel();
        } else if ("ai".equals(panel)) {
            renderAiPanel();
        } else {
            renderStudentPanel();
        }

        refreshStats();
    }

    private void renderStudentPanel() {
        panelContent.addView(sectionTitle("Student Lab"));
        panelContent.addView(buildPanelTabs());

        LinearLayout challenge = card();
        LinearLayout challengeTop = row();
        LinearLayout challengeCopy = new LinearLayout(this);
        challengeCopy.setOrientation(LinearLayout.VERTICAL);
        challengeCopy.addView(label("Challenge"));
        challengeValue = text("", 14, TEXT, true);
        challengeCopy.addView(challengeValue);
        challengeTop.addView(challengeCopy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        challengeTop.addView(button("Plant Build", SOFT_GREEN, GREEN, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyTemplate("plant", "Smart Plant");
            }
        }));
        challenge.addView(challengeTop);
        LinearLayout templateRow = row();
        templateRow.addView(button("LED Loop", SOFT_BLUE, PRIMARY, new TemplateClick("led", "LED Loop")));
        templateRow.addView(button("Door Alarm", SOFT_AMBER, Color.rgb(164, 94, 0), new TemplateClick("alarm", "Door Alarm")));
        templateRow.addView(button("Mini Fan", Color.rgb(231, 248, 252), CYAN, new TemplateClick("fan", "Mini Fan")));
        challenge.addView(templateRow);
        panelContent.addView(challenge);

        LinearLayout nameCard = card();
        nameCard.addView(label("Project name"));
        projectNameInput = input(projectName, "Project name");
        nameCard.addView(projectNameInput);
        nameCard.addView(button("Rename Project", PRIMARY, Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                projectName = projectNameInput.getText().toString().trim();
                if (projectName.length() == 0) {
                    projectName = "Untitled Circuit";
                    projectNameInput.setText(projectName);
                }
                toast("Project renamed");
            }
        }));
        panelContent.addView(nameCard);

        LinearLayout stats = card();
        statusValue = stat(stats, "Status", "");
        scoreValue = stat(stats, "Auto score", "");
        gradeValue = stat(stats, "Grade", "");
        outputsValue = stat(stats, "Outputs", "");
        countsValue = stat(stats, "Parts", "");
        panelContent.addView(stats);

        LinearLayout inspector = card();
        inspector.addView(label("Inspector"));
        selectedValue = text("", 14, TEXT, false);
        selectedValue.setLineSpacing(dp(2), 1.0f);
        inspector.addView(selectedValue);
        LinearLayout inspectorActions = row();
        inspectorActions.addView(button("Duplicate", SOFT_BLUE, PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (boardView.duplicateSelectedComponent()) {
                    toast("Component duplicated");
                } else {
                    toast("Select a non-battery component first");
                }
                refreshStats();
            }
        }));
        inspectorActions.addView(button("Delete", Color.rgb(255, 238, 238), RED, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (boardView.removeSelectedComponent()) {
                    toast("Component deleted");
                } else {
                    toast("Select a non-battery component first");
                }
                refreshStats();
            }
        }));
        inspector.addView(inspectorActions);
        panelContent.addView(inspector);

        LinearLayout coach = card();
        coach.addView(label("Circuit Coach"));
        coachValue = text("", 14, TEXT, false);
        coachValue.setLineSpacing(dp(2), 1.0f);
        coach.addView(coachValue);
        panelContent.addView(coach);

        LinearLayout voltage = card();
        voltage.addView(label("Battery voltage"));
        voltageValue = text("", 14, MUTED, false);
        voltage.addView(voltageValue);
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(60);
        seekBar.setProgress(Math.round(boardView.getBatteryVoltage() * 2));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    boardView.setBatteryVoltage(progress / 2.0f);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                runLab();
            }
        });
        voltage.addView(seekBar);
        panelContent.addView(voltage);

        LinearLayout palette = card();
        LinearLayout paletteHead = row();
        paletteHead.addView(label("Add components"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        paletteHead.addView(button("Auto Wire", SOFT_GREEN, GREEN, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boardView.autoWire();
                runLab();
            }
        }));
        palette.addView(paletteHead);
        String[] catalog = CircuitComponent.catalog();
        for (int index = 0; index < catalog.length; index += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            palette.addView(row);
            addPaletteButton(row, catalog[index]);
            if (index + 1 < catalog.length) {
                addPaletteButton(row, catalog[index + 1]);
            }
        }
        panelContent.addView(palette);
    }

    private void renderTeacherPanel() {
        panelContent.addView(sectionTitle("Teacher Panel"));
        panelContent.addView(buildPanelTabs());

        LinearLayout review = card();
        teacherMetricsValue = text("", 14, TEXT, false);
        teacherMetricsValue.setLineSpacing(dp(2), 1.0f);
        review.addView(teacherMetricsValue);
        panelContent.addView(review);

        LinearLayout gradeCard = card();
        gradeCard.addView(label("Grade"));
        teacherGradeInput = input(teacherGrade, "A+, 95, Excellent");
        gradeCard.addView(teacherGradeInput);
        gradeCard.addView(label("Feedback"));
        teacherFeedbackInput = input(teacherFeedback, "Write feedback for the student's circuit");
        teacherFeedbackInput.setMinLines(3);
        teacherFeedbackInput.setGravity(Gravity.TOP);
        gradeCard.addView(teacherFeedbackInput);
        gradeCard.addView(button("Apply Teacher Feedback", PRIMARY, Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                teacherGrade = teacherGradeInput.getText().toString().trim();
                teacherFeedback = teacherFeedbackInput.getText().toString().trim();
                toast("Teacher feedback applied");
                refreshStats();
            }
        }));
        gradeCard.addView(button("Copy Project Summary", SOFT_BLUE, PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copySummary();
            }
        }));
        panelContent.addView(gradeCard);
    }

    private void renderAiPanel() {
        panelContent.addView(sectionTitle("AI Coach"));
        panelContent.addView(buildPanelTabs());

        LinearLayout quick = card();
        quick.addView(label("Ask fast"));
        String[] prompts = {
                "Explain my current circuit",
                "Why is it not working?",
                "Teach me voltage",
                "Quiz me"
        };
        for (String prompt : prompts) {
            quick.addView(button(prompt, SOFT_BLUE, PRIMARY, new PromptClick(prompt)));
        }
        panelContent.addView(quick);

        LinearLayout chat = card();
        chatLog = text(chatHistory.length() == 0
                ? "Ask about voltage, resistors, short circuits, or the first exact fix."
                : chatHistory.toString(), 14, TEXT, false);
        chatLog.setLineSpacing(dp(3), 1.0f);
        chat.addView(chatLog);
        chatInput = input("", "Ask your AI coach");
        chat.addView(chatInput);
        chat.addView(button("Send", PRIMARY, Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendChat();
            }
        }));
        panelContent.addView(chat);
    }

    private final class PromptClick implements View.OnClickListener {
        private final String prompt;

        PromptClick(String prompt) {
            this.prompt = prompt;
        }

        @Override
        public void onClick(View view) {
            askCoach(prompt);
        }
    }

    private final class TemplateClick implements View.OnClickListener {
        private final String template;
        private final String name;

        TemplateClick(String template, String name) {
            this.template = template;
            this.name = name;
        }

        @Override
        public void onClick(View view) {
            applyTemplate(template, name);
        }
    }

    private void applyTemplate(String template, String name) {
        projectName = name;
        boardView.loadTemplate(template);
        latestResult = boardView.runSimulation();
        renderPanel("student");
        toast(name + " loaded");
    }

    private void runLab() {
        latestResult = boardView.runSimulation();
        refreshStats();
        toast(latestResult.status + " - " + latestResult.score + "/100");
    }

    private void saveProject() {
        if (projectNameInput != null) {
            String typedName = projectNameInput.getText().toString().trim();
            if (typedName.length() > 0) {
                projectName = typedName;
            }
        }

        try {
            projectStore.save(projectName, boardView.snapshot(), teacherGrade, teacherFeedback);
            toast("Project saved on this device");
        } catch (JSONException error) {
            toast("Save failed: " + error.getMessage());
        }
    }

    private void loadProject() {
        try {
            ProjectStore.StoredProject project = projectStore.load();
            if (project == null) {
                toast("No saved project yet");
                return;
            }

            projectName = project.name;
            teacherGrade = project.teacherGrade;
            teacherFeedback = project.teacherFeedback;
            boardView.loadSnapshot(project.snapshot);
            latestResult = boardView.runSimulation();
            renderPanel(activePanel);
            toast("Project loaded");
        } catch (JSONException error) {
            toast("Load failed: " + error.getMessage());
        }
    }

    private void copySummary() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("EduCircuit summary", buildSummary()));
            toast("Summary copied");
        }
    }

    private void sendChat() {
        if (chatInput == null) {
            return;
        }
        String prompt = chatInput.getText().toString().trim();
        if (prompt.length() == 0) {
            toast("Ask a question first");
            return;
        }
        chatInput.setText("");
        askCoach(prompt);
    }

    private void askCoach(String prompt) {
        CircuitEngine.SimulationResult result = currentResult();
        String reply = aiTeacher.reply(prompt, boardView.snapshot(), result);
        chatHistory.append("You: ").append(prompt).append("\n");
        chatHistory.append("Coach: ").append(reply).append("\n\n");
        if (chatLog != null) {
            chatLog.setText(chatHistory.toString());
        }
    }

    private CircuitEngine.SimulationResult currentResult() {
        if (latestResult == null) {
            latestResult = engine.simulate(boardView.snapshot());
        }
        return latestResult;
    }

    @SuppressLint("SetTextI18n")
    private void refreshStats() {
        CircuitEngine.SimulationResult result = currentResult();
        if (statusValue != null) {
            statusValue.setText(result.status);
        }
        if (scoreValue != null) {
            scoreValue.setText(result.score + "/100");
        }
        if (gradeValue != null) {
            String shownGrade = teacherGrade.length() == 0 ? result.grade : teacherGrade;
            gradeValue.setText(shownGrade);
        }
        if (outputsValue != null) {
            outputsValue.setText(outputsText(result));
        }
        if (coachValue != null) {
            coachValue.setText(result.hint + "\n\nFix: " + result.fix);
        }
        if (countsValue != null) {
            countsValue.setText(boardView.getComponentCount() + " components, " + boardView.getWireCount() + " wires");
        }
        if (voltageValue != null) {
            voltageValue.setText(String.format(Locale.US, "%.1fV", boardView.getBatteryVoltage()));
        }
        if (selectedValue != null) {
            selectedValue.setText(boardView.selectedComponentSummary());
        }
        if (challengeValue != null) {
            challengeValue.setText(challengeText(result));
        }
        if (teacherMetricsValue != null) {
            teacherMetricsValue.setText(buildSummary());
        }
    }

    private String challengeText(CircuitEngine.SimulationResult result) {
        if ("Working Circuit".equals(result.status) && result.score >= 90) {
            return "Advanced build complete. Add feedback and save the project.";
        }
        if ("Working Circuit".equals(result.status)) {
            return "Working build. Add a sensor to push the score higher.";
        }
        if (result.overload) {
            return "Safety check: fix the warning before submission.";
        }
        return "Goal: make a safe closed loop that powers one output.";
    }

    private String outputsText(CircuitEngine.SimulationResult result) {
        return "LED " + onOff(result.ledOn)
                + "  Motor " + onOff(result.motorOn)
                + "  Buzzer " + onOff(result.buzzerOn)
                + "  Pump " + onOff(result.pumpOn)
                + (result.overload ? "  Safety warning" : "");
    }

    private String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private String buildSummary() {
        CircuitEngine.SimulationResult result = currentResult();
        String finalGrade = teacherGrade.length() == 0 ? result.grade : teacherGrade;
        String feedback = teacherFeedback.length() == 0 ? "No teacher feedback yet." : teacherFeedback;
        return "Project: " + projectName + "\n"
                + "Status: " + result.status + "\n"
                + "Auto score: " + result.score + "/100\n"
                + "Grade: " + finalGrade + "\n"
                + "Components: " + boardView.getComponentCount() + "\n"
                + "Wires: " + boardView.getWireCount() + "\n"
                + "Outputs: " + outputsText(result) + "\n"
                + "Coach: " + result.hint + "\n"
                + "Teacher feedback: " + feedback;
    }

    private void addPaletteButton(LinearLayout row, final String type) {
        Button item = button(type, Color.rgb(245, 247, 251), TEXT, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boardView.addComponent(type);
                toast(type + " added");
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(0, dp(6), dp(8), dp(2));
        row.addView(item, params);
    }

    private LinearLayout buildPanelTabs() {
        LinearLayout tabs = row();
        tabs.setPadding(0, 0, 0, dp(8));
        tabs.addView(tabButton("Student", "student"));
        tabs.addView(tabButton("Teacher", "teacher"));
        tabs.addView(tabButton("AI Coach", "ai"));
        return tabs;
    }

    private Button tabButton(String label, final String panel) {
        boolean active = activePanel.equals(panel);
        return button(
                label,
                active ? PRIMARY_DARK : SOFT_BLUE,
                active ? Color.WHITE : PRIMARY,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        renderPanel(panel);
                    }
                }
        );
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private TextView stat(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        TextView left = text(label, 13, MUTED, false);
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView right = text(value, 14, TEXT, true);
        right.setGravity(Gravity.END);
        row.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));

        parent.addView(row);
        return right;
    }

    private TextView sectionTitle(String value) {
        TextView title = text(value, 18, TEXT, true);
        title.setPadding(0, 0, 0, dp(8));
        return title;
    }

    private TextView label(String value) {
        TextView label = text(value, 13, MUTED, true);
        label.setPadding(0, 0, 0, dp(6));
        return label;
    }

    private TextView pill(String value, int background, int foreground) {
        TextView pill = text(value, 12, foreground, true);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(12), dp(7), dp(12), dp(7));
        pill.setBackground(rounded(background, Color.TRANSPARENT, dp(8)));
        return pill;
    }

    private TextView metricChip(String label, String value, int background, int foreground) {
        TextView chip = text(label + "\n" + value, 12, foreground, true);
        chip.setGravity(Gravity.CENTER);
        chip.setMinHeight(dp(42));
        chip.setPadding(dp(10), dp(4), dp(10), dp(4));
        chip.setBackground(rounded(background, Color.TRANSPARENT, dp(8)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(0, 0, dp(8), 0);
        chip.setLayoutParams(params);
        return chip;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setIncludeFontPadding(true);
        if (bold) {
            text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return text;
    }

    private EditText input(String value, String hint) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setHint(hint);
        input.setTextSize(14);
        input.setSingleLine(false);
        input.setMinHeight(dp(46));
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setBackground(rounded(Color.rgb(245, 247, 251), LINE, dp(8)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(10));
        input.setLayoutParams(params);
        return input;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(rounded(SURFACE, LINE, dp(8)));
        card.setElevation(dp(2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(params);
        return card;
    }

    private Button button(String value, int background, int foreground, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setTextColor(foreground);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(38));
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(rounded(background, Color.TRANSPARENT, dp(8)));
        button.setElevation(dp(2));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                view.animate()
                        .scaleX(0.97f)
                        .scaleY(0.97f)
                        .setDuration(55)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                view.animate().scaleX(1f).scaleY(1f).setDuration(75).start();
                                listener.onClick(view);
                            }
                        })
                        .start();
            }
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, dp(8), dp(8));
        button.setLayoutParams(params);
        return button;
    }

    private Drawable premiumPanelBackground() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {
                        Color.rgb(255, 255, 255),
                        Color.rgb(244, 250, 255),
                        Color.rgb(239, 253, 246)
                }
        );
        drawable.setStroke(dp(1), Color.rgb(228, 235, 245));
        return drawable;
    }

    private GradientDrawable rounded(int color, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (stroke != Color.TRANSPARENT) {
            drawable.setStroke(dp(1), stroke);
        }
        return drawable;
    }

    private void clearPanelRefs() {
        statusValue = null;
        scoreValue = null;
        gradeValue = null;
        outputsValue = null;
        coachValue = null;
        countsValue = null;
        voltageValue = null;
        selectedValue = null;
        challengeValue = null;
        teacherMetricsValue = null;
        chatLog = null;
        projectNameInput = null;
        teacherGradeInput = null;
        teacherFeedbackInput = null;
        chatInput = null;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
