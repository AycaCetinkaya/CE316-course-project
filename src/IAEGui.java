import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.HashMap;
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
    private JComboBox<String> cmbComparator;
    private DefaultTableModel testCaseTableModel;
    private JTextField txtSubmissionsFolder;
    private final Map<String, ProjectFilePaths> projectPathsByName = new HashMap<>();

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
    private int recentProjectsPage = 1;
    private final int recentProjectsPerPage = 5;
    private volatile java.util.function.Consumer<String> evaluationProgressUpdater;

    public IAEGui() {
        this.configStore = new ConfigStore();
        this.allConfigs = loadConfigurationsFromDb();
        if (this.allConfigs.isEmpty()) {
            allConfigs.add(new Configuration("C Config", "C", "gcc *.c -o main", "./main", ".c", "int\\s+main"));
            allConfigs.add(new Configuration("Java Config", "JAVA", "javac *.java", "java $MAIN", ".java", "public\\s+static\\s+void\\s+main"));
            allConfigs.add(new Configuration("Python Config", "PYTHON", "echo skip", "python3 $MAIN", ".py", "if\\s+__name__\\s*==.*main"));
            allConfigs.add(new Configuration("Haskell Config", "HASKELL", "ghc --make $MAIN -o main", "./main", ".hs", "\\bmain\\s*[:=]"));
            for (Configuration c : allConfigs) {
                persistConfigurationToDb(c);
            }
        }
        setTitle("IAE - Integrated Assignment Environment");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 850);
        setMinimumSize(new Dimension(1000, 650));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        setJMenuBar(createMenuBar());

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
        mainContentPanel.add(wrapWithScroll(createProjectDetailsPanel()), "projectDetails");
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

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');

        JMenuItem openItem = new JMenuItem("Open Project...");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        openItem.addActionListener(e -> openProjectFromFile());

        JMenuItem saveAsItem = new JMenuItem("Save Project As...");
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
                        | InputEvent.SHIFT_DOWN_MASK));
        saveAsItem.addActionListener(e -> saveProjectAs());

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> dispose());

        fileMenu.add(openItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        return menuBar;
    }

    private Project pickProjectForExport() {
        List<Project> options = new ArrayList<>();
        if (currentProject != null) options.add(currentProject);
        for (Project p : recentProjects) {
            if (!options.contains(p)) options.add(p);
        }

        if (options.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "There are no projects to save.\nCreate or open a project first.",
                    "Nothing to save",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        if (options.size() == 1) return options.get(0);

        String[] names = new String[options.size()];
        for (int i = 0; i < options.size(); i++) names[i] = options.get(i).getName();
        String selected = (String) JOptionPane.showInputDialog(
                this,
                "Which project do you want to save?",
                "Save Project As",
                JOptionPane.QUESTION_MESSAGE,
                null,
                names,
                names[0]
        );
        if (selected == null) return null;
        for (Project p : options) {
            if (p.getName().equals(selected)) return p;
        }
        return null;
    }

    private void saveProjectAs() {
        Project project = pickProjectForExport();
        if (project == null) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Project As");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "IAE Project Bundle (*." + ProjectStore.FILE_EXTENSION + ")",
                ProjectStore.FILE_EXTENSION));
        chooser.setSelectedFile(new File(sanitizeFileName(project.getName())
                + "." + ProjectStore.FILE_EXTENSION));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase().endsWith("." + ProjectStore.FILE_EXTENSION)) {
            target = new File(target.getParentFile(),
                    target.getName() + "." + ProjectStore.FILE_EXTENSION);
        }

        if (target.exists()) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "File already exists:\n" + target.getAbsolutePath() + "\n\nOverwrite?",
                    "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        ProjectFilePaths paths = getProjectPaths(project);
        try {
            new ProjectStore().saveTo(target, project,
                    paths.inputFilePath,
                    paths.expectedOutputFilePath,
                    paths.submissionsFolderPath);
            JOptionPane.showMessageDialog(this,
                    "Project saved to:\n" + target.getAbsolutePath(),
                    "Project Saved",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Save failed: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openProjectFromFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Project");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "IAE Project Bundle (*." + ProjectStore.FILE_EXTENSION + ")",
                ProjectStore.FILE_EXTENSION));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File source = chooser.getSelectedFile();
        try {
            ProjectStore.Bundle bundle = new ProjectStore().loadFrom(source);
            importBundle(bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Open failed: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importBundle(ProjectStore.Bundle bundle) {
        Project bundleProject = bundle.project;
        String desiredName = bundleProject.getName();

        DatabaseManager db = new DatabaseManager();
        try {
            db.connect();
            db.initSchema();

            long existingId = db.findProjectIdByName(desiredName);
            String finalName = desiredName;

            if (existingId > 0) {
                String[] options = {"Overwrite", "Rename", "Skip"};
                int choice = JOptionPane.showOptionDialog(
                        this,
                        "A project named '" + desiredName + "' already exists.\n" +
                                "What would you like to do?",
                        "Project Name Conflict",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[1]
                );

                if (choice == JOptionPane.CLOSED_OPTION || choice == 2) {
                    return;
                }

                if (choice == 1) {
                    String suggested = makeUniqueName(db, desiredName);
                    String input = (String) JOptionPane.showInputDialog(
                            this,
                            "Enter a new name for the imported project:",
                            "Rename Imported Project",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            null,
                            suggested
                    );
                    if (input == null || input.trim().isEmpty()) return;
                    finalName = input.trim();
                    if (db.findProjectIdByName(finalName) > 0) {
                        JOptionPane.showMessageDialog(this,
                                "Name '" + finalName + "' is also in use. Import cancelled.",
                                "Name Conflict",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                } else {
                    db.deleteProject(existingId);
                }
            }

            persistConfigurationToDb(bundleProject.getConfiguration());

            long projectId = db.saveProject(
                    finalName,
                    bundleProject.getConfiguration(),
                    bundleProject.getTestCases()
            );

            List<TestCase> savedTestCases = db.getTestCasesForProject(projectId);

            for (StudentZipSubmission sub : bundleProject.getSubmissions()) {
                long subId = db.upsertSubmission(projectId, sub);

                List<PerTestResult> remapped = new ArrayList<>();
                for (PerTestResult ptr : sub.getPerTestResults()) {
                    int idx = (int) ptr.getTestCaseId();
                    if (idx < 0 || idx >= savedTestCases.size()) continue;

                    remapped.add(new PerTestResult(
                            savedTestCases.get(idx).getId(),
                            ptr.getStatus(),
                            ptr.getActualOutput(),
                            ptr.getErrorMessage(),
                            ptr.getExitCode()
                    ));
                }
                db.replaceDetailedResults(subId, remapped);
                sub.setPerTestResults(remapped);
            }

            if (bundleProject.getLastModified() > 0) {
                db.touchProject(projectId);
            }

            rememberProjectPaths(finalName,
                    bundle.inputFilePath,
                    bundle.expectedOutputFilePath,
                    bundle.submissionsFolderPath);

            refreshSavedProjects();
            JOptionPane.showMessageDialog(this,
                    "Project imported as '" + finalName + "'.",
                    "Project Imported",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshDashboard();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Import failed: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            db.disconnect();
        }
    }

    private String makeUniqueName(DatabaseManager db, String base) throws java.sql.SQLException {
        if (db.findProjectIdByName(base) <= 0) return base;
        for (int i = 2; i < 1000; i++) {
            String candidate = base + " (" + i + ")";
            if (db.findProjectIdByName(candidate) <= 0) return candidate;
        }
        return base + " (" + System.currentTimeMillis() + ")";
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
            case "projectDetails":
                return "Project Details";
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
        panel.setMaximumSize(new Dimension(1050, 520));
        panel.setPreferredSize(new Dimension(1050, 520));
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

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(BG_CARD);
        footer.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel showing = new JLabel();
        showing.setFont(FONT_BODY);
        showing.setForeground(TEXT_SECONDARY);

        JPanel pagination = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        pagination.setBackground(BG_CARD);

        JButton prev = createSecondaryButton("«");
        JButton page = createBluePrimaryButton(String.valueOf(recentProjectsPage));
        JButton next = createSecondaryButton("»");

        prev.setPreferredSize(new Dimension(44, 34));
        page.setPreferredSize(new Dimension(44, 34));
        next.setPreferredSize(new Dimension(44, 34));

        pagination.add(prev);
        pagination.add(page);
        pagination.add(next);

        footer.add(showing, BorderLayout.WEST);
        footer.add(pagination, BorderLayout.CENTER);

        final Runnable[] refreshRecentProjects = new Runnable[1];

        refreshRecentProjects[0] = () -> {
            list.removeAll();

            if (recentProjects.isEmpty()) {
                JLabel empty = new JLabel("No projects have been evaluated yet.");
                empty.setFont(FONT_BODY);
                empty.setForeground(TEXT_SECONDARY);
                empty.setBorder(new EmptyBorder(25, 20, 20, 20));
                list.add(empty);

                showing.setText("No projects");
                page.setText("1");
                prev.setEnabled(false);
                next.setEnabled(false);

                list.revalidate();
                list.repaint();
                return;
            }

            List<Project> orderedProjects = new ArrayList<>(recentProjects);
            orderedProjects.sort((a, b) -> {
                long la = a.getLastModified();
                long lb = b.getLastModified();
                if (la != lb) return Long.compare(lb, la);
                return Long.compare(b.getId(), a.getId());
            });

            int totalRows = orderedProjects.size();
            int totalPages = Math.max(1, (int) Math.ceil(totalRows / (double) recentProjectsPerPage));

            if (recentProjectsPage > totalPages) recentProjectsPage = totalPages;
            if (recentProjectsPage < 1) recentProjectsPage = 1;

            int start = (recentProjectsPage - 1) * recentProjectsPerPage;
            int end = Math.min(start + recentProjectsPerPage, totalRows);

            for (int i = start; i < end; i++) {
                list.add(createProjectRow(orderedProjects.get(i)));
            }

            showing.setText("Showing " + (start + 1) + " to " + end + " of " + totalRows + " projects");

            page.setText(String.valueOf(recentProjectsPage));
            prev.setEnabled(recentProjectsPage > 1);
            next.setEnabled(recentProjectsPage < totalPages);

            list.revalidate();
            list.repaint();
        };

        prev.addActionListener(e -> {
            if (recentProjectsPage > 1) {
                recentProjectsPage--;
                refreshRecentProjects[0].run();
            }
        });

        next.addActionListener(e -> {
            recentProjectsPage++;
            refreshRecentProjects[0].run();
        });

        refreshRecentProjects[0].run();

        panel.add(header, BorderLayout.NORTH);
        panel.add(list, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);

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

        JButton renameButton = createProjectRenameButton(project);
        JButton deleteButton = createProjectDeleteButton(project);
        JPanel rightBox = (JPanel) row.getClientProperty("rightBox");
        if (rightBox != null) {
            rightBox.add(renameButton);
            rightBox.add(deleteButton);
        }

        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isDescendingFrom(e.getComponent(), deleteButton)
                        || SwingUtilities.isDescendingFrom(e.getComponent(), renameButton)) {
                    return;
                }
                openEvaluationResults(project);
            }
        });

        return row;
    }

    private JButton createProjectRenameButton(Project project) {
        JButton button = new JButton("Rename");
        button.setFont(new Font("SansSerif", Font.BOLD, 11));
        button.setForeground(new Color(19, 99, 128));
        button.setBackground(new Color(236, 246, 252));
        button.setBorder(new LineBorder(new Color(186, 213, 234), 1, true));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(82, 30));

        button.addActionListener(e -> renameProject(project));
        return button;
    }

    private void renameProject(Project project) {
        String oldName = project.getName();
        String input = (String) JOptionPane.showInputDialog(
                this,
                "Enter a new name for the project:",
                "Rename Project",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                oldName
        );
        if (input == null) return;
        String newName = input.trim();
        if (newName.isEmpty() || newName.equals(oldName)) return;

        try {
            DatabaseManager db = new DatabaseManager();
            try {
                db.connect();
                db.initSchema();

                long projectId = project.getId() > 0
                        ? project.getId()
                        : db.findProjectIdByName(oldName);
                if (projectId <= 0) {
                    JOptionPane.showMessageDialog(this,
                            "Could not locate project '" + oldName + "'.",
                            "Rename Failed",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (db.findProjectIdByName(newName) > 0) {
                    JOptionPane.showMessageDialog(this,
                            "A project named '" + newName + "' already exists.",
                            "Name Conflict",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                db.renameProject(projectId, newName);
            } finally {
                db.disconnect();
            }

            ProjectFilePaths paths = projectPathsByName.remove(oldName);
            if (paths != null) {
                projectPathsByName.put(newName, paths);
            }
            project.setName(newName);
            if (currentProject != null && currentProject.getName().equals(oldName)) {
                currentProject.setName(newName);
            }

            refreshSavedProjects();
            refreshDashboard();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Rename failed: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
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

        Icon projectIcon = UIManager.getIcon("Tree.leafIcon");

        JLabel icon = new JLabel(projectIcon, SwingConstants.CENTER);
        icon.setOpaque(true);
        icon.setBackground(new Color(232, 241, 246));
        icon.setPreferredSize(new Dimension(38, 38));

        if (projectIcon == null) {
            icon.setText("▤");
            icon.setForeground(new Color(19, 99, 128));
            icon.setFont(new Font("SansSerif", Font.BOLD, 16));
        }

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
        projectInfoCard.setMaximumSize(new Dimension(720, 285));
        projectInfoCard.setPreferredSize(new Dimension(720, 285));
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

        cmbComparator = new JComboBox<>(new String[]{
                Project.COMPARATOR_EXACT_MATCH,
                Project.COMPARATOR_WHITESPACE_INSENSITIVE
        });
        cmbComparator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        cmbComparator.setPreferredSize(new Dimension(0, 38));
        cmbComparator.setFont(FONT_BODY);
        cmbComparator.setBackground(Color.WHITE);

        projectInfoCard.add(createSectionTitle("▤  Project Information"));
        projectInfoCard.add(Box.createVerticalStrut(14));
        projectInfoCard.add(createLabel("Project Name"));
        projectInfoCard.add(Box.createVerticalStrut(6));
        projectInfoCard.add(txtProjectName);
        projectInfoCard.add(Box.createVerticalStrut(14));
        projectInfoCard.add(createLabel("Configuration"));
        projectInfoCard.add(Box.createVerticalStrut(6));
        projectInfoCard.add(cmbConfiguration);

        projectInfoCard.add(Box.createVerticalStrut(14));
        projectInfoCard.add(createLabel("Output Comparator"));
        projectInfoCard.add(Box.createVerticalStrut(6));
        projectInfoCard.add(cmbComparator);

        JPanel filesCard = createCardPanel();
        filesCard.setMaximumSize(new Dimension(720, Integer.MAX_VALUE));
        filesCard.setLayout(new BoxLayout(filesCard, BoxLayout.Y_AXIS));

        testCaseTableModel = new DefaultTableModel(
                new String[]{"Input File (path)", "Expected Output File (path)"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable testCaseTable = new JTable(testCaseTableModel);
        testCaseTable.setFont(FONT_BODY);
        testCaseTable.setRowHeight(28);
        testCaseTable.setShowGrid(true);
        testCaseTable.setGridColor(new Color(226, 232, 240));
        testCaseTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        testCaseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        testCaseTable.setBackground(Color.WHITE);
        testCaseTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        testCaseTable.getColumnModel().getColumn(1).setPreferredWidth(300);

        JScrollPane tableScroll = new JScrollPane(testCaseTable);
        tableScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        tableScroll.setPreferredSize(new Dimension(0, 180));
        tableScroll.setBorder(new LineBorder(new Color(226, 232, 240), 1));

        JPanel tcToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tcToolbar.setBackground(BG_CARD);
        tcToolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
        tcToolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JButton btnAddTC    = createSecondaryButton("+ Add");
        JButton btnRemoveTC = createSecondaryButton("− Remove");
        JButton btnMoveUp   = createSecondaryButton("↑");
        JButton btnMoveDown = createSecondaryButton("↓");

        for (JButton b : new JButton[]{btnAddTC, btnRemoveTC, btnMoveUp, btnMoveDown}) {
            b.setFont(new Font("SansSerif", Font.BOLD, 12));
            b.setPreferredSize(new Dimension(b == btnMoveUp || b == btnMoveDown ? 42 : 88, 30));
        }
        tcToolbar.add(btnAddTC);
        tcToolbar.add(btnRemoveTC);
        tcToolbar.add(Box.createHorizontalStrut(6));
        tcToolbar.add(btnMoveUp);
        tcToolbar.add(btnMoveDown);

        btnAddTC.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(".");
            fc.setDialogTitle("Select Input File (or Cancel for no-input test)");
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            String inputPath = "";
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                inputPath = fc.getSelectedFile().getAbsolutePath();
            }
            fc.setDialogTitle("Select Expected Output File");
            String expectedPath = "";
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                expectedPath = fc.getSelectedFile().getAbsolutePath();
            }
            testCaseTableModel.addRow(new Object[]{inputPath, expectedPath});
        });

        btnRemoveTC.addActionListener(e -> {
            int row = testCaseTable.getSelectedRow();
            if (row >= 0) testCaseTableModel.removeRow(row);
        });

        btnMoveUp.addActionListener(e -> {
            int row = testCaseTable.getSelectedRow();
            if (row > 0) {
                Object input    = testCaseTableModel.getValueAt(row, 0);
                Object expected = testCaseTableModel.getValueAt(row, 1);
                testCaseTableModel.removeRow(row);
                testCaseTableModel.insertRow(row - 1, new Object[]{input, expected});
                testCaseTable.setRowSelectionInterval(row - 1, row - 1);
            }
        });

        btnMoveDown.addActionListener(e -> {
            int row = testCaseTable.getSelectedRow();
            if (row >= 0 && row < testCaseTableModel.getRowCount() - 1) {
                Object input    = testCaseTableModel.getValueAt(row, 0);
                Object expected = testCaseTableModel.getValueAt(row, 1);
                testCaseTableModel.removeRow(row);
                testCaseTableModel.insertRow(row + 1, new Object[]{input, expected});
                testCaseTable.setRowSelectionInterval(row + 1, row + 1);
            }
        });

        JLabel tcHint = new JLabel("Each row is one test case. Click + Add to browse for files.");
        tcHint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        tcHint.setForeground(TEXT_SECONDARY);
        tcHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        filesCard.add(createSectionTitle("▣  Test Cases"));
        filesCard.add(Box.createVerticalStrut(8));
        filesCard.add(tcHint);
        filesCard.add(Box.createVerticalStrut(10));
        filesCard.add(tcToolbar);
        filesCard.add(Box.createVerticalStrut(8));
        filesCard.add(tableScroll);
        filesCard.add(Box.createVerticalStrut(16));
        filesCard.add(createSectionTitle("   Student Submissions"));
        filesCard.add(Box.createVerticalStrut(10));

        txtSubmissionsFolder = createTextField("Select folder containing ZIP files...");
        File defaultFolder = new File("test-submissions");
        if (defaultFolder.exists()) {
            txtSubmissionsFolder.setText(defaultFolder.getAbsolutePath());
            txtSubmissionsFolder.setForeground(TEXT_PRIMARY);
        }

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
            String submissionsPath = getRealText(txtSubmissionsFolder, "Select folder containing ZIP files...");

            if (projectName.isEmpty() || selected == null) {
                JOptionPane.showMessageDialog(this, "Please fill project name and configuration.");
                return;
            }

            List<TestCase> testCases = buildTestCasesFromForm();
            if (testCases.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please add at least one test case.");
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
                db.saveProject(projectName, projectConfig, testCases);
                rememberProjectPaths(projectName, "", "", submissionsPath);
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
        List<TestCase> testCases = new ArrayList<>();

        for (int i = 0; i < testCaseTableModel.getRowCount(); i++) {
            String inputPath    = (String) testCaseTableModel.getValueAt(i, 0);
            String expectedPath = (String) testCaseTableModel.getValueAt(i, 1);

            String input = (inputPath == null || inputPath.isBlank())
                    ? ""
                    : java.nio.file.Files.readString(java.nio.file.Paths.get(inputPath)).trim();

            String expected = (expectedPath == null || expectedPath.isBlank())
                    ? ""
                    : java.nio.file.Files.readString(java.nio.file.Paths.get(expectedPath)).trim();

            testCases.add(new TestCase(input, expected));
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

    private List<Configuration> loadConfigurationsFromDb() {
        DatabaseManager db = new DatabaseManager();
        try {
            db.connect();
            db.initSchema();
            List<Configuration> filtered = new ArrayList<>();
            for (Configuration c : db.getAllConfigurations()) {
                if (c.getName() != null && !"AUTO".equalsIgnoreCase(c.getName())) {
                    filtered.add(c);
                }
            }
            return filtered;
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ArrayList<>();
        } finally {
            db.disconnect();
        }
    }

    private void persistConfigurationToDb(Configuration config) {
        DatabaseManager db = new DatabaseManager();
        try {
            db.connect();
            db.initSchema();
            db.upsertConfiguration(config);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            db.disconnect();
        }
    }

    private boolean deleteConfigurationFromDb(String name) {
        DatabaseManager db = new DatabaseManager();
        try {
            db.connect();
            db.initSchema();
            db.deleteConfigurationByName(name);
            return true;
        } catch (java.sql.SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("FOREIGN KEY")) {
                JOptionPane.showMessageDialog(this,
                        "Cannot delete '" + name + "' because it is used by a saved project.\n" +
                                "Delete the project first, or change its configuration to a different one.",
                        "Configuration In Use",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                ex.printStackTrace();
            }
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        } finally {
            db.disconnect();
        }
    }

    private void runProject() {
        final String projectName = getRealText(txtProjectName, "e.g., Data Structures - Assignment 1");
        final String submissionsPath = getRealText(txtSubmissionsFolder, "Select folder containing ZIP files...");
        final String selected = (String) cmbConfiguration.getSelectedItem();
        final String comparatorType = cmbComparator == null || cmbComparator.getSelectedItem() == null
                ? Project.COMPARATOR_EXACT_MATCH
                : cmbComparator.getSelectedItem().toString();

        if (projectName.isEmpty() || submissionsPath.isEmpty() || selected == null) {
            JOptionPane.showMessageDialog(this, "Please fill project name and submissions folder.");
            return;
        }

        final Configuration selectedConfig = getSelectedConfiguration(selected);
        final List<TestCase> testCases;
        try {
            testCases = buildTestCasesFromForm();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to read test case files: " + ex.getMessage());
            return;
        }

        if (testCases.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add at least one test case before running.");
            return;
        }

        runEvaluationAsync(
                "Evaluating submissions, please wait...",
                () -> {
                    ProjectRunnerService runner = new ProjectRunnerService();
                    return runner.runProject(
                            projectName,
                            new File(submissionsPath),
                            testCases,
                            selectedConfig,
                            (current, total, studentId) -> {
                                if (evaluationProgressUpdater != null) {
                                    evaluationProgressUpdater.accept(
                                            "Evaluating " + current + " / " + total + " - Student: " + studentId
                                    );
                                }
                            },
                            comparatorType
                    );
                },
                project -> {
                    try {
                        refreshSavedProjects();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    currentProject = project;
                    currentProject.setComparatorType(comparatorType);
                    rememberProjectPaths(projectName, "", "", submissionsPath);

                    JOptionPane.showMessageDialog(this, "Project evaluated successfully.");
                    refreshDashboard();
                },
                "Evaluation failed"
        );
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

        final File submissionsDir = chooser.getSelectedFile();
        final String projectName = currentProject.getName();
        final List<TestCase> testCases = currentProject.getTestCases();
        final Configuration projectConfig = currentProject.getConfiguration();
        final String comparatorType = currentProject.getComparatorType();

        runEvaluationAsync(
                "Re-running '" + projectName + "', please wait...",
                () -> {
                    ProjectRunnerService runner = new ProjectRunnerService();
                    runner.runProject(
                            projectName,
                            submissionsDir,
                            testCases,
                            projectConfig,
                            null,
                            comparatorType
                    );

                    DatabaseManager db = new DatabaseManager();
                    db.connect();
                    try {
                        return db.getProjects();
                    } finally {
                        db.disconnect();
                    }
                },
                refreshed -> {
                    recentProjects.clear();
                    recentProjects.addAll(refreshed);

                    Project updated = null;
                    for (Project p : refreshed) {
                        if (p.getName().equals(projectName)) {
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
                },
                "Re-run failed"
        );
    }

    private <T> void runEvaluationAsync(String message,
                                        java.util.concurrent.Callable<T> task,
                                        java.util.function.Consumer<T> onSuccess,
                                        String errorTitle) {
        JDialog progress = new JDialog(this, "Please wait", true);
        progress.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progress.setLayout(new BorderLayout(0, 12));

        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(FONT_BODY);
        label.setBorder(new EmptyBorder(20, 24, 6, 24));

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setBorder(new EmptyBorder(0, 24, 8, 24));

        JButton cancelButton = createSecondaryButton("Cancel");

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(BG_CARD);
        bottomPanel.setBorder(new EmptyBorder(0, 24, 14, 24));
        bottomPanel.add(cancelButton);

        progress.add(label, BorderLayout.NORTH);
        progress.add(bar, BorderLayout.CENTER);
        progress.add(bottomPanel, BorderLayout.SOUTH);
        progress.setSize(440, 165);
        progress.setLocationRelativeTo(this);

        javax.swing.SwingWorker<T, Void> worker = new javax.swing.SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                evaluationProgressUpdater = null;
                progress.dispose();

                if (isCancelled()) {
                    JOptionPane.showMessageDialog(
                            IAEGui.this,
                            "Evaluation was cancelled.",
                            "Cancelled",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    return;
                }

                try {
                    T result = get();
                    onSuccess.accept(result);
                } catch (java.util.concurrent.CancellationException ex) {
                    JOptionPane.showMessageDialog(
                            IAEGui.this,
                            "Evaluation was cancelled.",
                            "Cancelled",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    cause.printStackTrace();
                    JOptionPane.showMessageDialog(
                            IAEGui.this,
                            errorTitle + ": " + cause.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(
                            IAEGui.this,
                            errorTitle + ": " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        cancelButton.addActionListener(e -> {
            cancelButton.setEnabled(false);
            label.setText("Cancelling evaluation...");
            worker.cancel(true);
        });
        evaluationProgressUpdater = text ->
                SwingUtilities.invokeLater(() -> label.setText(text));
        worker.execute();
        progress.setVisible(true);
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
        testCaseTableModel.setRowCount(0);
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
                        for (Configuration imported : importedConfigs) {
                            persistConfigurationToDb(imported);
                        }
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
            if (allConfigs == null || allConfigs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nothing to export.");
                return;
            }

            // 1. Open up a modern modal window to select which configurations to extract
            JDialog exportDialog = new JDialog(this, "Select configurations to export", true);
            exportDialog.setLayout(new BorderLayout(10, 10));
            exportDialog.setSize(400, 300);
            exportDialog.setLocationRelativeTo(this);

            // List selection subpanel
            JPanel listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            listPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Dynamic checkboxes referencing back to our configs map list
            List<JCheckBox> checkBoxes = new ArrayList<>();
            for (Configuration config : allConfigs) {
                JCheckBox checkBox = new JCheckBox(config.getName() + " (" + config.getLanguage() + ")");
                checkBox.setSelected(true); // Pre-check all by default
                listPanel.add(checkBox);
                checkBoxes.add(checkBox);
            }

            JScrollPane scrollPane = new JScrollPane(listPanel);
            exportDialog.add(scrollPane, BorderLayout.CENTER);

            // Dialog Confirmation Buttons
            JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnConfirm = new JButton("Export Selected");
            JButton btnCancel = new JButton("Cancel");

            final boolean[] confirmed = { false };

            btnConfirm.addActionListener(evt -> {
                confirmed[0] = true;
                exportDialog.dispose();
            });
            btnCancel.addActionListener(evt -> exportDialog.dispose());

            actionButtonPanel.add(btnConfirm);
            actionButtonPanel.add(btnCancel);
            exportDialog.add(actionButtonPanel, BorderLayout.SOUTH);

            // Render configuration list modal
            exportDialog.setVisible(true);

            // 2. Process selection logic upon direct user confirmation
            if (confirmed[0]) {
                List<Configuration> selectedConfigs = new ArrayList<>();
                for (int i = 0; i < checkBoxes.size(); i++) {
                    if (checkBoxes.get(i).isSelected()) {
                        selectedConfigs.add(allConfigs.get(i));
                    }
                }

                if (selectedConfigs.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No configurations were selected for export.", "Export Cancelled", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // 3. Fall back to your native file selection pipeline with dynamic date filename
                String dateSuffix = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyy"));
                String defaultFilename = "iae-configs-" + dateSuffix + ".json";

                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Export Configurations");
                fileChooser.setSelectedFile(new File(defaultFilename));
                fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON Files (*.json)", "json"));

                if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File saveFile = fileChooser.getSelectedFile();

                    // Auto-append .json extension if absent
                    if (!saveFile.getName().toLowerCase().endsWith(".json")) {
                        saveFile = new File(saveFile.getAbsolutePath() + ".json");
                    }

                    try {
                        // Pass filtered subset directly into your persistent storage engine framework
                        configStore.saveTo(saveFile, selectedConfigs);
                        JOptionPane.showMessageDialog(this, "Configurations exported successfully to:\n" + saveFile.getAbsolutePath());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
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

    private void handleExportConfigurations(List<Configuration> configurations) {
        if (configurations == null || configurations.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No configurations available to export.", "Export Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // 1. Create the modal selection window
        JDialog exportDialog = new JDialog(this, "Select configurations to export", true);
        exportDialog.setLayout(new BorderLayout(10, 10));
        exportDialog.setSize(400, 300);
        exportDialog.setLocationRelativeTo(this);

        // Panel to hold the list of checkboxes
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Maintain a map or parallel list linking checkboxes to configurations
        List<JCheckBox> checkBoxes = new ArrayList<>();
        for (Configuration config : configurations) {
            JCheckBox checkBox = new JCheckBox(config.getName() + " (" + config.getLanguage() + ")");
            checkBox.setSelected(true); // Default to selected
            listPanel.add(checkBox);
            checkBoxes.add(checkBox);
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        exportDialog.add(scrollPane, BorderLayout.CENTER);

        // 2. Control Buttons Panel
        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnConfirm = new JButton("Export Selected");
        JButton btnCancel = new JButton("Cancel");

        // Flag to track confirmation state
        final boolean[] confirmed = { false };

        btnConfirm.addActionListener(e -> {
            confirmed[0] = true;
            exportDialog.dispose();
        });

        btnCancel.addActionListener(e -> exportDialog.dispose());

        actionButtonPanel.add(btnConfirm);
        actionButtonPanel.add(btnCancel);
        exportDialog.add(actionButtonPanel, BorderLayout.SOUTH);

        // Display the configuration selection dialog
        exportDialog.setVisible(true);

        // 3. Process the selections if the user clicked "Export Selected"
        if (confirmed[0]) {
            List<Configuration> selectedConfigs = new ArrayList<>();
            for (int i = 0; i < checkBoxes.size(); i++) {
                if (checkBoxes.get(i).isSelected()) {
                    selectedConfigs.add(configurations.get(i));
                }
            }

            if (selectedConfigs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No configurations were selected for export.", "Export Cancelled", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 4. File Chooser to select destination path with dynamic date filename
            String dateSuffix = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            String defaultFilename = "iae-configs-" + dateSuffix + ".json";

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Configurations File");
            fileChooser.setSelectedFile(new File(defaultFilename));
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON Files (*.json)", "json"));

            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();

                // Ensure proper file extension
                if (!fileToSave.getName().toLowerCase().endsWith(".json")) {
                    fileToSave = new File(fileToSave.getAbsolutePath() + ".json");
                }

                try {
                    // Formats components explicitly using the JSON schema provided by ConfigStore and JsonUtil
                    List<String> jsonObjects = new ArrayList<>();
                    for (Configuration c : selectedConfigs) {
                        jsonObjects.add(c.toJson());
                    }
                    String content = JsonUtil.encodeArray(jsonObjects);

                    // Write the raw content byte stream to file safely
                    java.nio.file.Files.write(fileToSave.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));

                    JOptionPane.showMessageDialog(this, "Configurations successfully exported to:\n" + fileToSave.getAbsolutePath(), "Export Successful", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Failed to save configurations:\n" + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
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

            panel.btnCopy.addActionListener(e -> {
                stopCellEditing();
                if (currentConfig != null) {
                    // Generate a unique duplicate name
                    String duplicateName = generateDuplicateName(currentConfig.getName());

                    // Create the new cloned Configuration object
                    Configuration duplicatedConfig = new Configuration(
                            duplicateName,
                            currentConfig.getLanguage(),
                            currentConfig.getCompileCommand(),
                            currentConfig.getRunCommand(),
                            currentConfig.getSourceExtension(),
                            currentConfig.getEntryPointPattern()
                    );

                    // Update runtime list, save to database, and refresh the UI layout
                    allConfigs.add(duplicatedConfig);
                    persistConfigurationToDb(duplicatedConfig);
                    refreshConfigPage();
                }
            });

            panel.btnDelete.addActionListener(e -> {
                stopCellEditing();
                if (currentConfig != null) {
                    int confirm = JOptionPane.showConfirmDialog(panel,
                            "Delete configuration: " + currentConfig.getName() + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        if (deleteConfigurationFromDb(currentConfig.getName())) {
                            allConfigs.remove(currentConfig);
                            refreshConfigPage();
                        }
                    }
                }
            });
        }

        /**
         * Loops through existing configurations to find a unique "Copy" name.
         * Prevents duplicate keys if the user clicks copy multiple times.
         */
        private String generateDuplicateName(String baseName) {
            String targetName = baseName + " (Copy)";
            int counter = 2;

            while (configExists(targetName)) {
                targetName = baseName + " (Copy " + counter + ")";
                counter++;
            }
            return targetName;
        }

        private boolean configExists(String name) {
            for (Configuration config : allConfigs) {
                if (config.getName().equalsIgnoreCase(name)) {
                    return true;
                }
            }
            return false;
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
        public final JButton btnCopy = new JButton("📋");
        public final JButton btnDelete = new JButton("🗑");

        public ActionButtonsPanel() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 8, 20));
            setBackground(Color.WHITE);
            styleActionButton(btnEdit, TEXT_PRIMARY);
            styleActionButton(btnCopy, new Color(16, 185, 129)); // Clean Emerald Green for copy action
            styleActionButton(btnDelete, new Color(220, 38, 38));
            add(btnEdit);
            add(btnCopy);
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

            if (config != null) {
                allConfigs.remove(config);
                if (!config.getName().equals(newConfig.getName())) {
                    deleteConfigurationFromDb(config.getName());
                }
            }
            allConfigs.add(newConfig);
            persistConfigurationToDb(newConfig);
            dialog.dispose();
            refreshConfigPage();
        });

        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setBorder(null);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        formScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        formScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        dialog.add(formScroll, BorderLayout.CENTER);
        dialog.add(btnSave, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setSize(new Dimension(520, Math.min(dialog.getHeight() + 40, 520)));
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private void refreshConfigPage() {
        allConfigs = loadConfigurationsFromDb();

        mainContentPanel.removeAll();
        addPages();

        cardLayout.show(mainContentPanel, "configurations");
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
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

        Object previousSelection = cmbConfiguration.getSelectedItem();

        cmbConfiguration.removeAllItems();
        cmbConfiguration.addItem("AUTO");

        for (Configuration config : allConfigs) {
            cmbConfiguration.addItem(config.getName());
        }

        if (previousSelection != null) {
            cmbConfiguration.setSelectedItem(previousSelection);
        }

        if (cmbConfiguration.getSelectedItem() == null) {
            cmbConfiguration.setSelectedItem("AUTO");
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

    private void openProjectDetails(Project project) {
        this.currentProject = project;

        mainContentPanel.removeAll();
        addPages();

        if (topBarTitle != null) {
            topBarTitle.setText("   Project Details");
        }

        cardLayout.show(mainContentPanel, "projectDetails");
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }
    private JPanel createProjectDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_CANVAS);
        panel.setBorder(new EmptyBorder(30, 185, 35, 60));

        if (currentProject == null) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setBackground(BG_CANVAS);
            emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));

            JLabel title = new JLabel("Project Details");
            title.setFont(FONT_HEADER);
            title.setForeground(TEXT_PRIMARY);

            JLabel subtitle = new JLabel("No project selected.");
            subtitle.setFont(FONT_BODY);
            subtitle.setForeground(TEXT_SECONDARY);
            subtitle.setBorder(new EmptyBorder(8, 0, 0, 0));

            emptyPanel.add(title);
            emptyPanel.add(subtitle);

            panel.add(emptyPanel, BorderLayout.NORTH);
            return panel;
        }

        int[] stats = calculateProjectStats(currentProject);
        int total = stats[0];
        int passed = stats[1];
        int failed = stats[2];
        int passRate = total == 0 ? 0 : (int) Math.round((passed * 100.0) / total);

        JPanel content = new JPanel();
        content.setBackground(BG_CANVAS);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton back = createTextButton("←  Back to Evaluation Results");
        back.addActionListener(e -> openEvaluationResults(currentProject));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setBackground(BG_CANVAS);
        headerRow.setMaximumSize(new Dimension(950, 95));
        headerRow.setPreferredSize(new Dimension(950, 95));
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titleBox = new JPanel();
        titleBox.setBackground(BG_CANVAS);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(currentProject.getName());
        title.setFont(FONT_HEADER);
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel projectId = new JLabel("Project ID: " + (currentProject.getId() > 0 ? currentProject.getId() : "N/A"));
        projectId.setFont(FONT_BODY);
        projectId.setForeground(TEXT_SECONDARY);
        projectId.setBorder(new EmptyBorder(8, 0, 0, 0));
        projectId.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBox.add(back);
        titleBox.add(Box.createVerticalStrut(18));
        titleBox.add(title);
        titleBox.add(projectId);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(BG_CANVAS);
        actions.setBorder(new EmptyBorder(42, 0, 0, 0));

        JButton btnViewResults = createToolbarButton("results", "View Results", false);
        JButton btnRunAgain = createOrangeButton("▷  Run Again");

        btnViewResults.setPreferredSize(new Dimension(160, 40));
        btnRunAgain.setPreferredSize(new Dimension(130, 40));

        btnViewResults.addActionListener(e -> openEvaluationResults(currentProject));
        btnRunAgain.addActionListener(e -> rerunCurrentProject());

        actions.add(btnViewResults);
        actions.add(btnRunAgain);

        headerRow.add(titleBox, BorderLayout.WEST);
        headerRow.add(actions, BorderLayout.EAST);

        JPanel topCards = new JPanel(new GridLayout(1, 2, 18, 0));
        topCards.setBackground(BG_CANVAS);
        topCards.setMaximumSize(new Dimension(950, 210));
        topCards.setPreferredSize(new Dimension(950, 210));
        topCards.setAlignmentX(Component.LEFT_ALIGNMENT);

        topCards.add(createProjectConfigurationDetailsCard(currentProject));
        topCards.add(createProjectResultsSummaryCard(total, passed, failed, passRate));

        JPanel filesCard = createProjectFilesDetailsCard(currentProject);
        filesCard.setMaximumSize(new Dimension(950, 300));
        filesCard.setPreferredSize(new Dimension(950, 300));
        filesCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel infoBox = createProjectInfoBox(currentProject, total);
        infoBox.setMaximumSize(new Dimension(950, 78));
        infoBox.setPreferredSize(new Dimension(950, 78));
        infoBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(headerRow);
        content.add(Box.createVerticalStrut(24));
        content.add(topCards);
        content.add(Box.createVerticalStrut(22));
        content.add(filesCard);
        content.add(Box.createVerticalStrut(22));
        content.add(infoBox);

        panel.add(content, BorderLayout.NORTH);
        return panel;
    }
    private int[] calculateProjectStats(Project project) {
        int total = project.getSubmissions() == null ? 0 : project.getSubmissions().size();
        int passed = 0;
        int failed = 0;

        if (project.getSubmissions() != null) {
            for (StudentZipSubmission submission : project.getSubmissions()) {
                if (submission.getResult() != null &&
                        submission.getResult().getStatus() == Status.SUCCESS) {
                    passed++;
                } else {
                    failed++;
                }
            }
        }

        return new int[]{total, passed, failed};
    }

    private JPanel createProjectConfigurationDetailsCard(Project project) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(new LineBorder(BORDER_COLOR, 1, true));

        JLabel title = new JLabel("⚙  Configuration");
        title.setFont(FONT_SUBHEADER);
        title.setForeground(TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(18, 20, 12, 20));

        JPanel body = new JPanel();
        body.setBackground(BG_CARD);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(10, 20, 18, 20));

        Configuration config = project.getConfiguration();

        String configName = config == null ? "AUTO" : safeText(config.getName(), "AUTO");
        String createdDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        body.add(createDetailsTextRow("Language Configuration", configName));
        body.add(Box.createVerticalStrut(18));
        body.add(createDetailsTextRow("Created Date", createdDate));

        card.add(title, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        return card;
    }

    private JPanel createProjectResultsSummaryCard(int total, int passed, int failed, int passRate) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(new LineBorder(BORDER_COLOR, 1, true));

        JLabel title = new JLabel("Results Summary");
        title.setIcon(createUiIcon("results", new Color(19, 99, 128), 16));
        title.setIconTextGap(8);
        title.setFont(FONT_SUBHEADER);
        title.setForeground(TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(18, 20, 12, 20));

        JPanel body = new JPanel();
        body.setBackground(BG_CARD);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(8, 20, 18, 20));

        body.add(createSummaryRow("Total Students", String.valueOf(total), TEXT_PRIMARY));
        body.add(Box.createVerticalStrut(14));
        body.add(createSummaryRow("Passed", String.valueOf(passed), new Color(5, 150, 105)));
        body.add(Box.createVerticalStrut(14));
        body.add(createSummaryRow("Failed", String.valueOf(failed), new Color(220, 38, 38)));
        body.add(Box.createVerticalStrut(14));

        JSeparator separator = new JSeparator();
        separator.setForeground(BORDER_COLOR);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        body.add(separator);
        body.add(Box.createVerticalStrut(14));

        body.add(createSummaryRow("Pass Rate", passRate + "%", ACCENT_ORANGE));

        card.add(title, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        return card;
    }

    private JPanel createProjectFilesDetailsCard(Project project) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(new LineBorder(BORDER_COLOR, 1, true));

        Icon folderIcon = UIManager.getIcon("FileView.directoryIcon");

        JLabel title = new JLabel("Files and Folders");
        title.setIcon(folderIcon);
        title.setIconTextGap(8);
        title.setFont(FONT_SUBHEADER);
        title.setForeground(TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(18, 20, 14, 20));

        JPanel body = new JPanel();
        body.setBackground(BG_CARD);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(8, 20, 18, 20));

        ProjectFilePaths paths = getProjectPaths(project);

        String inputPath = showPathOrFallback(paths.inputFilePath, "No input file path saved");
        String expectedPath = showPathOrFallback(paths.expectedOutputFilePath, "No expected output file path saved");
        String submissionsPath = showPathOrFallback(paths.submissionsFolderPath, "No submissions folder path saved");

        body.add(createPathDetailsRow("Input File", inputPath, "View", paths.inputFilePath));
        body.add(Box.createVerticalStrut(14));

        body.add(createPathDetailsRow("Expected Output File", expectedPath, "View", paths.expectedOutputFilePath));
        body.add(Box.createVerticalStrut(14));

        body.add(createPathDetailsRow("Student Submissions Folder", submissionsPath, "Open", paths.submissionsFolderPath));

        card.add(title, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        return card;
    }

    private JPanel createProjectInfoBox(Project project, int totalStudents) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(new Color(255, 251, 247));
        box.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(253, 186, 116), 1, true),
                new EmptyBorder(14, 18, 14, 18)
        ));

        Configuration config = project.getConfiguration();
        String language = config == null ? "AUTO" : safeText(config.getLanguage(), "AUTO");

        JLabel text = new JLabel(
                "<html><b style='color:#EA7317;'>▤  Project Information</b><br>" +
                        "<span style='color:#64748B;'>This project evaluates " +
                        totalStudents + " student submissions using the " +
                        language + " language configuration.</span></html>"
        );
        text.setFont(FONT_BODY);

        box.add(text, BorderLayout.CENTER);
        return box;
    }

    private JPanel createDetailsTextRow(String labelText, String valueText) {
        JPanel row = new JPanel();
        row.setBackground(BG_CARD);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        label.setForeground(TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel value = new JLabel(valueText);
        value.setFont(new Font("SansSerif", Font.BOLD, 13));
        value.setForeground(TEXT_PRIMARY);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.add(label);
        row.add(Box.createVerticalStrut(4));
        row.add(value);

        return row;
    }

    private JPanel createSummaryRow(String labelText, String valueText, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BG_CARD);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel label = new JLabel(labelText);
        label.setFont(FONT_BODY);
        label.setForeground(TEXT_SECONDARY);

        JLabel value = new JLabel(valueText);
        value.setFont(new Font("SansSerif", Font.BOLD, 15));
        value.setForeground(valueColor);
        value.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(label, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);

        return row;
    }

    private JPanel createPathDetailsRow(String labelText, String valueText, String actionText, String pathToOpen) {
        JPanel wrapper = new JPanel();
        wrapper.setBackground(BG_CARD);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));

        JPanel pathBox = new JPanel(new BorderLayout(12, 0));
        pathBox.setBackground(new Color(248, 250, 252));
        pathBox.setBorder(new EmptyBorder(9, 12, 9, 12));
        pathBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        pathBox.setPreferredSize(new Dimension(0, 38));
        pathBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel value = new JLabel(valueText);
        value.setFont(new Font("Monospaced", Font.PLAIN, 12));
        value.setForeground(TEXT_PRIMARY);
        value.setToolTipText(valueText);

        JLabel action = new JLabel(actionText);
        action.setFont(new Font("SansSerif", Font.PLAIN, 11));
        action.setForeground(new Color(19, 99, 128));
        action.setHorizontalAlignment(SwingConstants.RIGHT);
        action.setCursor(new Cursor(Cursor.HAND_CURSOR));

        MouseAdapter openListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openFileOrFolder(pathToOpen);
            }
        };

        action.addMouseListener(openListener);
        pathBox.addMouseListener(openListener);

        pathBox.add(value, BorderLayout.CENTER);
        pathBox.add(action, BorderLayout.EAST);

        wrapper.add(label);
        wrapper.add(Box.createVerticalStrut(6));
        wrapper.add(pathBox);

        return wrapper;
    }

    private String getProjectTestCasesText(Project project) {
        if (project.getTestCases() == null || project.getTestCases().isEmpty()) {
            return "No test cases";
        }

        return project.getTestCases().size() + " test case(s)";
    }

    private String getProjectFirstZipPath(Project project) {
        if (project.getSubmissions() == null || project.getSubmissions().isEmpty()) {
            return "No ZIP file found";
        }

        for (StudentZipSubmission submission : project.getSubmissions()) {
            if (submission.getZipFile() != null) {
                return submission.getZipFile().getAbsolutePath();
            }
        }

        return "No ZIP file found";
    }

    private String getProjectSubmissionsFolder(Project project) {
        if (project.getSubmissions() == null || project.getSubmissions().isEmpty()) {
            return "No submissions folder found";
        }

        for (StudentZipSubmission submission : project.getSubmissions()) {
            if (submission.getZipFile() != null && submission.getZipFile().getParentFile() != null) {
                return submission.getZipFile().getParentFile().getAbsolutePath();
            }
        }

        return "No submissions folder found";
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value;
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

        JButton btnProjectDetails = createInfoToolbarButton("Project Details");
        JButton btnRerun = createToolbarButton("refresh", "Re-run", false);
        JButton btnExport = createToolbarButton("download", "Export Results", true);

        btnProjectDetails.setPreferredSize(new Dimension(170, 46));
        btnRerun.setPreferredSize(new Dimension(125, 46));
        btnExport.setPreferredSize(new Dimension(170, 46));

        btnProjectDetails.addActionListener(e -> openProjectDetails(currentProject));
        btnRerun.addActionListener(e -> rerunCurrentProject());
        btnExport.addActionListener(e -> exportCurrentProjectResults());

        topActions.add(btnProjectDetails);
        topActions.add(btnRerun);
        topActions.add(btnExport);

        headerRow.add(titleBox, BorderLayout.WEST);
        headerRow.add(topActions, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout(24, 0));
        content.setBackground(BG_CANVAS);

        JPanel tableCard = createModernStudentResultsTable();
        tableCard.setPreferredSize(new Dimension(950, 650));

        JPanel rightArea = new JPanel();
        rightArea.setBackground(BG_CANVAS);
        rightArea.setLayout(new BoxLayout(rightArea, BoxLayout.Y_AXIS));
        rightArea.setPreferredSize(new Dimension(340, 650));

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
        rightArea.add(Box.createVerticalGlue());

        JPanel returnDashboardBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        returnDashboardBox.setBackground(BG_CANVAS);
        returnDashboardBox.setMaximumSize(new Dimension(340, 44));
        returnDashboardBox.setPreferredSize(new Dimension(340, 44));

        JButton btnReturnDashboard = createSecondaryButton("←  Return Dashboard");
        btnReturnDashboard.setPreferredSize(new Dimension(170, 40));
        btnReturnDashboard.addActionListener(e -> showPage("dashboard", btnDashboard));

        returnDashboardBox.add(btnReturnDashboard);
        rightArea.add(returnDashboardBox);

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

    private void exportCurrentProjectResults() {
        if (currentProject == null || currentProject.getSubmissions() == null || currentProject.getSubmissions().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nothing to export.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Evaluation Results");

        FileNameExtensionFilter csvFilter = new FileNameExtensionFilter("CSV Files (*.csv)", "csv");
        FileNameExtensionFilter jsonFilter = new FileNameExtensionFilter("JSON Files (*.json)", "json");
        fileChooser.addChoosableFileFilter(csvFilter);
        fileChooser.addChoosableFileFilter(jsonFilter);
        fileChooser.setFileFilter(csvFilter);
        fileChooser.setSelectedFile(new File(sanitizeFileName(currentProject.getName()) + "_result"));

        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File saveFile = fileChooser.getSelectedFile();
        String format = getExportFormat(saveFile, fileChooser.getFileFilter() == jsonFilter);
        saveFile = ensureExtension(saveFile, format);

        if (saveFile.exists()) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "The file already exists. Overwrite it?",
                    "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION
            );
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            String content = "json".equals(format)
                    ? ResultsExporter.toJson(currentProject)
                    : ResultsExporter.toCsv(currentProject);
            Files.writeString(saveFile.toPath(), content, StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Results exported successfully to:\n" + saveFile.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getExportFormat(File file, boolean jsonFilterSelected) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".json")) return "json";
        if (name.endsWith(".csv")) return "csv";
        return jsonFilterSelected ? "json" : "csv";
    }

    private File ensureExtension(File file, String extension) {
        String name = file.getName().toLowerCase();
        if (name.endsWith("." + extension)) return file;
        File parent = file.getParentFile();
        return parent == null
                ? new File(file.getName() + "." + extension)
                : new File(parent, file.getName() + "." + extension);
    }

    private String sanitizeFileName(String value) {
        if (value == null || value.isBlank()) return "evaluation";
        String sanitized = value.replaceAll("[\\\\/:*?\"<>|]+", "_").trim();
        return sanitized.isEmpty() ? "evaluation" : sanitized;
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

        JPanel outputGrid = createDiffVisualizationCard();
        outputGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

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

    private JPanel createDiffVisualizationCard() {
        JPanel card = createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = createSectionTitle("Expected vs Actual Output Diff");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(12));

        if (currentSubmission == null) {
            JLabel empty = new JLabel("No student submission selected.");
            empty.setFont(FONT_BODY);
            empty.setForeground(TEXT_SECONDARY);
            card.add(empty);
            card.setMaximumSize(new Dimension(1050, 120));
            return card;
        }

        if (currentProject == null || currentProject.getTestCases() == null || currentProject.getTestCases().isEmpty()) {
            JLabel empty = new JLabel("No test cases found.");
            empty.setFont(FONT_BODY);
            empty.setForeground(TEXT_SECONDARY);
            card.add(empty);
            card.setMaximumSize(new Dimension(1050, 120));
            return card;
        }

        List<TestCase> testCases = currentProject.getTestCases();
        List<PerTestResult> perTestResults = currentSubmission.getPerTestResults();

        int total = Math.max(testCases.size(), perTestResults.size());
        int totalRows = 0;

        for (int i = 0; i < total; i++) {
            TestCase testCase = i < testCases.size() ? testCases.get(i) : null;
            PerTestResult perTestResult = i < perTestResults.size() ? perTestResults.get(i) : null;

            String expected = testCase == null ? "" : testCase.getExpectedOutput();
            String actual = perTestResult == null ? "" : perTestResult.getActualOutput();
            Status rowStatus = perTestResult == null ? null : perTestResult.getStatus();

            List<LineDiff.Row> diffRows = LineDiff.diff(expected, actual);
            totalRows += diffRows.size();

            JPanel testPanel = createSingleTestDiffPanel(i + 1, rowStatus, diffRows);
            testPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            card.add(testPanel);

            if (i < total - 1) {
                card.add(Box.createVerticalStrut(14));
            }
        }

        int height = 70 + (total * 58) + (totalRows * 32);
        height = Math.max(150, height);
        height = Math.min(520, height);

        card.setPreferredSize(new Dimension(1050, height));
        card.setMaximumSize(new Dimension(1050, height));

        return card;
    }

    private JPanel createSingleTestDiffPanel(int testNumber, Status status, List<LineDiff.Row> diffRows) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(BG_CARD);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));

        String statusText = status == null ? "UNKNOWN" : status.name();

        JLabel header = new JLabel("Test Case " + testNumber + "  •  " + statusText);
        header.setFont(FONT_SUBHEADER);
        header.setForeground(TEXT_PRIMARY);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel columnHeader = new JPanel(new GridLayout(1, 2, 12, 0));
        columnHeader.setBackground(BG_CARD);
        columnHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        columnHeader.setPreferredSize(new Dimension(1000, 24));
        columnHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        columnHeader.add(createDiffHeaderLabel("Expected"));
        columnHeader.add(createDiffHeaderLabel("Actual"));

        JPanel rowsPanel = new JPanel();
        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
        rowsPanel.setBackground(BG_CARD);
        rowsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (LineDiff.Row row : diffRows) {
            rowsPanel.add(createDiffRow(row));
        }

        wrapper.add(header);
        wrapper.add(Box.createVerticalStrut(10));
        wrapper.add(columnHeader);
        wrapper.add(Box.createVerticalStrut(6));
        wrapper.add(rowsPanel);

        int rowHeight = 24;
        int panelHeight = 24 + 10 + 24 + 6 + (diffRows.size() * rowHeight) + 24;

        wrapper.setPreferredSize(new Dimension(1000, panelHeight));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, panelHeight));

        return wrapper;
    }

    private JLabel createDiffHeaderLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BODY);
        label.setForeground(TEXT_SECONDARY);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setBorder(new EmptyBorder(0, 10, 0, 10));
        return label;
    }

    private JPanel createDiffRow(LineDiff.Row row) {
        JPanel line = new JPanel(new GridLayout(1, 2, 12, 0));
        line.setBackground(getDiffColor(row.getType()));
        line.setPreferredSize(new Dimension(1000, 24));
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        line.setBorder(new EmptyBorder(0, 0, 0, 0));

        line.add(createDiffCell(row.getLineNumber(), row.getExpectedLine(), row.getType()));
        line.add(createDiffCell(row.getLineNumber(), row.getActualLine(), row.getType()));

        return line;
    }

    private JLabel createDiffCell(int lineNumber, String text, LineDiff.Type type) {
        String safeText = escapeHtml(text);

        if (safeText.isEmpty()) {
            safeText = "&nbsp;";
        }

        JLabel label = new JLabel(
                "<html><span style='color:#64748B;'>" + lineNumber + " | </span>" +
                        "<span style='font-family:monospace;'>" + safeText + "</span></html>"
        );

        label.setOpaque(true);
        label.setBackground(getDiffColor(type));
        label.setFont(new Font("Monospaced", Font.PLAIN, 12));
        label.setForeground(TEXT_PRIMARY);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setBorder(new EmptyBorder(2, 10, 2, 10));

        return label;
    }

    private Color getDiffColor(LineDiff.Type type) {
        if (type == LineDiff.Type.CHANGED) {
            return new Color(254, 249, 195);
        }

        if (type == LineDiff.Type.MISSING_ACTUAL) {
            return new Color(254, 226, 226);
        }

        if (type == LineDiff.Type.MISSING_EXPECTED) {
            return new Color(219, 234, 254);
        }

        return Color.WHITE;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
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

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BG_CARD);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        card.add(title, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
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
        area.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(30, 41, 59));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(30, 41, 59));
        wrapper.add(scrollPane, BorderLayout.CENTER);
        wrapper.setPreferredSize(new Dimension(1000, 220));

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

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(BG_CARD);

        JPanel statsStrip = createResultsStatsStrip(currentProject);
        statsStrip.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setBackground(BG_CARD);
        tableHeader.setBorder(new EmptyBorder(20, 22, 20, 22));
        tableHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Student Results");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT_PRIMARY);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filters.setBackground(BG_CARD);

        JComboBox<String> statusFilter = new JComboBox<>(new String[]{
                "All Statuses", "Passed", "Compile Errors", "Runtime Errors", "Wrong Output", "Extraction Errors", "Pending"
        });
        statusFilter.setFont(FONT_BODY);
        statusFilter.setPreferredSize(new Dimension(165, 40));
        statusFilter.setBackground(Color.WHITE);

        JTextField search = createTextField("Search student ID...");
        search.setPreferredSize(new Dimension(240, 40));
        search.setMaximumSize(new Dimension(240, 40));

        filters.add(statusFilter);
        filters.add(search);

        tableHeader.add(title, BorderLayout.WEST);
        tableHeader.add(filters, BorderLayout.EAST);

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

        JScrollPane tableHolder = new JScrollPane(table);
        tableHolder.setBackground(Color.WHITE);
        tableHolder.getViewport().setBackground(Color.WHITE);
        tableHolder.setBorder(null);
        tableHolder.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableHolder.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableHolder.getVerticalScrollBar().setUnitIncrement(16);

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
        page.setPreferredSize(new Dimension(74, 36));
        page.setEnabled(false);
        next.setPreferredSize(new Dimension(44, 36));

        pagination.add(prev);
        pagination.add(page);
        pagination.add(next);

        JLabel perPage = new JLabel(resultsPerPage + " per page");
        perPage.setFont(FONT_BODY);
        perPage.setForeground(TEXT_SECONDARY);
        perPage.setHorizontalAlignment(SwingConstants.RIGHT);
        perPage.setPreferredSize(new Dimension(125, 36));

        footer.add(showing, BorderLayout.WEST);
        footer.add(pagination, BorderLayout.CENTER);
        footer.add(perPage, BorderLayout.EAST);

        List<StudentZipSubmission> displayedSubmissions = new ArrayList<>();

        final Runnable[] refreshResultsTable = new Runnable[1];

        refreshResultsTable[0] = () -> {
            String query = getRealText(search, "Search student ID...").toLowerCase().trim();
            String selectedStatus = (String) statusFilter.getSelectedItem();

            List<StudentZipSubmission> filteredSubmissions = new ArrayList<>();

            for (StudentZipSubmission s : currentProject.getSubmissions()) {
                Status status = s.getResult() == null ? null : s.getResult().getStatus();

                boolean idMatches = query.isEmpty() || s.getStudentId().toLowerCase().contains(query);
                boolean statusMatches = matchesResultsStatusFilter(status, selectedStatus);

                if (idMatches && statusMatches) {
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

            page.setText(resultsPage + " / " + totalPages);
            prev.setEnabled(resultsPage > 1);
            next.setEnabled(resultsPage < totalPages);
            perPage.setText(resultsPerPage + " per page");
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

        statusFilter.addActionListener(e -> {
            resultsPage = 1;
            refreshResultsTable[0].run();
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

        topPanel.add(statsStrip);
        topPanel.add(tableHeader);

        tableCard.add(topPanel, BorderLayout.NORTH);
        tableCard.add(tableHolder, BorderLayout.CENTER);
        tableCard.add(footer, BorderLayout.SOUTH);

        return tableCard;
    }

    private JPanel createResultsStatsStrip(Project project) {
        int total = 0;
        int passed = 0;
        int compileErrors = 0;
        int runtimeErrors = 0;
        int wrongOutput = 0;

        if (project != null && project.getSubmissions() != null) {
            total = project.getSubmissions().size();
            for (StudentZipSubmission submission : project.getSubmissions()) {
                Status status = submission.getResult() == null ? null : submission.getResult().getStatus();
                if (status == Status.SUCCESS) passed++;
                else if (status == Status.COMPILE_ERROR) compileErrors++;
                else if (status == Status.RUNTIME_ERROR) runtimeErrors++;
                else if (status == Status.WRONG_OUTPUT) wrongOutput++;
            }
        }

        int passRate = total == 0 ? 0 : (int) Math.round((passed * 100.0) / total);

        JPanel strip = new JPanel(new GridLayout(1, 6, 10, 0));
        strip.setBackground(BG_CARD);
        strip.setBorder(new EmptyBorder(18, 22, 0, 22));
        strip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        strip.setPreferredSize(new Dimension(0, 74));

        strip.add(createCompactResultStat("Total", String.valueOf(total), new Color(37, 99, 235)));
        strip.add(createCompactResultStat("Passed", String.valueOf(passed), new Color(22, 163, 74)));
        strip.add(createCompactResultStat("Compile Errors", String.valueOf(compileErrors), new Color(234, 88, 12)));
        strip.add(createCompactResultStat("Runtime Errors", String.valueOf(runtimeErrors), new Color(220, 38, 38)));
        strip.add(createCompactResultStat("Wrong Output", String.valueOf(wrongOutput), new Color(147, 51, 234)));
        strip.add(createCompactResultStat("Pass Rate", passRate + "%", new Color(8, 145, 178)));

        return strip;
    }

    private JPanel createCompactResultStat(String label, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(8, 0));
        card.setBackground(new Color(248, 250, 252));
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(9, 12, 9, 12)
        ));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        valueLabel.setForeground(accent);

        JLabel labelText = new JLabel(label);
        labelText.setFont(new Font("SansSerif", Font.PLAIN, 12));
        labelText.setForeground(TEXT_SECONDARY);

        card.add(valueLabel, BorderLayout.NORTH);
        card.add(labelText, BorderLayout.CENTER);
        return card;
    }

    private boolean matchesResultsStatusFilter(Status status, String selectedStatus) {
        if (selectedStatus == null || selectedStatus.equals("All Statuses")) return true;
        if (selectedStatus.equals("Pending")) return status == null;
        if (selectedStatus.equals("Passed")) return status == Status.SUCCESS;
        if (selectedStatus.equals("Compile Errors")) return status == Status.COMPILE_ERROR;
        if (selectedStatus.equals("Runtime Errors")) return status == Status.RUNTIME_ERROR;
        if (selectedStatus.equals("Wrong Output")) return status == Status.WRONG_OUTPUT;
        if (selectedStatus.equals("Extraction Errors")) return status == Status.EXTRACTION_ERROR;
        return true;
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

                case "results":
                    g2.drawLine(x + s / 5, y + s * 4 / 5, x + s * 4 / 5, y + s * 4 / 5);
                    g2.drawLine(x + s / 5, y + s / 5, x + s / 5, y + s * 4 / 5);

                    g2.fillRoundRect(x + s / 3, y + s * 3 / 5, s / 10, s / 5, 2, 2);
                    g2.fillRoundRect(x + s / 2, y + s * 9 / 20, s / 10, s * 7 / 20, 2, 2);
                    g2.fillRoundRect(x + s * 2 / 3, y + s / 3, s / 10, s * 7 / 15, 2, 2);
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
    private JButton createInfoToolbarButton(String text) {
        JButton btn = createToolbarButton("file", text, false);

        Icon infoIcon = UIManager.getIcon("OptionPane.informationIcon");

        if (infoIcon != null) {
            btn.setIcon(resizeIcon(infoIcon, 18, 18));
        }

        btn.setIconTextGap(10);
        return btn;
    }
    private Icon resizeIcon(Icon icon, int width, int height) {
        if (icon == null) return null;

        BufferedImage image = new BufferedImage(
                icon.getIconWidth(),
                icon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        icon.paintIcon(null, g2, 0, 0);
        g2.dispose();

        Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }
    private String getProjectInputFileText(Project project) {
        if (project.getTestCases() == null || project.getTestCases().isEmpty()) {
            return "No input file selected";
        }

        return "Input data saved in " + project.getTestCases().size() + " test case(s)";
    }

    private String getProjectExpectedOutputFileText(Project project) {
        if (project.getTestCases() == null || project.getTestCases().isEmpty()) {
            return "No expected output file selected";
        }

        return "Expected output saved in " + project.getTestCases().size() + " test case(s)";
    }
    private static class ProjectFilePaths {
        String inputFilePath;
        String expectedOutputFilePath;
        String submissionsFolderPath;

        ProjectFilePaths(String inputFilePath, String expectedOutputFilePath, String submissionsFolderPath) {
            this.inputFilePath = inputFilePath;
            this.expectedOutputFilePath = expectedOutputFilePath;
            this.submissionsFolderPath = submissionsFolderPath;
        }
    }
    private void rememberProjectPaths(String projectName, String inputPath, String expectedPath, String submissionsPath) {
        if (projectName == null || projectName.isBlank()) return;

        projectPathsByName.put(
                projectName,
                new ProjectFilePaths(inputPath, expectedPath, submissionsPath)
        );
    }

    private ProjectFilePaths getProjectPaths(Project project) {
        if (project == null || project.getName() == null) {
            return new ProjectFilePaths("", "", "");
        }

        ProjectFilePaths paths = projectPathsByName.get(project.getName());

        if (paths != null) {
            return paths;
        }

        return new ProjectFilePaths(
                "",
                "",
                getProjectSubmissionsFolder(project)
        );
    }

    private String showPathOrFallback(String path, String fallback) {
        if (path == null || path.isBlank()) {
            return fallback;
        }

        return path;
    }
    private void openFileOrFolder(String path) {
        if (path == null || path.isBlank()) {
            JOptionPane.showMessageDialog(this, "No file or folder path available.");
            return;
        }

        File file = new File(path);

        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "Path does not exist:\n" + path);
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            JOptionPane.showMessageDialog(this, "Opening files is not supported on this system.");
            return;
        }

        try {
            Desktop.getDesktop().open(file);
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Could not open:\n" + path + "\n\n" + ex.getMessage(),
                    "Open Failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
