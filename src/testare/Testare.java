package testare;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import VacationManager.DatabaseManager;
import VacationManager.Employee;
import VacationManager.RequestStatus;
import VacationManager.VacationRequest;

class Testare {

    private DatabaseManager dbManager;
    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        // Creăm o bază de date în memorie pentru teste
    	connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    	dbManager = new DatabaseManager(connection);

        // Inițializăm tabele și date de test
        initTestDatabase();
    }

    private void initTestDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Creăm tabelele necesare
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

            stmt.execute("CREATE TABLE IF NOT EXISTS settings (" +
                    "key TEXT PRIMARY KEY, " +
                    "value TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS blocked_periods (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "startDate TEXT, " +
                    "endDate TEXT, " +
                    "description TEXT)");

            // Adăugăm câțiva angajați de test
            stmt.execute("INSERT INTO employees VALUES (1, 'Ion Popescu', 'Angajat', 'Development', 'parola123', 21)");
            stmt.execute("INSERT INTO employees VALUES (2, 'Maria Ionescu', 'Angajat', 'Design', 'parola456', 15)");
            stmt.execute("INSERT INTO employees VALUES (3, 'Vasile Marin', 'Manager', 'Development', 'manager123', 25)");
            stmt.execute("INSERT INTO employees VALUES (4, 'Ana Dumitrescu', 'Administrator', 'HR', 'admin123', 30)");

            // Adăugăm setări implicite
            stmt.execute("INSERT INTO settings VALUES ('maxTeamOnLeave', '2')");
        }
    }

    @Test
    public void testCompleteEmployeeVacationRequestFlow() {
        // 1. Autentificare angajat
        Employee employee = dbManager.authenticateEmployee(1, "parola12");
        assertNotNull(employee, "Autentificarea ar trebui să reușească");

        // 2. Verificare zile disponibile înainte de cerere
        int initialDays = dbManager.getEmployeeVacationDays(employee.getId());
        assertEquals(21, initialDays);

        // 3. Trimitere cerere de concediu
        String startDate = "2025-06-01";
        String endDate = "2025-06-10";
        VacationRequest request = new VacationRequest(0, employee.getId(), startDate, endDate,
                "Concediu de odihnă", RequestStatus.PENDING);
        dbManager.addVacationRequest(request);

        // 4. Verificare cerere adăugată
        List<VacationRequest> requests = dbManager.getVacationRequestsForEmployee(employee.getId());
        assertEquals(1, requests.size());

        // 5. Manager obține cereri în așteptare
        List<VacationRequest> pendingRequests = dbManager.getPendingRequestsForManager("Development");
        assertEquals(1, pendingRequests.size());

        // 6. Manager aprobă cererea
        VacationRequest pendingRequest = pendingRequests.get(0);
        dbManager.updateRequestStatusWithDeduction(pendingRequest, RequestStatus.APPROVED);

        // 7. Verificare zile deduse
        int remainingDays = dbManager.getEmployeeVacationDays(employee.getId());
        assertEquals(11, remainingDays, "Ar trebui să rămână 11 zile după deducere");

        // 8. Verificare status cerere
        requests = dbManager.getVacationRequestsForEmployee(employee.getId());
        assertEquals(RequestStatus.APPROVED, requests.get(0).getStatus());

        // 9. Verificare listare angajați în concediu
        List<String> employeesOnLeave = dbManager.getEmployeesOnVacationDuring("2025-06-05", "2025-06-07");
        assertEquals(1, employeesOnLeave.size());
        assertEquals("Ion Popescu", employeesOnLeave.get(0));
    }
}
