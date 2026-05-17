import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:iae.db";
    private final String dbUrl;
    private Connection connection;

    public DatabaseManager() {
        this(DB_URL);
    }

    public DatabaseManager(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    //Connection

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(dbUrl);
        // Enable foreign key support (off by default in SQLite)
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Schema Creation

    public void initSchema() throws SQLException {
        String[] statements = {
                """
            CREATE TABLE IF NOT EXISTS Configurations (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                name            TEXT    NOT NULL UNIQUE,
                language        TEXT    NOT NULL DEFAULT '',
                compile_command TEXT    NOT NULL,
                run_command     TEXT    NOT NULL,
                source_extension TEXT   NOT NULL DEFAULT '',
                entry_point_pattern TEXT NOT NULL DEFAULT ''
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS Projects (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                name      TEXT    NOT NULL UNIQUE,
                config_id INTEGER NOT NULL,
                FOREIGN KEY (config_id) REFERENCES Configurations(id)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS TestCases (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                project_id      INTEGER NOT NULL,
                input           TEXT,
                expected_output TEXT    NOT NULL,
                FOREIGN KEY (project_id) REFERENCES Projects(id)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS StudentSubmissions (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                project_id     INTEGER NOT NULL,
                student_id     TEXT    NOT NULL,
                zip_path       TEXT    NOT NULL,
                extracted_path TEXT,
                status         TEXT    NOT NULL DEFAULT 'PENDING',
                output         TEXT,
                error_message  TEXT,
                FOREIGN KEY (project_id) REFERENCES Projects(id)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS DetailedResults (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                submission_id INTEGER NOT NULL,
                test_case_id  INTEGER NOT NULL,
                status        TEXT    NOT NULL DEFAULT 'PENDING',
                actual_output TEXT,
                error_message TEXT,
                exit_code     INTEGER,
                FOREIGN KEY (submission_id) REFERENCES StudentSubmissions(id),
                FOREIGN KEY (test_case_id)  REFERENCES TestCases(id)
            )
            """
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : statements) {
                stmt.execute(sql);
            }
        }

        migrateConfigurationColumns();
        migrateDetailedResultColumns();
    }

    private void migrateConfigurationColumns() throws SQLException {
        addColumnIfMissing("Configurations", "language", "TEXT NOT NULL DEFAULT ''");
        addColumnIfMissing("Configurations", "source_extension", "TEXT NOT NULL DEFAULT ''");
        addColumnIfMissing("Configurations", "entry_point_pattern", "TEXT NOT NULL DEFAULT ''");
    }

    private void migrateDetailedResultColumns() throws SQLException {
        addColumnIfMissing("DetailedResults", "status", "TEXT NOT NULL DEFAULT 'PENDING'");
    }

    private void addColumnIfMissing(String table, String column, String definition) throws SQLException {
        if (columnExists(table, column)) return;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private boolean columnExists(String table, String column) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    //Configuration

    public long insertConfiguration(Configuration config) throws SQLException {
        String sql = """
                INSERT INTO Configurations
                (name, language, compile_command, run_command, source_extension, entry_point_pattern)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, config.getName());
            ps.setString(2, config.getLanguage());
            ps.setString(3, config.getCompileCommand());
            ps.setString(4, config.getRunCommand());
            ps.setString(5, config.getSourceExtension());
            ps.setString(6, config.getEntryPointPattern());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getLong(1) : -1;
        }
    }

    public long upsertConfiguration(Configuration config) throws SQLException {
        String selectSql = "SELECT id FROM Configurations WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setString(1, config.getName());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long id = rs.getLong("id");
                updateConfiguration(id, config);
                return id;
            }
        }
        return insertConfiguration(config);
    }

    public List<Configuration> getAllConfigurations() throws SQLException {
        List<Configuration> list = new ArrayList<>();
        String sql = """
                SELECT name, language, compile_command, run_command, source_extension, entry_point_pattern
                FROM Configurations
                ORDER BY name
                """;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Configuration(
                        rs.getString("name"),
                        rs.getString("language"),
                        rs.getString("compile_command"),
                        rs.getString("run_command"),
                        rs.getString("source_extension"),
                        rs.getString("entry_point_pattern")
                ));
            }
        }
        return list;
    }

    //Projects

    public long insertProject(String name, long configId) throws SQLException {
        String sql = "INSERT INTO Projects (name, config_id) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setLong(2, configId);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getLong(1) : -1;
        }
    }

    public long upsertProject(String name, long configId) throws SQLException {
        String findSql = "SELECT id FROM Projects WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(findSql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long existingId = rs.getLong("id");
                String updateSql = "UPDATE Projects SET config_id = ? WHERE id = ?";
                try (PreparedStatement ups = connection.prepareStatement(updateSql)) {
                    ups.setLong(1, configId);
                    ups.setLong(2, existingId);
                    ups.executeUpdate();
                }
                return existingId;
            }
        }
        return insertProject(name, configId);
    }

    public long findProjectIdByName(String name) throws SQLException {
        String sql = "SELECT id FROM Projects WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("id");
        }
        return -1;
    }

    //Test Cases

    public long insertTestCase(long projectId, TestCase testCase) throws SQLException {
        String sql = "INSERT INTO TestCases (project_id, input, expected_output) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, projectId);
            ps.setString(2, testCase.getInput());
            ps.setString(3, testCase.getExpectedOutput());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            long generatedId = keys.next() ? keys.getLong(1) : -1;
            if (generatedId > 0) {
                testCase.setId(generatedId);
            }
            return generatedId;
        }
    }

    public List<TestCase> getTestCasesForProject(long projectId) throws SQLException {
        List<TestCase> list = new ArrayList<>();
        String sql = "SELECT id, input, expected_output FROM TestCases WHERE project_id = ? ORDER BY id";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new TestCase(
                        rs.getLong("id"),
                        rs.getString("input"),
                        rs.getString("expected_output")
                ));
            }
        }
        return list;
    }

    public void deleteTestCasesForProject(long projectId) throws SQLException {
        String sql = "DELETE FROM TestCases WHERE project_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            ps.executeUpdate();
        }
    }

    public void replaceTestCasesForProject(long projectId, List<TestCase> testCases) throws SQLException {
        deleteTestCasesForProject(projectId);
        if (testCases == null) return;

        for (TestCase testCase : testCases) {
            insertTestCase(projectId, testCase);
        }
    }

    public long saveProject(String name, Configuration config, List<TestCase> testCases) throws SQLException {
        long configId = upsertConfiguration(config);
        long projectId = upsertProject(name, configId);
        replaceTestCasesForProject(projectId, testCases);
        return projectId;
    }

    //Submits

    public long insertSubmission(long projectId, StudentZipSubmission submission) throws SQLException {
        Result result = submission.getResult();

        String output = result == null ? "" : result.getOutput();
        String errorMessage = result == null ? "" : result.getErrorMessage();

        return insertSubmission(projectId, submission, output, errorMessage);
    }

    public long insertSubmission(long projectId,
                                 StudentZipSubmission submission,
                                 String output,
                                 String errorMessage) throws SQLException {
        String sql = """
        INSERT INTO StudentSubmissions 
        (project_id, student_id, zip_path, extracted_path, status, output, error_message)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, projectId);
            ps.setString(2, submission.getStudentId());
            ps.setString(3, submission.getZipFile().getAbsolutePath());
            ps.setString(4, submission.getExtractedFolder() == null ? null
                    : submission.getExtractedFolder().getAbsolutePath());
            ps.setString(5, submission.getResult() == null ? "PENDING"
                    : submission.getResult().getStatus().name());
            ps.setString(6, output);
            ps.setString(7, errorMessage);

            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getLong(1) : -1;
        }
    }

    public void updateSubmissionStatus(long submissionId, Status status) throws SQLException {
        String sql = "UPDATE StudentSubmissions SET status = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, submissionId);
            ps.executeUpdate();
        }
    }

    public long findSubmissionId(long projectId, String studentId) throws SQLException {
        String sql = "SELECT id FROM StudentSubmissions WHERE project_id = ? AND student_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            ps.setString(2, studentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("id");
        }
        return -1;
    }

    public void updateSubmission(long submissionId,
                                 StudentZipSubmission submission,
                                 String output,
                                 String errorMessage) throws SQLException {
        String sql = """
            UPDATE StudentSubmissions
            SET zip_path = ?, extracted_path = ?, status = ?, output = ?, error_message = ?
            WHERE id = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, submission.getZipFile().getAbsolutePath());
            ps.setString(2, submission.getExtractedFolder() == null ? null
                    : submission.getExtractedFolder().getAbsolutePath());
            ps.setString(3, submission.getResult() == null ? "PENDING"
                    : submission.getResult().getStatus().name());
            ps.setString(4, output);
            ps.setString(5, errorMessage);
            ps.setLong(6, submissionId);
            ps.executeUpdate();
        }
    }

    public long upsertSubmission(long projectId, StudentZipSubmission submission) throws SQLException {
        Result result = submission.getResult();
        String output = result == null ? "" : result.getOutput();
        String errorMessage = result == null ? "" : result.getErrorMessage();

        long existingId = findSubmissionId(projectId, submission.getStudentId());
        if (existingId > 0) {
            updateSubmission(existingId, submission, output, errorMessage);
            return existingId;
        }
        return insertSubmission(projectId, submission, output, errorMessage);
    }

    public void deleteSubmissionsForProject(long projectId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM DetailedResults WHERE submission_id IN (SELECT id FROM StudentSubmissions WHERE project_id = ?)")) {
            ps.setLong(1, projectId);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM StudentSubmissions WHERE project_id = ?")) {
            ps.setLong(1, projectId);
            ps.executeUpdate();
        }
    }

    //Detailed Results

    public void insertDetailedResult(long submissionId, PerTestResult result) throws SQLException {
        String sql = """
            INSERT INTO DetailedResults (submission_id, test_case_id, status, actual_output, error_message, exit_code)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, submissionId);
            ps.setLong(2, result.getTestCaseId());
            ps.setString(3, result.getStatus() == null ? Status.RUNTIME_ERROR.name() : result.getStatus().name());
            ps.setString(4, result.getActualOutput());
            ps.setString(5, result.getErrorMessage());
            ps.setInt(6, result.getExitCode());
            ps.executeUpdate();
        }
    }

    public void deleteDetailedResultsForSubmission(long submissionId) throws SQLException {
        String sql = "DELETE FROM DetailedResults WHERE submission_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, submissionId);
            ps.executeUpdate();
        }
    }

    public void replaceDetailedResults(long submissionId, List<PerTestResult> results) throws SQLException {
        deleteDetailedResultsForSubmission(submissionId);
        if (results == null) return;
        for (PerTestResult result : results) {
            insertDetailedResult(submissionId, result);
        }
    }

    public List<PerTestResult> getDetailedResultsForSubmission(long submissionId) throws SQLException {
        List<PerTestResult> list = new ArrayList<>();
        String sql = """
            SELECT test_case_id, status, actual_output, error_message, exit_code
            FROM DetailedResults
            WHERE submission_id = ?
            ORDER BY id
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, submissionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Status status;
                try {
                    status = Status.valueOf(rs.getString("status"));
                } catch (IllegalArgumentException | NullPointerException e) {
                    status = Status.RUNTIME_ERROR;
                }
                list.add(new PerTestResult(
                        rs.getLong("test_case_id"),
                        status,
                        rs.getString("actual_output"),
                        rs.getString("error_message"),
                        rs.getInt("exit_code")
                ));
            }
        }
        return list;
    }
    public List<Project> getProjects() throws SQLException {
        List<Project> projects = new ArrayList<>();

        String sql = """
        SELECT p.id, p.name, c.name AS config_name, c.language,
               c.compile_command, c.run_command, c.source_extension, c.entry_point_pattern
        FROM Projects p
        JOIN Configurations c ON p.config_id = c.id
        ORDER BY p.id
    """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                long projectId = rs.getLong("id");
                String projectName = rs.getString("name");
                Configuration config = new Configuration(
                        rs.getString("config_name"),
                        rs.getString("language"),
                        rs.getString("compile_command"),
                        rs.getString("run_command"),
                        rs.getString("source_extension"),
                        rs.getString("entry_point_pattern")
                );

                List<TestCase> testCases = getTestCasesForProject(projectId);
                List<StudentZipSubmission> submissions = getSubmissionsForProject(projectId);

                Project project = new Project(projectName, config, submissions, testCases);
                project.setId(projectId);
                projects.add(project);
            }
        }

        return projects;
    }

    private List<StudentZipSubmission> getSubmissionsForProject(long projectId) throws SQLException {
        List<StudentZipSubmission> submissions = new ArrayList<>();

        String sql = """
        SELECT id, student_id, zip_path, extracted_path, status, output, error_message
        FROM StudentSubmissions
        WHERE project_id = ?
    """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                long submissionId = rs.getLong("id");
                File zipFile = new File(rs.getString("zip_path"));
                StudentZipSubmission submission = new StudentZipSubmission(
                        rs.getString("student_id"),
                        zipFile
                );

                String extractedPath = rs.getString("extracted_path");
                if (extractedPath != null) {
                    submission.setExtractedFolder(new File(extractedPath));
                }

                String statusStr = rs.getString("status");
                Status status;
                try {
                    status = Status.valueOf(statusStr);
                } catch (IllegalArgumentException | NullPointerException e) {
                    status = Status.RUNTIME_ERROR;
                }

                String output = rs.getString("output");
                String errorMessage = rs.getString("error_message");

                submission.setResult(new Result(status, output, errorMessage));
                submission.setPerTestResults(getDetailedResultsForSubmission(submissionId));

                submissions.add(submission);
            }
        }

        return submissions;
    }

    public void updateConfiguration(long id, Configuration config) throws SQLException {
        String sql = """
                UPDATE Configurations
                SET name = ?, language = ?, compile_command = ?, run_command = ?,
                    source_extension = ?, entry_point_pattern = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, config.getName());
            ps.setString(2, config.getLanguage());
            ps.setString(3, config.getCompileCommand());
            ps.setString(4, config.getRunCommand());
            ps.setString(5, config.getSourceExtension());
            ps.setString(6, config.getEntryPointPattern());
            ps.setLong(7, id);
            ps.executeUpdate();
        }
    }

    public void deleteConfiguration(long id) throws SQLException {
        String sql = "DELETE FROM Configurations WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void deleteConfigurationByName(String name) throws SQLException {
        String sql = "DELETE FROM Configurations WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.executeUpdate();
        }
    }

    public void deleteProject(long id) throws SQLException {
        // Must delete children first due to foreign keys
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM DetailedResults WHERE submission_id IN (SELECT id FROM StudentSubmissions WHERE project_id = ?)")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM StudentSubmissions WHERE project_id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM TestCases WHERE project_id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM Projects WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

}
