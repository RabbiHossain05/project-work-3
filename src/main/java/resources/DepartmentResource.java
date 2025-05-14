package resources;

import io.quarkus.qute.Template;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import resources.response.VisitWithGuest;
import service.*;
import model.*;
import model.visit.*;

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
    private final BadgeManager badgeManager;
    private final UtilService utilService;

    public DepartmentResource(Template addVisit, Template addVisitor, Template department, SessionManager sessionManager, VisitorManager visitorManager, EmployeeManager employeeManager, VisitManager visitManager, BadgeManager badgeManager, UtilService utilService) {
        this.addVisit = addVisit;
        this.addVisitor = addVisitor;
        this.department = department;
        this.sessionManager = sessionManager;
        this.visitorManager = visitorManager;
        this.employeeManager = employeeManager;
        this.visitManager = visitManager;
        this.badgeManager = badgeManager;
        this.utilService = utilService;
    }


    /**
     * Render to department home page
     * @param sessionId Session id created after login
     * @return Response that render to the page
     */
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

                    List<VisitWithGuest> visitWithGuests = utilService.getVisitWithGuests(visitors, filteredVisits);
                    return Response.ok(department.data(
                            "employee", employee,
                            "visitWithGuest", visitWithGuests,
                            "successMessage", null,
                            "errorMessage", null
                    )).build();
                }
            }
        }
        return Response.seeOther(URI.create("/")).build();
    }

    /**
     * Render to add visitor page
     * @param sessionId Session id created after login
     * @return Response that render to the page
     */
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
                            "employee", employee,
                            "successMessage", null,
                            "errorMessage", null
                            )).build();
                }
            }
        }
        return Response.seeOther(URI.create("/")).build();
    }


    /**
     * Function that add a visitor
     * @param sessionId Session id created after login
     * @param firstName First name of the new visitor
     * @param lastName Last name of the new visitor
     * @param email Email of the new visitor
     * @param phone Phone number of the new visitor
     * @param company Company of the new visitor (optional)
     * @return Response that render department page if there isn't errors, add visitor page otherwise
     */
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

                    String errorMessage = utilService.checkAddVisitorData(firstName, lastName, email, phone);

                    if (errorMessage.isEmpty()){
                        String newId = "" + visitorManager.getNewId();
                        Visitor visitor = new Visitor(newId, firstName, lastName, email, phone, company);

                        if (!visitorManager.isVisitorAlreadyExisting(visitor)) {
                            errorMessage += "Esiste già un ospite con questa email\n";

                            return Response.ok(addVisitor.data(
                                    "employee", employee,
                                    "errorMessage", errorMessage,
                                    "successMessage", null
                            )).build();
                        }

                        visitorManager.saveVisitor(visitor);

                        List<Visit> visits = visitManager.filterVisitsByEmployeeId(visitManager.getVisitsFromFile(), employee.getId());
                        List<VisitWithGuest> visitWithGuests = utilService.getVisitWithGuests(visitManager.getVisitors(visits), visits);

                        return Response.ok(department.data(
                                "employee", employee,
                                "visitWithGuest", visitWithGuests,
                                "successMessage", "Visitatore aggiunto con successo",
                                "errorMessage", null
                        )).build();
                    }
                    else{
                        return Response.ok(addVisitor.data(
                                "employee", employee,
                                "errorMessage", errorMessage,
                                "successMessage", null
                        )).build();
                    }

                }
            }
        }
        return Response.seeOther(URI.create("/")).build();
    }


    /**
     * Render to add visitor page
     * @param sessionId Session id created after loginSession id created after login
     * @return Response that render to the page
     */
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
                            "employees", employeeManager.getEmployeesExcludingReception(),
                            "successMessage", null,
                            "errorMessage", null
                    )).build();
                }
            }
        }
        return Response.seeOther(URI.create("/")).build();
    }


    /**
     * Function that add a visit
     * @param sessionId Session id created after login
     * @param date Date of the new visit
     * @param expectedStart Expected start of the new visit
     * @param expectedEnd Expected end of the new visit
     * @param visitorId Visitor id of the new visit
     * @param employeeId Employee id of the new visit
     * @return Response that render department page if there isn't errors, add visit page otherwise
     */
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

                    String errorMessage = utilService.checkAddVisitData(date, expectedStart, expectedEnd, visitorId, employeeId);

                    List<Visitor> visitors = visitorManager.getVisitorsFromFile();
                    if(errorMessage.isEmpty()) {


                        List<Visit> visitsOfDate = visitManager.getVisitsByDate(date);
                        int countOverlapVisits = 0;

                        for (Visit visit : visitsOfDate) {
                            if (visit.getExpectedStartingHour().isBefore(expectedEnd) && visit.getExpectedEndingHour().isAfter(expectedStart)) {
                                countOverlapVisits++;
                            }
                        }

                        if (countOverlapVisits == badgeManager.countBadges()) {
                            errorMessage += "Non ci sono più badge disponibili\n";

                            return Response.ok(addVisit.data(
                                    "employee", employee,
                                    "visitors", visitors,
                                    "employees", employeeManager.getEmployeesExcludingReception(),
                                    "successMessage", null,
                                    "errorMessage", errorMessage
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

                            return Response.seeOther(URI.create("/department")).build();
                        } else {
                            return Response.ok(addVisit.data(
                                    "employee", employee,
                                    "visitors", visitors,
                                    "employees", employeeManager.getEmployeesExcludingReception(),
                                    "successMessage", null,
                                    "errorMessage", "Errore nell'inserimento della visita"
                            )).build();
                        }
                    }
                    return Response.ok(addVisit.data(
                            "employee", employee,
                            "visitors", visitors,
                            "employees", employeeManager.getEmployeesExcludingReception(),
                            "successMessage", null,
                            "errorMessage", errorMessage
                    )).build();
                }
            }
        }
        return Response.seeOther(URI.create("/")).build();
    }


    /**
     * Function that delete a visit
     * @param sessionId Session id created after login
     * @param visitId Visit id of the visit to delete
     * @return Response that render department page
     */
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

                    return Response.seeOther(URI.create("/department")).build();
                }
            }
        }
        return Response.seeOther(URI.create("/")).build();
    }

}
