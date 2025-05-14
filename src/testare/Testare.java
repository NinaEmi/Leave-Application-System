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
                    "status TEXT," +
                    "rejectionReason TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS settings (" +
                    "key TEXT PRIMARY KEY, " +
                    "value TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS blocked_periods (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "startDate TEXT, " +
                    "endDate TEXT, " +
                    "description TEXT)");

            // Adăugăm câțiva angajați de test
            stmt.execute("INSERT INTO employees VALUES (6, 'Ion Popescu', 'Angajat', 'Development', 'parola123', 21)");
            stmt.execute("INSERT INTO employees VALUES (7, 'Mihaela Marinescu', 'Angajat', 'Development', 'parola128', 21)");
            stmt.execute("INSERT INTO employees VALUES (2, 'Maria Ionescu', 'Angajat', 'Design', 'parola456', 15)");
            stmt.execute("INSERT INTO employees VALUES (3, 'Vasile Marin', 'Manager', 'Development', 'manager123', 25)");
            stmt.execute("INSERT INTO employees VALUES (4, 'Ana Dumitrescu', 'Administrator', 'HR', 'admin123', 30)");

            //Adaugam o perioada blocata 
            //stmt.execute("INSERT INTO blocked_periods VALUES (1, '2025-07-10', '2025-07-19', 'Petrecere de vara')");
            
            //Adaugam deja o cerere
            stmt.execute("INSERT INTO vacation_requests VALUES (1, 6, '2025-11-10', '2025-11-15', 'nu stiu', 'APPROVED')");
            stmt.execute("INSERT INTO vacation_requests VALUES (0, 6, '2025-11-17', '2025-11-19', 'nu stiu', 'PENDING')");
            stmt.execute("INSERT INTO vacation_requests VALUES (3, 7, '2025-11-20', '2025-11-25', 'nu stiu', 'PENDING')");
            
            // Adăugăm setări implicite
          
            stmt.execute("INSERT INTO settings VALUES ('maxTeamOnLeave', '2')");
        }
    }

    @Test
    public void testEmployeeVacationRequestFlow() {
    	
        // 1. Autentificare angajat
        Employee employee = dbManager.authenticateEmployee(6, "parola123");
        assertNotNull(employee, "Autentificarea ar trebui să reușească");

        // 2. Verificare zile disponibile înainte de cerere
        int initialDays = dbManager.getEmployeeVacationDays(employee.getId());
        assertEquals(21, initialDays);
        
        //Date pentru cererea de concediu 
        String startDate = "2025-01-11";
        String endDate = "2025-01-15";
        
        // 4. Verificăm că nu există suprapuneri
        boolean hasOverlap = dbManager.hasOverlappingRequest(employee.getId(), startDate, endDate);
        assertFalse(hasOverlap);
        
        // 5. Verificăm că nu este în perioadă blocată
        assertFalse(dbManager.isInBlockedPeriod(startDate, endDate));        

        //Crearea cererii de concediu
        VacationRequest request = new VacationRequest(0, employee.getId(), startDate, endDate,
                "Concediu de odihnă", RequestStatus.PENDING);
        dbManager.addVacationRequest(request);

        // 6. Verificare cerere adăugată
        List<VacationRequest> requests = dbManager.getVacationRequestsForEmployee(employee.getId());
        assertEquals(3, requests.size());

        // 8. Verificare status cerere
        requests = dbManager.getVacationRequestsForEmployee(employee.getId());
        assertEquals(RequestStatus.APPROVED, requests.get(1).getStatus());
    }
    
    @Test
    public void testMedicalLeaveRequestFlow() {
        // 1. Autentificare angajat
        Employee employee = dbManager.authenticateEmployee(6, "parola123");
        assertNotNull(employee, "Autentificarea ar trebui să reușească");

        // 2. Verificare zile disponibile (nu se modifică la medical)
        int initialDays = dbManager.getEmployeeVacationDays(employee.getId());
        assertEquals(21, initialDays);

        // 3. Date pentru cererea de concediu medical
        String startDate = "2025-02-01";
        String endDate = "2025-02-03";
        String reason = "Concediu medical";
        RequestStatus status = RequestStatus.APPROVED;

        // 4. Cream cererea de concediu medical
        VacationRequest medicalRequest = new VacationRequest(0, employee.getId(), startDate, endDate, reason, status);
        dbManager.addVacationRequest(medicalRequest);

        // 5. Verificăm că cererea a fost adăugată
        List<VacationRequest> requests = dbManager.getVacationRequestsForEmployee(employee.getId());
        assertTrue(requests.stream().anyMatch(req -> 
            req.getReason().equals("Concediu medical") && req.getStatus() == RequestStatus.APPROVED));

        // 6. Verificăm că zilele de vacanță nu s-au modificat
        int afterRequestDays = dbManager.getEmployeeVacationDays(employee.getId());
        assertEquals(initialDays, afterRequestDays, "Zilele de concediu nu trebuie afectate de concediul medical.");
    }
    
    @Test
    public void testManagerVisualizationRequestsFlow() {
        try {
            // 1. Autentificare manager
            Employee employee = dbManager.authenticateEmployee(3, "manager123");
            assertNotNull(employee, "Autentificarea ar trebui să reușească");
            System.out.println("Autentificare manager reușită.");

            // 2. Obținerea cererilor în așteptare
            List<VacationRequest> pendingRequests = dbManager.getPendingRequestsForManager(employee.getTeam());
            assertNotNull(pendingRequests, "Lista cererilor în așteptare nu trebuie să fie null");
            System.out.println("Lista cererilor în așteptare obținută.");

            // Dacă există cereri în așteptare, aprobăm una și respingem alta
            if (!pendingRequests.isEmpty()) {
                VacationRequest requestToApprove = pendingRequests.get(0);
                VacationRequest requestToReject = pendingRequests.get(pendingRequests.size() - 1);

                // Aprobare cerere
                dbManager.updateRequestStatusWithDeduction(requestToApprove, RequestStatus.APPROVED);
                System.out.println("✅ Cererea ID " + requestToApprove.getId() + " a fost aprobată.");

                // Verificăm statusul cererii aprobate
                VacationRequest updatedRequestApprove = dbManager.getVacationRequestById(requestToApprove.getId());
                assertEquals(RequestStatus.APPROVED, updatedRequestApprove.getStatus(), "Cererea nu a fost aprobată corect");
                System.out.println("Cererea a fost aprobată corect.");

                // Respingere cerere
                //String rejectionReason = "Nu se poate acorda concediu în această perioadă.";
                dbManager.updateRequestStatusWithDeduction(requestToReject, RequestStatus.REJECTED);
                System.out.println("❌ Cererea ID " + requestToReject.getId());

                // Verificăm statusul cererii respinse
                VacationRequest updatedRequestReject = dbManager.getVacationRequestById(requestToReject.getId());
                assertEquals(RequestStatus.REJECTED, updatedRequestReject.getStatus(), "Cererea nu a fost respinsă corect");
                System.out.println("Cererea a fost respinsă corect.");
            }
        } catch (SQLException e) {
            System.out.println("Eroare la accesarea bazei de date:");
            e.printStackTrace();
            fail("Testul a eșuat din cauza unei erori SQL");
        }
    }
    
    @Test
    public void testBlockedPeriodFlow() throws SQLException {
        // 1. Verificăm că nu există perioade blocate la început
        List<String> periodsBefore = dbManager.getBlockedPeriods();
        assertTrue(periodsBefore.isEmpty(), "Nu ar trebui să existe perioade blocate înainte de adăugare.");
        System.out.println("Nu există perioade blocate înainte de adăugare.");

        // 2. Adăugăm o perioadă blocată
        String startDate = "2025-07-10";
        String endDate = "2025-07-19";
        String description = "Petrecere de vara";
        dbManager.addBlockedPeriod(startDate, endDate, description);
        System.out.println("Perioada blocată a fost adăugată: " + startDate + " - " + endDate + " | " + description);
        
        // 3. Verificăm că perioada blocată a fost adăugată corect
        List<String> periodsAfterAdd = dbManager.getBlockedPeriods();
        assertEquals(1, periodsAfterAdd.size(), "Ar trebui să existe o perioadă blocată.");
        assertTrue(periodsAfterAdd.stream().anyMatch(p -> p.contains(startDate) && p.contains(endDate) && p.contains(description)), "Perioada blocată nu a fost adăugată corect.");
        System.out.println("Perioada blocată a fost adăugată corect.");
        
        // 4. Vizualizăm perioadele blocate
        System.out.println("Vizualizare perioade blocate:");
        periodsAfterAdd.forEach(p -> System.out.println(p));

        // 5. Ștergem perioada blocată pe baza ID-ului (presupunem că ID-ul este 1)
        int delId = 1;
        dbManager.deleteBlockedPeriod(delId);
        System.out.println("Perioada blocată cu ID " + delId + " a fost ștearsă.");

        // 6. Verificăm că perioada blocată a fost ștearsă
        List<String> periodsAfterDelete = dbManager.getBlockedPeriods();
        assertTrue(periodsAfterDelete.isEmpty(), "Ar trebui să nu mai existe perioade blocate după ștergere.");
        System.out.println("Perioada blocată a fost ștearsă cu succes.");
        
        System.out.println("Testul pentru gestionare perioade in care nu se accepta zile libere a fost finalizat cu succes.");
    }
}
