package com.mugetsu.injector;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Injector extends JFrame {

    private static final Color C_BG      = new Color(0x0A0A14);
    private static final Color C_PANEL   = new Color(0x14141E);
    private static final Color C_ACCENT  = new Color(0x7722CC);
    private static final Color C_DIM_ACC = new Color(0x551199);
    private static final Color C_TEXT    = new Color(0xE0E0FF);
    private static final Color C_DIM     = new Color(0xBBBBEE);
    private static final Color C_SEL     = new Color(0x2A1040);
    private static final Color C_OK      = new Color(0x44DD88);
    private static final Color C_ERR     = new Color(0xFF4466);
    private static final Color C_CLOSE   = new Color(0xCC2244);

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> processList          = new JList<>(listModel);
    private final JLabel statusLabel                 = new JLabel("Select a process and click Inject.");
    private final JPanel statusDot                   = new JPanel();
    private final List<VirtualMachineDescriptor> foundVMs = new ArrayList<>();

    private Point dragOrigin;

    public Injector() {
        super("Mugetsu Client");
        buildUI();
        refreshProcesses();
    }

    private void buildUI() {
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        // Size relative to screen — same principle as NMIT-POS maximized windows,
        // avoids fixed pixel sizes that look wrong at different HiDPI scales
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.max(560, Math.min(780, (int)(screen.width  * 0.38)));
        int h = Math.max(400, Math.min(540, (int)(screen.height * 0.48)));
        setSize(w, h);
        setLocationRelativeTo(null);

        getRootPane().setBorder(BorderFactory.createLineBorder(C_DIM_ACC, 1));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);
        setContentPane(root);

        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(buildCenter(),   BorderLayout.CENTER);
        root.add(buildBottom(),   BorderLayout.SOUTH);

        setVisible(true);
    }

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, C_DIM_ACC, getWidth(), 0, C_ACCENT));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(0, 52));
        bar.setBorder(new EmptyBorder(6, 14, 6, 10));

        JPanel brand = new JPanel(new GridLayout(2, 1, 0, 1));
        brand.setOpaque(false);
        JLabel name = new JLabel("Mugetsu Client");
        name.setFont(new Font("SansSerif", Font.BOLD, 18));
        name.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Injection Interface");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 10));
        sub.setForeground(new Color(0xCCAEFF));
        brand.add(name);
        brand.add(sub);
        bar.add(brand, BorderLayout.CENTER);

        JLabel close = new JLabel("  ×  ");
        close.setFont(new Font("SansSerif", Font.BOLD, 18));
        close.setForeground(Color.WHITE);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { close.setForeground(C_CLOSE); }
            @Override public void mouseExited(MouseEvent e)  { close.setForeground(Color.WHITE); }
            @Override public void mouseClicked(MouseEvent e) { System.exit(0); }
        });
        bar.add(close, BorderLayout.EAST);

        MouseAdapter drag = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { dragOrigin = e.getLocationOnScreen(); }
            @Override public void mouseDragged(MouseEvent e)  {
                if (dragOrigin == null) return;
                Point cur = e.getLocationOnScreen();
                setLocation(getX() + cur.x - dragOrigin.x, getY() + cur.y - dragOrigin.y);
                dragOrigin = cur;
            }
        };
        bar.addMouseListener(drag);
        bar.addMouseMotionListener(drag);
        return bar;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(C_BG);
        p.setBorder(new EmptyBorder(10, 12, 6, 12));

        JLabel lbl = new JLabel("Minecraft Processes");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(C_DIM);
        p.add(lbl, BorderLayout.NORTH);

        processList.setBackground(C_PANEL);
        processList.setForeground(C_DIM);
        processList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        processList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        processList.setCellRenderer(new ProcessRenderer());
        processList.setBorder(new EmptyBorder(4, 6, 4, 6));
        processList.setFixedCellHeight(26);

        JScrollPane scroll = new JScrollPane(processList);
        scroll.setBorder(BorderFactory.createLineBorder(C_DIM_ACC, 1));
        scroll.getViewport().setBackground(C_PANEL);
        scroll.getVerticalScrollBar().setBackground(C_PANEL);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildBottom() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(C_BG);
        p.setBorder(new EmptyBorder(4, 12, 12, 12));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setBackground(C_BG);
        statusDot.setPreferredSize(new Dimension(8, 8));
        statusDot.setBackground(C_DIM);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setForeground(C_DIM);
        left.add(statusDot);
        left.add(statusLabel);

        JButton refresh = makeBtn("Refresh", false);
        JButton inject  = makeBtn("Inject",  true);
        refresh.addActionListener(e -> refreshProcesses());
        inject.addActionListener(e -> inject());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(C_BG);
        right.add(refresh);
        right.add(inject);

        p.add(left,  BorderLayout.CENTER);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    // ---- Logic ----

    private void refreshProcesses() {
        listModel.clear();
        foundVMs.clear();
        for (VirtualMachineDescriptor vm : VirtualMachine.list()) {
            String n = vm.displayName().toLowerCase();
            if (n.contains("minecraft") || n.contains("net.minecraft")
                    || n.contains("com.mojang") || n.contains("launchwrapper")
                    || n.contains("fabriclauncher") || n.contains("gradleclient")) {
                foundVMs.add(vm);
                listModel.addElement(String.format("[%s]  %s", vm.id(), vm.displayName()));
            }
        }
        if (listModel.isEmpty()) {
            listModel.addElement("No Minecraft instances found — launch Minecraft first.");
            setStatus("No processes detected.", false);
        } else {
            setStatus(foundVMs.size() + " instance(s) found.", true);
        }
    }

    private void inject() {
        int idx = processList.getSelectedIndex();
        if (idx < 0 || idx >= foundVMs.size()) { setStatus("Select a process first.", false); return; }
        File agent = resolveAgentJar();
        if (agent == null || !agent.exists()) {
            setStatus("agent.jar not found — run: ./gradlew :agent:jar", false); return;
        }
        VirtualMachineDescriptor desc = foundVMs.get(idx);
        setStatus("Attaching to PID " + desc.id() + "...", true);
        new Thread(() -> {
            try {
                VirtualMachine vm = VirtualMachine.attach(desc.id());
                vm.loadAgent(agent.getAbsolutePath());
                vm.detach();
                SwingUtilities.invokeLater(() ->
                    setStatus("Injected into " + desc.id() + ". Press RShift in-game.", true));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> setStatus("Failed: " + e.getMessage(), false));
            }
        }, "Mugetsu-Inject").start();
    }

    private File resolveAgentJar() {
        try {
            File self = new File(Injector.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
            File s = new File(self.getParent(), "agent.jar");
            if (s.exists()) return s;
        } catch (Exception ignored) {}
        if (new File("agent.jar").exists()) return new File("agent.jar");
        return new File("dist/agent.jar");
    }

    private void setStatus(String msg, boolean ok) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setForeground(ok ? C_OK : C_ERR);
            statusLabel.setText(msg);
            statusDot.setBackground(ok ? C_OK : C_ERR);
        });
    }

    // ---- Helpers ----

    private JButton makeBtn(String text, boolean primary) {
        Color bg = primary ? C_ACCENT : C_PANEL;
        Color hover = primary ? new Color(0x9933FF) : new Color(0x1E1030);
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? hover : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(C_DIM_ACC);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setForeground(C_TEXT);
        b.setPreferredSize(new Dimension(100, 32));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private class ProcessRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(
                JList<?> list, Object value, int idx, boolean sel, boolean focus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(list, value, idx, sel, focus);
            l.setFont(new Font("Monospaced", Font.PLAIN, 12));
            l.setBorder(new EmptyBorder(3, 8, 3, 8));
            l.setBackground(sel ? C_SEL : (idx % 2 == 0 ? C_PANEL : new Color(0x11111A)));
            l.setForeground(sel ? C_TEXT : C_DIM);
            return l;
        }
    }

    // ---- Entry point — mirrors NMIT-POS App.java ----

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext",                "true");
        System.setProperty("sun.java2d.xrender",          "true");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}
            new Injector();
        });
    }
}
