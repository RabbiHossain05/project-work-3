package service;

import jakarta.enterprise.context.ApplicationScoped;
import model.Employee;
import model.Visitor;
import model.visit.Visit;
import model.visit.VisitStatus;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class VisitManager {

    static final String FILE_PATH = "data/visits.csv";
    private final VisitorManager visitorManager;
    private final EmployeeManager employeeManager;
    private final BadgeManager badgeManager;

    public VisitManager(VisitorManager visitorManager, EmployeeManager employeeManager, BadgeManager badgeManager) {
        this.visitorManager = visitorManager;
        this.employeeManager = employeeManager;
        this.badgeManager = badgeManager;
    }

    public List<Visit> getVisitsFromFile() {

        List<Visit> visits = new ArrayList<>();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        try (Reader reader = new FileReader(FILE_PATH); CSVParser csvParser = new CSVParser(reader, CSVFormat.EXCEL.withHeader());) {
            for (CSVRecord record : csvParser) {
                String id = record.get("id");
                LocalDate date = LocalDate.parse(record.get("date").trim(), dateFormatter);
                LocalTime expectedStartingHour = LocalTime.parse(record.get("expected_starting_hour"), timeFormatter);
                LocalTime actualStartingHour = LocalTime.parse(record.get("actual_starting_hour"), timeFormatter);
                LocalTime expectedEndingHour = LocalTime.parse(record.get("expected_ending_hour"), timeFormatter);
                LocalTime actualEndingHour = LocalTime.parse(record.get("actual_ending_time"), timeFormatter);
                String expectedDuration = record.get("expected_duration");
                VisitStatus visitStatus = VisitStatus.valueOf(record.get("visit_status"));
                String guestId = record.get("guest_id");
                String employeeId = record.get("employee_id");
                String badgeCode = record.get("badge_code");

                Visit visit = new Visit(id, date, expectedStartingHour, actualStartingHour, expectedEndingHour, actualEndingHour, expectedDuration, visitStatus, guestId, employeeId, badgeCode);
                visits.add(visit);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return visits;
    }


    public boolean saveVisit(Visit visit) {

        if (!checkDouble(visit)) {
            try (Writer writer = new FileWriter(FILE_PATH, true); CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL)) {
                csvPrinter.printRecord(
                        visit.getId(),
                        visit.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        visit.getExpectedStartingHour().format(DateTimeFormatter.ofPattern("HH:mm")),
                        visit.getActualStartingHour().format(DateTimeFormatter.ofPattern("HH:mm")),
                        visit.getExpectedEndingHour().format(DateTimeFormatter.ofPattern("HH:mm")),
                        visit.getActualEndingHour().format(DateTimeFormatter.ofPattern("HH:mm")),
                        visit.getExpectedDuration(),
                        visit.getStatus().name(),
                        visit.getVisitorId(),
                        visit.getEmployeeId(),
                        visit.getBadgeCode()
                );
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    public List<Visit> getVisitsByDate(LocalDate date) {
        List<Visit> visits = getVisitsFromFile();
        List<Visit> filteredVisits = new ArrayList<>();

        for (Visit visit : visits) {
            if (visit.getDate().equals(date)) {
                filteredVisits.add(visit);
            }
        }

        return filteredVisits;
    }


    public List<Visit> filterVisitsByEmployeeId(List<Visit> visits, String employeeId) {
        List<Visit> filteredVisits = new ArrayList<>();

        for (Visit visit : visits) {
            if (visit.getEmployeeId().equals(employeeId)) {
                filteredVisits.add(visit);
            }
        }

        return filteredVisits;
    }


    public List<Visit> getUnfinishedVisits() {
        List<Visit> visits = getVisitsFromFile();

        List<Visit> filteredVisits = new ArrayList<>();

        for (Visit visit : visits) {
            if (!(visit.getActualStartingHour() != LocalTime.parse("00:00")
                    && visit.getActualEndingHour() == LocalTime.parse("00:00"))) {
                continue;
            }
            filteredVisits.add(visit);
        }

        return filteredVisits;
    }


    public List<Visit> getUnstartedVisits() {
        List<Visit> visits = getVisitsFromFile();
        List<Visit> filteredVisits = new ArrayList<>();

        for (Visit visit : visits) {
            if (!visit.getBadgeCode().isEmpty()) {
                continue;
            } else if (!(visit.getActualStartingHour() == LocalTime.parse("00:00")
                    && visit.getActualEndingHour() == LocalTime.parse("00:00"))) {
                continue;
            }
            filteredVisits.add(visit);
        }

        return filteredVisits;
    }


    public List<Visit> getUnstartedVisitsByDate(LocalDate date) {
        List<Visit> visits = getVisitsFromFile();
        List<Visit> filteredVisits = new ArrayList<>();

        for (Visit visit : visits) {
            if (visit.getDate().equals(date)) {
                if (!visit.getBadgeCode().isEmpty()) {
                    continue;
                } else if (!(visit.getActualStartingHour() == LocalTime.parse("00:00")
                        && visit.getActualEndingHour() == LocalTime.parse("00:00"))) {
                    continue;
                }
                filteredVisits.add(visit);
            }
        }

        return filteredVisits;
    }


    public Visit getVisitById(String inputVisitId) {
        List<Visit> visits = getVisitsFromFile();

        for (Visit visit : visits) {
            if (visit.getId().equals(inputVisitId)) {
                return visit;
            }
        }
        return null;
    }


    public List<Visit> getFilteredVisits(Visit visit) {
        List<Visit> visits = getVisitsFromFile();
        List<Visit> filteredVisits = new ArrayList<>();

        for (Visit visit1 : visits) {
            if (!visit1.getId().equals(visit.getId())) {
                filteredVisits.add(visit1);
            }
        }

        return filteredVisits;
    }


    public boolean overwriteVisits(List<Visit> visits) {

        try (FileWriter writer = new FileWriter(FILE_PATH); CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL.withHeader("id", "date", "expected_starting_hour", "actual_starting_hour", "expected_ending_hour", "actual_ending_time", "expected_duration", "visit_status", "guest_id", "employee_id", "badge_code"))) {
            for (Visit newVisit : visits) {
                csvPrinter.printRecord(
                        newVisit.getId(),
                        newVisit.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        newVisit.getExpectedStartingHour().format(DateTimeFormatter.ofPattern("HH:mm")),
                        newVisit.getActualStartingHour().format(DateTimeFormatter.ofPattern("HH:mm")),
                        newVisit.getExpectedEndingHour().format(DateTimeFormatter.ofPattern("HH:mm")),
                        newVisit.getActualEndingHour().format(DateTimeFormatter.ofPattern("HH:mm")),
                        newVisit.getExpectedDuration(),
                        newVisit.getStatus().name(),
                        newVisit.getVisitorId(),
                        newVisit.getEmployeeId(),
                        newVisit.getBadgeCode()
                );
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }


    public int getNewId() {
        List<Visit> visits = getVisitsFromFile();

        if (visits.isEmpty()) {
            return 1;
        } else {
            return Integer.parseInt(visits.getLast().getId()) + 1;
        }
    }


    public boolean checkDouble(Visit visit) {
        List<Visit> visits = getVisitsFromFile();

        for (Visit v : visits) {
            if (v.getDate().equals(visit.getDate()) && v.getExpectedStartingHour().equals(visit.getExpectedStartingHour())
                    && v.getExpectedEndingHour().equals(visit.getExpectedEndingHour()) && v.getVisitorId().equals(visit.getVisitorId())
                    && v.getEmployeeId().equals(visit.getEmployeeId())) {

                return true;
            }
        }
        return false;
    }


    public List<Visit> changeIdsInSurnames(List<Visit> visits, VisitorManager visitorManager, EmployeeManager employeeManager) {

        List<Visit> changedVisits = new ArrayList<>();

        for (Visit visit : visits) {
            Visitor visitor = visitorManager.getVisitorById(visit.getVisitorId());
            Employee employee = employeeManager.getEmployeeById(visit.getEmployeeId());

            visit.setVisitorId(visitor.getLastName());
            visit.setEmployeeId(employee.getLastName());

            changedVisits.add(visit);
        }
        return changedVisits;
    }


    public List<Visitor> getVisitors(List<Visit> visits) {
        List<Visitor> visitors = visitorManager.getVisitorsFromFile();
        List<Visitor> filteredVisitors = new ArrayList<>();

        for (Visit visit : visits) {
            String visitorId = visit.getVisitorId();

            for (Visitor visitor : visitors) {
                if (visitor.getId().equals(visitorId)) {
                    filteredVisitors.add(visitor);
                }
            }
        }

        return filteredVisitors;
    }

    public List<Employee> getEmployees(List<Visit> visits) {
        List<Employee> employees = employeeManager.getEmployeesFromFile();
        List<Employee> filteredEmployees = new ArrayList<>();

        for (Visit visit : visits) {
            String employeeId = visit.getEmployeeId();

            for (Employee employee : employees) {
                if (employee.getId().equals(employeeId)) {
                    filteredEmployees.add(employee);
                }
            }
        }

        return filteredEmployees;
    }


    public String assignBadge(String visitId) {
        Visit visit = getVisitById(visitId);
        if (visit == null) {
            return "Visita non valida.";
        }
        if (visit.getStatus() != VisitStatus.NON_INIZIATA) {
            return "Il badge può essere assegnato solo a visite non iniziate.";
        }
        LocalTime now = LocalDateTime.now().toLocalTime();
        LocalTime startTime = visit.getExpectedStartingHour();
        if (now.isBefore(startTime)) {
            return "Non puoi assegnare il badge prima dell'orario di inizio della visita.";
        }
        String badgeCode = badgeManager.assignFirstAvailableBadge();
        if (badgeCode == null) {
            return "Nessun badge disponibile.";
        }
        visit.setBadgeCode(badgeCode);
        visit.setStatus(VisitStatus.INIZIATA);
        updateVisit(visit);
        return "Successo";
    }

    public void updateVisit(Visit updatedVisit) {
        List<Visit> visits = getVisitsFromFile();
        for (int i = 0; i < visits.size(); i++) {
            if (visits.get(i).getId().equals(updatedVisit.getId())) {
                visits.set(i, updatedVisit);
                break;
            }
        }
        overwriteVisits(visits);
    }

    public String endVisit(String visitId) {
        Visit visit = getVisitById(visitId);
        if (visit == null) {
            return "Visita non valida.";
        }
        if (visit.getStatus() != VisitStatus.INIZIATA) {
            return "Solo le visite iniziate possono essere terminate.";
        }
        visit.setStatus(VisitStatus.FINITA);
        visit.setActualEndingHour(LocalTime.now());
        updateVisit(visit);
        if (visit.getBadgeCode() != null) {
            badgeManager.releaseBadge(visit.getBadgeCode());
        }
        return "Successo";
    }

}
