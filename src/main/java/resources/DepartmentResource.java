package resources;

import io.quarkus.qute.Template;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import resources.response.VisitWithGuest;
import service.*;
import model.*;
import model.visit.*;
import resources.utility.FormValidator;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static service.SessionManager.NAME_COOKIE_SESSION;

@Path("/department")
public class DepartmentResource {

    private final Template addVisit;
    private final Template addVisitor;
    private final Template department;
    private final SessionManager sessionManager;
    private final VisitorManager visitorManager;
    private final EmployeeManager employeeManager;
    private final VisitManager visitManager;
    private final FormValidator formValidator;
    private final BadgeManager badgeManager;

    public DepartmentResource(Template addVisit, Template addVisitor, Template department, SessionManager sessionManager, VisitorManager visitorManager, EmployeeManager employeeManager, VisitManager visitManager, FormValidator formValidator, BadgeManager badgeManager) {
        this.addVisit = addVisit;
        this.addVisitor = addVisitor;
        this.department = department;
        this.sessionManager = sessionManager;
        this.visitorManager = visitorManager;
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
                if(!employee.getDepartment().equals("Reception")) {
                    List<Visit> filteredVisits = visitManager.filterVisitsByEmployeeId(visitManager.getVisitsFromFile(), employee.getId());
                    filteredVisits.sort(Comparator.comparing(Visit::getDate));

                    List<Visitor> visitors = visitManager.getVisitors(filteredVisits);

                    List<VisitWithGuest> visitWithGuests = getVisitWithGuests(visitors, filteredVisits);
                    return Response.ok(department.data(
                            "employee", employee,
                            "visitWithGuest", visitWithGuests
                    )).build();
                }
            }
        }
        return Response.seeOther(URI.create("/")).build();
    }

    @GET
    @Path("/add-visitor")
    public Response showAddVisitor(@CookieParam(NAME_COOKIE_SESSION) String sessionId) {
        if (sessionId != null) {
            Employee employee = sessionManager.getEmployee(sessionId);
            if (employee == null) {
                return Response.seeOther(URI.create("/")).build();
            }
            else {
                if (!employee.getDepartment().equals("Reception")) {
                    return Response.ok(addVisitor.data(
                                    "employee", employee))
                            .build();
                }
            }
        }
        return Response.seeOther(URI.create("/")).build();
    }


    @POST
    @Path("/add-visitor")
    public Response addVisitor(
            @CookieParam(NAME_COOKIE_SESSION) String sessionId,
            @FormParam("first-name") String firstName,
            @FormParam("last-name") String lastName,
            @FormParam("email") String email,
            @FormParam("phone") String phone,
            @FormParam("company") String company
    ){
        if (sessionId != null) {

            Employee employee = sessionManager.getEmployee(sessionId);

            if (employee == null) {
                return Response.seeOther(URI.create("/")).build();
            }
            else {
                if (!employee.getDepartment().equals("Reception")) {
                    String errorMessage = null;

                    if (!formValidator.checkStringNotNullOrEmpty(firstName)) {
                        errorMessage = "Nome non valido";
                    }

                    if (errorMessage == null && !formValidator.checkStringNotNullOrEmpty(lastName)) {
                        errorMessage = "Cognome non valido";
                    }

                    if (errorMessage == null && !formValidator.checkStringNotNullOrEmpty(email)) {
                        errorMessage = "Email non valida";
                    }

                    if (errorMessage == null && !formValidator.isEmailValid(email)) {
                        errorMessage = "Email deve contenere una @";
                    }

                    phone = formValidator.checkPhoneNumber(phone);
                    if (errorMessage == null && (!formValidator.checkStringNotNullOrEmpty(phone))) {
                        errorMessage = "Numero di telefono non valido";
                    }

                    String newId = "" + visitorManager.getNewId();
                    Visitor visitor = new Visitor(newId, firstName, lastName, email, phone, company);

                    if (errorMessage == null && !visitorManager.isVisitorAlreadyExisting(visitor)) {
                        errorMessage = "Esiste già un ospite con questa email";
                    }

                    if (errorMessage != null) {
                        return Response.ok(addVisitor.data(
                                "employee", employee,
                                "errorMessage", errorMessage
                        )).build();
                    }

                    visitorManager.saveVisitor(visitor);

                    List<Visit> visits = visitManager.filterVisitsByEmployeeId(visitManager.getVisitsFromFile(), employee.getId());
                    List<VisitWithGuest> visitWithGuests = getVisitWithGuests(visitManager.getVisitors(visits), visits);

                    return Response.ok(department.data(
                            "employee", employee,
                            "visitWithGuest", visitWithGuests
                    )).build();
                }
            }
        }
        return Response.seeOther(URI.create("/")).build();
    }


    @GET
    @Path("/add-visit")
    public Response showFormAddVisit(
            @CookieParam(NAME_COOKIE_SESSION) String sessionId
    ){
        if (sessionId != null) {
            List<Visitor> visitors = visitorManager.getVisitorsFromFile();

            Employee employee = sessionManager.getEmployee(sessionId);
            if (employee == null) {
                return Response.seeOther(URI.create("/")).build();
            }
            else {
                if (!employee.getDepartment().equals("Reception")) {

                    return Response.ok(addVisit.data(
                            "employee", employee,
                            "visitors", visitors,
                            "employees", employeeManager.getEmployeesExcludingReception()
                    )).build();
                }
            }
        }
        return Response.seeOther(URI.create("/")).build();
    }


    @POST
    @Path("/add-visit")
    public Response addVisit(
            @CookieParam(NAME_COOKIE_SESSION) String sessionId,
            @FormParam("date") LocalDate date,
            @FormParam("start-time") LocalTime expectedStart,
            @FormParam("end-time") LocalTime expectedEnd,
            @FormParam("visitor") String visitorId,
            @FormParam("employee") String employeeId
    ) {
        if (sessionId != null) {

            Employee employee = sessionManager.getEmployee(sessionId);

            if (employee == null) {
                return Response.seeOther(URI.create("/")).build();
            }
            else {
                if (!employee.getDepartment().equals("Reception")) {


                    List<Visitor> visitors = visitorManager.getVisitorsFromFile();
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

                    if (errorMessage == null && !formValidator.checkStringNotNullOrEmpty(visitorId)) {
                        errorMessage = "Ospite non valido";
                    }

                    if (errorMessage == null && !formValidator.checkStringNotNullOrEmpty(employeeId)) {
                        errorMessage = "Dipendente non valido";
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


                    if (errorMessage != null) {
                        return Response.ok(addVisit.data(
                                "employee", employee,
                                "visitors", visitors,
                                "employees", employeeManager.getEmployeesExcludingReception()
                        )).build();
                    }


                    String newId = "" + visitManager.getNewId();
                    LocalTime actualStart = LocalTime.ofSecondOfDay(0);
                    LocalTime actualEnd = LocalTime.ofSecondOfDay(0);
                    String expectedDuration = String.valueOf(Duration.between(expectedStart, expectedEnd).toMinutes());
                    VisitStatus visitStatus = VisitStatus.NON_INIZIATA;


                    Visit visit = new Visit(newId, date, expectedStart, actualStart, expectedEnd, actualEnd, expectedDuration, visitStatus, visitorId, employeeId, null);
                    boolean status = visitManager.saveVisit(visit);

                    if (status) {

                        List<Visit> visits = visitManager.filterVisitsByEmployeeId(visitManager.getVisitsFromFile(), employee.getId());
                        List<VisitWithGuest> visitWithGuests = getVisitWithGuests(visitManager.getVisitors(visits), visits);

                        return Response.seeOther(URI.create("/department")).build();
                    } else {
                        return Response.ok(addVisit.data(
                                "employee", employee,
                                "visitors", visitors,
                                "employees", employeeManager.getEmployeesExcludingReception()
                        )).build();
                    }
                }
            }
        }
        return Response.seeOther(URI.create("/")).build();
    }


    @POST
    @Path("/delete-visit")
    public Response deleteVisit(
            @CookieParam(NAME_COOKIE_SESSION) String sessionId,
            @FormParam("visit-id") String visitId
    ) {
        if (sessionId != null) {
            Employee employee = sessionManager.getEmployee(sessionId);

            if (employee == null) {
                return Response.seeOther(URI.create("/")).build();
            }
            else {
                if (!employee.getDepartment().equals("Reception")) {
                    Visit visit = visitManager.getVisitById(visitId);
                    List<Visit> filteredVisits = visitManager.getFilteredVisits(visit);
                    visitManager.overwriteVisits(filteredVisits);

                    List<Visit> visits = visitManager.getUnstartedVisits();
                    List<Visit> visitsByEmployeeId = visitManager.filterVisitsByEmployeeId(visits, employee.getId());
                    filteredVisits.sort(Comparator.comparing(Visit::getDate));

                    return Response.seeOther(URI.create("/department")).build();
                }
            }
        }
        return Response.seeOther(URI.create("/")).build();
    }

    private static List<VisitWithGuest> getVisitWithGuests(List<Visitor> visitors, List<Visit> filteredVisits) {
        Map<String, List<Visitor>> guestMultiMap = visitors.stream()
                .collect(Collectors.groupingBy(Visitor::getId));

        List<VisitWithGuest> visitWithGuests = new ArrayList<>();
        for (Visit visit : filteredVisits) {
            List<Visitor> matchingVisitors = guestMultiMap.get(visit.getGuestId());
            if (matchingVisitors != null) {
                for (Visitor visitor : matchingVisitors) {
                    visitWithGuests.add(new VisitWithGuest(visit, visitor));
                }
            }
        }
        return visitWithGuests;
    }

}
