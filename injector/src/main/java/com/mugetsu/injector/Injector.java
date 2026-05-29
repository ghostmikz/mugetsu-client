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

    // Void Purple palette
    private static final Color C_BG_DARK   = new Color(0x0A0A14);
    private static final Color C_BG_PANEL  = new Color(0x14141E);
    private static final Color C_ACCENT    = new Color(0x7722CC);
    private static final Color C_HIGHLIGHT = new Color(0x9933FF);
    private static final Color C_TEXT      = new Color(0xE0E0FF);
    private static final Color C_DIM       = new Color(0xBBBBEE);
    private static final Color C_SEL_BG    = new Color(0x2A1040);
    private static final Color C_OK        = new Color(0x44DD88);
    private static final Color C_ERR       = new Color(0xFF4466);

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
        setSize(620, 440);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(C_BG_DARK);
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildBottom(), BorderLayout.SOUTH);
    }

    // ---- Header ----
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(C_ACCENT);
        header.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel name = new JLabel("Mugetsu Client");
        name.setFont(tryFont("Segoe UI", Font.BOLD, 20));
        name.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Injection Interface");
        sub.setFont(tryFont("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(new Color(0xCCAEFF));

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 1));
        text.setOpaque(false);
        text.add(name);
        text.add(sub);
        header.add(text, BorderLayout.CENTER);
        return header;
    }

    // ---- Process list ----
    private JPanel buildCenter() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(C_BG_DARK);
        panel.setBorder(new EmptyBorder(10, 12, 6, 12));

        JLabel title = new JLabel("Minecraft Processes");
        title.setFont(tryFont("Segoe UI", Font.BOLD, 11));
        title.setForeground(C_DIM);
        panel.add(title, BorderLayout.NORTH);

        processList.setBackground(C_BG_PANEL);
        processList.setForeground(C_DIM);
        processList.setFont(tryFont("JetBrains Mono", Font.PLAIN, 12));
        processList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        processList.setCellRenderer(new ProcessCellRenderer());
        processList.setBorder(new EmptyBorder(4, 6, 4, 6));
        processList.setFixedCellHeight(26);

        JScrollPane scroll = new JScrollPane(processList);
        scroll.setBorder(BorderFactory.createLineBorder(C_ACCENT.darker(), 1));
        scroll.getViewport().setBackground(C_BG_PANEL);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ---- Bottom bar ----
    private JPanel buildBottom() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(C_BG_DARK);
        bar.setBorder(new EmptyBorder(4, 12, 12, 12));

        JPanel statusArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        statusArea.setBackground(C_BG_DARK);
        statusDot.setPreferredSize(new Dimension(9, 9));
        statusDot.setBackground(C_DIM);
        statusLabel.setFont(tryFont("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(C_DIM);
        statusArea.add(statusDot);
        statusArea.add(statusLabel);

        JButton refreshBtn = makeButton("Refresh", false);
        JButton injectBtn  = makeButton("Inject",  true);
        refreshBtn.addActionListener(e -> refreshProcesses());
        injectBtn.addActionListener(e -> inject());

        JPanel btnArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnArea.setBackground(C_BG_DARK);
        btnArea.add(refreshBtn);
        btnArea.add(injectBtn);

        bar.add(statusArea, BorderLayout.CENTER);
        bar.add(btnArea,    BorderLayout.EAST);
        return bar;
    }

    // ---- Logic ----

    private void refreshProcesses() {
        listModel.clear();
        foundVMs.clear();
        for (VirtualMachineDescriptor vm : VirtualMachine.list()) {
            String name = vm.displayName().toLowerCase();
            if (name.contains("minecraft") || name.contains("net.minecraft")
                    || name.contains("com.mojang") || name.contains("launchwrapper")
                    || name.contains("fabriclauncher") || name.contains("gradleclient")) {
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

        File agentJar = resolveAgentJar();
        if (agentJar == null || !agentJar.exists()) {
            setStatus("agent.jar not found — run: ./gradlew :agent:jar", false);
            return;
        }

        VirtualMachineDescriptor desc = foundVMs.get(idx);
        setStatus("Attaching to PID " + desc.id() + "...", true);

        new Thread(() -> {
            try {
                VirtualMachine vm = VirtualMachine.attach(desc.id());
                vm.loadAgent(agentJar.getAbsolutePath());
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
            File sibling = new File(self.getParent(), "agent.jar");
            if (sibling.exists()) return sibling;
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

    private JButton makeButton(String text, boolean primary) {
        JButton btn = new JButton(text);
        btn.setFont(tryFont("Segoe UI", Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(110, 34));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // FlatLaf client properties for rounded style
        btn.putClientProperty("JButton.buttonType", "roundRect");
        if (primary) {
            btn.setBackground(C_ACCENT);
            btn.setForeground(Color.WHITE);
            btn.putClientProperty("JComponent.outline", "button");
        } else {
            btn.setBackground(C_BG_PANEL);
            btn.setForeground(C_TEXT);
        }
        return btn;
    }

    private static Font tryFont(String name, int style, int size) {
        Font f = new Font(name, style, size);
        return f.getFamily().equalsIgnoreCase(name) ? f : new Font("SansSerif", style, size);
    }

    private class ProcessCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int idx, boolean selected, boolean focused) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(
                list, value, idx, selected, focused);
            lbl.setFont(tryFont("JetBrains Mono", Font.PLAIN, 12));
            lbl.setBorder(new EmptyBorder(3, 8, 3, 8));
            lbl.setBackground(selected ? C_SEL_BG : (idx % 2 == 0 ? C_BG_PANEL : new Color(0x11111A)));
            lbl.setForeground(selected ? C_TEXT : C_DIM);
            return lbl;
        }
    }

    // ---- Entry point ----

    public static void main(String[] args) {
        // HiDPI / Wayland scaling
        String gdkScale = System.getenv("GDK_SCALE");
        System.setProperty("sun.java2d.uiScale",
            (gdkScale != null && !gdkScale.isEmpty()) ? gdkScale : "auto");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        FlatDarkLaf.setup();

        // Void Purple accent applied globally to FlatLaf
        UIManager.put("Component.accentColor",             new Color(0x7722CC));
        UIManager.put("Button.default.background",         new Color(0x7722CC));
        UIManager.put("Button.default.hoverBackground",    new Color(0x9933FF));
        UIManager.put("Button.default.pressedBackground",  new Color(0x551199));
        UIManager.put("ScrollBar.thumb",                   new Color(0x551199));
        UIManager.put("ScrollBar.thumbHoverColor",         new Color(0x7722CC));
        UIManager.put("TextField.caretForeground",         new Color(0x9933FF));
        UIManager.put("List.selectionBackground",          new Color(0x2A1040));
        UIManager.put("List.selectionForeground",          new Color(0xE0E0FF));

        // Let FlatLaf paint window decorations (works on Wayland)
        JFrame.setDefaultLookAndFeelDecorated(true);

        SwingUtilities.invokeLater(Injector::new);
    }
}
