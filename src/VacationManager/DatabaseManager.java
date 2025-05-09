package VacationManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class DatabaseManager {
    private Connection connection;

    public DatabaseManager() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:vacation_system_final.db");
            initializeDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initializeDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS employees (" +
                    "id INTEGER PRIMARY KEY, " +
                    "name TEXT, " +
                    "role TEXT, " +
                    "team TEXT, " +
                    "password TEXT, " +
                    "vacationDays INTEGER)");

            stmt.execute("CREATE TABLE IF NOT EXISTS vacation_requests (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "employeeId INTEGER, " +
                    "startDate TEXT, " +
                    "endDate TEXT, " +
                    "reason TEXT, " +
                    "status TEXT)");
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✅ Conexiunea la baza de date a fost inchisa cu succes.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Employee authenticateEmployee(int id, String password) {
        String sql = "SELECT * FROM employees WHERE id = ? AND password = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Employee(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("role"),
                        rs.getString("team"),
                        rs.getString("password"),
                        rs.getInt("vacationDays")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addVacationRequest(VacationRequest request) {
        String sql = "INSERT INTO vacation_requests (employeeId, startDate, endDate, reason, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, request.getEmployeeId());
            pstmt.setString(2, request.getStartDate());
            pstmt.setString(3, request.getEndDate());
            pstmt.setString(4, request.getReason());
            pstmt.setString(5, request.getStatus().name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<VacationRequest> getVacationRequestsForEmployee(int employeeId) {
        List<VacationRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM vacation_requests WHERE employeeId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                VacationRequest request = new VacationRequest(
                        rs.getInt("id"),
                        rs.getInt("employeeId"),
                        rs.getString("startDate"),
                        rs.getString("endDate"),
                        rs.getString("reason"),
                        RequestStatus.valueOf(rs.getString("status"))
                );
                requests.add(request);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }

    public List<VacationRequest> getPendingRequestsForManager(String team) {
        List<VacationRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM vacation_requests vr JOIN employees e ON vr.employeeId = e.id " +
                "WHERE vr.status = 'PENDING' AND e.team = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, team);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                VacationRequest request = new VacationRequest(
                        rs.getInt("id"),
                        rs.getInt("employeeId"),
                        rs.getString("startDate"),
                        rs.getString("endDate"),
                        rs.getString("reason"),
                        RequestStatus.valueOf(rs.getString("status"))
                );
                requests.add(request);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }

    public int getEmployeeVacationDays(int employeeId) {
        String sql = "SELECT vacationDays FROM employees WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("vacationDays");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean hasOverlappingRequest(int employeeId, String startDate, String endDate) {
        String sql = "SELECT * FROM vacation_requests WHERE employeeId = ? " +
                "AND ((startDate <= ? AND endDate >= ?) OR (startDate <= ? AND endDate >= ?)) " +
                "AND status != 'REJECTED'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            pstmt.setString(2, endDate);
            pstmt.setString(3, startDate);
            pstmt.setString(4, endDate);
            pstmt.setString(5, startDate);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int countTeamMembersOnVacation(String team, String startDate, String endDate) {
        String sql = "SELECT COUNT(*) AS total FROM vacation_requests vr JOIN employees e ON vr.employeeId = e.id " +
                "WHERE e.team = ? AND vr.status = 'APPROVED' " +
                "AND ((vr.startDate <= ? AND vr.endDate >= ?) OR (vr.startDate <= ? AND vr.endDate >= ?)) " +
                "AND vr.reason != 'Concediu medical'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, team);
            pstmt.setString(2, endDate);
            pstmt.setString(3, startDate);
            pstmt.setString(4, endDate);
            pstmt.setString(5, startDate);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<String> getEmployeesOnVacationDuring(String startDate, String endDate) {
        List<String> employees = new ArrayList<>();
        String sql = "SELECT DISTINCT e.name FROM vacation_requests vr JOIN employees e ON vr.employeeId = e.id " +
                "WHERE vr.status = 'APPROVED' " +
                "AND ((vr.startDate <= ? AND vr.endDate >= ?) OR (vr.startDate <= ? AND vr.endDate >= ?))";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, endDate);
            pstmt.setString(2, startDate);
            pstmt.setString(3, endDate);
            pstmt.setString(4, startDate);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                employees.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return employees;
    }

    public Map<String, Integer> getRemainingVacationDaysForAllEmployees() {
        Map<String, Integer> report = new HashMap<>();
        String sql = "SELECT name, vacationDays FROM employees";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String name = rs.getString("name");
                int remainingDays = rs.getInt("vacationDays");
                report.put(name, remainingDays);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return report;
    }


    public void updateRequestStatusWithDeduction(VacationRequest request, RequestStatus status) {
        String updateSql = "UPDATE vacation_requests SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
            pstmt.setString(1, status.name());
            pstmt.setInt(2, request.getId());
            pstmt.executeUpdate();

            if (status == RequestStatus.APPROVED && !request.getReason().equalsIgnoreCase("Concediu medical")) {
                LocalDate start = LocalDate.parse(request.getStartDate());
                LocalDate end = LocalDate.parse(request.getEndDate());
                int usedDays = (int) (end.toEpochDay() - start.toEpochDay() + 1);
                deductVacationDays(request.getEmployeeId(), usedDays);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deductVacationDays(int employeeId, int days) {
        String sql = "UPDATE employees SET vacationDays = vacationDays - ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, days);
            pstmt.setInt(2, employeeId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public int getSetting(String key, int defaultValue) {
        String sql = "SELECT value FROM settings WHERE key = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Integer.parseInt(rs.getString("value"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    public void updateSetting(String key, int newValue) {
        String sql = "UPDATE settings SET value = ? WHERE key = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, String.valueOf(newValue));
            pstmt.setString(2, key);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public List<String> getBlockedPeriods() {
        List<String> periods = new ArrayList<>();
        String sql = "SELECT id, startDate, endDate, description FROM blocked_periods";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                periods.add("ID: " + rs.getInt("id") + ", " +
                            rs.getString("startDate") + " - " +
                            rs.getString("endDate") + " (" +
                            rs.getString("description") + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return periods;
    }

    public void addBlockedPeriod(String startDate, String endDate, String description) {
        String sql = "INSERT INTO blocked_periods (startDate, endDate, description) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            pstmt.setString(3, description);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteBlockedPeriod(int id) {
        String sql = "DELETE FROM blocked_periods WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public boolean isInBlockedPeriod(String startDate, String endDate) {
        String sql = "SELECT * FROM blocked_periods WHERE " +
            "((startDate <= ? AND endDate >= ?) OR (startDate <= ? AND endDate >= ?))";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, endDate);
            pstmt.setString(2, startDate);
            pstmt.setString(3, endDate);
            pstmt.setString(4, startDate);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


}
