package VacationManager;

public class Employee {
    private int id;
    private String name;
    private String role;
    private String team;
    private String password;
    private int vacationDays;

    public Employee(int id, String name, String role, String team, String password, int vacationDays) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.team = team;
        this.password = password;
        this.vacationDays = vacationDays;
    }
    

    public int getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getTeam() { return team; }
    public String getPassword() { return password; }
    public int getVacationDays() { return vacationDays; }
    public void setVacationDays(int days) { this.vacationDays = days; }

    @Override
    public String toString() {
        return id + ": " + name + ", Rol: " + role + ", Echipa: " + team + ", Zile concediu: " + vacationDays;
    }
}
