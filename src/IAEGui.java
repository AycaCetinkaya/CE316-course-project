import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class IAEGui extends JFrame {
    private StudentZipSubmission currentSubmission;
    private JLabel topBarTitle;
    private Project currentProject;
    private JPanel mainContentPanel;
    private CardLayout cardLayout;
    private List<Configuration> allConfigs;
    private ConfigStore configStore;

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
    private int resultsPage = 1;
    private final int resultsPerPage = 8;

    public IAEGui() {
        this.configStore = new ConfigStore();
        this.allConfigs = configStore.loadAll();
        if (this.allConfigs.isEmpty()) {
            allConfigs.add(new Configuration("C Config", "C", "gcc *.c -o main", "./main", ".c", "int\\s+main"));
            allConfigs.add(new Configuration("Java Config", "JAVA", "javac *.java", "java $MAIN", ".java", "public\\s+static\\s+void\\s+main"));
            allConfigs.add(new Configuration("Python Config", "PYTHON", "echo skip", "python3 $MAIN", ".py", "if\\s+__name__\\s*==.*main"));
            allConfigs.add(new Configuration("Haskell Config", "HASKELL", "ghc --make $MAIN -o main", "./main", ".hs", "\\bmain\\s*[:=]"));
            configStore.saveAll(allConfigs);
        }
        setTitle("IAE - Integrated Assignment Environment");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 850);
        setMinimumSize(new Dimension(1000, 650));
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
        mainContentPanel.add(wrapWithScroll(createDashboardPanel()), "dashboard");
        mainContentPanel.add(wrapWithScroll(createCreateProjectPanel()), "createProject");
        mainContentPanel.add(wrapWithScroll(createConfigurationsPanel()), "configurations");
        mainContentPanel.add(wrapWithScroll(createHelpPanel()), "help");
        mainContentPanel.add(wrapWithScroll(createEvaluationResultsPanel()), "evaluationResults");
        mainContentPanel.add(wrapWithScroll(createStudentDetailsPanel()), "studentDetails");
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

        JLabel right = new JLabel("Lecturer Mode");
        right.setFont(FONT_BODY);
        right.setForeground(TEXT_SECONDARY);
        right.setBorder(new EmptyBorder(0, 0, 0, 24));
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        right.setPreferredSize(new Dimension(160, 48));

        topBar.add(topBarTitle, BorderLayout.WEST);
        topBar.add(right, BorderLayout.EAST);

        return topBar;
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

        JButton deleteButton = createProjectDeleteButton(project);
        JPanel rightBox = (JPanel) row.getClientProperty("rightBox");
        if (rightBox != null) {
            rightBox.add(deleteButton);
        }

        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isDescendingFrom(e.getComponent(), deleteButton)) {
                    return;
                }
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
        row.putClientProperty("rightBox", rightBox);

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

    private JButton createProjectDeleteButton(Project project) {
        JButton button = new JButton("Delete");
        button.setFont(new Font("SansSerif", Font.BOLD, 11));
        button.setForeground(new Color(220, 38, 38));
        button.setBackground(new Color(254, 242, 242));
        button.setBorder(new LineBorder(new Color(252, 165, 165), 1, true));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(76, 30));

        button.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Delete project: " + project.getName() + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            try {
                DatabaseManager db = new DatabaseManager();
                try {
                    db.connect();
                    db.initSchema();
                    long projectId = project.getId() > 0
                            ? project.getId()
                            : db.findProjectIdByName(project.getName());

                    if (projectId > 0) {
                        db.deleteProject(projectId);
                    }
                } finally {
                    db.disconnect();
                }

                recentProjects.remove(project);
                if (currentProject == project) {
                    currentProject = null;
                }
                refreshSavedProjects();
                refreshDashboard();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return button;
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
        updateConfigDropdown();
        List<String> configNames = new ArrayList<>();
        for (Configuration c : allConfigs) {
            configNames.add(c.getName());
        }
        configNames.add(0, "AUTO");

        cmbConfiguration = new JComboBox<>(configNames.toArray(new String[0]));
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
        btnSave.addActionListener(e -> saveProject());
        btnRun.addActionListener(e -> runProject());

        buttons.add(btnCancel);
        buttons.add(btnSave);
        buttons.add(btnRun);

        content.add(projectInfoCard);
        content.add(Box.createVerticalStrut(20));
        content.add(filesCard);
        content.add(Box.createVerticalStrut(20));

        panel.add(content, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }
    private String getRealText(JTextField field, String placeholder) {
        String text = field.getText().trim();
        return text.equals(placeholder) ? "" : text;
    }

    private void saveProject() {
        try {
            String projectName = getRealText(txtProjectName, "e.g., Data Structures - Assignment 1");
            String selected = (String) cmbConfiguration.getSelectedItem();

            if (projectName.isEmpty() || selected == null) {
                JOptionPane.showMessageDialog(this, "Please fill project name and configuration.");
                return;
            }

            Configuration selectedConfig = getSelectedConfiguration(selected);
            Configuration projectConfig = selectedConfig != null
                    ? selectedConfig
                    : new Configuration("AUTO", "AUTO", "", "", "", "");

            DatabaseManager db = new DatabaseManager();
            try {
                db.connect();
                db.initSchema();
                db.saveProject(projectName, projectConfig, buildTestCasesFromForm());
            } finally {
                db.disconnect();
            }

            refreshSavedProjects();
            JOptionPane.showMessageDialog(this, "Project saved successfully.");
            refreshDashboard();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<TestCase> buildTestCasesFromForm() throws java.io.IOException {
        String inputPath = getRealText(txtInputFile, "Select input file...");
        String expectedPath = getRealText(txtExpectedOutputFile, "Select expected output file...");

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

        return testCases;
    }

    private void refreshSavedProjects() throws java.sql.SQLException {
        DatabaseManager db = new DatabaseManager();
        try {
            db.connect();
            db.initSchema();
            recentProjects.clear();
            recentProjects.addAll(db.getProjects());
        } finally {
            db.disconnect();
        }
    }

    private void runProject() {
        try {
            String projectName = getRealText(txtProjectName, "e.g., Data Structures - Assignment 1");
            String submissionsPath = getRealText(txtSubmissionsFolder, "Select folder containing ZIP files...");

            String selected = (String) cmbConfiguration.getSelectedItem();

            if (projectName.isEmpty() || submissionsPath.isEmpty() || selected == null) {
                JOptionPane.showMessageDialog(this, "Please fill project name and submissions folder.");
                return;
            }

            Configuration selectedConfig = getSelectedConfiguration(selected);
            List<TestCase> testCases = buildTestCasesFromForm();

            ProjectRunnerService runner = new ProjectRunnerService();

            Project project = runner.runProject(
                    projectName,
                    new File(submissionsPath),
                    testCases,
                    selectedConfig
            );

            refreshSavedProjects();
            currentProject = project;

            JOptionPane.showMessageDialog(this, "Project evaluated successfully.");
            refreshDashboard();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Evaluation failed: " + ex.getMessage());
        }
    }
    private Configuration getSelectedConfiguration(String selected) {
        if (selected == null || selected.equals("AUTO")) {
            return null;
        }

        for (Configuration config : allConfigs) {
            if (config.getName().equals(selected)) {
                return config;
            }
        }

        return null;
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
                    currentProject.getConfiguration()
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
        if (selected == null) {
            return Language.AUTO;
        }

        if (selected.equals("AUTO")) {
            return Language.AUTO;
        }

        for (Configuration config : allConfigs) {
            if (config.getName().equals(selected)) {
                try {
                    return Language.valueOf(config.getLanguage().toUpperCase());
                } catch (IllegalArgumentException e) {
                    return Language.AUTO;
                }
            }
        }

        return Language.AUTO;
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
        JPanel panel = createPageBase(
                "Configuration Management",
                "Manage programming language configurations for compilation and execution"
        );

        JButton btnImport = createSecondaryButton("📥  Import");
        JButton btnExport = createSecondaryButton("📤  Export");
        JButton btnAdd = createOrangeButton("+  Add Configuration");

        btnAdd.addActionListener(e -> showConfigForm(null));

        btnImport.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Import Configurations");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON Files", "json"));

            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                try {
                    List<Configuration> importedConfigs = configStore.loadFrom(selectedFile);

                    if (importedConfigs != null && !importedConfigs.isEmpty()) {
                        allConfigs.addAll(importedConfigs);
                        configStore.saveAll(allConfigs);
                        refreshConfigPage();

                        JOptionPane.showMessageDialog(this, importedConfigs.size() + " configurations imported.");
                    } else {
                        JOptionPane.showMessageDialog(this, "The file is empty or invalid.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Import failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnExport.addActionListener(e -> {
            if (allConfigs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nothing to export.");
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Configurations");
            fileChooser.setSelectedFile(new File("exported_configs.json"));

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File saveFile = fileChooser.getSelectedFile();
                try {
                    configStore.saveTo(saveFile, allConfigs);
                    JOptionPane.showMessageDialog(this, "Configurations exported successfully to:\n" + saveFile.getAbsolutePath());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        Dimension actionBtnSize = new Dimension(120, 38);
        btnImport.setPreferredSize(actionBtnSize);
        btnExport.setPreferredSize(actionBtnSize);
        btnAdd.setPreferredSize(new Dimension(170, 38));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setBackground(BG_CANVAS);
        actionPanel.add(btnImport);
        actionPanel.add(btnExport);
        actionPanel.add(btnAdd);

        JPanel existingHeader = (JPanel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        JPanel topMasterPanel = new JPanel(new BorderLayout());
        topMasterPanel.setBackground(BG_CANVAS);
        topMasterPanel.add(existingHeader, BorderLayout.WEST);
        topMasterPanel.add(actionPanel, BorderLayout.EAST);
        topMasterPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        panel.add(topMasterPanel, BorderLayout.NORTH);

        String[] columns = {"Language", "Config Name", "Compile Command", "Run Command", "Extension", "Actions"};

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5;
            }
        };

        for (Configuration config : allConfigs) {
            model.addRow(new Object[]{
                    config.getLanguage(),
                    config.getName(),
                    config.getCompileCommand(),
                    config.getRunCommand(),
                    config.getSourceExtension(),
                    config
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(75);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 10));
        table.setBackground(BG_CANVAS);

        table.getTableHeader().setFont(FONT_SUBHEADER);
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setPreferredSize(new Dimension(0, 45));
        table.getTableHeader().setBorder(new MatteBorder(0, 0, 1, 0, BORDER_COLOR));

        ConfigTableRenderer contentRenderer = new ConfigTableRenderer();
        for (int i = 0; i < table.getColumnCount() - 1; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(contentRenderer);
        }

        table.getColumnModel().getColumn(5).setCellRenderer(new ActionButtonRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new ActionButtonEditor());

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        footerPanel.setBackground(new Color(255, 251, 242));
        footerPanel.setBorder(new LineBorder(new Color(253, 230, 138), 1, true));

        JLabel lblTemplate = new JLabel("<html><b style='color:#D97706;'>&lt;&gt; Configuration Templates</b><br>" +
                "<span style='color:#92400E;'>Use placeholders in your commands: $MAIN, {source}, {output}</span></html>");
        lblTemplate.setFont(FONT_BODY);
        footerPanel.add(lblTemplate);

        JPanel contentWrapper = new JPanel(new BorderLayout(0, 20));
        contentWrapper.setBackground(BG_CANVAS);
        contentWrapper.add(scrollPane, BorderLayout.CENTER);
        contentWrapper.add(footerPanel, BorderLayout.SOUTH);

        panel.add(contentWrapper, BorderLayout.CENTER);

        return panel;
    }

    private class ActionButtonRenderer extends DefaultTableCellRenderer {
        private final ActionButtonsPanel panel = new ActionButtonsPanel();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            panel.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            return panel;
        }
    }

    private class ActionButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final ActionButtonsPanel panel = new ActionButtonsPanel();
        private Configuration currentConfig;

        public ActionButtonEditor() {
            panel.btnEdit.addActionListener(e -> {
                stopCellEditing();
                if (currentConfig != null) {
                    showConfigForm(currentConfig);
                }
            });

            panel.btnDelete.addActionListener(e -> {
                stopCellEditing();
                if (currentConfig != null) {
                    int confirm = JOptionPane.showConfirmDialog(panel,
                            "Delete configuration: " + currentConfig.getName() + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        allConfigs.remove(currentConfig);
                        configStore.saveAll(allConfigs);
                        refreshConfigPage();
                    }
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value instanceof Configuration) {
                currentConfig = (Configuration) value;
            }
            panel.setBackground(table.getSelectionBackground());
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return currentConfig;
        }
    }

    private class ActionButtonsPanel extends JPanel {
        public final JButton btnEdit = new JButton("✎");
        public final JButton btnDelete = new JButton("🗑");

        public ActionButtonsPanel() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 8, 20));
            setBackground(Color.WHITE);
            styleActionButton(btnEdit, TEXT_PRIMARY);
            styleActionButton(btnDelete, new Color(220, 38, 38));
            add(btnEdit);
            add(btnDelete);
        }

        private void styleActionButton(JButton btn, Color color) {
            btn.setFont(new Font("SansSerif", Font.PLAIN, 18));
            btn.setForeground(color);
            btn.setBackground(new Color(241, 245, 249));
            btn.setBorder(new LineBorder(BORDER_COLOR, 1, true));
            btn.setFocusPainted(false);
            btn.setPreferredSize(new Dimension(40, 35));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
    }

    private class ConfigTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JPanel cell = new JPanel(new BorderLayout());
            cell.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            cell.setBorder(new EmptyBorder(5, 15, 5, 15));

            String text = (value != null) ? value.toString() : "";

            if (column >= 2 && column <= 4) {
                JLabel code = new JLabel(text);
                code.setOpaque(true);
                code.setBackground(new Color(241, 245, 249));
                code.setFont(new Font("Monospaced", Font.PLAIN, 12));
                code.setBorder(new EmptyBorder(4, 8, 4, 8));
                cell.add(code, BorderLayout.WEST);
            } else {
                JLabel label = new JLabel("<html>" + text.replace("\n", "<br>") + "</html>");
                label.setFont(FONT_BODY);
                label.setForeground(column == 0 ? TEXT_PRIMARY : TEXT_SECONDARY);
                cell.add(label, BorderLayout.CENTER);
            }
            return cell;
        }
    }

    private void showConfigForm(Configuration config) {
        JDialog dialog = new JDialog((Frame)null, config == null ? "Add Configuration" : "Edit Configuration", true);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JTextField nameField = new JTextField(config != null ? config.getName() : "");
        JTextField langField = new JTextField(config != null ? config.getLanguage() : "");
        JTextField compileField = new JTextField(config != null ? config.getCompileCommand() : "");
        JTextField runField = new JTextField(config != null ? config.getRunCommand() : "");
        JTextField extField = new JTextField(config != null ? config.getSourceExtension() : "");
        JTextField patternField = new JTextField(config != null ? config.getEntryPointPattern() : "");

        formPanel.add(new JLabel("Config Name:")); formPanel.add(nameField);
        formPanel.add(new JLabel("Language:")); formPanel.add(langField);
        formPanel.add(new JLabel("Compile Command:")); formPanel.add(compileField);
        formPanel.add(new JLabel("Run Command:")); formPanel.add(runField);
        formPanel.add(new JLabel("Source Extension:")); formPanel.add(extField);
        formPanel.add(new JLabel("Entry Pattern (Regex):")); formPanel.add(patternField);

        JButton btnSave = createStyledButton("Save Configuration", true);
        btnSave.addActionListener(e -> {
            Configuration newConfig = new Configuration(
                    nameField.getText(), langField.getText(), compileField.getText(),
                    runField.getText(), extField.getText(), patternField.getText()
            );

            if (config != null) allConfigs.remove(config);
            allConfigs.add(newConfig);
            configStore.saveAll(allConfigs);
            dialog.dispose();
            refreshConfigPage();
        });

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(btnSave, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private void refreshConfigPage() {
        mainContentPanel.add(createConfigurationsPanel(), "CONFIGURATIONS");
        updateConfigDropdown();
        cardLayout.show(mainContentPanel, "CONFIGURATIONS");
    }

    private JButton createStyledButton(String text, boolean primary) {
        JButton btn = new JButton(text);

        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));

        if (primary) {
            btn.setBackground(ACCENT_ORANGE);
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(Color.WHITE);
            btn.setForeground(SIDEBAR_COLOR);
            btn.setBorder(new LineBorder(new Color(203, 213, 225), 1));
        }
        return btn;
    }

    private void updateConfigDropdown() {
        if (cmbConfiguration == null) return;
        cmbConfiguration.removeAllItems();
        for (Configuration config : allConfigs) {
            cmbConfiguration.addItem(config.getName());
        }
    }

    private JPanel createHelpPanel() {
        JPanel panel = createPageBase("Help & Documentation", "Complete guide to using the Integrated Assignment Environment");

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(BG_CANVAS);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 0, 0, 0);

        contentPanel.add(createHelpTopicBox("Getting Started", new String[][]{
                {"What is IAE?", "The Integrated Assignment Environment (IAE) is a desktop application designed for lecturers to automatically evaluate programming assignments submitted by students. It compiles student code, runs it with test inputs, and compares outputs against expected results."},
                {"System Requirements", "IAE requires Windows 10 or later. You must have the necessary compilers/interpreters installed for the programming languages you want to evaluate (e.g., GCC for C, Python 3 for Python)."}
        }), gbc);
        gbc.gridy++;

        contentPanel.add(createHelpTopicBox("How to Create a Project", new String[][]{
                {"Step 1: Navigate to Create Project", "Click the 'Create New Project' button on the Dashboard or use the sidebar navigation."},
                {"Step 2: Fill in Project Details", "Enter a descriptive project name (e.g., 'Data Structures - Assignment 1') and select an appropriate language configuration from the dropdown menu."},
                {"Step 3: Select Files", "Choose your input file (test data), expected output file (correct results), and the folder containing student ZIP submissions."},
                {"Step 4: Save and Run", "Click 'Save Project' to store the configuration, or 'Run Project' to immediately start the evaluation process."}
        }), gbc);
        gbc.gridy++;

        contentPanel.add(createHelpTopicBox("How to Define Configurations", new String[][]{
                {"What are Configurations?", "Configurations define how to compile and run programs for different programming languages. Each configuration specifies compiler paths, compile commands, and run commands."},
                {"Command Placeholders", "Use these placeholders in your commands: {source} for source file path, {output} for executable name, {classname} for Java class names."},
                {"Import/Export", "Save time by exporting existing configurations and importing them on other machines or sharing them with colleagues."}
        }), gbc);
        gbc.gridy++;

        gbc.weighty = 1.0;
        contentPanel.add(Box.createVerticalGlue(), gbc);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(BG_CANVAS);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHelpTopicBox(String mainTitle, String[][] sections) {
        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());
        container.setBackground(BG_CANVAS);
        container.setBorder(new EmptyBorder(0, 0, 30, 0));
        container.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 12));
        titleBox.setBackground(new Color(226, 232, 240));
        titleBox.setBorder(new MatteBorder(1, 1, 0, 1, BORDER_COLOR));

        JLabel lblTitle = new JLabel(mainTitle);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 20)); // Slightly larger for full-screen
        lblTitle.setForeground(TEXT_PRIMARY);
        titleBox.add(lblTitle);

        JPanel contentBox = new JPanel();
        contentBox.setLayout(new BoxLayout(contentBox, BoxLayout.Y_AXIS));
        contentBox.setBackground(BG_CARD);
        contentBox.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                new EmptyBorder(25, 25, 10, 25)
        ));

        for (String[] section : sections) {
            JLabel subtitle = new JLabel(section[0]);
            subtitle.setFont(FONT_SUBHEADER);
            subtitle.setForeground(TEXT_PRIMARY);
            subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

            JTextArea content = new JTextArea(section[1]);
            content.setFont(FONT_BODY);
            content.setForeground(TEXT_SECONDARY);
            content.setLineWrap(true);
            content.setWrapStyleWord(true);
            content.setEditable(false);
            content.setFocusable(false);
            content.setBackground(BG_CARD);
            content.setBorder(new EmptyBorder(8, 0, 20, 0));
            content.setAlignmentX(Component.LEFT_ALIGNMENT);

            contentBox.add(subtitle);
            contentBox.add(content);
        }

        container.add(titleBox, BorderLayout.NORTH);
        container.add(contentBox, BorderLayout.CENTER);

        container.setMaximumSize(new Dimension(1600, container.getPreferredSize().height));

        return container;
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
        btn.setOpaque(true);
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
        btn.setOpaque(true);
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
        btn.setOpaque(true);
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
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_CANVAS);
        panel.setBorder(new EmptyBorder(28, 40, 35, 40));

        if (currentProject == null) {
            JPanel headerOnly = new JPanel();
            headerOnly.setLayout(new BoxLayout(headerOnly, BoxLayout.Y_AXIS));
            headerOnly.setBackground(BG_CANVAS);

            JLabel titleLabel = new JLabel("Evaluation Results");
            titleLabel.setFont(FONT_HEADER);
            titleLabel.setForeground(TEXT_PRIMARY);

            JLabel subtitleLabel = new JLabel("No project selected");
            subtitleLabel.setFont(FONT_BODY);
            subtitleLabel.setForeground(TEXT_SECONDARY);
            subtitleLabel.setBorder(new EmptyBorder(8, 0, 25, 0));

            JLabel empty = new JLabel("No evaluation results available.");
            empty.setFont(FONT_BODY);
            empty.setForeground(TEXT_SECONDARY);

            headerOnly.add(titleLabel);
            headerOnly.add(subtitleLabel);
            headerOnly.add(empty);

            panel.add(headerOnly, BorderLayout.NORTH);
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

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setBackground(BG_CANVAS);
        headerRow.setBorder(new EmptyBorder(0, 0, 28, 0));

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setBackground(BG_CANVAS);

        JLabel titleLabel = new JLabel("Evaluation Results");
        titleLabel.setFont(FONT_HEADER);
        titleLabel.setForeground(TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel(currentProject.getName());
        subtitleLabel.setFont(FONT_BODY);
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setBorder(new EmptyBorder(8, 0, 0, 0));

        titleBox.add(titleLabel);
        titleBox.add(subtitleLabel);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        topActions.setBackground(BG_CANVAS);
        topActions.setBorder(new EmptyBorder(6, 0, 0, 0));

        JButton btnProjectDetails = createToolbarButton("file", "Project Details", false);
        JButton btnRerun = createToolbarButton("refresh", "Re-run", false);
        JButton btnExport = createToolbarButton("download", "Export Results", true);

        btnProjectDetails.setPreferredSize(new Dimension(170, 46));
        btnRerun.setPreferredSize(new Dimension(125, 46));
        btnExport.setPreferredSize(new Dimension(170, 46));

        btnRerun.addActionListener(e -> rerunCurrentProject());
        btnExport.addActionListener(e -> JOptionPane.showMessageDialog(this, "Export feature will be added next."));

        topActions.add(btnProjectDetails);
        topActions.add(btnRerun);
        topActions.add(btnExport);

        headerRow.add(titleBox, BorderLayout.WEST);
        headerRow.add(topActions, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout(24, 0));
        content.setBackground(BG_CANVAS);

        JPanel tableCard = createModernStudentResultsTable();
        tableCard.setPreferredSize(new Dimension(950, 620));

        JPanel rightArea = new JPanel();
        rightArea.setBackground(BG_CANVAS);
        rightArea.setLayout(new BoxLayout(rightArea, BoxLayout.Y_AXIS));
        rightArea.setPreferredSize(new Dimension(340, 620));

        rightArea.add(createSideStatCard("Total Students", String.valueOf(total), "users",
                new Color(37, 99, 235), new Color(219, 234, 254)));
        rightArea.add(Box.createVerticalStrut(14));

        rightArea.add(createSideStatCard("Passed", String.valueOf(passed), "check",
                new Color(22, 163, 74), new Color(220, 252, 231)));
        rightArea.add(Box.createVerticalStrut(14));

        rightArea.add(createSideStatCard("Failed", String.valueOf(failed), "x",
                new Color(220, 38, 38), new Color(254, 226, 226)));
        rightArea.add(Box.createVerticalStrut(14));

        rightArea.add(createPassRateCard(passRate, passed, total));

        content.add(tableCard, BorderLayout.CENTER);
        content.add(rightArea, BorderLayout.EAST);

        panel.add(headerRow, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);

        return panel;
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
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < currentProject.getTestCases().size(); i++) {
                TestCase tc = currentProject.getTestCases().get(i);
                sb.append("Test Case ").append(i + 1).append(":\n");
                sb.append(tc.getExpectedOutput()).append("\n\n");
            }

            expectedOutput = sb.toString().trim();
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
    private JScrollPane wrapWithScroll(JPanel panel) {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BG_CANVAS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }
    private JPanel createModernStudentResultsTable() {
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(BG_CARD);
        tableCard.setBorder(new LineBorder(BORDER_COLOR, 1, true));

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setBackground(BG_CARD);
        tableHeader.setBorder(new EmptyBorder(20, 22, 20, 22));

        JLabel title = new JLabel("Student Results");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT_PRIMARY);

        JTextField search = createTextField("Search by ID or name...");
        search.setPreferredSize(new Dimension(300, 40));
        search.setMaximumSize(new Dimension(300, 40));

        tableHeader.add(title, BorderLayout.WEST);
        tableHeader.add(search, BorderLayout.EAST);

        String[] columns = {
                "STUDENT ID", "COMPILE STATUS", "RUN STATUS",
                "OUTPUT STATUS", "FINAL RESULT", "ACTIONS"
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(68);
        table.setFont(FONT_BODY);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(239, 246, 255));
        table.setDefaultRenderer(Object.class, new ModernEvaluationTableRenderer());

        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setForeground(new Color(71, 85, 105));
        table.getTableHeader().setPreferredSize(new Dimension(0, 52));
        table.getTableHeader().setBorder(new MatteBorder(1, 0, 1, 0, BORDER_COLOR));

        JPanel tableHolder = new JPanel(new BorderLayout());
        tableHolder.setBackground(Color.WHITE);
        tableHolder.add(table.getTableHeader(), BorderLayout.NORTH);
        tableHolder.add(table, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(BG_CARD);
        footer.setBorder(new EmptyBorder(14, 22, 14, 22));

        JLabel showing = new JLabel();
        showing.setFont(FONT_BODY);
        showing.setForeground(TEXT_SECONDARY);

        JPanel pagination = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        pagination.setBackground(BG_CARD);

        JButton prev = createSecondaryButton("«");
        JButton page = createBluePrimaryButton(String.valueOf(resultsPage));
        JButton next = createSecondaryButton("»");

        prev.setPreferredSize(new Dimension(44, 36));
        page.setPreferredSize(new Dimension(44, 36));
        next.setPreferredSize(new Dimension(44, 36));

        pagination.add(prev);
        pagination.add(page);
        pagination.add(next);

        JButton perPage = createSecondaryButton(resultsPerPage + " per page  ⌄");
        perPage.setPreferredSize(new Dimension(125, 36));

        footer.add(showing, BorderLayout.WEST);
        footer.add(pagination, BorderLayout.CENTER);
        footer.add(perPage, BorderLayout.EAST);

        List<StudentZipSubmission> displayedSubmissions = new ArrayList<>();

        final Runnable[] refreshResultsTable = new Runnable[1];

        refreshResultsTable[0] = () -> {
            String query = getRealText(search, "Search by ID or name...").toLowerCase().trim();

            List<StudentZipSubmission> filteredSubmissions = new ArrayList<>();

            for (StudentZipSubmission s : currentProject.getSubmissions()) {
                Status status = s.getResult() == null ? null : s.getResult().getStatus();

                String searchableText = (
                        s.getStudentId() + " " +
                                getCompileStatus(status) + " " +
                                getRunStatus(status) + " " +
                                getOutputStatus(status) + " " +
                                getFinalStatus(status)
                ).toLowerCase();

                if (query.isEmpty() || searchableText.contains(query)) {
                    filteredSubmissions.add(s);
                }
            }

            int totalRows = filteredSubmissions.size();
            int totalPages = Math.max(1, (int) Math.ceil(totalRows / (double) resultsPerPage));

            if (resultsPage > totalPages) resultsPage = totalPages;
            if (resultsPage < 1) resultsPage = 1;

            int start = (resultsPage - 1) * resultsPerPage;
            int end = Math.min(start + resultsPerPage, totalRows);

            model.setRowCount(0);
            displayedSubmissions.clear();

            for (int i = start; i < end; i++) {
                StudentZipSubmission s = filteredSubmissions.get(i);
                Status status = s.getResult() == null ? null : s.getResult().getStatus();

                displayedSubmissions.add(s);

                model.addRow(new Object[]{
                        s.getStudentId(),
                        getCompileStatus(status),
                        getRunStatus(status),
                        getOutputStatus(status),
                        getFinalStatus(status),
                        "Student Details  ›"
                });
            }

            if (totalRows == 0) {
                showing.setText("Showing 0 results");
            } else {
                showing.setText("Showing " + (start + 1) + " to " + end + " of " + totalRows + " results");
            }

            page.setText(String.valueOf(resultsPage));
            prev.setEnabled(resultsPage > 1);
            next.setEnabled(resultsPage < totalPages);
            perPage.setText(resultsPerPage + " per page  ⌄");
        };

        search.getDocument().addDocumentListener(new DocumentListener() {
            private void updateSearch() {
                resultsPage = 1;
                refreshResultsTable[0].run();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSearch();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSearch();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSearch();
            }
        });

        prev.addActionListener(e -> {
            if (resultsPage > 1) {
                resultsPage--;
                refreshResultsTable[0].run();
            }
        });

        next.addActionListener(e -> {
            resultsPage++;
            refreshResultsTable[0].run();
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());

                if (row >= 0 && col == 5 && row < displayedSubmissions.size()) {
                    StudentZipSubmission submission = displayedSubmissions.get(row);
                    openStudentDetails(submission);
                }
            }
        });

        refreshResultsTable[0].run();

        tableCard.add(tableHeader, BorderLayout.NORTH);
        tableCard.add(tableHolder, BorderLayout.CENTER);
        tableCard.add(footer, BorderLayout.SOUTH);

        return tableCard;
    }
    private JPanel createSideStatCard(String title, String value, String iconType, Color accent, Color softBg) {
        JPanel card = new JPanel(new BorderLayout(18, 0));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(18, 18, 18, 18)
        ));
        card.setMaximumSize(new Dimension(340, 110));
        card.setPreferredSize(new Dimension(340, 110));

        RoundedPanel iconBox = new RoundedPanel(18, softBg);
        iconBox.setLayout(new GridBagLayout());
        iconBox.setPreferredSize(new Dimension(72, 72));
        iconBox.setMinimumSize(new Dimension(72, 72));
        iconBox.setMaximumSize(new Dimension(72, 72));

        JLabel icon = new JLabel(createUiIcon(iconType, accent, 40));
        iconBox.add(icon);

        JPanel textBox = new JPanel();
        textBox.setBackground(BG_CARD);
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLabel.setForeground(TEXT_PRIMARY);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 34));
        valueLabel.setForeground(accent);

        textBox.add(titleLabel);
        textBox.add(Box.createVerticalStrut(8));
        textBox.add(valueLabel);

        card.add(iconBox, BorderLayout.WEST);
        card.add(textBox, BorderLayout.CENTER);

        return card;
    }

    private JPanel createPassRateCard(int passRate, int passed, int total) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(18, 18, 18, 18)
        ));
        card.setMaximumSize(new Dimension(340, 270));
        card.setPreferredSize(new Dimension(340, 270));

        JLabel title = new JLabel("Pass Rate");
        title.setFont(FONT_SUBHEADER);
        title.setForeground(TEXT_PRIMARY);

        PassRateCirclePanel circle = new PassRateCirclePanel(passRate);
        circle.setPreferredSize(new Dimension(250, 175));

        JLabel summary = new JLabel(passed + " out of " + total + " students passed", SwingConstants.CENTER);
        summary.setFont(FONT_BODY);
        summary.setForeground(TEXT_SECONDARY);

        card.add(title, BorderLayout.NORTH);
        card.add(circle, BorderLayout.CENTER);
        card.add(summary, BorderLayout.SOUTH);

        return card;
    }

    private JButton createBluePrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBackground(new Color(37, 99, 235));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorder(new EmptyBorder(9, 18, 9, 18));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
    private class PassRateCirclePanel extends JPanel {
        private final int percent;

        public PassRateCirclePanel(int percent) {
            this.percent = percent;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = 150;
            int x = (getWidth() - size) / 2;
            int y = 8;

            g2.setStroke(new BasicStroke(14, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            g2.setColor(new Color(226, 232, 240));
            g2.drawArc(x, y, size, size, 0, 360);

            g2.setColor(new Color(34, 197, 94));
            int angle = (int) Math.round(360 * (percent / 100.0));
            g2.drawArc(x, y, size, size, 90, -angle);

            g2.setFont(new Font("SansSerif", Font.BOLD, 34));
            g2.setColor(new Color(22, 163, 74));
            String text = percent + "%";
            FontMetrics fm = g2.getFontMetrics();
            int tx = (getWidth() - fm.stringWidth(text)) / 2;
            int ty = y + size / 2 + 10;
            g2.drawString(text, tx, ty);

            g2.setFont(FONT_BODY);
            g2.setColor(TEXT_SECONDARY);
            String label = "Pass Rate";
            FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(label, (getWidth() - fm2.stringWidth(label)) / 2, ty + 28);

            g2.dispose();
        }
    }

    private class ModernEvaluationTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            String text = value == null ? "" : value.toString();

            if (column >= 1 && column <= 4) {
                JPanel wrapper = new JPanel(new GridBagLayout());
                wrapper.setOpaque(true);
                wrapper.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                wrapper.setBorder(new EmptyBorder(0, 12, 0, 12));

                RoundedStatusBadge badge;

                if (text.contains("Success") || text.contains("Match") || text.equals("Passed")) {
                    badge = new RoundedStatusBadge(
                            text,
                            new Color(220, 252, 231),
                            new Color(5, 150, 105),
                            column == 4 ? null : "check"
                    );
                } else if (text.contains("Failed") || text.contains("Mismatch")) {
                    badge = new RoundedStatusBadge(
                            text,
                            new Color(254, 226, 226),
                            new Color(220, 38, 38),
                            column == 4 ? null : "x"
                    );
                } else if (text.contains("Error")) {
                    badge = new RoundedStatusBadge(
                            text,
                            new Color(255, 237, 213),
                            new Color(234, 88, 12),
                            "warning"
                    );
                } else {
                    badge = new RoundedStatusBadge(
                            text,
                            new Color(241, 245, 249),
                            new Color(71, 85, 105),
                            null
                    );
                }

                wrapper.add(badge);
                return wrapper;
            }

            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column
            );

            label.setFont(FONT_BODY);
            label.setOpaque(true);
            label.setBorder(new EmptyBorder(0, 20, 0, 20));
            label.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);

            if (column == 5) {
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setForeground(new Color(37, 99, 235));
                label.setFont(new Font("SansSerif", Font.BOLD, 13));
                label.setText("Student Details  ›");
            } else {
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setForeground(TEXT_PRIMARY);
                label.setText(text);
            }

            return label;
        }
    }
    private JButton createToolbarButton(String iconType, String text, boolean primary) {
        JButton btn = new JButton(text);
        btn.setIcon(createUiIcon(iconType, primary ? Color.WHITE : TEXT_PRIMARY, 18));
        btn.setIconTextGap(10);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setHorizontalAlignment(SwingConstants.CENTER);

        if (primary) {
            btn.setBackground(new Color(37, 99, 235));
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(37, 99, 235), 1, true),
                    new EmptyBorder(11, 18, 11, 18)
            ));
        } else {
            btn.setBackground(Color.WHITE);
            btn.setForeground(TEXT_PRIMARY);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(203, 213, 225), 1, true),
                    new EmptyBorder(11, 18, 11, 18)
            ));
        }

        return btn;
    }
    private class RoundedStatusBadge extends JLabel {
        private final Color bg;

        public RoundedStatusBadge(String text, Color bg, Color fg, String iconType) {
            super(text);
            this.bg = bg;

            setForeground(fg);
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setBorder(new EmptyBorder(7, 14, 7, 14));
            setOpaque(false);

            if (iconType != null) {
                setIcon(createUiIcon(iconType, fg, 14));
                setIconTextGap(6);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

            g2.dispose();
            super.paintComponent(g);
        }
    }
    private Icon createUiIcon(String type, Color color, int size) {
        return new UiIcon(type, color, size);
    }

    private class UiIcon implements Icon {
        private final String type;
        private final Color color;
        private final int size;

        public UiIcon(String type, Color color, int size) {
            this.type = type;
            this.color = color;
            this.size = size;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(Math.max(2f, size / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int s = size;
            int cx = x + s / 2;
            int cy = y + s / 2;

            switch (type) {
                case "file":
                    g2.drawRoundRect(x + s / 4, y + s / 8, s / 2, s * 3 / 4, 3, 3);
                    g2.drawLine(x + s * 5 / 8, y + s / 8, x + s * 3 / 4, y + s / 4);
                    g2.drawLine(x + s * 3 / 4, y + s / 4, x + s * 3 / 4, y + s / 3);
                    break;

                case "refresh":
                    g2.drawArc(x + s / 5, y + s / 5, s * 3 / 5, s * 3 / 5, 35, 285);
                    g2.drawLine(x + s * 3 / 4, y + s / 3, x + s * 7 / 8, y + s / 3);
                    g2.drawLine(x + s * 3 / 4, y + s / 3, x + s * 3 / 4, y + s / 5);
                    break;

                case "download":
                    g2.drawLine(cx, y + s / 5, cx, y + s * 3 / 5);
                    g2.drawLine(cx, y + s * 3 / 5, x + s / 3, y + s / 2);
                    g2.drawLine(cx, y + s * 3 / 5, x + s * 2 / 3, y + s / 2);
                    g2.drawLine(x + s / 4, y + s * 4 / 5, x + s * 3 / 4, y + s * 4 / 5);
                    break;

                case "users":
                    g2.drawOval(x + s / 3, y + s / 6, s / 3, s / 3);
                    g2.drawArc(x + s / 4, y + s / 2, s / 2, s / 3, 0, 180);

                    g2.drawOval(x + s / 12, y + s / 3, s / 4, s / 4);
                    g2.drawArc(x, y + s * 3 / 5, s / 3, s / 4, 0, 180);

                    g2.drawOval(x + s * 2 / 3, y + s / 3, s / 4, s / 4);
                    g2.drawArc(x + s * 2 / 3, y + s * 3 / 5, s / 3, s / 4, 0, 180);
                    break;

                case "check":
                    g2.drawLine(x + s / 4, y + s / 2, x + s * 2 / 5, y + s * 2 / 3);
                    g2.drawLine(x + s * 2 / 5, y + s * 2 / 3, x + s * 3 / 4, y + s / 3);
                    break;

                case "x":
                    g2.drawLine(x + s / 4, y + s / 4, x + s * 3 / 4, y + s * 3 / 4);
                    g2.drawLine(x + s * 3 / 4, y + s / 4, x + s / 4, y + s * 3 / 4);
                    break;

                case "checkCircle":
                    g2.drawOval(x + s / 8, y + s / 8, s * 3 / 4, s * 3 / 4);
                    g2.drawLine(x + s / 3, y + s / 2, x + s * 9 / 20, y + s * 13 / 20);
                    g2.drawLine(x + s * 9 / 20, y + s * 13 / 20, x + s * 2 / 3, y + s / 3);
                    break;

                case "xCircle":
                    g2.drawOval(x + s / 8, y + s / 8, s * 3 / 4, s * 3 / 4);
                    g2.drawLine(x + s / 3, y + s / 3, x + s * 2 / 3, y + s * 2 / 3);
                    g2.drawLine(x + s * 2 / 3, y + s / 3, x + s / 3, y + s * 2 / 3);
                    break;

                case "warning":
                    Polygon triangle = new Polygon();
                    triangle.addPoint(cx, y + s / 8);
                    triangle.addPoint(x + s * 7 / 8, y + s * 7 / 8);
                    triangle.addPoint(x + s / 8, y + s * 7 / 8);
                    g2.drawPolygon(triangle);
                    g2.drawLine(cx, y + s / 3, cx, y + s * 3 / 5);
                    g2.fillOval(cx - 1, y + s * 7 / 10, 3, 3);
                    break;

                default:
                    g2.fillOval(cx - 2, cy - 2, 4, 4);
                    break;
            }

            g2.dispose();
        }
    }
    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bgColor;

        public RoundedPanel(int radius, Color bgColor) {
            this.radius = radius;
            this.bgColor = bgColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
