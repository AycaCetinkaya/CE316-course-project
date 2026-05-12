import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

public class IAEGui extends JFrame {
    private StudentZipSubmission currentSubmission;
    private JLabel topBarTitle;
    private Project currentProject;
    private JPanel mainContentPanel;
    private CardLayout cardLayout;

    private JButton btnDashboard, btnCreateProject, btnConfigurations, btnHelp;

    private JTextField txtProjectName;
    private JComboBox<String> cmbConfiguration;
    private JTextField txtInputFile;
    private JTextField txtExpectedOutputFile;
    private JTextField txtSubmissionsFolder;

    private final List<Project> recentProjects = new ArrayList<>();

    private final Color BG_CANVAS = new Color(248, 250, 252);
    private final Color BG_CARD = Color.WHITE;
    private final Color SIDEBAR_COLOR = new Color(30, 41, 59);
    private final Color SIDEBAR_HOVER = new Color(51, 65, 85);
    private final Color ACCENT_ORANGE = new Color(234, 115, 23);
    private final Color TEXT_PRIMARY = new Color(15, 23, 42);
    private final Color TEXT_SECONDARY = new Color(100, 116, 139);
    private final Color BORDER_COLOR = new Color(226, 232, 240);

    private final Font FONT_HEADER = new Font("SansSerif", Font.BOLD, 24);
    private final Font FONT_SUBHEADER = new Font("SansSerif", Font.BOLD, 15);
    private final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 13);

    public IAEGui() {
        setTitle("IAE - Integrated Assignment Environment");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 850);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(createSidebar(), BorderLayout.WEST);

        JPanel rightArea = new JPanel(new BorderLayout());
        rightArea.setBackground(BG_CANVAS);
        rightArea.add(createTopBar("Dashboard"), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(BG_CANVAS);

        DatabaseManager db = new DatabaseManager();
        try {
            db.connect();
            db.initSchema();
            List<Project> saved = db.getProjects();
            recentProjects.addAll(saved);
            db.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        addPages();

        rightArea.add(mainContentPanel, BorderLayout.CENTER);
        add(rightArea, BorderLayout.CENTER);

        setActiveButton(btnDashboard);
        cardLayout.show(mainContentPanel, "dashboard");
    }

    private void addPages() {
        mainContentPanel.add(createDashboardPanel(), "dashboard");
        mainContentPanel.add(createCreateProjectPanel(), "createProject");
        mainContentPanel.add(createConfigurationsPanel(), "configurations");
        mainContentPanel.add(createHelpPanel(), "help");
        mainContentPanel.add(createEvaluationResultsPanel(), "evaluationResults");
        mainContentPanel.add(createStudentDetailsPanel(), "studentDetails");
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBackground(SIDEBAR_COLOR);

        JPanel menuPanel = new JPanel();
        menuPanel.setBackground(SIDEBAR_COLOR);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(new EmptyBorder(25, 14, 20, 14));

        JLabel logo = new JLabel(
                "<html><b style='font-size:20px;color:white;'>IAE</b><br>" +
                        "<span style='color:#CBD5E1;'>Integrated Assignment<br>Environment</span></html>"
        );
        logo.setBorder(new EmptyBorder(0, 8, 28, 0));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnDashboard = createMenuButton("⌂  Dashboard");
        btnCreateProject = createMenuButton("⊞  Create Project");
        btnConfigurations = createMenuButton("⚙  Configurations");
        btnHelp = createMenuButton("?  Help");

        btnDashboard.addActionListener(e -> showPage("dashboard", btnDashboard));
        btnCreateProject.addActionListener(e -> showPage("createProject", btnCreateProject));
        btnConfigurations.addActionListener(e -> showPage("configurations", btnConfigurations));
        btnHelp.addActionListener(e -> showPage("help", btnHelp));

        menuPanel.add(logo);
        menuPanel.add(btnDashboard);
        menuPanel.add(Box.createVerticalStrut(8));
        menuPanel.add(btnCreateProject);
        menuPanel.add(Box.createVerticalStrut(8));
        menuPanel.add(btnConfigurations);
        menuPanel.add(Box.createVerticalStrut(8));
        menuPanel.add(btnHelp);

        JPanel footer = new JPanel();
        footer.setBackground(SIDEBAR_COLOR);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(new EmptyBorder(15, 18, 15, 18));

        JLabel version = new JLabel("Version 1.0.0");
        version.setForeground(new Color(203, 213, 225));
        version.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JLabel copyright = new JLabel("© 2026 IAE System");
        copyright.setForeground(new Color(203, 213, 225));
        copyright.setFont(new Font("SansSerif", Font.PLAIN, 11));

        footer.add(version);
        footer.add(copyright);

        sidebar.add(menuPanel, BorderLayout.CENTER);
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel createTopBar(String title) {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setPreferredSize(new Dimension(0, 48));
        topBar.setBackground(BG_CARD);
        topBar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_COLOR));

        topBarTitle = new JLabel("   " + title);
        topBarTitle.setFont(FONT_BODY);
        topBarTitle.setForeground(TEXT_SECONDARY);

        JLabel right = new JLabel("Lecturer Mode   L   ");
        right.setFont(FONT_BODY);
        right.setForeground(TEXT_SECONDARY);

        topBar.add(topBarTitle, BorderLayout.WEST);
        topBar.add(right, BorderLayout.EAST);

        return topBar;
    }
    private class EvaluationTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column
            );

            label.setFont(FONT_BODY);
            label.setBorder(new EmptyBorder(0, 14, 0, 14));
            label.setOpaque(true);

            if (!isSelected) {
                label.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
            }

            String text = value == null ? "" : value.toString();
            label.setText(text);

            if (column == 2 || column == 3 || column == 4) {
                if (text.contains("Success") || text.contains("Match")) {
                    label.setForeground(new Color(5, 150, 105));
                    label.setText("◎ " + text);
                } else if (text.contains("Failed") || text.contains("Mismatch")) {
                    label.setForeground(new Color(220, 38, 38));
                    label.setText("⊗ " + text);
                } else if (text.contains("Error")) {
                    label.setForeground(ACCENT_ORANGE);
                    label.setText("! " + text);
                } else {
                    label.setForeground(TEXT_SECONDARY);
                }
            } else if (column == 5) {
                label.setHorizontalAlignment(SwingConstants.CENTER);

                if (text.equals("Passed")) {
                    label.setText("<html><span style='background:#DCFCE7;color:#059669;padding:4px 10px;'>Passed</span></html>");
                } else {
                    label.setText("<html><span style='background:#FEE2E2;color:#DC2626;padding:4px 10px;'>Failed</span></html>");
                }
            } else if (column == 6) {
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setForeground(new Color(19, 99, 128));
            } else {
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setForeground(TEXT_PRIMARY);
            }

            return label;
        }
    }
    private RoundedMenuButton createMenuButton(String text) {
        RoundedMenuButton btn = new RoundedMenuButton(text);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btn.setPreferredSize(new Dimension(200, 46));
        btn.setFocusPainted(false);
        btn.setBackgroundColor(SIDEBAR_COLOR);
        btn.setForeground(new Color(203, 213, 225));
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(12, 18, 12, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (btn.isActive()) return;
                btn.setBackgroundColor(SIDEBAR_HOVER);
                btn.setForeground(Color.WHITE);
            }

            public void mouseExited(MouseEvent e) {
                if (btn.isActive()) return;
                btn.setBackgroundColor(SIDEBAR_COLOR);
                btn.setForeground(new Color(203, 213, 225));
            }
        });

        return btn;
    }

    private void showPage(String pageName, JButton activeButton) {
        setActiveButton(activeButton);

        if (topBarTitle != null) {
            topBarTitle.setText("   " + getPageTitle(pageName));
        }

        cardLayout.show(mainContentPanel, pageName);
    }
    private String getPageTitle(String pageName) {
        switch (pageName) {
            case "dashboard":
                return "Dashboard";
            case "createProject":
                return "Create Project";
            case "configurations":
                return "Configurations";
            case "help":
                return "Help";
            case "evaluationResults":
                return "Evaluation Results";
            case "studentDetails":
                return "Student Details";
            default:
                return "IAE";
        }
    }
    private void setActiveButton(JButton activeButton) {
        JButton[] buttons = {btnDashboard, btnCreateProject, btnConfigurations, btnHelp};

        for (JButton button : buttons) {
            RoundedMenuButton btn = (RoundedMenuButton) button;
            btn.setActive(false);
            btn.setBackgroundColor(SIDEBAR_COLOR);
            btn.setForeground(new Color(203, 213, 225));
        }

        RoundedMenuButton active = (RoundedMenuButton) activeButton;
        active.setActive(true);
        active.setBackgroundColor(ACCENT_ORANGE);
        active.setForeground(Color.WHITE);
    }

    private JPanel createPageBase(String title, String subtitle) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_CANVAS);

        panel.setBorder(new EmptyBorder(28, 40, 35, 40));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(BG_CANVAS);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_HEADER);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(FONT_BODY);
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setBorder(new EmptyBorder(8, 0, 25, 0));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(titleLabel);
        header.add(subtitleLabel);

        panel.add(header, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = createPageBase(
                "Welcome to IAE",
                "Integrated Assignment Environment - Automate evaluation of programming assignments"
        );

        JPanel content = new JPanel();
        content.setBackground(BG_CANVAS);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setBackground(BG_CANVAS);
        cards.setMaximumSize(new Dimension(1050, 140));
        cards.setPreferredSize(new Dimension(1050, 140));
        cards.setAlignmentX(Component.LEFT_ALIGNMENT);

        cards.add(createActionCard("Create New Project", "Set up a new assignment evaluation", "+", new Color(19, 99, 128)));
        cards.add(createActionCard("Open Existing", "Continue working on a project", "📂", ACCENT_ORANGE));
        cards.add(createActionCard("Manage Configs", "Language configurations", "⚙", new Color(107, 114, 128)));
        cards.add(createActionCard("Help & Manual", "Documentation and guides", "?", new Color(107, 114, 128)));

        content.add(cards);
        content.add(Box.createVerticalStrut(25));
        content.add(createRecentProjectsPanel());

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createActionCard(String title, String subtitle, String iconText, Color accentColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(18, 18, 18, 18)
        ));

        JLabel icon = new JLabel(iconText, SwingConstants.CENTER);
        icon.setOpaque(true);
        icon.setBackground(makeSoftColor(accentColor));
        icon.setForeground(accentColor);
        icon.setFont(new Font("SansSerif", Font.BOLD, 18));
        icon.setMaximumSize(new Dimension(42, 42));
        icon.setPreferredSize(new Dimension(42, 42));
        icon.setMinimumSize(new Dimension(42, 42));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_SUBHEADER);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(FONT_BODY);
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(icon);
        card.add(Box.createVerticalStrut(20));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(subtitleLabel);
        card.add(Box.createVerticalGlue());

        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(accentColor, 1, true),
                        new EmptyBorder(18, 18, 18, 18)
                ));
                icon.setBackground(accentColor);
                icon.setForeground(Color.WHITE);
            }

            public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(BORDER_COLOR, 1, true),
                        new EmptyBorder(18, 18, 18, 18)
                ));
                icon.setBackground(makeSoftColor(accentColor));
                icon.setForeground(accentColor);
            }

            public void mouseClicked(MouseEvent e) {
                if (title.equals("Create New Project")) {
                    showPage("createProject", btnCreateProject);
                } else if (title.equals("Manage Configs")) {
                    showPage("configurations", btnConfigurations);
                } else if (title.equals("Help & Manual")) {
                    showPage("help", btnHelp);
                }else if (title.equals("Open Existing")) {
                    showPage("dashboard", btnDashboard);
                }
            }
        });

        return card;
    }

    private JPanel createRecentProjectsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_CARD);
        panel.setMaximumSize(new Dimension(1050, 300));
        panel.setPreferredSize(new Dimension(1050, 300));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(new LineBorder(BORDER_COLOR, 1, true));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel title = new JLabel(
                "<html><b>Recent Projects</b><br><span style='color:#64748B;font-size:10px;'>Your latest assignment evaluations</span></html>"
        );
        title.setFont(FONT_SUBHEADER);
        title.setForeground(TEXT_PRIMARY);

        JButton viewAll = createSecondaryButton("View All");

        header.add(title, BorderLayout.WEST);
        header.add(viewAll, BorderLayout.EAST);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(BG_CARD);

        if (recentProjects.isEmpty()) {
            JLabel empty = new JLabel("No projects have been evaluated yet.");
            empty.setFont(FONT_BODY);
            empty.setForeground(TEXT_SECONDARY);
            empty.setBorder(new EmptyBorder(25, 20, 20, 20));
            list.add(empty);
        } else {
            for (int i = recentProjects.size() - 1; i >= 0; i--) {
                list.add(createProjectRow(recentProjects.get(i)));
            }
        }

        panel.add(header, BorderLayout.NORTH);
        panel.add(list, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createProjectRow(Project project) {
        int total = project.getSubmissions().size();
        int passed = 0;
        int failed = 0;

        for (StudentZipSubmission submission : project.getSubmissions()) {
            if (submission.getResult() != null &&
                    submission.getResult().getStatus() == Status.SUCCESS) {
                passed++;
            } else {
                failed++;
            }
        }

        JPanel row = createProjectRow(
                project.getName(),
                total + " students",
                String.valueOf(passed),
                String.valueOf(failed),
                "Completed"
        );

        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openEvaluationResults(project);
            }
        });

        return row;
    }

    private JPanel createProjectRow(String name, String students, String passed, String failed, String status) {
        JPanel row = new JPanel(new BorderLayout());
        Color normalBg = BG_CARD;
        Color hoverBg = new Color(255, 251, 247);

        row.setBackground(normalBg);
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));
        row.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(16, 20, 16, 20)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        JLabel icon = new JLabel("▤", SwingConstants.CENTER);
        icon.setOpaque(true);
        icon.setBackground(new Color(232, 241, 246));
        icon.setForeground(new Color(19, 99, 128));
        icon.setFont(new Font("SansSerif", Font.BOLD, 16));
        icon.setPreferredSize(new Dimension(38, 38));

        JLabel left = new JLabel(
                "<html><b>" + name + "</b><br>" +
                        "<span style='color:#64748B;'>" + students + "</span></html>"
        );
        left.setFont(FONT_BODY);
        left.setBorder(new EmptyBorder(0, 14, 0, 0));

        JPanel leftBox = new JPanel(new BorderLayout());
        leftBox.setBackground(normalBg);
        leftBox.add(icon, BorderLayout.WEST);
        leftBox.add(left, BorderLayout.CENTER);

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 4));
        rightBox.setBackground(normalBg);

        rightBox.add(new JLabel("<html><span style='color:#059669;'>◎ <b>" + passed + "</b></span><br><span style='font-size:9px;color:#64748B;'>Passed</span></html>"));
        rightBox.add(new JLabel("<html><span style='color:#DC2626;'>⊗ <b>" + failed + "</b></span><br><span style='font-size:9px;color:#64748B;'>Failed</span></html>"));

        JLabel badge = new JLabel(" " + status + " ");
        badge.setOpaque(true);
        badge.setFont(new Font("SansSerif", Font.BOLD, 11));
        badge.setBorder(new EmptyBorder(5, 12, 5, 12));
        badge.setBackground(new Color(220, 252, 231));
        badge.setForeground(new Color(22, 163, 74));

        rightBox.add(badge);

        MouseAdapter rowHover = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                row.setBackground(hoverBg);
                leftBox.setBackground(hoverBg);
                rightBox.setBackground(hoverBg);
                row.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(ACCENT_ORANGE, 1, true),
                        new EmptyBorder(16, 20, 16, 20)
                ));
                row.repaint();
            }

            public void mouseExited(MouseEvent e) {
                row.setBackground(normalBg);
                leftBox.setBackground(normalBg);
                rightBox.setBackground(normalBg);
                row.setBorder(BorderFactory.createCompoundBorder(
                        new MatteBorder(1, 0, 0, 0, BORDER_COLOR),
                        new EmptyBorder(16, 20, 16, 20)
                ));
                row.repaint();
            }
        };

        row.addMouseListener(rowHover);
        leftBox.addMouseListener(rowHover);
        rightBox.addMouseListener(rowHover);
        icon.addMouseListener(rowHover);
        left.addMouseListener(rowHover);

        row.add(leftBox, BorderLayout.WEST);
        row.add(rightBox, BorderLayout.EAST);

        return row;
    }

    private JPanel createCreateProjectPanel() {
        JPanel panel = createPageBase(
                "Create New Project",
                "Configure a new assignment evaluation project"
        );

        JPanel content = new JPanel();
        content.setBackground(BG_CANVAS);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel projectInfoCard = createCardPanel();
        projectInfoCard.setMaximumSize(new Dimension(720, 210));
        projectInfoCard.setPreferredSize(new Dimension(720, 210));
        projectInfoCard.setLayout(new BoxLayout(projectInfoCard, BoxLayout.Y_AXIS));

        txtProjectName = createTextField("e.g., Data Structures - Assignment 1");

        cmbConfiguration = new JComboBox<>();
        cmbConfiguration.addItem("AUTO");
        cmbConfiguration.addItem("C Language");
        cmbConfiguration.addItem("Java");
        cmbConfiguration.addItem("Python 3");
        cmbConfiguration.addItem("Haskell");
        cmbConfiguration.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        cmbConfiguration.setPreferredSize(new Dimension(0, 38));
        cmbConfiguration.setFont(FONT_BODY);
        cmbConfiguration.setBackground(Color.WHITE);

        projectInfoCard.add(createSectionTitle("▤  Project Information"));
        projectInfoCard.add(Box.createVerticalStrut(14));
        projectInfoCard.add(createLabel("Project Name"));
        projectInfoCard.add(Box.createVerticalStrut(6));
        projectInfoCard.add(txtProjectName);
        projectInfoCard.add(Box.createVerticalStrut(14));
        projectInfoCard.add(createLabel("Configuration"));
        projectInfoCard.add(Box.createVerticalStrut(6));
        projectInfoCard.add(cmbConfiguration);

        JPanel filesCard = createCardPanel();
        filesCard.setMaximumSize(new Dimension(720, 340));
        filesCard.setPreferredSize(new Dimension(720, 340));
        filesCard.setLayout(new BoxLayout(filesCard, BoxLayout.Y_AXIS));

        txtInputFile = createTextField("Select input file...");
        txtExpectedOutputFile = createTextField("Select expected output file...");
        txtSubmissionsFolder = createTextField("Select folder containing ZIP files...");
        File defaultFolder = new File("test-submissions");
        if (defaultFolder.exists()) {
            txtSubmissionsFolder.setText(defaultFolder.getAbsolutePath());
            txtSubmissionsFolder.setForeground(TEXT_PRIMARY);
        }

        filesCard.add(createSectionTitle("▣  Files and Folders"));
        filesCard.add(Box.createVerticalStrut(16));
        filesCard.add(createFileChooserRow("Input File", txtInputFile, false, "Test input file for student programs"));
        filesCard.add(Box.createVerticalStrut(12));
        filesCard.add(createFileChooserRow("Expected Output File", txtExpectedOutputFile, false, "Expected output for comparison"));
        filesCard.add(Box.createVerticalStrut(12));
        filesCard.add(createFileChooserRow("Student Submissions Folder", txtSubmissionsFolder, true, "Directory containing student submission ZIP files"));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setBackground(BG_CANVAS);
        buttons.setMaximumSize(new Dimension(720, 50));
        buttons.setPreferredSize(new Dimension(720, 50));
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnCancel = createSecondaryButton("Cancel");
        JButton btnSave = createBlueButton("▣  Save Project");
        JButton btnRun = createOrangeButton("▷  Run Project");

        btnCancel.addActionListener(e -> clearCreateProjectForm());
        btnSave.addActionListener(e -> JOptionPane.showMessageDialog(this, "Project saved successfully."));
        btnRun.addActionListener(e -> runProject());

        buttons.add(btnCancel);
        buttons.add(btnSave);
        buttons.add(btnRun);

        content.add(projectInfoCard);
        content.add(Box.createVerticalStrut(20));
        content.add(filesCard);
        content.add(Box.createVerticalStrut(20));
        content.add(buttons);

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }
    private String getRealText(JTextField field, String placeholder) {
        String text = field.getText().trim();
        return text.equals(placeholder) ? "" : text;
    }
    private void runProject() {
        try {
            String projectName = getRealText(txtProjectName, "e.g., Data Structures - Assignment 1");
            String inputPath = getRealText(txtInputFile, "Select input file...");
            String expectedPath = getRealText(txtExpectedOutputFile, "Select expected output file...");
            String submissionsPath = getRealText(txtSubmissionsFolder, "Select folder containing ZIP files...");

            String selected = (String) cmbConfiguration.getSelectedItem();

            if (projectName.isEmpty() || submissionsPath.isEmpty() || selected == null) {
                JOptionPane.showMessageDialog(this, "Please fill project name and submissions folder.");
                return;
            }

            Language language = mapLanguage(selected);

            List<TestCase> testCases = new ArrayList<>();

            if (!expectedPath.isEmpty()) {
                String input = inputPath.isEmpty()
                        ? ""
                        : java.nio.file.Files.readString(java.nio.file.Paths.get(inputPath)).trim();

                String expected = java.nio.file.Files.readString(java.nio.file.Paths.get(expectedPath)).trim();
                testCases.add(new TestCase(input, expected));
            } else {
                testCases.add(new TestCase("3 1 2", "1 2 3"));
                testCases.add(new TestCase("9 4 7", "4 7 9"));
                testCases.add(new TestCase("10 2 5", "2 5 10"));
            }

            ProjectRunnerService runner = new ProjectRunnerService();

            Project project = runner.runProject(
                    projectName,
                    new File(submissionsPath),
                    testCases,
                    language
            );

            recentProjects.add(project);

            JOptionPane.showMessageDialog(this, "Project evaluated successfully.");
            refreshDashboard();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Evaluation failed: " + ex.getMessage());
        }
    }
    private void rerunCurrentProject() {
        if (currentProject == null) {
            JOptionPane.showMessageDialog(this, "No project selected.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Re-run '" + currentProject.getName() + "'? Existing results will be replaced.",
                "Confirm Re-run",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select submissions folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File submissionsDir = chooser.getSelectedFile();

        try {
            ProjectRunnerService runner = new ProjectRunnerService();
            runner.runProject(
                    currentProject.getName(),
                    submissionsDir,
                    currentProject.getTestCases(),
                    Language.AUTO
            );

            DatabaseManager db = new DatabaseManager();
            db.connect();
            List<Project> refreshed = db.getProjects();
            db.disconnect();

            recentProjects.clear();
            recentProjects.addAll(refreshed);

            Project updated = null;
            for (Project p : refreshed) {
                if (p.getName().equals(currentProject.getName())) {
                    updated = p;
                    break;
                }
            }

            if (updated != null) {
                currentProject = updated;
                openEvaluationResults(updated);
            } else {
                refreshDashboard();
            }

            JOptionPane.showMessageDialog(this, "Project re-evaluated successfully.");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Re-run failed: " + ex.getMessage());
        }
    }

    private Language mapLanguage(String selected) {
        switch (selected) {
            case "AUTO":
                return Language.AUTO;
            case "C Language":
                return Language.C;
            case "Java":
                return Language.JAVA;
            case "Python 3":
                return Language.PYTHON;
            case "Haskell":
                return Language.HASKELL;
            default:
                return Language.AUTO;
        }
    }

    private void refreshDashboard() {
        mainContentPanel.removeAll();
        addPages();
        setActiveButton(btnDashboard);

        if (topBarTitle != null) {
            topBarTitle.setText("   Dashboard");
        }

        cardLayout.show(mainContentPanel, "dashboard");
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    private void clearCreateProjectForm() {
        resetPlaceholder(txtProjectName, "e.g., Data Structures - Assignment 1");
        cmbConfiguration.setSelectedIndex(0);
        resetPlaceholder(txtInputFile, "Select input file...");
        resetPlaceholder(txtExpectedOutputFile, "Select expected output file...");
        resetPlaceholder(txtSubmissionsFolder, "Select folder containing ZIP files...");
    }

    private void resetPlaceholder(JTextField field, String placeholder) {
        field.setText(placeholder);
        field.setForeground(new Color(148, 163, 184));
    }

    private JPanel createConfigurationsPanel() {
        return createPageBase("Configuration Management", "Manage programming language configurations");
    }

    private JPanel createHelpPanel() {
        return createPageBase("Help & Documentation", "Complete guide to using the Integrated Assignment Environment");
    }

    private JPanel createCardPanel() {
        JPanel card = new JPanel();
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SUBHEADER);
        label.setForeground(TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    private JTextField createTextField(String placeholder) {
        JTextField field = new JTextField(placeholder);
        field.setFont(FONT_BODY);
        field.setForeground(new Color(148, 163, 184));
        field.setBackground(Color.WHITE);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setPreferredSize(new Dimension(0, 38));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_PRIMARY);
                }
            }

            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(new Color(148, 163, 184));
                }
            }
        });

        return field;
    }

    private JPanel createFileChooserRow(String labelText, JTextField textField, boolean directoryOnly, String hint) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(BG_CARD);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));

        JLabel label = createLabel(labelText);

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(BG_CARD);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        row.setPreferredSize(new Dimension(0, 38));

        textField.setEditable(false);

        JButton browse = createSecondaryButton("Browse");
        browse.setPreferredSize(new Dimension(90, 38));

        browse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("."));
            chooser.setFileSelectionMode(directoryOnly ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);

            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                textField.setText(chooser.getSelectedFile().getAbsolutePath());
                textField.setForeground(TEXT_PRIMARY);
            }
        });

        JLabel hintLabel = new JLabel(hint);
        hintLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hintLabel.setForeground(TEXT_SECONDARY);
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.add(textField, BorderLayout.CENTER);
        row.add(browse, BorderLayout.EAST);

        wrapper.add(label);
        wrapper.add(Box.createVerticalStrut(6));
        wrapper.add(row);
        wrapper.add(Box.createVerticalStrut(4));
        wrapper.add(hintLabel);

        return wrapper;
    }

    private JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBackground(new Color(241, 245, 249));
        btn.setForeground(TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(9, 16, 9, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createBlueButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBackground(new Color(19, 99, 128));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(9, 18, 9, 18));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createOrangeButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBackground(ACCENT_ORANGE);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(9, 18, 9, 18));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private Color makeSoftColor(Color color) {
        return new Color(
                Math.min(color.getRed() + 210, 255),
                Math.min(color.getGreen() + 210, 255),
                Math.min(color.getBlue() + 210, 255)
        );
    }

    private static class RoundedMenuButton extends JButton {
        private Color backgroundColor;
        private boolean active = false;
        private final int radius = 12;

        public RoundedMenuButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setOpaque(false);
        }

        public void setBackgroundColor(Color color) {
            this.backgroundColor = color;
            repaint();
        }

        public void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        public boolean isActive() {
            return active;
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (backgroundColor != null) {
                g2.setColor(backgroundColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
    private void openEvaluationResults(Project project) {
        this.currentProject = project;

        mainContentPanel.removeAll();
        addPages();

        if (topBarTitle != null) {
            topBarTitle.setText("   Evaluation Results");
        }

        cardLayout.show(mainContentPanel, "evaluationResults");
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }
    private JPanel createEvaluationResultsPanel() {
        JPanel panel = createPageBase(
                "Evaluation Results",
                currentProject == null ? "No project selected" : currentProject.getName()
        );

        if (currentProject == null) {
            JLabel empty = new JLabel("No evaluation results available.");
            empty.setFont(FONT_BODY);
            empty.setForeground(TEXT_SECONDARY);
            panel.add(empty, BorderLayout.CENTER);
            return panel;
        }

        int total = currentProject.getSubmissions().size();
        int passed = 0;
        int failed = 0;

        for (StudentZipSubmission s : currentProject.getSubmissions()) {
            Result r = s.getResult();
            if (r != null && r.getStatus() == Status.SUCCESS) passed++;
            else failed++;
        }

        int passRate = total == 0 ? 0 : (int) Math.round((passed * 100.0) / total);

        JPanel content = new JPanel();
        content.setBackground(BG_CANVAS);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topActions.setBackground(BG_CANVAS);
        topActions.setMaximumSize(new Dimension(1050, 40));
        topActions.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnProjectDetails = createSecondaryButton("▧  Project Details");
        JButton btnRerun = createSecondaryButton("↻  Re-run");
        JButton btnExport = createOrangeButton("⇩  Export Results");

        btnRerun.addActionListener(e -> rerunCurrentProject());
        btnExport.addActionListener(e -> JOptionPane.showMessageDialog(this, "Export feature will be added next."));
        topActions.add(btnProjectDetails);
        topActions.add(btnRerun);
        topActions.add(btnExport);

        JPanel stats = new JPanel(new GridLayout(1, 4, 14, 0));
        stats.setBackground(BG_CANVAS);
        stats.setMaximumSize(new Dimension(1050, 78));
        stats.setPreferredSize(new Dimension(1050, 78));
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);

        stats.add(createResultStatCard(String.valueOf(total), "Total Students", TEXT_PRIMARY, BG_CARD, BORDER_COLOR));
        stats.add(createResultStatCard(String.valueOf(passed), "Passed", new Color(5, 150, 105), new Color(236, 253, 245), new Color(167, 243, 208)));
        stats.add(createResultStatCard(String.valueOf(failed), "Failed", new Color(220, 38, 38), new Color(254, 242, 242), new Color(252, 165, 165)));
        stats.add(createResultStatCard(passRate + "%", "Pass Rate", ACCENT_ORANGE, new Color(255, 247, 237), new Color(253, 186, 116)));

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(BG_CARD);
        tableCard.setMaximumSize(new Dimension(1050, 430));
        tableCard.setPreferredSize(new Dimension(1050, 430));
        tableCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableCard.setBorder(new LineBorder(BORDER_COLOR, 1, true));

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setBackground(BG_CARD);
        tableHeader.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel title = new JLabel("Student Results");
        title.setFont(FONT_SUBHEADER);
        title.setForeground(TEXT_PRIMARY);

        JTextField search = createTextField("Search by ID or name...");
        search.setPreferredSize(new Dimension(220, 36));
        search.setMaximumSize(new Dimension(220, 36));

        tableHeader.add(title, BorderLayout.WEST);
        tableHeader.add(search, BorderLayout.EAST);

        String[] columns = {
                "Student ID ↕", "Name", "Compile Status", "Run Status",
                "Output Status", "Final Result", "Actions"
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (StudentZipSubmission s : currentProject.getSubmissions()) {
            Status status = s.getResult() == null ? null : s.getResult().getStatus();

            model.addRow(new Object[]{
                    s.getStudentId(),
                    s.getStudentId(),
                    getCompileStatus(status),
                    getRunStatus(status),
                    getOutputStatus(status),
                    getFinalStatus(status),
                    "◎"
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(52);
        table.setFont(FONT_BODY);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER_COLOR);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(255, 251, 247));

        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.getTableHeader().setForeground(TEXT_PRIMARY);
        table.getTableHeader().setPreferredSize(new Dimension(0, 42));
        table.getTableHeader().setBorder(new MatteBorder(0, 0, 1, 0, BORDER_COLOR));

        table.setDefaultRenderer(Object.class, new EvaluationTableRenderer());

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        search.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String text = getRealText(search, "Search by ID or name...");
                if (text.isEmpty()) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());

                if (row >= 0 && col == 6) {
                    int modelRow = table.convertRowIndexToModel(row);
                    StudentZipSubmission submission = currentProject.getSubmissions().get(modelRow);
                    openStudentDetails(submission);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);

        tableCard.add(tableHeader, BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);

        content.add(topActions);
        content.add(Box.createVerticalStrut(12));
        content.add(stats);
        content.add(Box.createVerticalStrut(20));
        content.add(tableCard);

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }
    private JPanel createResultStatCard(String value, String label, Color valueColor, Color bgColor, Color borderColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(borderColor, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valueLabel.setForeground(valueColor);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel textLabel = new JLabel(label);
        textLabel.setFont(FONT_BODY);
        textLabel.setForeground(TEXT_SECONDARY);
        textLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(valueLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(textLabel);

        return card;
    }

    private String getCompileStatus(Status status) {
        if (status == null) return "Not Evaluated";
        if (status == Status.EXTRACTION_ERROR) return "Failed";
        if (status == Status.COMPILE_ERROR) return "Failed";
        return "Success";
    }

    private String getRunStatus(Status status) {
        if (status == null) return "Not Run";
        if (status == Status.COMPILE_ERROR || status == Status.EXTRACTION_ERROR) return "Not Run";
        if (status == Status.RUNTIME_ERROR) return "Error";
        return "Success";
    }

    private String getOutputStatus(Status status) {
        if (status == null) return "Not Compared";
        if (status == Status.SUCCESS) return "Match";
        if (status == Status.WRONG_OUTPUT) return "Mismatch";
        return "Not Compared";
    }

    private String getFinalStatus(Status status) {
        if (status == null) return "Not Evaluated";
        return status == Status.SUCCESS ? "Passed" : "Failed";
    }

    private void openStudentDetails(StudentZipSubmission submission) {
        this.currentSubmission = submission;

        mainContentPanel.removeAll();
        addPages();

        if (topBarTitle != null) {
            topBarTitle.setText("   Student Details");
        }

        cardLayout.show(mainContentPanel, "studentDetails");
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    private JPanel createStudentDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_CANVAS);
        panel.setBorder(new EmptyBorder(30, 185, 35, 60));

        if (currentSubmission == null) {
            panel.add(new JLabel("No student selected."), BorderLayout.CENTER);
            return panel;
        }

        Result result = currentSubmission.getResult();
        Status status = result == null ? null : result.getStatus();

        JPanel content = new JPanel();
        content.setBackground(BG_CANVAS);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton back = createTextButton("←  Back to Results");
        back.addActionListener(e -> openEvaluationResults(currentProject));

        JLabel studentTitle = new JLabel("Student Details");
        studentTitle.setFont(FONT_HEADER);
        studentTitle.setForeground(TEXT_PRIMARY);
        studentTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel studentInfo = new JLabel(currentSubmission.getStudentId() + "   •   Student");
        studentInfo.setFont(FONT_BODY);
        studentInfo.setForeground(TEXT_SECONDARY);
        studentInfo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel badge = createStatusBadge(status == Status.SUCCESS ? "Passed" : "Failed");

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setBackground(BG_CANVAS);
        headerRow.setMaximumSize(new Dimension(1050, 92));
        headerRow.setPreferredSize(new Dimension(1050, 92));
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel leftHeader = new JPanel();
        leftHeader.setLayout(new BoxLayout(leftHeader, BoxLayout.Y_AXIS));
        leftHeader.setBackground(BG_CANVAS);
        leftHeader.add(back);
        leftHeader.add(Box.createVerticalStrut(18));
        leftHeader.add(studentTitle);
        leftHeader.add(Box.createVerticalStrut(8));
        leftHeader.add(studentInfo);

        headerRow.add(leftHeader, BorderLayout.WEST);
        headerRow.add(badge, BorderLayout.EAST);

        JPanel topCards = new JPanel(new GridLayout(1, 2, 18, 0));
        topCards.setBackground(BG_CANVAS);
        topCards.setMaximumSize(new Dimension(1050, 155));
        topCards.setPreferredSize(new Dimension(1050, 155));
        topCards.setAlignmentX(Component.LEFT_ALIGNMENT);

        topCards.add(createSourceFilesCard(currentSubmission));
        topCards.add(createCompilationCard(status, result));

        JPanel compilationOutput = createOutputBlock(
                "Compilation Output (stderr)",
                result == null || result.getErrorMessage() == null || result.getErrorMessage().isBlank()
                        ? "(No compilation errors)"
                        : result.getErrorMessage(),
                true
        );
        compilationOutput.setMaximumSize(new Dimension(1050, 165));
        compilationOutput.setPreferredSize(new Dimension(1050, 165));
        compilationOutput.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel outputGrid = new JPanel(new GridLayout(1, 2, 18, 0));
        outputGrid.setBackground(BG_CANVAS);
        outputGrid.setMaximumSize(new Dimension(1050, 210));
        outputGrid.setPreferredSize(new Dimension(1050, 210));
        outputGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        String programOutput = result == null || result.getOutput() == null || result.getOutput().isBlank()
                ? "(Program did not run)"
                : result.getOutput();

        String expectedOutput = "(No expected output)";
        if (currentProject != null && currentProject.getTestCases() != null && !currentProject.getTestCases().isEmpty()) {
            expectedOutput = currentProject.getTestCases().get(0).getExpectedOutput();
        }

        outputGrid.add(createOutputBlock("Program Output", programOutput, false));
        outputGrid.add(createOutputBlock("Expected Output", expectedOutput, false));

        JPanel comparison = createComparisonCard(status);
        comparison.setMaximumSize(new Dimension(1050, 115));
        comparison.setPreferredSize(new Dimension(1050, 115));
        comparison.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(headerRow);
        content.add(Box.createVerticalStrut(22));
        content.add(topCards);
        content.add(Box.createVerticalStrut(20));
        content.add(compilationOutput);
        content.add(Box.createVerticalStrut(20));
        content.add(outputGrid);
        content.add(Box.createVerticalStrut(20));
        content.add(comparison);

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }
    private JLabel createStatusBadge(String text) {
        JLabel badge = new JLabel("  ⊗  " + text + "  ");
        badge.setOpaque(true);
        badge.setFont(new Font("SansSerif", Font.BOLD, 13));
        badge.setBorder(new EmptyBorder(8, 14, 8, 14));

        if (text.equals("Passed")) {
            badge.setBackground(new Color(220, 252, 231));
            badge.setForeground(new Color(22, 163, 74));
        } else {
            badge.setBackground(new Color(254, 226, 226));
            badge.setForeground(new Color(220, 38, 38));
        }

        return badge;
    }

    private JPanel createSourceFilesCard(StudentZipSubmission submission) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(new LineBorder(BORDER_COLOR, 1, true));

        JLabel title = new JLabel("▤  Source Files");
        title.setFont(FONT_SUBHEADER);
        title.setForeground(TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(BG_CARD);
        list.setBorder(new EmptyBorder(8, 18, 14, 18));

        File folder = submission.getExtractedFolder();
        File[] files = folder == null ? null : folder.listFiles();

        if (files == null || files.length == 0) {
            list.add(createFileRow("(No files found)"));
        } else {
            for (File file : files) {
                if (file.isFile()) {
                    list.add(createFileRow(file.getName()));
                    list.add(Box.createVerticalStrut(8));
                }
            }
        }

        card.add(title, BorderLayout.NORTH);
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private JPanel createFileRow(String fileName) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(new Color(248, 250, 252));
        row.setBorder(new EmptyBorder(9, 12, 9, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel name = new JLabel(fileName);
        name.setFont(new Font("Monospaced", Font.PLAIN, 12));
        name.setForeground(TEXT_PRIMARY);

        JLabel view = new JLabel("View");
        view.setFont(new Font("SansSerif", Font.PLAIN, 11));
        view.setForeground(new Color(19, 99, 128));

        row.add(name, BorderLayout.WEST);
        row.add(view, BorderLayout.EAST);
        return row;
    }

    private JPanel createCompilationCard(Status status, Result result) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(new LineBorder(BORDER_COLOR, 1, true));

        JLabel title = new JLabel(">_  Compilation Result");
        title.setFont(FONT_SUBHEADER);
        title.setForeground(TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BG_CARD);
        body.setBorder(new EmptyBorder(10, 18, 18, 18));

        boolean compileFailed = status == Status.COMPILE_ERROR || status == Status.EXTRACTION_ERROR;

        JLabel compileStatus = new JLabel(compileFailed ? "⊗  Compilation Failed" : "◎  Compilation Successful");
        compileStatus.setFont(new Font("SansSerif", Font.BOLD, 13));
        compileStatus.setForeground(compileFailed ? new Color(220, 38, 38) : new Color(5, 150, 105));

        JLabel exitCode = new JLabel(compileFailed ? "Exit Code: 1" : "Exit Code: 0");
        exitCode.setFont(FONT_BODY);
        exitCode.setForeground(TEXT_SECONDARY);

        body.add(compileStatus);
        body.add(Box.createVerticalStrut(10));
        body.add(exitCode);

        card.add(title, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel createOutputBlock(String titleText, String contentText, boolean errorStyle) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(new LineBorder(BORDER_COLOR, 1, true));

        JLabel title = new JLabel(titleText);
        title.setFont(FONT_SUBHEADER);
        title.setForeground(TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(14, 16, 10, 16));

        JTextArea area = new JTextArea(contentText);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setBackground(new Color(30, 41, 59));
        area.setForeground(errorStyle ? Color.RED : Color.WHITE);
        area.setBorder(new EmptyBorder(14, 16, 14, 16));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        // 🔥 EN ÖNEMLİ KISIM (AUTO HEIGHT)
        int lines = area.getLineCount();
        int height = Math.max(80, lines * 18 + 20);
        area.setPreferredSize(new Dimension(1000, height));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(30, 41, 59));
        wrapper.add(area, BorderLayout.CENTER);

        card.add(title, BorderLayout.NORTH);
        card.add(wrapper, BorderLayout.CENTER);

        return card;
    }

    private JPanel createComparisonCard(Status status) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(new LineBorder(BORDER_COLOR, 1, true));

        JLabel title = new JLabel("Comparison Result");
        title.setFont(FONT_SUBHEADER);
        title.setForeground(TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BG_CARD);
        body.setBorder(new EmptyBorder(14, 16, 14, 16));

        boolean success = status == Status.SUCCESS;

        JLabel resultLabel = new JLabel(success ? "◎  Output Comparison Successful" : "⊗  Output Comparison Failed");
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        resultLabel.setForeground(success ? new Color(5, 150, 105) : new Color(220, 38, 38));

        JLabel desc = new JLabel(success ? "Program output matches expected output." : "Compilation failed or output does not match expected output.");
        desc.setFont(FONT_BODY);
        desc.setForeground(TEXT_SECONDARY);

        body.add(resultLabel);
        body.add(Box.createVerticalStrut(8));
        body.add(desc);

        card.add(title, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        return card;
    }
    private JButton createTextButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY);
        btn.setForeground(new Color(19, 99, 128));
        btn.setBackground(BG_CANVAS);
        btn.setBorder(new EmptyBorder(0, 0, 0, 0));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        return btn;
    }
}
