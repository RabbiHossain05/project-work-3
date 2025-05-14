package resources;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import io.quarkus.qute.Template;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import service.*;
import static service.SessionManager.NAME_COOKIE_SESSION;
import model.*;
import model.visit.*;
import resources.utility.FormValidator;

@Path("/reception")
public class ReceptionResource {

    private final Template reception;
    private final SessionManager sessionManager;
    private final VisitManager visitManager;
    private final FormValidator formValidator;
    private final VisitorManager visitorManager;
    private final EmployeeManager employeeManager;
    private final BadgeManager badgeManager;

    public ReceptionResource(Template reception, SessionManager sessionManager, VisitManager visitManager,
                             FormValidator formValidator, VisitorManager visitorManager, EmployeeManager employeeManager,
                             BadgeManager badgeManager) {
        this.reception = reception;
        this.sessionManager = sessionManager;
        this.visitManager = visitManager;
        this.formValidator = formValidator;
        this.visitorManager = visitorManager;
        this.employeeManager = employeeManager;
        this.badgeManager = badgeManager;
    }


    @GET
    public Response showReceptionHome(@CookieParam(NAME_COOKIE_SESSION) String sessionId) {
        if (sessionId != null) {
            Employee employee = sessionManager.getEmployee(sessionId);

            if (employee == null) {
                return Response.seeOther(URI.create("/")).build();
            } else {
                if(employee.getDepartment().equals("Reception")) {
                    return Response.ok(reception.instance()).build();
                }
            }
        }
        return Response.seeOther(URI.create("/")).build();
    }


    @Path("/show-visits")
    @GET
    public Response showVisits(@CookieParam(NAME_COOKIE_SESSION) String sessionId) {
        if (sessionId != null) {

            List<Visit> visits = visitManager.getVisitsFromFile();
            visits.sort(Comparator.comparing(Visit::getDate));

            Employee employee = sessionManager.getEmployee(sessionId);
            return Response.ok(reception.data(
                    "employee", employee,
                    "visits", visitManager.changeIdsInSurnames(visits, visitorManager, employeeManager),
                    "type", "showVisits",
                    "date", null)).build();
        }
        return Response.seeOther(URI.create("/")).build();

    }


    @Path("/assign-badge")
    @GET
    public Response showAssignBadge(@CookieParam(NAME_COOKIE_SESSION) String sessionId) {
        if (sessionId != null) {
            List<Visit> unstartedVisits = visitManager.getUnstartedVisitsByDate(LocalDate.now());
            Employee employee = sessionManager.getEmployee(sessionId);

            return Response.ok(reception.data(
                            "type", "assignBadge",
                            "employee", employee,
                            "errorMessage", null,
                            "successMessage", null,
                            "visits", visitManager.changeIdsInSurnames(unstartedVisits, visitorManager, employeeManager)))
                    .build();
        }
        return null;
    }


    @Path("/assign-badge")
    @POST
    public Response assignBadge(
            @FormParam("badge") String badgeCode,
            @FormParam("visitId") String visitId,
            @CookieParam(NAME_COOKIE_SESSION) String sessionId) {

        if (sessionId != null) {
            List<String> badges = badgeManager.getBadgesFromFile();
            String errorMessage = null;

            if (!formValidator.checkStringNotNullOrEmpty(badgeCode)) {
                errorMessage = "L'input del codice è vuoto.";
            }

            boolean badgeStatus = false;
            for (String badge : badges) {
                if (badge.equals(badgeCode)) {
                    badgeStatus = true;
                    break;
                }
            }

            if (errorMessage == null && !badgeStatus) {
                errorMessage = "Questo codice non esiste.";
            }

            List<Visit> unfinishedVisits = visitManager.getUnfinishedVisits();

            for (Visit visit : unfinishedVisits) {
                if (errorMessage == null && visit.getBadgeCode().equals(badgeCode)) {
                    errorMessage = "Questo badge non è disponibile";
                    break;
                }
            }

            Employee employee = sessionManager.getEmployee(sessionId);
            if (errorMessage != null) {
                return Response.ok(reception.data(
                                "employee", employee,
                                "type", "assignBadge",
                                "errorMessage", errorMessage,
                                "successMessage", null,
                                "visits", visitManager.changeIdsInSurnames(
                                        visitManager.getUnstartedVisitsByDate(LocalDate.now()), visitorManager, employeeManager)))
                        .build();
            }

            List<Visit> visits = visitManager.getVisitsFromFile();

            for (Visit visit : visits) {
                if (visit.getId().equals(visitId)) {
                    visit.setBadgeCode(badgeCode);
                    visit.setActualStartingHour(
                            LocalTime.parse(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))));
                    visit.setStatus(VisitStatus.INIZIATA);
                }
            }

            boolean status = visitManager.overwriteVisits(visits);
            if (!status) {
                errorMessage = "Errore nel salvare il badge";
                return Response.ok(reception.data(
                                "employee", employee,
                                "type", "assignBadge",
                                "errorMessage", errorMessage,
                                "successMessage", null,
                                "visits", visitManager.changeIdsInSurnames(
                                        visitManager.getUnstartedVisitsByDate(LocalDate.now()), visitorManager, employeeManager)))
                        .build();
            }

            String successMessage = "Badge assegnato";

            return Response.ok(reception.data(
                            "employee", employee,
                            "type", "assignBadge",
                            "errorMessage", null,
                            "successMessage", successMessage,
                            "visits", visitManager.changeIdsInSurnames(visitManager.getUnstartedVisitsByDate(LocalDate.now()),
                                    visitorManager, employeeManager)))
                    .build();
        }
        return Response.seeOther(URI.create("/")).build();
    }


    @Path("/close-visit")
    @GET
    public Response showUnfinishedVisit(@CookieParam(NAME_COOKIE_SESSION) String sessionId) {
        if (sessionId != null) {
            List<Visit> unfinishedVisits = visitManager.getUnfinishedVisits();

            Employee employee = sessionManager.getEmployee(sessionId);

            return Response.ok(reception.data(
                    "employee", employee,
                    "visits", visitManager.changeIdsInSurnames(unfinishedVisits, visitorManager, employeeManager),
                    "type", "closeVisit")).build();
        }
        return Response.seeOther(URI.create("/")).build();
    }


    @Path("/close-visit")
    @POST
    public Response closeVisit(@FormParam("visitId") String visitId,
                               @CookieParam(NAME_COOKIE_SESSION) String sessionId) {

        if (sessionId != null) {
            List<Visit> visits = visitManager.getVisitsFromFile();

            for (Visit visit : visits) {
                if (visit.getId().equals(visitId)) {
                    visit.setActualEndingHour(
                            LocalTime.parse(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))));
                    visit.setStatus(VisitStatus.FINITA);
                }
            }

            boolean status = visitManager.overwriteVisits(visits);
            if (!status) {
                Employee employee = sessionManager.getEmployee(sessionId);
                return Response.ok(reception.data(
                                "employee", employee,
                                "type", "closeVisit",
                                "errorMessage", "Errore nel concludere la visita",
                                "successMessage", null,
                                "visits", visitManager.changeIdsInSurnames(visitManager.getUnfinishedVisits(), visitorManager,
                                        employeeManager)))
                        .build();
            }
            return Response.seeOther(URI.create("home-reception/close-visit")).build();
        }
        return Response.seeOther(URI.create("/")).build();
    }


    @GET
    @Path("/add-guest")
    public Response showFormAddGuest(@CookieParam(NAME_COOKIE_SESSION) String sessionId) {
        if (sessionId != null) {
            Employee employee = sessionManager.getEmployee(sessionId);
            return Response.ok(reception.data(
                    "employee", employee,
                    "type", "addGuest",
                    "errorMessage", null,
                    "successMessage", null,
                    "visits", null)).build();
        }
        return Response.seeOther(URI.create("/")).build();
    }


    @POST
    @Path("/add-guest")
    public Response addGuest(
            @CookieParam(NAME_COOKIE_SESSION) String sessionId,
            @FormParam("name") String name,
            @FormParam("surname") String surname,
            @FormParam("email") String email,
            @FormParam("phoneNumber") String phoneNumber,
            @FormParam("company") String company) {
        if (sessionId != null) {
            String errorMessage = null;

            if (!formValidator.checkStringNotNullOrEmpty(name)) {
                errorMessage = "Nome non valido";
            }

            if (errorMessage == null && !formValidator.checkStringNotNullOrEmpty(surname)) {
                errorMessage = "Cognome non valido";
            }

            if (errorMessage == null && !formValidator.checkStringNotNullOrEmpty(email)) {
                errorMessage = "Email non valida";
            }

            if (errorMessage == null && !formValidator.isEmailValid(email)) {
                errorMessage = "Email deve contenere una @";
            }

            phoneNumber = formValidator.checkPhoneNumber(phoneNumber);
            if (errorMessage == null
                    && (!formValidator.checkStringNotNullOrEmpty(phoneNumber) || phoneNumber.isEmpty())) {
                errorMessage = "Numero di telefono non valido";
            }

            if (errorMessage == null && !formValidator.checkStringNotNullOrEmpty(company)) {
                errorMessage = "Azienda non valida";
            }

            String newId = "" + visitorManager.getNewId();
            Visitor visitor = new Visitor(newId, name, surname, email, phoneNumber, company);

            if (errorMessage == null && !visitorManager.isVisitorAlreadyExisting(visitor)) {
                errorMessage = "L'ospite è già inserito";
            }

            Employee employee = sessionManager.getEmployee(sessionId);
            if (errorMessage != null) {

                return Response.ok(reception.data(
                        "employee", employee,
                        "type", "addGuest",
                        "errorMessage", errorMessage,
                        "successMessage", null,
                        "visits", null)).build();
            }

            visitorManager.saveVisitor(visitor);
            String successMessage = "Ospite aggiunto";

            return Response.ok(reception.data(
                    "employee", employee,
                    "type", "addGuest",
                    "errorMessage", null,
                    "successMessage", successMessage,
                    "visits", null)).build();
        }
        return Response.seeOther(URI.create("/")).build();
    }


    @GET
    @Path("/add-visit")
    public Response showFormAddVisit(
            @CookieParam(NAME_COOKIE_SESSION) String sessionId) {
        if (sessionId != null) {
            Employee employee = sessionManager.getEmployee(sessionId);
            List<Visitor> visitors = visitorManager.getVisitorsFromFile();
            List<Employee> employees = employeeManager.getEmployeesExcludingReception();

            return Response.ok(reception
                    .data("type", "addVisit")
                    .data("errorMessage", null)
                    .data("successMessage", null)
                    .data("guests", visitors)
                    .data("employees", employees)
                    .data("employee", employee)

            ).build();
        }
        return Response.seeOther(URI.create("/")).build();
    }


    @POST
    @Path("/add-visit")
    public Response addVisit(
            @CookieParam(NAME_COOKIE_SESSION) String sessionId,
            @FormParam("date") LocalDate date,
            @FormParam("expectedStart") LocalTime expectedStart,
            @FormParam("expectedEnd") LocalTime expectedEnd,
            @FormParam("guest") String guestId,
            @FormParam("employee") String employeeId) {
        if (sessionId != null) {
            List<Visitor> visitors = visitorManager.getVisitorsFromFile();
            List<Employee> employees = employeeManager.getEmployeesExcludingReception();
            String errorMessage = null;

            if (!formValidator.checkDateNotNull(date)) {
                errorMessage = "Data non può essere vuota";
            }

            if (errorMessage == null && formValidator.checkDateIsBeforeToday(date)) {
                errorMessage = "La data della visita non può essere precedente ad oggi";
            }

            if (errorMessage == null && !formValidator.checkTimeNotNull(expectedStart)) {
                errorMessage = "L'ora di inizio non può essere vuota";
            }

            if (errorMessage == null && !formValidator.checkTimeNotNull(expectedEnd)) {
                errorMessage = "L'ora di inizio non può essere vuota";
            }

            if (errorMessage == null && formValidator.checkStartingTimeIsAfterEndingTime(expectedStart, expectedEnd)) {
                errorMessage = "L'ora di inizio non deve essere successiva a quella di fine";
            }

            if (errorMessage == null && !formValidator.checkStringNotNullOrEmpty(guestId)) {
                errorMessage = "Ospite non valido";
            }

            if (errorMessage == null && !formValidator.checkStringNotNullOrEmpty(employeeId)) {
                errorMessage = "Dipendente non valido";
            }

            List<Visit> visitsOfDate = visitManager.getVisitsByDate(date);
            int countOverlapVisits = 0;

            for (Visit visit : visitsOfDate) {
                if (visit.getExpectedStartingHour().isBefore(expectedEnd)
                        && visit.getExpectedEndingHour().isAfter(expectedStart)) {
                    countOverlapVisits++;
                }
            }

            if (countOverlapVisits == badgeManager.countBadges()) {
                errorMessage = "Non ci sono più badge disponibili";
            }

            if (errorMessage != null) {
                Employee employee = sessionManager.getEmployee(sessionId);
                return Response.ok(reception
                        .data("type", "addVisit")
                        .data("errorMessage", errorMessage)
                        .data("successMessage", null)
                        .data("guests", visitors)
                        .data("employees", employees)
                        .data("employee", employee)).build();
            }

            String newId = "" + visitManager.getNewId();
            LocalTime actualStart = LocalTime.ofSecondOfDay(0);
            LocalTime actualEnd = LocalTime.ofSecondOfDay(0);
            String expectedDuration = String.valueOf(Duration.between(expectedStart, expectedEnd).toMinutes());
            VisitStatus visitStatus = VisitStatus.NON_INIZIATA;

            Employee employee = sessionManager.getEmployee(sessionId);

            Visit visit = new Visit(newId, date, expectedStart, actualStart, expectedEnd, actualEnd, expectedDuration,
                    visitStatus, guestId, employeeId, null);
            boolean status = visitManager.saveVisit(visit);

            if (status) {
                String successMessage = "Visita aggiunta";

                return Response.ok(reception
                        .data("type", "addVisit")
                        .data("errorMessage", null)
                        .data("successMessage", successMessage)
                        .data("guests", visitors)
                        .data("employees", employees)
                        .data("employee", employee)).build();
            } else {
                return Response.ok(reception
                        .data("type", "addVisit")
                        .data("errorMessage", "Esiste già un altra visita con questi dati")
                        .data("successMessage", null)
                        .data("guests", visitors)
                        .data("employees", employees)
                        .data("employee", employee)).build();
            }
        }
        return Response.seeOther(URI.create("/")).build();
    }


    @GET
    @Path("/delete-visit")
    public Response showDeleteVisit(@CookieParam(NAME_COOKIE_SESSION) String sessionId) {
        if (sessionId != null) {
            Employee employee = sessionManager.getEmployee(sessionId);

            List<Visit> visits = visitManager.getUnstartedVisits();
            visits.sort(Comparator.comparing(Visit::getDate));

            return Response.ok(reception.data(
                    "type", "deleteVisit",
                    "errorMessage", null,
                    "successMessage", null,
                    "visits", visitManager.changeIdsInSurnames(visits, visitorManager, employeeManager),
                    "employee", employee)).build();
        }
        return Response.seeOther(URI.create("/")).build();
    }


    @POST
    @Path("/delete-visit")
    public Response deleteVisit(
            @CookieParam(NAME_COOKIE_SESSION) String sessionId,
            @FormParam("visitId") String visitId) {
        if (sessionId != null) {
            Employee employee = sessionManager.getEmployee(sessionId);

            Visit visit = visitManager.getVisitById(visitId);
            List<Visit> filteredVisits = visitManager.getFilteredVisits(visit);
            visitManager.overwriteVisits(filteredVisits);

            List<Visit> visits = visitManager.getUnstartedVisits();
            filteredVisits.sort(Comparator.comparing(Visit::getDate));

            return Response.ok(reception.data(
                    "employee", employee,
                    "type", "deleteVisit",
                    "errorMessage", null,
                    "successMessage", null,
                    "visits", visitManager.changeIdsInSurnames(visits, visitorManager, employeeManager))).build();
        }
        return Response.seeOther(URI.create("/")).build();
    }
}
