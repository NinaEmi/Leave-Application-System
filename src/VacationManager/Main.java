package VacationManager;

import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        DatabaseManager dbManager = new DatabaseManager();

        while (true) {
            System.out.println("\nBine ai venit in sistemul de management al concediilor!");
            System.out.println("1. Autentificare ca Angajat");
            System.out.println("2. Autentificare ca Manager");
            System.out.println("3. Autentificare ca Administrator");
            System.out.println("4. Iesire");
            System.out.print("Alege optiunea: ");
            int opt = scanner.nextInt();
            scanner.nextLine();

            if (opt == 4) {
                System.out.println("La revedere!");
                break;
            }

            System.out.print("Introdu ID: ");
            int id = scanner.nextInt();
            scanner.nextLine();

            System.out.print("Introdu parola: ");
            String password = scanner.nextLine();

            Employee user = dbManager.authenticateEmployee(id, password);

            if (user == null) {
                System.out.println("ID sau parola gresita.");
                continue;
            }

            switch (opt) {
                case 1:
                    if (!user.getRole().equalsIgnoreCase("Angajat")) {
                        System.out.println("Nu esti inregistrat ca angajat.");
                        continue;
                    }
                    handleEmployeeMenu(scanner, dbManager, user);
                    break;
                case 2:
                    if (!user.getRole().equalsIgnoreCase("Manager")) {
                        System.out.println("Nu esti inregistrat ca manager.");
                        continue;
                    }
                    handleManagerMenu(scanner, dbManager, user);
                    break;
                case 3:
                    if (!user.getRole().equalsIgnoreCase("Administrator")) {
                        System.out.println("Nu esti inregistrat ca administrator.");
                        continue;
                    }
                    handleAdminMenu(scanner, dbManager);
                    break;
                default:
                    System.out.println("Optiune invalida.");
            }
        }

        scanner.close();
        dbManager.closeConnection();
        System.out.println("Conexiunea la baza de date a fost inchisa. La revedere!");
    }

    private static void handleAdminMenu(Scanner scanner, DatabaseManager dbManager) {
        while (true) {
            System.out.println("\nMeniu Administrator:");
            System.out.println("1. Vezi angajatii in concediu intr-o perioada");
            System.out.println("2. Vezi zilele de concediu ramase pentru toti angajatii");
            System.out.println("3. Modifica limita de colegi in concediu simultan");
            System.out.println("4. Gestioneaza perioade in care NU se permite concediu");
            System.out.println("5. Deconectare");
            System.out.print("Alege optiunea: ");
            int option = Integer.parseInt(scanner.nextLine());

            switch (option) {
                case 1:
                    System.out.print("Data inceput (YYYY-MM-DD): ");
                    String start = scanner.nextLine();
                    System.out.print("Data sfarsit (YYYY-MM-DD): ");
                    String end = scanner.nextLine();
                    List<String> employees = dbManager.getEmployeesOnVacationDuring(start, end);
                    if (employees.isEmpty()) {
                        System.out.println("Nimeni nu este in concediu in acea perioada.");
                    } else {
                        System.out.println("Angajatii in concediu:");
                        employees.forEach(System.out::println);
                    }
                    break;
                case 2:
                    Map<String, Integer> report = dbManager.getRemainingVacationDaysForAllEmployees();
                    System.out.println("Zile ramase:");
                    report.forEach((name, days) -> System.out.println(name + ": " + days));
                    break;
                case 3:
                    System.out.print("Introdu noua limita maxima: ");
                    int newLimit = Integer.parseInt(scanner.nextLine());
                    dbManager.updateSetting("maxTeamOnLeave", newLimit);
                    System.out.println("✅ Regula actualizata.");
                    break;
                case 4:
                    System.out.println("1. Vezi perioade blocate");
                    System.out.println("2. Adauga perioada blocata");
                    System.out.println("3. Sterge perioada blocata");
                    int subOpt = Integer.parseInt(scanner.nextLine());
                    switch (subOpt) {
                        case 1:
                            List<String> periods = dbManager.getBlockedPeriods();
                            if (periods.isEmpty()) {
                                System.out.println("Nu exista perioade blocate.");
                            } else {
                                periods.forEach(System.out::println);
                            }
                            break;
                        case 2:
                            System.out.print("Start (YYYY-MM-DD): ");
                            String bStart = scanner.nextLine();
                            System.out.print("End (YYYY-MM-DD): ");
                            String bEnd = scanner.nextLine();
                            System.out.print("Descriere: ");
                            String desc = scanner.nextLine();
                            dbManager.addBlockedPeriod(bStart, bEnd, desc);
                            System.out.println("✅ Perioada blocata adaugata.");
                            break;
                        case 3:
                            System.out.print("ID perioada de sters: ");
                            int delId = Integer.parseInt(scanner.nextLine());
                            dbManager.deleteBlockedPeriod(delId);
                            System.out.println("✅ Perioada blocata stearsa.");
                            break;
                        default:
                            System.out.println("Optiune invalida.");
                    }
                    break;
                case 5:
                    return;
                default:
                    System.out.println("Optiune invalida.");
            }
        }
    }


    private static void handleEmployeeMenu(Scanner scanner, DatabaseManager dbManager, Employee employee) {
        while (true) {
            System.out.println("\nMeniu Angajat:");
            System.out.println("1. Trimite cerere de concediu");
            System.out.println("2. Vizualizeaza cererile tale");
            System.out.println("3. Deconectare");
            System.out.print("Alege optiunea: ");
            int option = scanner.nextInt();
            scanner.nextLine();

            if (option == 3) {
                return;
            }

            switch (option) {
                case 1:
                    try {
                        System.out.print("Data inceput concediu (YYYY-MM-DD): ");
                        String start = scanner.nextLine();
                        LocalDate startDate = LocalDate.parse(start);

                        System.out.print("Data sfarsit concediu (YYYY-MM-DD): ");
                        String end = scanner.nextLine();
                        LocalDate endDate = LocalDate.parse(end);

                        if (endDate.isBefore(startDate)) {
                            System.out.println("⚠️ Data de sfarsit nu poate fi inainte de data de inceput!");
                            break;
                        }

                        int requestedDays = (int) (endDate.toEpochDay() - startDate.toEpochDay() + 1);
                        int availableDays = dbManager.getEmployeeVacationDays(employee.getId());

                        if (requestedDays > availableDays) {
                            System.out.println("⚠️ Nu ai suficiente zile de concediu disponibile!");
                            break;
                        }

                        if (dbManager.hasOverlappingRequest(employee.getId(), start, end)) {
                            System.out.println("⚠️ Exista deja o cerere de concediu pentru aceasta perioada.");
                            break;
                        }

                        System.out.print("Este concediu medical? (da/nu): ");
                        String isMedical = scanner.nextLine();
                        String reason;
                        RequestStatus status;

                        if (isMedical.equalsIgnoreCase("da")) {
                            reason = "Concediu medical";
                            status = RequestStatus.APPROVED;
                            System.out.println("Cerere aprobata automat. Te rugam sa aduci adeverinta medicala la manager.");
                        } else {
                        	int teamLimit = dbManager.getSetting("maxTeamOnLeave", 2);
                            int currentTeamOnLeave = dbManager.countTeamMembersOnVacation(employee.getTeam(), start, end);

                            if (currentTeamOnLeave >= teamLimit) {
                                System.out.println("Limita pe echipa a fost atinsa. Cererea ta va fi marcata ca PENDING pentru aprobare de catre manager.");
                                status = RequestStatus.PENDING;
                            } else {
                                status = RequestStatus.PENDING;
                                System.out.println("Cererea ta a fost inregistrata si este in asteptarea aprobarii managerului.");
                            }

                            System.out.print("Introdu motivul concediului: ");
                            reason = scanner.nextLine();
                        }

                        dbManager.addVacationRequest(new VacationRequest(0, employee.getId(), start, end, reason, status));
                        System.out.println("Cerere trimisa cu succes!");

                    } catch (DateTimeParseException e) {
                        System.out.println("⚠️ Formatul datei este invalid! Foloseste formatul YYYY-MM-DD.");
                    }
                    break;

                case 2:
                    List<VacationRequest> requests = dbManager.getVacationRequestsForEmployee(employee.getId());
                    if (requests.isEmpty()) {
                        System.out.println("Nu ai cereri inregistrate.");
                    } else {
                        for (VacationRequest req : requests) {
                            System.out.println(req);
                        }
                    }
                    break;

                default:
                    System.out.println("Optiune invalida.");
            }
        }
    }

    private static void handleManagerMenu(Scanner scanner, DatabaseManager dbManager, Employee manager) {
        while (true) {
            System.out.println("\nMeniu Manager:");
            System.out.println("1. Vizualizeaza cereri PENDING");
            System.out.println("2. Rapoarte");
            System.out.println("3. Deconectare");
            System.out.print("Alege optiunea: ");
            int option = scanner.nextInt();
            scanner.nextLine();

            if (option == 3) {
                return;
            }

            switch (option) {
                case 1:
                    List<VacationRequest> requests = dbManager.getPendingRequestsForManager(manager.getTeam());
                    if (requests.isEmpty()) {
                        System.out.println("Nu exista cereri PENDING pentru echipa ta.");
                    } else {
                        for (VacationRequest request : requests) {
                            System.out.println(request);
                            System.out.print("Aprobi cererea ID " + request.getId() + "? (da/nu): ");
                            String decision = scanner.nextLine();
                            if (decision.equalsIgnoreCase("da")) {
                                dbManager.updateRequestStatusWithDeduction(request, RequestStatus.APPROVED);
                                System.out.println("Cererea a fost aprobata.");
                            } else {
                                dbManager.updateRequestStatusWithDeduction(request, RequestStatus.REJECTED);
                                System.out.println("Cererea a fost respinsa.");
                            }
                        }
                    }
                    break;

                case 2:
                    System.out.println("1. Vezi angajatii in concediu intr-o perioada");
                    System.out.println("2. Vezi zilele de concediu ramase pentru toti angajatii");
                    System.out.print("Alege optiunea: ");
                    int reportOption = scanner.nextInt();
                    scanner.nextLine();

                    if (reportOption == 1) {
                        System.out.print("Data inceput (YYYY-MM-DD): ");
                        String start = scanner.nextLine();
                        System.out.print("Data sfarsit (YYYY-MM-DD): ");
                        String end = scanner.nextLine();
                        List<String> employeesOnLeave = dbManager.getEmployeesOnVacationDuring(start, end);
                        if (employeesOnLeave.isEmpty()) {
                            System.out.println("Nu sunt angajati in concediu in aceasta perioada.");
                        } else {
                            System.out.println("Angajatii in concediu:");
                            for (String name : employeesOnLeave) {
                                System.out.println(name);
                            }
                        }
                    } else if (reportOption == 2) {
                        Map<String, Integer> report = dbManager.getRemainingVacationDaysForAllEmployees();
                        System.out.println("Zile de concediu ramase:");
                        for (Map.Entry<String, Integer> entry : report.entrySet()) {
                            System.out.println(entry.getKey() + ": " + entry.getValue() + " zile");
                        }
                    } else {
                        System.out.println("Optiune invalida.");
                    }
                    break;

                default:
                    System.out.println("Optiune invalida.");
            }
        }
    }
}
