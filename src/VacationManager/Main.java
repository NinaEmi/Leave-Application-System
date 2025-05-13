package VacationManager;

import java.util.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class Main {
	public static void main(String[] args) throws SQLException {
		Scanner scanner = new Scanner(System.in);
		DatabaseManager dbManager = new DatabaseManager();

		while (true) {
			System.out.println("\nBine ai venit in sistemul de management al concediilor!");
			System.out.println("1. Autentificare ca Angajat");
			System.out.println("2. Autentificare ca Manager");
			System.out.println("3. Autentificare ca Administrator");
			System.out.println("4. Iesire");

			int opt = 0;

			while (true) {
				System.out.print("Alege optiunea: ");

				try {
					opt = Integer.parseInt(scanner.nextLine());
					if (opt < 1 || opt > 4) {
						System.out.println("Te rog alege o optiune valida (1-4).");
						continue;
					}
					break;
				} catch (NumberFormatException e) {
					System.out.println("Te rog introdu un numar valid!");
				}
			}

			if (opt == 4) {
				System.out.println("La revedere!");
				break;
			}

			int id = 0;
			while (true) {
				System.out.print("Introdu ID: ");
				try {
					id = Integer.parseInt(scanner.nextLine());
					break;
				} catch (NumberFormatException e) {
					System.out.println("ID invalid. Introdu un numar valid.");
				}
			}

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

	private static void handleAdminMenu(Scanner scanner, DatabaseManager dbManager) throws SQLException {
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
				 LocalDate startDate = null;
				    LocalDate endDate = null;

				    // Validare dată de început
				    while (true) {
				        System.out.print("Data început (YYYY-MM-DD): ");
				        String startInput = scanner.nextLine();
				        try {
				            startDate = LocalDate.parse(startInput);
				            break;
				        } catch (DateTimeParseException e) {
				            System.out.println("Format invalid. Te rog să introduci data în formatul YYYY-MM-DD.");
				        }
				    }

				    // Validare dată de sfârșit
				    while (true) {
				        System.out.print("Data sfârșit (YYYY-MM-DD): ");
				        String endInput = scanner.nextLine();
				        try {
				            endDate = LocalDate.parse(endInput);
				            if (endDate.isBefore(startDate)) {
				                System.out.println("Data de sfârșit nu poate fi înainte de data de început.");
				            } else {
				                break;
				            }
				        } catch (DateTimeParseException e) {
				            System.out.println("Format invalid. Te rog să introduci data în formatul YYYY-MM-DD.");
				        }
				    }

				    // Apelăm metoda
				    List<String> employees = dbManager.getEmployeesOnVacationDuring(startDate.toString(), endDate.toString());

				    if (employees.isEmpty()) {
				        System.out.println("Nimeni nu este în concediu în acea perioadă.");
				    } else {
				        System.out.println("Angajații în concediu:");
				        employees.forEach(System.out::println);
				    }

				    break;
			case 2:
				Map<String, Integer> report = dbManager.getRemainingVacationDaysForAllEmployees();
				System.out.println();
				System.out.println("Zile ramase:");
				report.forEach((name, days) -> System.out.println(name + ": " + days));
				break;
			case 3:
				System.out.println();
				int maxAllowed = dbManager.returnMaxValue();
				System.out.println("Numărul maxim de angajați care își pot lua concediu simultan este: " + maxAllowed);
				System.out.print("Introdu noua limita maxima: ");
				int newLimit = Integer.parseInt(scanner.nextLine());
				dbManager.updateSetting("maxTeamOnLeave", newLimit);
				System.out.println("Regula actualizata.");
				break;
			case 4:
				System.out.println();
				System.out.println("1. Vezi perioade blocate");
				System.out.println("2. Adauga perioada blocata");
				System.out.println("3. Sterge perioada blocata");
				System.out.print("Alege optiunea: ");
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
					System.out.println("Perioada blocata adaugata.");
					break;
				case 3:
					System.out.print("ID perioada de sters: ");
					int delId = Integer.parseInt(scanner.nextLine());
					dbManager.deleteBlockedPeriod(delId);
					System.out.println("Perioada blocata stearsa.");
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
				while (true) {
					try {
						LocalDate today = LocalDate.now();

						System.out.print("Data inceput concediu (YYYY-MM-DD): ");
						String start = scanner.nextLine();
						LocalDate startDate = LocalDate.parse(start);

						System.out.print("Data sfarsit concediu (YYYY-MM-DD): ");
						String end = scanner.nextLine();
						LocalDate endDate = LocalDate.parse(end);

						if (endDate.isBefore(startDate)) {
							System.out.println("Data de sfarsit nu poate fi inainte de data de inceput.");
							continue;
						}

						if (startDate.isBefore(today)) {
							System.out.println("Nu se pot face cereri pentru o perioada care incepe in trecut.");
							continue;
						}

						int requestedDays = (int) (endDate.toEpochDay() - startDate.toEpochDay() + 1);
						int availableDays = dbManager.getEmployeeVacationDays(employee.getId());

						if (requestedDays > availableDays) {
							System.out.println("Nu ai suficiente zile de concediu disponibile.");
							continue;
						}

						if (dbManager.hasOverlappingRequest(employee.getId(), start, end)) {
							System.out.println("Exista deja o cerere de concediu pentru aceasta perioada.");
							continue;
						}

						System.out.print("Este concediu medical? (da/nu): ");
						String isMedical = scanner.nextLine();
						String reason;
						RequestStatus status;

						if (isMedical.equalsIgnoreCase("da")) {
							reason = "Concediu medical";
							status = RequestStatus.APPROVED;
							System.out.println(
									"Cerere aprobata automat. Te rugam sa aduci adeverinta medicala la manager.");
						} else {
							int teamLimit = dbManager.getSetting("maxTeamOnLeave", 2);
							int currentTeamOnLeave = dbManager.countTeamMembersOnVacation(employee.getTeam(), start,
									end);

							if (currentTeamOnLeave >= teamLimit) {
								System.out.println(
										"Limita pe echipa a fost atinsa. Cererea va fi PENDING pentru aprobare de catre manager.");
								status = RequestStatus.PENDING;
							} else {
								status = RequestStatus.PENDING;
								System.out.println("Cererea a fost inregistrata si asteapta aprobarea managerului.");
							}

							System.out.print("Introdu motivul concediului: ");
							reason = scanner.nextLine();
						}

						dbManager.addVacationRequest(
								new VacationRequest(0, employee.getId(), start, end, reason, status));
						System.out.println("Cerere trimisa cu succes.");
						break;

					} catch (DateTimeParseException e) {
						System.out.println("Formatul datei este invalid. Foloseste formatul YYYY-MM-DD.");
					} catch (Exception e) {
						System.out.println("A aparut o eroare: " + e.getMessage());
					}
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
			    try {
			    	System.out.println("⚠️ Perioade blocate (angajatii nu isi pot lua liber in aceasta perioada, se accepta doar in perioade exceptionale)!");
			    	List<String> periods = dbManager.getBlockedPeriods();
					if (periods.isEmpty()) {
						System.out.println("Nu exista perioade blocate.");
					} else {
						periods.forEach(System.out::println);
					}
					
					System.out.println();
					
					// Afișăm cererile deja aprobate pentru echipă
			        List<VacationRequest> approvedRequestsList = dbManager.getApprovedRequestsForManager(manager.getTeam());
			        System.out.println("Lista cererilor deja aprobate:");
			        if (approvedRequestsList.isEmpty()) {
			            System.out.println("Nu există cereri aprobate în acest moment.");
			        } else {
			            for (VacationRequest approvedRequest : approvedRequestsList) {
			                System.out.println(approvedRequest);
			            }
			        }

			        System.out.println(); // Linie liberă

			        // Obținem cererile pending
			        List<VacationRequest> pendingRequests = dbManager.getPendingRequestsForManager(manager.getTeam());
			        if (pendingRequests.isEmpty()) {
			            System.out.println("Nu există cereri PENDING pentru echipa ta.");
			        } else {
			            int maxAllowed = dbManager.returnMaxValue(); // Luăm limita o singură dată
			            for (VacationRequest request : pendingRequests) {
			                System.out.println("Cerere în așteptare:");
			                System.out.println(request);
			                System.out.println("⚠️ ️️Numărul maxim de angajați care își pot lua concediu simultan este: " + maxAllowed);

			                String decision;
			                do {
			                    System.out.print("Aprobi cererea ID " + request.getId() + "? (da/nu): ");
			                    decision = scanner.nextLine().trim().toLowerCase();
			                } while (!decision.equals("da") && !decision.equals("nu"));

			                if (decision.equals("da")) {
			                    dbManager.updateRequestStatusWithDeduction(request, RequestStatus.APPROVED);
			                    System.out.println("✅ Cererea a fost aprobată.");
			                } else {
			                    dbManager.updateRequestStatusWithDeduction(request, RequestStatus.REJECTED);
			                    System.out.println("❌ Cererea a fost respinsă.");
			                }

			                System.out.println(); // Linie liberă între cereri
			            }
			        }
			    } catch (SQLException e) {
			        System.out.println("Eroare la accesarea bazei de date:");
			        e.printStackTrace();
			    }
			    break;

			case 2:
				int reportOption = 0;

				// Citire opțiune raport cu validare
				while (true) {
					System.out.println("1. Vezi angajatii in concediu intr-o perioada");
					System.out.println("2. Vezi zilele de concediu ramase pentru toti angajatii");
					System.out.print("Alege optiunea: ");
					try {
						reportOption = Integer.parseInt(scanner.nextLine());
						if (reportOption != 1 && reportOption != 2) {
							System.out.println("Te rog alege 1 sau 2.");
							continue;
						}
						break;
					} catch (NumberFormatException e) {
						System.out.println("Introdu un numar valid (1 sau 2).");
					}
				}

				if (reportOption == 1) {
					System.out.print("Data inceput (YYYY-MM-DD): ");
					String start = scanner.nextLine();
					System.out.print("Data sfarsit (YYYY-MM-DD): ");
					String end = scanner.nextLine();

					// Opțional: validare format dată
					if (!isValidDate(start) || !isValidDate(end)) {
						System.out.println("⚠️ Formatul datei este invalid. Foloseste formatul YYYY-MM-DD.");
						break;
					}

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
				}
				break;

			default:
				System.out.println("Optiune invalida.");
			}
		}
	}

	private static boolean isValidDate(String dateStr) {
		try {
			LocalDate.parse(dateStr); // implicit format ISO (YYYY-MM-DD)
			return true;
		} catch (DateTimeParseException e) {
			return false;
		}
	}

}
