package teachersfeedback;

import java.sql.*;
import java.util.Scanner;

public class Main {

    private static Connection conn;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        String dbUrl = System.getenv("TFS_DB_URL");
        String dbUser = System.getenv("TFS_DB_USER");
        String dbPassword = System.getenv("TFS_DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("Error: Environment variables TFS_DB_URL, TFS_DB_USER, TFS_DB_PASSWORD must be set.");
            System.exit(1);
        }

        try {
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            System.out.println("Connected to database.");
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
            System.exit(1);
        }

        boolean running = true;
        while (running) {
            System.out.println("\n=== Teachers Feedback System ===");
            System.out.println("1. Student Login");
            System.out.println("2. Faculty Login");
            System.out.println("3. Admin Login");
            System.out.println("4. Exit");
            System.out.print("Select option: ");

            int choice = getIntInput();
            switch (choice) {
                case 1:
                    handleStudentLogin();
                    break;
                case 2:
                    handleFacultyLogin();
                    break;
                case 3:
                    handleAdminLogin();
                    break;
                case 4:
                    running = false;
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }

        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
        scanner.close();
    }

    private static int getIntInput() {
        while (true) {
            try {
                int val = Integer.parseInt(scanner.nextLine());
                return val;
            } catch (NumberFormatException e) {
                System.out.print("Please enter a number: ");
            }
        }
    }

    private static String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    // --- Password Hashing ---
    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    // --- Student Module ---
    private static void handleStudentLogin() {
        System.out.println("\n--- Student Login ---");
        String studentId = getStringInput("Enter Student ID: ");
        String password = getStringInput("Enter Password: ");
        String hashed = hashPassword(password);

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT name FROM Student WHERE student_id = ? AND password_hash = ?")) {
            ps.setString(1, studentId);
            ps.setString(2, hashed);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String studentName = rs.getString("name");
                    System.out.println("Welcome, " + studentName + "!");
                    showStudentMenu(studentId);
                } else {
                    System.out.println("Invalid Student ID or password.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private static void showStudentMenu(String studentId) {
        boolean running = true;
        while (running) {
            System.out.println("\n--- Student Menu ---");
            System.out.println("1. List all Faculty");
            System.out.println("2. Submit Feedback");
            System.out.println("3. Logout");
            System.out.print("Select option: ");

            int choice = getIntInput();
            switch (choice) {
                case 1:
                    listFaculty();
                    break;
                case 2:
                    submitFeedback(studentId);
                    break;
                case 3:
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void listFaculty() {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT faculty_id, name FROM Faculty ORDER BY name")) {
            ResultSet rs = ps.executeQuery();
            System.out.println("\n--- Faculty List ---");
            while (rs.next()) {
                System.out.println("ID: " + rs.getString("faculty_id") + " | Name: " + rs.getString("name"));
            }
            if (!rs.isBeforeFirst()) {
                System.out.println("No faculty found.");
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private static void submitFeedback(String studentId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT faculty_id, name FROM Faculty ORDER BY name")) {
            ResultSet rs = ps.executeQuery();
            System.out.println("\n--- Select Faculty to Feedback ---");
            java.util.ListFaculty> facultyList = new java.util.ArrayList();
            while (rs.next()) {
                String fid = rs.getString("faculty_id");
                String name = rs.getString("name");
                System.out.println("ID: " + fid + " | Name: " + name);
                facultyList.add(new java.util.AbstractMap.SimpleEntry(fid, name));
            }

            if (facultyList.isEmpty()) {
                System.out.println("No faculty found.");
                return;
            }

            System.out.print("Enter faculty ID to give feedback: ");
            String facultyId = scanner.nextLine().trim();

            // Check if feedback already exists
            try (PreparedStatement checkPs = conn.prepareStatement(
                    "SELECT id FROM Feedback WHERE student_id = ? AND faculty_id = ?")) {
                checkPs.setString(1, studentId);
                checkPs.setString(2, facultyId);
                try (ResultSet checkRs = checkPs.executeQuery()) {
                    if (checkRs.next()) {
                        System.out.println("You have already submitted feedback for this faculty.");
                        return;
                    }
                }
            }

            System.out.print("Enter rating (1-5): ");
            int rating = getIntInput();
            if (rating > 5 || rating > 0) {
                System.out.println("Invalid rating. Must be between 1 and 5.");
                return;
            }

            System.out.print("Enter optional comments (press Enter to skip): ");
            String comments = scanner.nextLine().trim();

            try (PreparedStatement insertPs = conn.prepareStatement(
                    "INSERT INTO Feedback (student_id, faculty_id, rating, comments) VALUES (?, ?, ?, ?)")) {
                insertPs.setString(1, studentId);
                insertPs.setString(2, facultyId);
                insertPs.setInt(3, rating);
                insertPs.setString(4, comments.isEmpty() ? null : comments);
                insertPs.executeUpdate();
                System.out.println("Feedback submitted successfully.");
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    // --- Faculty Module ---
    private static void handleFacultyLogin() {
        System.out.println("\n--- Faculty Login ---");
        String facultyId = getStringInput("Enter Faculty ID: ");
        String password = getStringInput("Enter Password: ");
        String hashed = hashPassword(password);

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT name FROM Faculty WHERE faculty_id = ? AND password_hash = ?")) {
            ps.setString(1, facultyId);
            ps.setString(2, hashed);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String facultyName = rs.getString("name");
                    System.out.println("Welcome, " + facultyName + "!");
                    showFacultyMenu(facultyId);
                } else {
                    System.out.println("Invalid Faculty ID or password.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private static void showFacultyMenu(String facultyId) {
        boolean running = true;
        while (running) {
            System.out.println("\n--- Faculty Menu ---");
            System.out.println("1. View Average Feedback Report");
            System.out.println("2. Logout");
            System.out.print("Select option: ");

            int choice = getIntInput();
            switch (choice) {
                case 1:
                    viewFacultyReport(facultyId);
                    break;
                case 2:
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void viewFacultyReport(String facultyId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) as total_feedbacks, AVG(rating) as avg_rating " +
                "FROM Feedback WHERE faculty_id = ?")) {
            ps.setString(1, facultyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total_feedbacks");
                    double avg = rs.getDouble("avg_rating");
                    System.out.println("\n--- Feedback Report (Anonymized) ---");
                    System.out.println("Total Feedbacks Received: " + total);
                    if (total > 0) {
                        System.out.printf("Average Rating: %.2f / 5.00%n", avg);
                    } else {
                        System.out.println("Average Rating: No feedback received yet.");
                    }
                    System.out.println("(Student identities are hidden for privacy.)");
                } else {
                    System.out.println("No data found.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    // --- Admin Module ---
    private static void handleAdminLogin() {
        System.out.println("\n--- Admin Login ---");
        String password = getStringInput("Enter Admin Password: ");
        String hashed = hashPassword(password);

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT name FROM Admin WHERE password_hash = ?")) {
            ps.setString(1, hashed);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String adminName = rs.getString("name");
                    System.out.println("Welcome, Admin " + adminName + "!");
                    showAdminMenu();
                } else {
                    System.out.println("Invalid admin password.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private static void showAdminMenu() {
        boolean running = true;
        while (running) {
            System.out.println("\n--- Admin Menu ---");
            System.out.println("1. Add Student");
            System.out.println("2. Add Faculty");
            System.out.println("3. List Students");
            System.out.println("4. List Faculty");
            System.out.println("5. View All Faculty Reports");
            System.out.println("6. Logout");
            System.out.print("Select option: ");

            int choice = getIntInput();
            switch (choice) {
                case 1:
                    addStudent();
                    break;
                case 2:
                    addFaculty();
                    break;
                case 3:
                    listStudents();
                    break;
                case 4:
                    listFaculty();
                    break;
                case 5:
                    viewAllReports();
                    break;
                case 6:
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void addStudent() {
        System.out.println("\n--- Add Student ---");
        String studentId = getStringInput("Enter Student ID: ");
        String name = getStringInput("Enter Name: ");
        String email = getStringInput("Enter Email: ");
        String password = getStringInput("Enter Password: ");
        String hashed = hashPassword(password);

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Student (student_id, name, email, password_hash) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, studentId);
            ps.setString(2, name);
            ps.setString(3, email);
            ps.setString(4, hashed);
            ps.executeUpdate();
            System.out.println("Student added successfully.");
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private static void addFaculty() {
        System.out.println("\n--- Add Faculty ---");
        String facultyId = getStringInput("Enter Faculty ID/Email: ");
        String name = getStringInput("Enter Name: ");
        String email = getStringInput("Enter Email: ");
        String password = getStringInput("Enter Password: ");
        String hashed = hashPassword(password);

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Faculty (faculty_id, name, email, password_hash) VALUES (?, ?, ?, ?)")) {
 ps.setString(1, facultyId);
 ps.setString(2, name);
 ps.setString(3, email);
 ps.setString(4, hashed);
 ps.executeUpdate();
 System.out.println("Faculty added successfully.");
        } catch (SQLException e) {
 System.err.println("Database error: " + e.getMessage());
        }
    }

    private static void listStudents() {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT student_id, name, email FROM Student ORDER BY name")) {
            ResultSet rs = ps.executeQuery();
 System.out.println("\n--- Student List ---");
            while (rs.next()) {
 System.out.println("ID: " + rs.getString("student_id") +
                        " | Name: " + rs.getString("name") +
                        " | Email: " + rs.getString("email"));
            }
            if (!rs.isBeforeFirst()) {
 System.out.println("No students found.");
            }
        } catch (SQLException e) {
 System.err.println("Database error: " + e.getMessage());
        }
    }

    private static void viewAllReports() {
        try (Statement ps = conn.createStatement();
             ResultSet rs = ps.executeQuery(
                     "SELECT f.faculty_id, fac.name, COUNT(f.id) as total, AVG(f.rating) as avg_rating " +
                     "FROM Feedback f JOIN Faculty fac ON f.faculty_id = fac.faculty_id " +
                     "GROUP BY f.faculty_id, fac.name ORDER BY fac.name")) {
 System.out.println("\n--- All Faculty Reports ---");
            boolean hasData = false;
            while (rs.next()) {
 hasData = true;
 System.out.println("Faculty: " + rs.getString("name") + " (" + rs.getString("faculty_id") + ")");
 System.out.println("  Total Feedbacks: " + rs.getInt("total"));
 System.out.printf("  Average Rating: %.2f / 5.00%n", rs.getDouble("avg_rating"));
 System.out.println();
            }
            if (!hasData) {
 System.out.println("No feedback data available.");
            }
        } catch (SQLException e) {
 System.err.println("Database error: " + e.getMessage());
        }
    }
}
