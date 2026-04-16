package com.educircuit.mobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(244, 247, 250);
    private static final int SURFACE = Color.WHITE;
    private static final int INK = Color.rgb(18, 24, 31);
    private static final int GRAPHITE = Color.rgb(29, 35, 43);
    private static final int GRAPHITE_2 = Color.rgb(44, 51, 61);
    private static final int MUTED = Color.rgb(103, 116, 132);
    private static final int LINE = Color.rgb(222, 230, 239);
    private static final int PRIMARY = Color.rgb(9, 139, 119);
    private static final int PRIMARY_DARK = Color.rgb(6, 93, 82);
    private static final int CORAL = Color.rgb(232, 87, 82);
    private static final int GOLD = Color.rgb(235, 171, 48);
    private static final int CYAN = Color.rgb(0, 145, 170);
    private static final int SOFT_TEAL = Color.rgb(226, 247, 242);
    private static final int SOFT_CORAL = Color.rgb(255, 238, 236);
    private static final int SOFT_GOLD = Color.rgb(255, 248, 229);
    private static final int SOFT_CYAN = Color.rgb(229, 248, 251);
    private static final int SOFT_GRAPHITE = Color.rgb(239, 242, 246);

    private final CircuitEngine engine = new CircuitEngine();
    private final AiTeacher aiTeacher = new AiTeacher();
    private final StringBuilder chatHistory = new StringBuilder();

    private ProjectStore projectStore;
    private CircuitBoardView boardView;
    private FrameLayout pageHost;
    private LinearLayout navBar;
    private String activePage = "studio";
    private String projectName = "Smart Circuit Lab";
    private String teacherGrade = "";
    private String teacherFeedback = "";
    private CircuitEngine.SimulationResult latestResult;

    private TextView headerStatusValue;
    private TextView headerScoreValue;
    private TextView headerProjectValue;
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

        boardView = new CircuitBoardView(this);
        boardView.setOnBoardChangedListener(new CircuitBoardView.OnBoardChangedListener() {
            @Override
            public void onBoardChanged() {
                latestResult = null;
                refreshStats();
            }
        });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        applySystemBarInsets(root);

        root.addView(buildHeader());

        pageHost = new FrameLayout(this);
        pageHost.setBackgroundColor(BG);
        root.addView(pageHost, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        navBar = buildNavBar();
        root.addView(navBar);

        setContentView(root);
        latestResult = engine.simulate(boardView.snapshot());
        boardView.setSimulationResult(latestResult);
        renderPage("studio");
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
                view.setPadding(0, topInset, 0, bottomInset);
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
        header.setPadding(dp(16), dp(12), dp(16), dp(12));
        header.setBackground(headerBackground());
        header.setElevation(dp(8));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text("EduCircuit Studio", 24, Color.WHITE, true));
        TextView subtitle = text("Build clean. Test fast. Present with confidence.", 13, Color.rgb(216, 226, 235), false);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(subtitle);
        titleRow.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        titleRow.addView(pill("Demo Ready", PRIMARY, Color.WHITE));
        header.addView(titleRow);

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        metrics.setPadding(0, dp(12), 0, 0);
        headerProjectValue = headerMetric(metrics, "Project", projectName, 1.4f);
        headerStatusValue = headerMetric(metrics, "Status", "Ready", 1f);
        headerScoreValue = headerMetric(metrics, "Score", "--", 0.8f);
        header.addView(metrics);
        return header;
    }

    private LinearLayout buildNavBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(dp(10), dp(8), dp(10), dp(8));
        bar.setBackground(navBackground());
        bar.setElevation(dp(10));
        return bar;
    }

    private void renderPage(String page) {
        activePage = page;
        clearPageRefs();
        pageHost.removeAllViews();

        if ("build".equals(page)) {
            renderBuildPage();
        } else if ("review".equals(page)) {
            renderReviewPage();
        } else if ("coach".equals(page)) {
            renderCoachPage();
        } else {
            renderStudioPage();
        }

        updateNavBar();
        refreshStats();
    }

    private void renderStudioPage() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(14), dp(14), dp(10));

        LinearLayout mission = card();
        mission.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout missionTop = new LinearLayout(this);
        missionTop.setOrientation(LinearLayout.HORIZONTAL);
        missionTop.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout missionCopy = new LinearLayout(this);
        missionCopy.setOrientation(LinearLayout.VERTICAL);
        missionCopy.addView(label("Current Mission"));
        challengeValue = text("", 15, INK, true);
        challengeValue.setLineSpacing(dp(2), 1f);
        missionCopy.addView(challengeValue);
        missionTop.addView(missionCopy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        missionTop.addView(action("Run Lab", PRIMARY, Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runLab();
            }
        }));
        mission.addView(missionTop);
        page.addView(mission);

        FrameLayout boardShell = new FrameLayout(this);
        boardShell.setPadding(dp(1), dp(1), dp(1), dp(1));
        boardShell.setBackground(boardShellBackground());
        boardShell.setElevation(dp(3));
        attachBoard(boardShell);
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        boardParams.setMargins(0, 0, 0, dp(12));
        page.addView(boardShell, boardParams);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.addView(weightedAction("Auto Wire", SOFT_TEAL, PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boardView.autoWire();
                runLab();
            }
        }));
        controls.addView(weightedAction("Build Parts", SOFT_CYAN, CYAN, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                renderPage("build");
            }
        }));
        controls.addView(weightedAction("Clear", SOFT_CORAL, CORAL, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boardView.clearBoard();
                latestResult = null;
                refreshStats();
                toast("Board cleared");
            }
        }));
        page.addView(controls);

        pageHost.addView(page);
    }

    private void renderBuildPage() {
        ScrollView scroll = pageScroll();
        LinearLayout content = pageContent(scroll);
        content.addView(pageHeading("Build Room", "Templates, components, wiring, and power are kept here."));

        LinearLayout templates = card();
        templates.addView(sectionTitle("Templates"));
        templates.addView(tileRow(
                templateTile("LED Loop", "Safe starter circuit", "led", SOFT_TEAL, PRIMARY),
                templateTile("Door Alarm", "Switch and buzzer", "alarm", SOFT_GOLD, Color.rgb(151, 105, 23))
        ));
        templates.addView(tileRow(
                templateTile("Mini Fan", "Switch and motor", "fan", SOFT_CYAN, CYAN),
                templateTile("Plant Build", "Sensor and pump", "plant", SOFT_CORAL, CORAL)
        ));
        content.addView(templates);

        LinearLayout project = card();
        project.addView(sectionTitle("Project"));
        projectNameInput = input(projectName, "Project name");
        project.addView(projectNameInput);
        LinearLayout projectActions = actionRow();
        projectActions.addView(weightedAction("Rename", PRIMARY, Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                renameProject();
            }
        }));
        projectActions.addView(weightedAction("Save", SOFT_GOLD, Color.rgb(151, 105, 23), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveProject();
            }
        }));
        projectActions.addView(weightedAction("Load", SOFT_GRAPHITE, GRAPHITE, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadProject();
            }
        }));
        project.addView(projectActions);
        content.addView(project);

        LinearLayout palette = card();
        palette.addView(sectionTitle("Component Bay"));
        palette.addView(text("Add one part at a time, then open Studio to place and wire it.", 13, MUTED, false));
        String[] catalog = CircuitComponent.catalog();
        for (int index = 0; index < catalog.length; index += 2) {
            LinearLayout row = tileRow(
                    componentTile(catalog[index]),
                    index + 1 < catalog.length ? componentTile(catalog[index + 1]) : spacer()
            );
            palette.addView(row);
        }
        content.addView(palette);

        LinearLayout inspector = card();
        inspector.addView(sectionTitle("Inspector"));
        selectedValue = text("", 14, INK, false);
        selectedValue.setLineSpacing(dp(3), 1f);
        inspector.addView(selectedValue);
        LinearLayout inspectorActions = actionRow();
        inspectorActions.addView(weightedAction("Duplicate", SOFT_TEAL, PRIMARY, new View.OnClickListener() {
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
        inspectorActions.addView(weightedAction("Delete", SOFT_CORAL, CORAL, new View.OnClickListener() {
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
        content.addView(inspector);

        LinearLayout voltage = card();
        voltage.addView(sectionTitle("Battery Voltage"));
        voltageValue = text("", 18, PRIMARY_DARK, true);
        voltage.addView(voltageValue);
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(60);
        seekBar.setProgress(Math.round(boardView.getBatteryVoltage() * 2));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    boardView.setBatteryVoltage(progress / 2.0f);
                    refreshStats();
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
        content.addView(voltage);

        pageHost.addView(scroll);
    }

    private void renderReviewPage() {
        ScrollView scroll = pageScroll();
        LinearLayout content = pageContent(scroll);
        content.addView(pageHeading("Review Deck", "Score, feedback, and a clean handoff for presentation."));

        LinearLayout stats = card();
        stats.addView(sectionTitle("Snapshot"));
        LinearLayout rowOne = tileRow(
                statTile("Status", "", PRIMARY),
                statTile("Score", "", GOLD)
        );
        statusValue = (TextView) rowOne.getChildAt(0).findViewWithTag("value");
        scoreValue = (TextView) rowOne.getChildAt(1).findViewWithTag("value");
        stats.addView(rowOne);
        LinearLayout rowTwo = tileRow(
                statTile("Grade", "", CORAL),
                statTile("Parts", "", CYAN)
        );
        gradeValue = (TextView) rowTwo.getChildAt(0).findViewWithTag("value");
        countsValue = (TextView) rowTwo.getChildAt(1).findViewWithTag("value");
        stats.addView(rowTwo);
        outputsValue = text("", 14, INK, false);
        outputsValue.setPadding(0, dp(10), 0, 0);
        stats.addView(outputsValue);
        content.addView(stats);

        LinearLayout coach = card();
        coach.addView(sectionTitle("Circuit Coach"));
        coachValue = text("", 14, INK, false);
        coachValue.setLineSpacing(dp(3), 1f);
        coach.addView(coachValue);
        content.addView(coach);

        LinearLayout teacher = card();
        teacher.addView(sectionTitle("Teacher Notes"));
        teacherGradeInput = input(teacherGrade, "A+, 95, Excellent");
        teacher.addView(teacherGradeInput);
        teacherFeedbackInput = input(teacherFeedback, "Write feedback for the student's circuit");
        teacherFeedbackInput.setMinLines(4);
        teacherFeedbackInput.setGravity(Gravity.TOP);
        teacher.addView(teacherFeedbackInput);
        LinearLayout teacherActions = actionRow();
        teacherActions.addView(weightedAction("Apply", PRIMARY, Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                teacherGrade = teacherGradeInput.getText().toString().trim();
                teacherFeedback = teacherFeedbackInput.getText().toString().trim();
                toast("Teacher feedback applied");
                refreshStats();
            }
        }));
        teacherActions.addView(weightedAction("Copy Summary", SOFT_GRAPHITE, GRAPHITE, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copySummary();
            }
        }));
        teacher.addView(teacherActions);
        content.addView(teacher);

        LinearLayout summary = card();
        summary.addView(sectionTitle("Presentation Summary"));
        teacherMetricsValue = text("", 13, MUTED, false);
        teacherMetricsValue.setLineSpacing(dp(3), 1f);
        summary.addView(teacherMetricsValue);
        content.addView(summary);

        pageHost.addView(scroll);
    }

    private void renderCoachPage() {
        ScrollView scroll = pageScroll();
        LinearLayout content = pageContent(scroll);
        content.addView(pageHeading("AI Coach", "Fast explanations without leaving the lab."));

        LinearLayout quick = card();
        quick.addView(sectionTitle("Quick Prompts"));
        quick.addView(tileRow(
                promptTile("Explain my circuit"),
                promptTile("Why is it not working?")
        ));
        quick.addView(tileRow(
                promptTile("Teach me voltage"),
                promptTile("Quiz me")
        ));
        content.addView(quick);

        LinearLayout chat = card();
        chat.addView(sectionTitle("Coach Chat"));
        chatLog = text(chatHistory.length() == 0
                ? "Ask about voltage, resistors, short circuits, or the first exact fix."
                : chatHistory.toString(), 14, INK, false);
        chatLog.setLineSpacing(dp(3), 1f);
        chat.addView(chatLog);
        chatInput = input("", "Ask your AI coach");
        chat.addView(chatInput);
        chat.addView(fullAction("Send", PRIMARY, Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendChat();
            }
        }));
        content.addView(chat);

        pageHost.addView(scroll);
    }

    private void attachBoard(FrameLayout parent) {
        ViewGroup oldParent = (ViewGroup) boardView.getParent();
        if (oldParent != null) {
            oldParent.removeView(boardView);
        }
        parent.addView(boardView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    private TextView templateTile(final String title, String detail, final String template, int background, int foreground) {
        TextView tile = tile(title + "\n" + detail, background, foreground);
        tile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyTemplate(template, title);
            }
        });
        return tile;
    }

    private TextView componentTile(final String type) {
        TextView tile = tile(type + "\nAdd to board", Color.rgb(248, 250, 252), CircuitComponent.colorFor(type));
        tile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boardView.addComponent(type);
                latestResult = null;
                refreshStats();
                toast(type + " added");
            }
        });
        return tile;
    }

    private TextView promptTile(final String prompt) {
        TextView tile = tile(prompt, SOFT_TEAL, PRIMARY);
        tile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askCoach(prompt);
            }
        });
        return tile;
    }

    private LinearLayout statTile(String label, String value, int accent) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dp(12), dp(10), dp(12), dp(10));
        tile.setBackground(rounded(Color.rgb(248, 250, 252), LINE, dp(8)));

        TextView caption = text(label, 12, MUTED, true);
        TextView statValue = text(value, 18, accent, true);
        statValue.setTag("value");
        statValue.setSingleLine(true);
        statValue.setEllipsize(TextUtils.TruncateAt.END);
        tile.addView(caption);
        tile.addView(statValue);
        return tile;
    }

    private void applyTemplate(String template, String name) {
        projectName = name;
        boardView.loadTemplate(template);
        latestResult = boardView.runSimulation();
        renderPage("studio");
        toast(name + " loaded");
    }

    private void renameProject() {
        if (projectNameInput != null) {
            projectName = projectNameInput.getText().toString().trim();
        }
        if (projectName.length() == 0) {
            projectName = "Untitled Circuit";
            if (projectNameInput != null) {
                projectNameInput.setText(projectName);
            }
        }
        refreshStats();
        toast("Project renamed");
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
            refreshStats();
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
            renderPage("studio");
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
        if (headerProjectValue != null) {
            headerProjectValue.setText(projectName);
        }
        if (headerStatusValue != null) {
            headerStatusValue.setText(result.status);
        }
        if (headerScoreValue != null) {
            headerScoreValue.setText(result.score + "/100");
        }
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
            coachValue.setText(result.hint + "\n\nNext move: " + result.fix);
        }
        if (countsValue != null) {
            countsValue.setText(boardView.getComponentCount() + " parts, " + boardView.getWireCount() + " wires");
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
            return "Advanced build complete. Open Review for final notes.";
        }
        if ("Working Circuit".equals(result.status)) {
            return "Working build. Add a sensor to push the score higher.";
        }
        if (result.overload) {
            return "Safety check: fix the warning before presenting.";
        }
        return "Make a safe closed loop that powers one output.";
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

    private ScrollView pageScroll() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setBackgroundColor(BG);
        return scroll;
    }

    private LinearLayout pageContent(ScrollView scroll) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(14), dp(14), dp(18));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return content;
    }

    private LinearLayout pageHeading(String title, String subtitle) {
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.setPadding(0, 0, 0, dp(12));
        heading.addView(text(title, 22, INK, true));
        TextView detail = text(subtitle, 13, MUTED, false);
        detail.setLineSpacing(dp(2), 1f);
        heading.addView(detail);
        return heading;
    }

    private LinearLayout tileRow(View first, View second) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams firstParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        firstParams.setMargins(0, dp(8), dp(6), 0);
        row.addView(first, firstParams);
        LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        secondParams.setMargins(dp(6), dp(8), 0, 0);
        row.addView(second, secondParams);
        return row;
    }

    private View spacer() {
        View spacer = new View(this);
        spacer.setVisibility(View.INVISIBLE);
        return spacer;
    }

    private LinearLayout actionRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dp(4), 0, 0);
        return row;
    }

    private TextView weightedAction(String value, int background, int foreground, View.OnClickListener listener) {
        TextView button = action(value, background, foreground, listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1f);
        params.setMargins(dp(4), 0, dp(4), 0);
        button.setLayoutParams(params);
        return button;
    }

    private TextView fullAction(String value, int background, int foreground, View.OnClickListener listener) {
        TextView button = action(value, background, foreground, listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
        );
        params.setMargins(0, dp(4), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private TextView action(String value, int background, int foreground, final View.OnClickListener listener) {
        TextView button = text(value, 13, foreground, true);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(42));
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(rounded(background, Color.TRANSPARENT, dp(8)));
        button.setClickable(true);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                view.animate()
                        .scaleX(0.98f)
                        .scaleY(0.98f)
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
        return button;
    }

    private TextView tile(String value, int background, int foreground) {
        TextView tile = text(value, 13, foreground, true);
        tile.setGravity(Gravity.CENTER);
        tile.setMinHeight(dp(64));
        tile.setPadding(dp(10), dp(8), dp(10), dp(8));
        tile.setLineSpacing(dp(2), 1f);
        tile.setBackground(rounded(background, LINE, dp(8)));
        tile.setClickable(true);
        return tile;
    }

    private TextView sectionTitle(String value) {
        TextView title = text(value, 17, INK, true);
        title.setPadding(0, 0, 0, dp(8));
        return title;
    }

    private TextView label(String value) {
        TextView label = text(value, 12, MUTED, true);
        label.setPadding(0, 0, 0, dp(5));
        return label;
    }

    private TextView pill(String value, int background, int foreground) {
        TextView pill = text(value, 12, foreground, true);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(12), dp(7), dp(12), dp(7));
        pill.setBackground(rounded(background, Color.TRANSPARENT, dp(8)));
        return pill;
    }

    private TextView headerMetric(LinearLayout parent, String label, String value, float weight) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(10), dp(7), dp(10), dp(7));
        chip.setBackground(rounded(Color.argb(48, 255, 255, 255), Color.argb(68, 255, 255, 255), dp(8)));

        TextView caption = text(label, 10, Color.rgb(196, 207, 218), true);
        TextView main = text(value, 12, Color.WHITE, true);
        main.setSingleLine(true);
        main.setEllipsize(TextUtils.TruncateAt.END);
        chip.addView(caption);
        chip.addView(main);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        params.setMargins(0, 0, dp(8), 0);
        parent.addView(chip, params);
        return main;
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
        input.setMinHeight(dp(48));
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        input.setTextColor(INK);
        input.setHintTextColor(MUTED);
        input.setBackground(rounded(Color.rgb(248, 250, 252), LINE, dp(8)));
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

    private void updateNavBar() {
        navBar.removeAllViews();
        navBar.addView(navItem("Studio", "studio"));
        navBar.addView(navItem("Build", "build"));
        navBar.addView(navItem("Review", "review"));
        navBar.addView(navItem("Coach", "coach"));
    }

    private TextView navItem(String label, final String page) {
        boolean active = activePage.equals(page);
        TextView item = text(label, 13, active ? Color.WHITE : MUTED, true);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(8), dp(10), dp(8), dp(10));
        item.setBackground(rounded(active ? GRAPHITE : Color.TRANSPARENT, Color.TRANSPARENT, dp(8)));
        item.setClickable(true);
        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                renderPage(page);
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        item.setLayoutParams(params);
        return item;
    }

    private Drawable headerBackground() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] { GRAPHITE, GRAPHITE_2, Color.rgb(21, 69, 62) }
        );
        drawable.setCornerRadius(0);
        return drawable;
    }

    private Drawable navBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.WHITE);
        drawable.setStroke(dp(1), LINE);
        return drawable;
    }

    private Drawable boardShellBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.WHITE);
        drawable.setStroke(dp(1), Color.rgb(208, 218, 229));
        drawable.setCornerRadius(dp(8));
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

    private void clearPageRefs() {
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
