package com.mugetsu.injector;

import com.formdev.flatlaf.FlatDarkLaf;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Injector extends JFrame {

    private static final Color C_BG       = new Color(0x0A0A14);
    private static final Color C_PANEL    = new Color(0x14141E);
    private static final Color C_ACCENT   = new Color(0x7722CC);
    private static final Color C_TEXT     = new Color(0xE0E0FF);
    private static final Color C_DIM      = new Color(0xBBBBEE);
    private static final Color C_SEL      = new Color(0x2A1040);
    private static final Color C_OK       = new Color(0x44DD88);
    private static final Color C_ERR      = new Color(0xFF4466);

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> processList          = new JList<>(listModel);
    private final JLabel statusLabel                 = new JLabel("Select a process and click Inject.");
    private final JPanel statusDot                   = new JPanel();
    private final List<VirtualMachineDescriptor> foundVMs = new ArrayList<>();

    public Injector() {
        super("Mugetsu Client");
        buildUI();
        refreshProcesses();
    }

    private void buildUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(640, 460);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(C_BG);
        setContentPane(root);

        root.add(buildBrand(),  BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildBottom(), BorderLayout.SOUTH);

        setVisible(true);
    }

    // Branding strip below GNOME's native title bar
    private JPanel buildBrand() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_PANEL);
        p.setBorder(new EmptyBorder(8, 14, 8, 14));

        JLabel sub = new JLabel("Injection Interface  —  Mugetsu Client");
        sub.setFont(tryFont("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(new Color(0x9966CC));
        p.add(sub, BorderLayout.WEST);

        JSeparator sep = new JSeparator();
        sep.setForeground(C_ACCENT.darker());
        p.add(sep, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(C_BG);
        p.setBorder(new EmptyBorder(10, 12, 6, 12));

        JLabel lbl = new JLabel("Minecraft Processes");
        lbl.setFont(tryFont("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(C_DIM);
        p.add(lbl, BorderLayout.NORTH);

        processList.setBackground(C_PANEL);
        processList.setForeground(C_DIM);
        processList.setFont(tryFont("JetBrains Mono", Font.PLAIN, 12));
        processList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        processList.setCellRenderer(new ProcessRenderer());
        processList.setBorder(new EmptyBorder(4, 6, 4, 6));
        processList.setFixedCellHeight(26);

        JScrollPane scroll = new JScrollPane(processList);
        scroll.setBorder(BorderFactory.createLineBorder(C_ACCENT.darker(), 1));
        scroll.getViewport().setBackground(C_PANEL);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildBottom() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(C_BG);
        p.setBorder(new EmptyBorder(4, 12, 12, 12));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setBackground(C_BG);
        statusDot.setPreferredSize(new Dimension(9, 9));
        statusDot.setBackground(C_DIM);
        statusLabel.setFont(tryFont("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(C_DIM);
        left.add(statusDot);
        left.add(statusLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(C_BG);
        JButton refresh = makeBtn("Refresh", false);
        JButton inject  = makeBtn("Inject",  true);
        refresh.addActionListener(e -> refreshProcesses());
        inject.addActionListener(e -> inject());
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
            listModel.addElement("No Minecraft instances found — launch Minecraft first, then Refresh.");
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
            setStatus("agent.jar not found — run: ./gradlew :agent:jar", false);
            return;
        }

        VirtualMachineDescriptor desc = foundVMs.get(idx);
        setStatus("Attaching to PID " + desc.id() + "...", true);

        new Thread(() -> {
            try {
                VirtualMachine vm = VirtualMachine.attach(desc.id());
                vm.loadAgent(agent.getAbsolutePath());
                vm.detach();
                SwingUtilities.invokeLater(() ->
                    setStatus("Injected into PID " + desc.id() + ".  Press RShift in-game.", true));
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
        JButton b = new JButton(text);
        b.setFont(tryFont("Segoe UI", Font.BOLD, 13));
        b.setPreferredSize(new Dimension(110, 34));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "roundRect");
        if (primary) { b.setBackground(C_ACCENT); b.setForeground(Color.WHITE); }
        else         { b.setBackground(C_PANEL);  b.setForeground(C_TEXT); }
        return b;
    }

    private static Font tryFont(String name, int style, int size) {
        Font f = new Font(name, style, size);
        return f.getFamily().equalsIgnoreCase(name) ? f : new Font("SansSerif", style, size);
    }

    private class ProcessRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int idx, boolean sel, boolean focus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(list, value, idx, sel, focus);
            l.setFont(tryFont("JetBrains Mono", Font.PLAIN, 12));
            l.setBorder(new EmptyBorder(3, 8, 3, 8));
            l.setBackground(sel ? C_SEL : (idx % 2 == 0 ? C_PANEL : new Color(0x11111A)));
            l.setForeground(sel ? C_TEXT : C_DIM);
            return l;
        }
    }

    // ---- Entry point ----

    public static void main(String[] args) {
        applyScale();
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        FlatDarkLaf.setup();
        UIManager.put("Component.accentColor",          new Color(0x7722CC));
        UIManager.put("Button.default.background",      new Color(0x7722CC));
        UIManager.put("Button.default.hoverBackground", new Color(0x9933FF));
        UIManager.put("ScrollBar.thumb",                new Color(0x551199));
        UIManager.put("ScrollBar.thumbHoverColor",      new Color(0x7722CC));
        UIManager.put("List.selectionBackground",       new Color(0x2A1040));
        UIManager.put("List.selectionForeground",       new Color(0xE0E0FF));
        // Let GNOME/the compositor own window decorations — no FlatLaf chrome
        JFrame.setDefaultLookAndFeelDecorated(false);

        SwingUtilities.invokeLater(Injector::new);
    }

    private static void applyScale() {
        // 1. Explicit env var (user can set GDK_SCALE=2 before launching)
        String scale = System.getenv("GDK_SCALE");

        // 2. Fractional GDK scale
        if (scale == null) scale = System.getenv("GDK_DPI_SCALE");

        // 3. Try to read GNOME scaling-factor via gsettings
        if (scale == null) {
            try {
                Process p = Runtime.getRuntime().exec(
                    new String[]{"gsettings", "get",
                        "org.gnome.desktop.interface", "scaling-factor"});
                String out = new String(p.getInputStream().readAllBytes()).strip();
                if (!out.equals("0") && !out.isEmpty()) scale = out;
            } catch (Throwable ignored) {}
        }

        // 4. Derive from screen DPI reported by AWT
        if (scale == null) {
            int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
            if (dpi > 120) scale = String.valueOf(Math.round(dpi / 96.0));
        }

        if (scale != null && !scale.isEmpty()) {
            System.setProperty("sun.java2d.uiScale", scale.trim());
        }
    }
}
