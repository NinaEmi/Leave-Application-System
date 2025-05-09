package VacationManager;

public class VacationRequest {
    private int id;
    private int employeeId;
    private String startDate;
    private String endDate;
    private String reason;
    private RequestStatus status;

    public VacationRequest(int id, int employeeId, String startDate, String endDate, String reason, RequestStatus status) {
        this.id = id;
        this.employeeId = employeeId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = status;
    }

    public int getId() { return id; }
    public int getEmployeeId() { return employeeId; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getReason() { return reason; }
    public RequestStatus getStatus() { return status; }

    @Override
    public String toString() {
        return "Cerere ID: " + id + ", Angajat ID: " + employeeId + ", Perioada: " + startDate + " - " + endDate +
                ", Motiv: " + reason + ", Status: " + status;
    }
}
