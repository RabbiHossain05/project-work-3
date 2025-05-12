package resources;

import io.quarkus.qute.Template;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import service.*;
import model.*;
import model.visit.*;
import resources.utility.FormValidator;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import static service.SessionManager.NAME_COOKIE_SESSION;

@Path("/department")
public class DepartmentResource {

    private final Template homeEmployee;
    private final SessionManager sessionManager;
    private final GuestManager guestManager;
    private final EmployeeManager employeeManager;
    private final VisitManager visitManager;
    private final FormValidator formValidator;
    private final BadgeManager badgeManager;

    public DepartmentResource(Template homeEmployee, SessionManager sessionManager, GuestManager guestManager, EmployeeManager employeeManager, VisitManager visitManager, FormValidator formValidator, BadgeManager badgeManager) {
        this.homeEmployee = homeEmployee;
        this.sessionManager = sessionManager;
        this.guestManager = guestManager;
        this.employeeManager = employeeManager;
        this.visitManager = visitManager;
        this.formValidator = formValidator;
        this.badgeManager = badgeManager;
    }


    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response showDepartmentHome(
            @CookieParam(NAME_COOKIE_SESSION) String sessionId
    ) {

        if (sessionId != null) {
            Employee employee = sessionManager.getEmployee(sessionId);

            if (employee == null) {
                return Response.seeOther(URI.create("/")).build();
            }
            else {
                List<Visit> visits = visitManager.getVisitsFromFile();
                List<Visit> filteredVisits = visitManager.filterVisitsByEmployeeId(visits, employee.getId());
                filteredVisits.sort(Comparator.comparing(Visit::getDate));
                return Response.ok(homeEmployee.data(
                        "employee", employee,
                        "type", "homePage",
                        "visits", visitManager.changeIdsInSurnames(filteredVisits, guestManager, employeeManager)
                )).build();
            }
        }
        return Response.seeOther(URI.create("/")).build();
    }


    @GET
    @Path("/add-guest")
    public Response showFormAddGuest(@CookieParam(NAME_COOKIE_SESSION) String sessionId) {
        if (sessionId != null) {
            Employee employee = sessionManager.getEmployee(sessionId);
            return Response.ok(homeEmployee.data(
                "employee", employee,
                "type", "addGuest",
                "errorMessage", null,
                "successMessage", null))
                    .build();
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
            @FormParam("role") String role,
            @FormParam("company") String company
    ){
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
            if (errorMessage == null && (!formValidator.checkStringNotNullOrEmpty(phoneNumber))) {
                errorMessage = "Numero di telefono non valido";
            }

            if (errorMessage == null && !formValidator.checkStringNotNullOrEmpty(role)) {
                errorMessage = "Ruolo non valido";
            }

            if (errorMessage == null && !formValidator.checkStringNotNullOrEmpty(company)) {
                errorMessage = "Azienda non valida";
            }

            String newId = "" + guestManager.getNewId();
            Guest guest = new Guest(newId, name, surname, email, phoneNumber, role, company);

            if (errorMessage == null && !guestManager.isGuestAlreadyExisting(guest)) {
                errorMessage = "Esiste già un ospite con questa email";
            }

            Employee employee = sessionManager.getEmployee(sessionId);
            if (errorMessage != null) {
                return Response.ok(homeEmployee.data(
                        "employee", employee,
                        "errorMessage", errorMessage,
                        "successMessage", null,
                        "type", "addGuest"
                )).build();
            }


            guestManager.saveGuest(guest);

            String successMessage = "Ospite aggiunto";

            return Response.ok(homeEmployee.data(
                    "employee", employee,
                    "errorMessage", null,
                    "successMessage", successMessage,
                    "type", "addGuest"
            )).build();

        }
        return Response.seeOther(URI.create("/")).build();
    }


    @GET
    @Path("/add-visit")
    public Response showFormAddVisit(
            @CookieParam(NAME_COOKIE_SESSION) String sessionId
    ){
        if (sessionId != null) {
        List<Guest> guests = guestManager.getGuestsFromFile();

            Employee employee = sessionManager.getEmployee(sessionId);
            return Response.ok(homeEmployee.data(
                    "employee",employee,
                    "guests", guests,
                    "type", "addVisit",
                    "errorMessage", null,
                    "successMessage", null
            )).build();
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
            @FormParam("guest") String guestId
    ) {
        if (sessionId != null) {
            List<Guest> guests = guestManager.getGuestsFromFile();
            String errorMessage = null;

            if (!formValidator.checkDateNotNull(date)) {
                errorMessage = "La data non può essere vuota";
            }

            if (errorMessage == null && !formValidator.checkDateIsAfterToday(date)) {
                errorMessage = "La visita deve essere aggiunta almeno un giorno in anticipo";
            }

            if (errorMessage == null && !formValidator.checkTimeNotNull(expectedStart)) {
                errorMessage = "L'ora di inizio non può essere vuota";
            }

            if (errorMessage == null && !formValidator.checkTimeNotNull(expectedEnd)) {
                errorMessage = "L'ora di fine non può essere vuota";
            }

            if (errorMessage == null && formValidator.checkStartingTimeIsAfterEndingTime(expectedStart, expectedEnd)) {
                errorMessage = "L'ora di inizio non deve essere successiva a quella di fine";
            }

            if (errorMessage == null && !formValidator.checkStringNotNullOrEmpty(guestId)) {
                errorMessage = "Ospite non valido";
            }

            List<Visit> visitsOfDate = visitManager.getVisitsByDate(date);
            int countOverlapVisits = 0;

            for (Visit visit : visitsOfDate) {
                if (visit.getExpectedStartingHour().isBefore(expectedEnd) && visit.getExpectedEndingHour().isAfter(expectedStart)) {
                    countOverlapVisits++;
                }
            }

            if (countOverlapVisits == badgeManager.countBadges()) {
                errorMessage = "Non ci sono più badge disponibili";
            }
            Employee employee = sessionManager.getEmployee(sessionId);

            if (errorMessage != null) {
                return Response.ok(homeEmployee.data(
                        "employee", employee,
                        "errorMessage", errorMessage,
                        "successMessage", null,
                        "guests", guests,
                        "type", "addVisit"
                )).build();
            }


            String newId = "" + visitManager.getNewId();
            LocalTime actualStart = LocalTime.ofSecondOfDay(0);
            LocalTime actualEnd = LocalTime.ofSecondOfDay(0);
            VisitStatus visitStatus = VisitStatus.NON_INIZIATA;

            String employeeId = employee.getId();

            Visit visit = new Visit(newId, date, expectedStart, actualStart, expectedEnd, actualEnd, visitStatus, guestId, employeeId, null);
            boolean status = visitManager.saveVisit(visit);

            if (status) {
                String successMessage = "Visita aggiunta";

                return Response.ok(homeEmployee.data(
                        "employee", employee,
                        "successMessage", successMessage,
                        "errorMessage", null,
                        "guests", guests,
                        "type", "addVisit"
                )).build();
            } else {
                return Response.ok(homeEmployee.data(
                        "employee", employee,
                        "successMessage", null,
                        "errorMessage", "Esiste già un altra visita con questi dati",
                        "guests", guests,
                        "type", "addVisit"
                )).build();
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
            List<Visit> filteredVisits = visitManager.filterVisitsByEmployeeId(visits, employee.getId());
            filteredVisits.sort(Comparator.comparing(Visit::getDate));

            return Response.ok(homeEmployee.data(
                    "employee", employee,
                    "visits", visitManager.changeIdsInSurnames(filteredVisits, guestManager, employeeManager),
                    "type", "deleteVisit",
                    "errorMessage", null,
                    "successMessage", null
            )).build();
        }
        return Response.seeOther(URI.create("/")).build();
    }


    @POST
    @Path("/delete-visit")
    public Response deleteVisit(
            @CookieParam(NAME_COOKIE_SESSION) String sessionId,
            @FormParam("visitId") String visitId
    ) {
        if (sessionId != null) {
            Employee employee = sessionManager.getEmployee(sessionId);

            Visit visit = visitManager.getVisitById(visitId);
            List<Visit> filteredVisits = visitManager.getFilteredVisits(visit);
            visitManager.overwriteVisits(filteredVisits);

            List<Visit> visits = visitManager.getUnstartedVisits();
            List<Visit> visitsByEmployeeId = visitManager.filterVisitsByEmployeeId(visits, employee.getId());
            filteredVisits.sort(Comparator.comparing(Visit::getDate));

            return Response.ok(homeEmployee.data(
                    "employee", employee,
                    "visits", visitManager.changeIdsInSurnames(visitsByEmployeeId, guestManager, employeeManager),
                    "type", "deleteVisit",
                    "errorMessage", null,
                    "successMessage", null
            )).build();
        }
        return Response.seeOther(URI.create("/")).build();
    }
}
