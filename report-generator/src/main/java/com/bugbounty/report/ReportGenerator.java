package com.bugbounty.report;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Report Generator — One-click export of bug bounty findings to formatted Markdown reports.
 * Right-click requests to add them as findings, then generate a professional report.
 */
public class ReportGenerator implements BurpExtension {

    private MontoyaApi api;
    private final List<Finding> findings = new ArrayList<>();
    private DefaultTableModel tableModel;
    private JTextField programNameField;
    private JTextField reporterField;
    private JTextArea notesArea;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("Report Generator");
        api.userInterface().registerSuiteTab("Report Generator", buildUI());
        api.userInterface().registerContextMenuItemsProvider(new ReportContextMenu());
        api.logging().logToOutput("[ReportGenerator] Loaded. Right-click requests to add findings.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────────────────────

    private Component buildUI() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ── Top settings bar ──────────────────────────────────────────────────
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topBar.setBorder(BorderFactory.createTitledBorder("Report Settings"));

        topBar.add(new JLabel("Program Name:"));
        programNameField = new JTextField(20);
        programNameField.setToolTipText("e.g., HackerOne - Acme Corp");
        topBar.add(programNameField);

        topBar.add(new JLabel("Reporter:"));
        reporterField = new JTextField(15);
        reporterField.setToolTipText("Your username or handle");
        topBar.add(reporterField);

        JButton generateBtn = new JButton("Generate Report");
        generateBtn.addActionListener(e -> generateReport());
        topBar.add(generateBtn);

        JButton clearBtn = new JButton("Clear All");
        clearBtn.addActionListener(e -> {
            findings.clear();
            tableModel.setRowCount(0);
        });
        topBar.add(clearBtn);

        // ── Findings table ────────────────────────────────────────────────────
        String[] cols = {"#", "Severity", "Title", "Endpoint", "Method"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        int[] widths = {40, 80, 250, 350, 60};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Color rows by severity
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel && row < findings.size()) {
                    String sev = findings.get(row).severity;
                    switch (sev) {
                        case "Critical" -> {
                            c.setBackground(new Color(230, 80, 80));
                            c.setForeground(Color.BLACK);
                        }
                        case "High" -> {
                            c.setBackground(new Color(255, 160, 80));
                            c.setForeground(Color.BLACK);
                        }
                        case "Medium" -> {
                            c.setBackground(new Color(255, 230, 100));
                            c.setForeground(Color.BLACK);
                        }
                        case "Low" -> {
                            c.setBackground(new Color(200, 230, 255));
                            c.setForeground(Color.BLACK);
                        }
                        case "Info" -> {
                            c.setBackground(new Color(220, 220, 220));
                            c.setForeground(Color.BLACK);
                        }
                        default -> {
                            c.setBackground(t.getBackground());
                            c.setForeground(t.getForeground());
                        }
                    }
                }
                return c;
            }
        });

        // Right-click menu for the table
        JPopupMenu tablePopup = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Edit Finding");
        editItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && row < findings.size()) {
                editFinding(row);
            }
        });
        tablePopup.add(editItem);

        JMenuItem deleteItem = new JMenuItem("Delete Finding");
        deleteItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && row < findings.size()) {
                findings.remove(row);
                refreshTable();
            }
        });
        tablePopup.add(deleteItem);

        JMenuItem moveUpItem = new JMenuItem("Move Up");
        moveUpItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row > 0 && row < findings.size()) {
                Finding f = findings.remove(row);
                findings.add(row - 1, f);
                refreshTable();
                table.setRowSelectionInterval(row - 1, row - 1);
            }
        });
        tablePopup.add(moveUpItem);

        JMenuItem moveDownItem = new JMenuItem("Move Down");
        moveDownItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && row < findings.size() - 1) {
                Finding f = findings.remove(row);
                findings.add(row + 1, f);
                refreshTable();
                table.setRowSelectionInterval(row + 1, row + 1);
            }
        });
        tablePopup.add(moveDownItem);

        table.setComponentPopupMenu(tablePopup);

        JScrollPane tableScroll = new JScrollPane(table);

        // ── Notes area ────────────────────────────────────────────────────────
        JPanel notesPanel = new JPanel(new BorderLayout());
        notesPanel.setBorder(BorderFactory.createTitledBorder("Additional Notes (included in report)"));
        notesArea = new JTextArea(5, 40);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesPanel.add(new JScrollPane(notesArea), BorderLayout.CENTER);

        // ── Split pane ────────────────────────────────────────────────────────
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, notesPanel);
        splitPane.setResizeWeight(0.7);

        // ── Status label ──────────────────────────────────────────────────────
        JLabel statusLabel = new JLabel("Right-click on requests in Proxy/Repeater to add findings.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 0));

        root.add(topBar, BorderLayout.NORTH);
        root.add(splitPane, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);

        return root;
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        int idx = 1;
        for (Finding f : findings) {
            tableModel.addRow(new Object[]{idx++, f.severity, f.title, f.endpoint, f.method});
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Context Menu
    // ─────────────────────────────────────────────────────────────────────────

    private class ReportContextMenu implements ContextMenuItemsProvider {
        @Override
        public List<Component> provideMenuItems(ContextMenuEvent event) {
            List<Component> items = new ArrayList<>();

            if (event.selectedRequestResponses().isEmpty()) {
                return items;
            }

            JMenuItem addFinding = new JMenuItem("Add to Report");
            addFinding.addActionListener(e -> {
                for (HttpRequestResponse reqRes : event.selectedRequestResponses()) {
                    showAddFindingDialog(reqRes);
                }
            });
            items.add(addFinding);

            return items;
        }
    }

    private void showAddFindingDialog(HttpRequestResponse reqRes) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Title:"), gbc);
        JTextField titleField = new JTextField(30);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(titleField, gbc);

        // Severity
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Severity:"), gbc);
        JComboBox<String> severityBox = new JComboBox<>(new String[]{"Critical", "High", "Medium", "Low", "Info"});
        gbc.gridx = 1;
        panel.add(severityBox, gbc);

        // Description
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Description:"), gbc);
        JTextArea descArea = new JTextArea(5, 30);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(descArea), gbc);

        // Impact
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Impact:"), gbc);
        JTextArea impactArea = new JTextArea(3, 30);
        impactArea.setLineWrap(true);
        impactArea.setWrapStyleWord(true);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(impactArea), gbc);

        // Steps to Reproduce
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Steps to Reproduce:"), gbc);
        JTextArea stepsArea = new JTextArea(4, 30);
        stepsArea.setLineWrap(true);
        stepsArea.setWrapStyleWord(true);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(stepsArea), gbc);

        // Remediation
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Remediation:"), gbc);
        JTextArea remediationArea = new JTextArea(2, 30);
        remediationArea.setLineWrap(true);
        remediationArea.setWrapStyleWord(true);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(remediationArea), gbc);

        int result = JOptionPane.showConfirmDialog(null, panel, "Add Finding to Report",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            Finding f = new Finding();
            f.title = titleField.getText().trim();
            f.severity = (String) severityBox.getSelectedItem();
            f.description = descArea.getText().trim();
            f.impact = impactArea.getText().trim();
            f.stepsToReproduce = stepsArea.getText().trim();
            f.remediation = remediationArea.getText().trim();

            // Extract endpoint info from request
            if (reqRes.request() != null) {
                f.method = reqRes.request().method();
                f.endpoint = reqRes.request().url();
                f.requestStr = reqRes.request().toString();
            }

            // Extract response if available
            if (reqRes.response() != null) {
                f.responseStr = reqRes.response().toString();
                f.statusCode = reqRes.response().statusCode();
            }

            findings.add(f);
            refreshTable();

            api.logging().logToOutput("[ReportGenerator] Added finding: " + f.title);
        }
    }

    private void editFinding(int index) {
        Finding f = findings.get(index);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Title:"), gbc);
        JTextField titleField = new JTextField(f.title, 30);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(titleField, gbc);

        // Severity
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Severity:"), gbc);
        JComboBox<String> severityBox = new JComboBox<>(new String[]{"Critical", "High", "Medium", "Low", "Info"});
        severityBox.setSelectedItem(f.severity);
        gbc.gridx = 1;
        panel.add(severityBox, gbc);

        // Description
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Description:"), gbc);
        JTextArea descArea = new JTextArea(f.description, 5, 30);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(descArea), gbc);

        // Impact
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Impact:"), gbc);
        JTextArea impactArea = new JTextArea(f.impact, 3, 30);
        impactArea.setLineWrap(true);
        impactArea.setWrapStyleWord(true);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(impactArea), gbc);

        // Steps to Reproduce
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Steps to Reproduce:"), gbc);
        JTextArea stepsArea = new JTextArea(f.stepsToReproduce, 4, 30);
        stepsArea.setLineWrap(true);
        stepsArea.setWrapStyleWord(true);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(stepsArea), gbc);

        // Remediation
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Remediation:"), gbc);
        JTextArea remediationArea = new JTextArea(f.remediation, 2, 30);
        remediationArea.setLineWrap(true);
        remediationArea.setWrapStyleWord(true);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(remediationArea), gbc);

        int result = JOptionPane.showConfirmDialog(null, panel, "Edit Finding",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            f.title = titleField.getText().trim();
            f.severity = (String) severityBox.getSelectedItem();
            f.description = descArea.getText().trim();
            f.impact = impactArea.getText().trim();
            f.stepsToReproduce = stepsArea.getText().trim();
            f.remediation = remediationArea.getText().trim();
            refreshTable();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Report Generation
    // ─────────────────────────────────────────────────────────────────────────

    private void generateReport() {
        if (findings.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No findings to report!", "Report Generator",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Report");
        fileChooser.setSelectedFile(new File("security_report.md"));

        int userSelection = fileChooser.showSaveDialog(null);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fileChooser.getSelectedFile();
        if (!file.getName().endsWith(".md")) {
            file = new File(file.getAbsolutePath() + ".md");
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            String programName = programNameField.getText().trim();
            String reporter = reporterField.getText().trim();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // Header
            writer.println("# Security Assessment Report");
            writer.println();
            if (!programName.isEmpty()) {
                writer.println("**Program:** " + programName);
            }
            if (!reporter.isEmpty()) {
                writer.println("**Reporter:** " + reporter);
            }
            writer.println("**Date:** " + timestamp);
            writer.println("**Total Findings:** " + findings.size());
            writer.println();

            // Summary Table
            writer.println("## Executive Summary");
            writer.println();
            writer.println("| # | Severity | Title |");
            writer.println("|---|----------|-------|");
            int idx = 1;
            for (Finding f : findings) {
                writer.println("| " + idx++ + " | " + f.severity + " | " + f.title + " |");
            }
            writer.println();

            // Severity Breakdown
            writer.println("### Severity Breakdown");
            writer.println();
            long critical = findings.stream().filter(f -> "Critical".equals(f.severity)).count();
            long high = findings.stream().filter(f -> "High".equals(f.severity)).count();
            long medium = findings.stream().filter(f -> "Medium".equals(f.severity)).count();
            long low = findings.stream().filter(f -> "Low".equals(f.severity)).count();
            long info = findings.stream().filter(f -> "Info".equals(f.severity)).count();
            writer.println("- **Critical:** " + critical);
            writer.println("- **High:** " + high);
            writer.println("- **Medium:** " + medium);
            writer.println("- **Low:** " + low);
            writer.println("- **Informational:** " + info);
            writer.println();

            // Additional Notes
            String notes = notesArea.getText().trim();
            if (!notes.isEmpty()) {
                writer.println("### Additional Notes");
                writer.println();
                writer.println(notes);
                writer.println();
            }

            writer.println("---");
            writer.println();

            // Detailed Findings
            writer.println("## Detailed Findings");
            writer.println();

            idx = 1;
            for (Finding f : findings) {
                writer.println("### " + idx + ". " + f.title);
                writer.println();
                writer.println("**Severity:** " + f.severity);
                writer.println();
                writer.println("**Endpoint:** `" + f.method + " " + f.endpoint + "`");
                writer.println();

                if (!f.description.isEmpty()) {
                    writer.println("#### Description");
                    writer.println();
                    writer.println(f.description);
                    writer.println();
                }

                if (!f.impact.isEmpty()) {
                    writer.println("#### Impact");
                    writer.println();
                    writer.println(f.impact);
                    writer.println();
                }

                if (!f.stepsToReproduce.isEmpty()) {
                    writer.println("#### Steps to Reproduce");
                    writer.println();
                    writer.println(f.stepsToReproduce);
                    writer.println();
                }

                // Request
                if (f.requestStr != null && !f.requestStr.isEmpty()) {
                    writer.println("#### HTTP Request");
                    writer.println();
                    writer.println("```http");
                    writer.println(truncate(f.requestStr, 2000));
                    writer.println("```");
                    writer.println();
                }

                // Response
                if (f.responseStr != null && !f.responseStr.isEmpty()) {
                    writer.println("#### HTTP Response (Status: " + f.statusCode + ")");
                    writer.println();
                    writer.println("```http");
                    writer.println(truncate(f.responseStr, 2000));
                    writer.println("```");
                    writer.println();
                }

                if (!f.remediation.isEmpty()) {
                    writer.println("#### Remediation");
                    writer.println();
                    writer.println(f.remediation);
                    writer.println();
                }

                writer.println("---");
                writer.println();
                idx++;
            }

            // Footer
            writer.println("*Report generated by Report Generator Burp Extension*");

            JOptionPane.showMessageDialog(null,
                    "Report saved to:\n" + file.getAbsolutePath(),
                    "Report Generator", JOptionPane.INFORMATION_MESSAGE);

            api.logging().logToOutput("[ReportGenerator] Report saved to: " + file.getAbsolutePath());

        } catch (Exception e) {
            api.logging().logToError("[ReportGenerator] Failed to save report: " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                    "Failed to save report: " + e.getMessage(),
                    "Report Generator", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen) + "\n\n[... truncated ...]";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Finding Model
    // ─────────────────────────────────────────────────────────────────────────

    private static class Finding {
        String title = "";
        String severity = "Medium";
        String description = "";
        String impact = "";
        String stepsToReproduce = "";
        String remediation = "";
        String method = "";
        String endpoint = "";
        String requestStr = "";
        String responseStr = "";
        int statusCode = 0;
    }
}
