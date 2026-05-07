import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:iae.db";
    private Connection connection;

    //Connection

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(DB_URL);
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
                name            TEXT    NOT NULL,
                compile_command TEXT    NOT NULL,
                run_command     TEXT    NOT NULL
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS Projects (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                name      TEXT    NOT NULL,
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
                FOREIGN KEY (project_id) REFERENCES Projects(id)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS DetailedResults (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                submission_id INTEGER NOT NULL,
                test_case_id  INTEGER NOT NULL,
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
    }

    //Configuration

    public long insertConfiguration(Configuration config) throws SQLException {
        String sql = "INSERT INTO Configurations (name, compile_command, run_command) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, config.getName());
            ps.setString(2, config.getCompileCommand());
            ps.setString(3, config.getRunCommand());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getLong(1) : -1;
        }
    }

    public List<Configuration> getAllConfigurations() throws SQLException {
        List<Configuration> list = new ArrayList<>();
        String sql = "SELECT name, compile_command, run_command FROM Configurations ORDER BY name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Configuration(
                        rs.getString("name"),
                        rs.getString("compile_command"),
                        rs.getString("run_command")
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

    //Test Cases

    public long insertTestCase(long projectId, TestCase testCase) throws SQLException {
        String sql = "INSERT INTO TestCases (project_id, input, expected_output) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, projectId);
            ps.setString(2, testCase.getInput());
            ps.setString(3, testCase.getExpectedOutput());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getLong(1) : -1;
        }
    }

    public List<TestCase> getTestCasesForProject(long projectId) throws SQLException {
        List<TestCase> list = new ArrayList<>();
        String sql = "SELECT input, expected_output FROM TestCases WHERE project_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new TestCase(rs.getString("input"), rs.getString("expected_output")));
            }
        }
        return list;
    }

    //Submits

    public long insertSubmission(long projectId, StudentZipSubmission submission) throws SQLException {
        String sql = """
            INSERT INTO StudentSubmissions (project_id, student_id, zip_path, extracted_path, status)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, projectId);
            ps.setString(2, submission.getStudentId());
            ps.setString(3, submission.getZipFile().getAbsolutePath());
            ps.setString(4, submission.getExtractedFolder() == null ? null
                    : submission.getExtractedFolder().getAbsolutePath());
            ps.setString(5, submission.getResult() == null ? "PENDING"
                    : submission.getResult().getStatus().name());
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

    //Detailed Results

    public void insertDetailedResult(long submissionId, long testCaseId,
                                     EvaluationResult evalResult) throws SQLException {
        String sql = """
            INSERT INTO DetailedResults (submission_id, test_case_id, actual_output, error_message, exit_code)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, submissionId);
            ps.setLong(2, testCaseId);
            ps.setString(3, evalResult.getRunStdout());
            ps.setString(4, evalResult.getRunStderr());
            ps.setInt(5, evalResult.getRunExitCode());
            ps.executeUpdate();
        }
    }
    public List<Project> getProjects() throws SQLException {
        List<Project> projects = new ArrayList<>();

        String sql = """
        SELECT p.id, p.name, c.name AS config_name, c.compile_command, c.run_command
        FROM Projects p
        JOIN Configurations c ON p.config_id = c.id
    """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                long projectId = rs.getLong("id");
                String projectName = rs.getString("name");
                Configuration config = new Configuration(
                        rs.getString("config_name"),
                        rs.getString("compile_command"),
                        rs.getString("run_command")
                );

                List<TestCase> testCases = getTestCasesForProject(projectId);
                List<StudentZipSubmission> submissions = getSubmissionsForProject(projectId);

                projects.add(new Project(projectName, config, submissions, testCases));
            }
        }

        return projects;
    }

    private List<StudentZipSubmission> getSubmissionsForProject(long projectId) throws SQLException {
        List<StudentZipSubmission> submissions = new ArrayList<>();

        String sql = """
        SELECT student_id, zip_path, extracted_path, status
        FROM StudentSubmissions
        WHERE project_id = ?
    """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
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
                Status status = Status.valueOf(statusStr);
                submission.setResult(new Result(status, "", ""));

                submissions.add(submission);
            }
        }

        return submissions;
    }

    public void updateConfiguration(long id, Configuration config) throws SQLException {
        String sql = "UPDATE Configurations SET name = ?, compile_command = ?, run_command = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, config.getName());
            ps.setString(2, config.getCompileCommand());
            ps.setString(3, config.getRunCommand());
            ps.setLong(4, id);
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