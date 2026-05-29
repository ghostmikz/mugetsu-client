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

/**
 * Mugetsu Client — Injection Interface
 * Undecorated window with custom title bar, Void Purple theme, Wayland/GNOME compatible.
 */
public class Injector extends JFrame {

    // ---- Void Purple palette ----
    private static final Color C_BG_DARK   = new Color(0x0A0A14);
    private static final Color C_BG_PANEL  = new Color(0x14141E);
    private static final Color C_ACCENT    = new Color(0x7722CC);
    private static final Color C_ACCENT_DIM= new Color(0x551199);
    private static final Color C_HIGHLIGHT = new Color(0x9933FF);
    private static final Color C_TEXT      = new Color(0xE0E0FF);
    private static final Color C_DIM       = new Color(0xBBBBEE);
    private static final Color C_SEL_BG    = new Color(0x2A1040);
    private static final Color C_CLOSE_HOV = new Color(0xCC2244);
    private static final Color C_OK        = new Color(0x44DD88);
    private static final Color C_ERR       = new Color(0xFF4466);

    // ---- State ----
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> processList          = new JList<>(listModel);
    private final JLabel statusLabel                 = new JLabel("Select a process and click Inject.");
    private final JPanel statusDot                   = new JPanel();
    private final List<VirtualMachineDescriptor> foundVMs = new ArrayList<>();

    // Window drag support
    private Point dragOrigin;

    public Injector() {
        super("Mugetsu Client");
        buildUI();
        refreshProcesses();
    }

    // ===================================================================
    // UI Construction
    // ===================================================================

    private void buildUI() {
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(620, 440);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG_DARK);
        root.setBorder(BorderFactory.createLineBorder(C_ACCENT_DIM, 1));
        setContentPane(root);

        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(buildCenter(),   BorderLayout.CENTER);
        root.add(buildBottom(),   BorderLayout.SOUTH);

        setVisible(true);
    }

    // ---- Custom title bar (drag-to-move + close button) ----
    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Gradient: accent_dim → accent
                GradientPaint gp = new GradientPaint(0, 0, C_ACCENT_DIM, getWidth(), 0, C_ACCENT);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(0, 60));
        bar.setBorder(new EmptyBorder(8, 16, 8, 12));

        // Brand text
        JPanel brand = new JPanel(new GridLayout(2, 1, 0, 0));
        brand.setOpaque(false);

        JLabel nameLabel = new JLabel("Mugetsu Client");
        nameLabel.setFont(tryFont("Segoe UI", Font.BOLD, 22));
        nameLabel.setForeground(C_TEXT);

        JLabel subLabel = new JLabel("Injection Interface");
        subLabel.setFont(tryFont("Segoe UI", Font.PLAIN, 11));
        subLabel.setForeground(new Color(0xCCAEFF));

        brand.add(nameLabel);
        brand.add(subLabel);
        bar.add(brand, BorderLayout.WEST);

        // Close button
        JLabel closeBtn = new JLabel("  ×  ");
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 20));
        closeBtn.setForeground(C_TEXT);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setOpaque(true);
        closeBtn.setBackground(new Color(0, 0, 0, 0));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { closeBtn.setForeground(C_CLOSE_HOV); }
            @Override public void mouseExited(MouseEvent e)  { closeBtn.setForeground(C_TEXT); }
            @Override public void mouseClicked(MouseEvent e) { System.exit(0); }
        });
        bar.add(closeBtn, BorderLayout.EAST);

        // Drag-to-move
        MouseAdapter drag = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { dragOrigin = e.getLocationOnScreen(); }
            @Override public void mouseDragged(MouseEvent e) {
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

    // ---- Process list area ----
    private JPanel buildCenter() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(C_BG_DARK);
        panel.setBorder(new EmptyBorder(10, 12, 6, 12));

        JLabel listTitle = new JLabel("Minecraft Processes");
        listTitle.setFont(tryFont("Segoe UI", Font.BOLD, 12));
        listTitle.setForeground(C_DIM);
        panel.add(listTitle, BorderLayout.NORTH);

        processList.setBackground(C_BG_PANEL);
        processList.setForeground(C_DIM);
        processList.setFont(tryFont("JetBrains Mono", Font.PLAIN, 12));
        processList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        processList.setCellRenderer(new ProcessCellRenderer());
        processList.setBorder(new EmptyBorder(4, 6, 4, 6));

        JScrollPane scroll = new JScrollPane(processList);
        scroll.setBorder(BorderFactory.createLineBorder(C_ACCENT_DIM, 1));
        scroll.setBackground(C_BG_PANEL);
        scroll.getViewport().setBackground(C_BG_PANEL);
        scroll.getVerticalScrollBar().setUI(new DarkScrollBarUI());

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ---- Bottom bar: status + buttons ----
    private JPanel buildBottom() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(C_BG_DARK);
        bar.setBorder(new EmptyBorder(4, 12, 12, 12));

        // Left: status dot + text
        JPanel statusArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        statusArea.setBackground(C_BG_DARK);

        statusDot.setPreferredSize(new Dimension(9, 9));
        statusDot.setBackground(C_DIM);
        statusDot.setOpaque(true);

        statusLabel.setFont(tryFont("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(C_DIM);

        statusArea.add(statusDot);
        statusArea.add(statusLabel);
        bar.add(statusArea, BorderLayout.CENTER);

        // Right: buttons
        JPanel btnArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnArea.setBackground(C_BG_DARK);

        JButton refreshBtn = makeButton("Refresh", false);
        JButton injectBtn  = makeButton("Inject",  true);

        refreshBtn.addActionListener(e -> refreshProcesses());
        injectBtn.addActionListener(e -> inject());

        btnArea.add(refreshBtn);
        btnArea.add(injectBtn);
        bar.add(btnArea, BorderLayout.EAST);

        return bar;
    }

    // ===================================================================
    // Core logic (unchanged from original)
    // ===================================================================

    private void refreshProcesses() {
        listModel.clear();
        foundVMs.clear();

        List<VirtualMachineDescriptor> all = VirtualMachine.list();
        for (VirtualMachineDescriptor vm : all) {
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
        if (idx < 0 || idx >= foundVMs.size()) {
            setStatus("Select a process first.", false);
            return;
        }

        File agentJar = resolveAgentJar();
        if (agentJar == null || !agentJar.exists()) {
            setStatus("agent.jar not found — build the project first.", false);
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
                    setStatus("Injected into PID " + desc.id() + ".  Press RShift in-game for ClickGUI.", true));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                    setStatus("Injection failed: " + e.getMessage(), false));
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
        File cwd = new File("agent.jar");
        if (cwd.exists()) return cwd;
        return new File("dist/agent.jar");
    }

    private void setStatus(String msg, boolean ok) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setForeground(ok ? C_OK : C_ERR);
            statusLabel.setText(msg);
            statusDot.setBackground(ok ? C_OK : C_ERR);
        });
    }

    // ===================================================================
    // UI helpers
    // ===================================================================

    private JButton makeButton(String text, boolean primary) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover()
                    ? (primary ? C_HIGHLIGHT : C_ACCENT)
                    : (primary ? C_ACCENT : C_BG_PANEL);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(C_ACCENT_DIM);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(C_TEXT);
        btn.setFont(tryFont("Segoe UI", Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(110, 34));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(false);
        return btn;
    }

    private static Font tryFont(String name, int style, int size) {
        Font f = new Font(name, style, size);
        return f.getFamily().equalsIgnoreCase(name) ? f : new Font("SansSerif", style, size);
    }

    // ---- Custom list cell renderer ----
    private class ProcessCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int idx, boolean selected, boolean focused) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, idx, selected, focused);
            lbl.setFont(tryFont("JetBrains Mono", Font.PLAIN, 12));
            lbl.setBorder(new EmptyBorder(3, 8, 3, 8));
            if (selected) {
                lbl.setBackground(C_SEL_BG);
                lbl.setForeground(C_TEXT);
            } else {
                lbl.setBackground(idx % 2 == 0 ? C_BG_PANEL : new Color(0x11111A));
                lbl.setForeground(C_DIM);
            }
            return lbl;
        }
    }

    // ---- Minimal dark scrollbar ----
    private static class DarkScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            thumbColor       = C_ACCENT_DIM;
            trackColor       = C_BG_PANEL;
            thumbDarkShadowColor = C_BG_DARK;
            thumbHighlightColor  = C_ACCENT;
            thumbLightShadowColor= C_BG_PANEL;
        }
        @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
        @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
        private static JButton zeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }
    }

    // ===================================================================
    // Entry point
    // ===================================================================

    public static void main(String[] args) {
        // Wayland / GNOME rendering quality
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.xrender", "True");
        SwingUtilities.invokeLater(Injector::new);
    }
}
