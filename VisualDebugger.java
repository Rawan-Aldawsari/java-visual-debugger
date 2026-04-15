import java.awt.*;
import java.awt.event.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;

public class VisualDebugger extends JFrame {

    static final Color BG_DARK = new Color(30, 30, 40);
    static final Color BG_PANEL = new Color(40, 42, 58);
    static final Color BG_HIGHLIGHT = new Color(255, 200, 50, 60);
    static final Color BG_DONE = new Color(255, 255, 255, 15);
    static final Color COLOR_KEYWORD = new Color(180, 130, 255);
    static final Color COLOR_TYPE = new Color(86, 182, 194);
    static final Color COLOR_NUMBER = new Color(152, 195, 121);
    static final Color COLOR_VAR = new Color(229, 192, 123);
    static final Color COLOR_TEXT = new Color(220, 220, 220);
    static final Color COLOR_MUTED = new Color(130, 130, 150);
    static final Color COLOR_ARROW = new Color(255, 200, 50);
    static final Color COLOR_NEW_VAR = new Color(100, 220, 120);
    static final Color COLOR_CHANGED = new Color(255, 160, 50);
    static final Color BORDER_COLOR = new Color(60, 64, 80);

    static final String[] CODE_LINES = {
        "int a = 5;",
        "int b = 3;",
        "int c = a + b;",
        "b = b * 2;",
        "int d = c - b;",
        "System.out.println(d);"
    };

    static final String[] DESCRIPTIONS = {
        "Initialize variable a with value 5",
        "Initialize variable b with value 3",
        "Compute a + b and store in c",
        "Update b by multiplying by 2",
        "Compute c - b and store in d",
        "Print value of d"
    };

    int currentLine = -1;
    LinkedHashMap<String, Integer> variables = new LinkedHashMap<>();
    String lastChanged = null;
    String lastAdded = null;
    javax.swing.Timer autoTimer;
    boolean running = false;

    CodePanel codePanel;
    StackPanel stackPanel;
    VarsPanel varsPanel;
    JLabel statusLabel;
    JButton btnPlay, btnStep, btnReset, btnPause;
    JSlider speedSlider;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VisualDebugger().setVisible(true));
    }

    public VisualDebugger() {
        setTitle("Visual Debugger - Java");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 620);
        setLocationRelativeTo(null);
        setBackground(BG_DARK);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(BG_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel center = new JPanel(new GridLayout(1, 2, 10, 0));
        center.setOpaque(false);

        codePanel = new CodePanel();
        center.add(wrapInCard("Code Panel", codePanel));

        JPanel rightCol = new JPanel(new GridLayout(2, 1, 0, 10));
        rightCol.setOpaque(false);
        stackPanel = new StackPanel();
        varsPanel = new VarsPanel();
        rightCol.add(wrapInCard("Stack / Memory", stackPanel));
        rightCol.add(wrapInCard("Variables", varsPanel));
        center.add(rightCol);

        root.add(center, BorderLayout.CENTER);
        root.add(buildControls(), BorderLayout.SOUTH);

        add(root);
    }

    JPanel buildControls() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        btnPlay = styledButton("Play", new Color(100, 80, 200));
        btnStep = styledButton("Step", new Color(60, 130, 180));
        btnPause = styledButton("Pause", new Color(180, 130, 40));
        btnReset = styledButton("Reset", new Color(80, 80, 100));

        btnPause.setEnabled(false);

        btnPlay.addActionListener(e -> startAuto());
        btnStep.addActionListener(e -> stepOnce());
        btnPause.addActionListener(e -> pauseAuto());
        btnReset.addActionListener(e -> resetAll());

        JLabel speedLbl = new JLabel("Speed:");
        speedLbl.setForeground(COLOR_MUTED);
        speedLbl.setFont(new Font("Dialog", Font.PLAIN, 12));

        speedSlider = new JSlider(200, 2000, 900);
        speedSlider.setOpaque(false);
        speedSlider.setPreferredSize(new Dimension(120, 28));

        statusLabel = new JLabel("Press Play or Step to start");
        statusLabel.setForeground(COLOR_MUTED);
        statusLabel.setFont(new Font("Dialog", Font.PLAIN, 12));

        bar.add(btnPlay);
        bar.add(btnStep);
        bar.add(btnPause);
        bar.add(btnReset);
        bar.add(Box.createHorizontalStrut(12));
        bar.add(speedLbl);
        bar.add(speedSlider);
        bar.add(Box.createHorizontalStrut(16));
        bar.add(statusLabel);

        return bar;
    }

    void stepOnce() {
        if (currentLine >= CODE_LINES.length - 1) return;
        currentLine++;
        executeCurrentLine();
        repaintAll();

        statusLabel.setForeground(COLOR_ARROW);
        statusLabel.setText("Line " + (currentLine + 1) + ": " + DESCRIPTIONS[currentLine]);

        if (currentLine == CODE_LINES.length - 1) {
            btnPlay.setEnabled(false);
            btnStep.setEnabled(false);
            btnPause.setEnabled(false);
            statusLabel.setForeground(COLOR_NEW_VAR);
            statusLabel.setText("Execution finished - Output: " + variables.getOrDefault("d", 0));
        }
    }

    void executeCurrentLine() {
        lastChanged = null;
        lastAdded = null;
        switch (currentLine) {
            case 0: addVar("a", 5); break;
            case 1: addVar("b", 3); break;
            case 2: addVar("c", getV("a") + getV("b")); break;
            case 3: updateVar("b", getV("b") * 2); break;
            case 4: addVar("d", getV("c") - getV("b")); break;
        }
    }

    void addVar(String name, int value) {
        variables.put(name, value);
        lastAdded = name;
    }

    void updateVar(String name, int value) {
        variables.put(name, value);
        lastChanged = name;
    }

    int getV(String name) {
        return variables.getOrDefault(name, 0);
    }

    void startAuto() {
        if (currentLine >= CODE_LINES.length - 1) return;
        running = true;
        btnPlay.setEnabled(false);
        btnStep.setEnabled(false);
        btnPause.setEnabled(true);

        int delay = speedSlider.getValue();
        autoTimer = new javax.swing.Timer(delay, null);
        autoTimer.addActionListener(e -> {
            if (currentLine >= CODE_LINES.length - 1) {
                autoTimer.stop();
                running = false;
                btnPause.setEnabled(false);
                return;
            }
            stepOnce();
        });
        autoTimer.setDelay(delay);
        autoTimer.start();
    }

    void pauseAuto() {
        if (autoTimer != null) autoTimer.stop();
        running = false;
        btnPlay.setEnabled(true);
        btnStep.setEnabled(true);
        btnPause.setEnabled(false);
        statusLabel.setForeground(COLOR_MUTED);
        statusLabel.setText("Paused - press Play to continue");
    }

    void resetAll() {
        if (autoTimer != null) autoTimer.stop();
        running = false;
        currentLine = -1;
        variables.clear();
        lastChanged = null;
        lastAdded = null;

        btnPlay.setEnabled(true);
        btnStep.setEnabled(true);
        btnPause.setEnabled(false);
        statusLabel.setForeground(COLOR_MUTED);
        statusLabel.setText("Press Play or Step to start");

        repaintAll();
    }

    void repaintAll() {
        codePanel.repaint();
        stackPanel.repaint();
        varsPanel.repaint();
    }

    class CodePanel extends JPanel {
        CodePanel() {
            setBackground(BG_PANEL);
            setPreferredSize(new Dimension(300, 200));
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            int lineH = 38;
            int startY = 20;

            for (int i = 0; i < CODE_LINES.length; i++) {
                int y = startY + i * lineH;

                if (i == currentLine) {
                    g2.setColor(BG_HIGHLIGHT);
                    g2.fillRoundRect(8, y - 14, getWidth() - 16, lineH - 4, 6, 6);
                } else if (i < currentLine) {
                    g2.setColor(BG_DONE);
                    g2.fillRoundRect(8, y - 14, getWidth() - 16, lineH - 4, 6, 6);
                }

                g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
                g2.setColor(COLOR_MUTED);
                g2.drawString(String.valueOf(i + 1), 12, y);

                if (i == currentLine) {
                    g2.setColor(COLOR_ARROW);
                    g2.setFont(new Font("Dialog", Font.BOLD, 13));
                    g2.drawString(">", 32, y);
                }

                g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
                g2.setColor(COLOR_TEXT);
                g2.drawString(CODE_LINES[i], 55, y);
            }
        }
    }

    class StackPanel extends JPanel {
        StackPanel() {
            setBackground(BG_PANEL);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            if (variables.isEmpty()) {
                g2.setColor(COLOR_MUTED);
                g2.drawString("Memory is empty", getWidth() / 2 - 40, getHeight() / 2);
                return;
            }

            String[] keys = variables.keySet().toArray(new String[0]);
            int frameH = 36;
            int padding = 10;
            int totalH = keys.length * (frameH + 6);
            int startY = Math.max(padding, getHeight() - totalH - padding);

            for (int i = 0; i < keys.length; i++) {
                String k = keys[i];
                int y = startY + (keys.length - 1 - i) * (frameH + 6);

                g2.setColor(new Color(55, 58, 78));
                g2.fillRoundRect(padding, y, getWidth() - padding * 2, frameH, 8, 8);

                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(padding, y, getWidth() - padding * 2, frameH, 8, 8);

                g2.setColor(COLOR_VAR);
                g2.drawString(k, padding + 12, y + 23);

                g2.setColor(COLOR_NUMBER);
                String val = String.valueOf(variables.get(k));
                int valW = g2.getFontMetrics().stringWidth(val);
                g2.drawString(val, getWidth() - padding - valW - 10, y + 24);
            }
        }
    }

    class VarsPanel extends JPanel {
        VarsPanel() {
            setBackground(BG_PANEL);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int y = 30;

            for (Map.Entry<String, Integer> entry : variables.entrySet()) {
                g.drawString(entry.getKey() + " = " + entry.getValue(), 20, y);
                y += 25;
            }
        }
    }

    JPanel wrapInCard(String title, JPanel content) {
        JPanel card = new JPanel(new BorderLayout());
        JLabel header = new JLabel("  " + title);
        header.setForeground(COLOR_MUTED);
        card.add(header, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    JButton styledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bg.brighter());
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }
}