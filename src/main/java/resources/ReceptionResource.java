package resources;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import io.quarkus.qute.Template;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import resources.response.VisitWithVisitorWithEmployee;
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
    private final UtilService utilService;

    public ReceptionResource(Template reception, SessionManager sessionManager, VisitManager visitManager,
                             FormValidator formValidator, VisitorManager visitorManager, EmployeeManager employeeManager,
                             BadgeManager badgeManager, UtilService utilService) {
        this.reception = reception;
        this.sessionManager = sessionManager;
        this.visitManager = visitManager;
        this.formValidator = formValidator;
        this.visitorManager = visitorManager;
        this.employeeManager = employeeManager;
        this.badgeManager = badgeManager;
        this.utilService = utilService;
    }


    @GET
    public Response showReceptionHome(@CookieParam(NAME_COOKIE_SESSION) String sessionId) {
        if (sessionId != null) {
            Employee employee = sessionManager.getEmployee(sessionId);

            if (employee == null) {
                return Response.seeOther(URI.create("/")).build();
            }
            else {
                if(employee.getDepartment().equals("Reception")) {
                    List<VisitWithVisitorWithEmployee> visitWithVisitorWithEmployees = getVisitWithVisitorWithEmployees();
                    Map<String, Integer> badgeStats = badgeManager.getBadgeStats();
                    return Response.ok(reception.data(
                            "employee", employee,
                            "visitWithVisitorWithEmployees", visitWithVisitorWithEmployees,
                            "successMessage", null,
                            "errorMessage", null,
                            "badgeStats", badgeStats
                    )).build();
                }
            }
        }
        return Response.seeOther(URI.create("/")).build();
    }


    @Path("/assign-badge")
    @POST
    public Response assignBadge(
            @FormParam("visit-id") String visitId,
            @CookieParam(NAME_COOKIE_SESSION) String sessionId) {

        if (sessionId != null) {
            Employee employee = sessionManager.getEmployee(sessionId);

            if (employee == null) {
                return Response.seeOther(URI.create("/")).build();
            }
            else {
                if (employee.getDepartment().equals("Reception")) {
                    String responseMessage = visitManager.assignBadge(visitId);
                    if (!responseMessage.equals("Successo")) {

                        List<VisitWithVisitorWithEmployee> visitWithVisitorWithEmployees = getVisitWithVisitorWithEmployees();
                        Map<String, Integer> badgeStats = badgeManager.getBadgeStats();
                        return Response.ok(reception.data(
                                "employee", employee,
                                "visitWithVisitorWithEmployees", visitWithVisitorWithEmployees,
                                "successMessage", null,
                                "errorMessage", responseMessage,
                                "badgeStats", badgeStats
                        )).build();
                    }
                    return Response.seeOther(URI.create("/reception")).build();
                }
            }
        }
        return Response.seeOther(URI.create("/")).build();
    }


    @Path("/close-visit")
    @POST
    public Response closeVisit(@FormParam("visit-id") String visitId,
                               @CookieParam(NAME_COOKIE_SESSION) String sessionId) {

        if (sessionId != null) {
            Employee employee = sessionManager.getEmployee(sessionId);

            if (employee == null) {
                return Response.seeOther(URI.create("/")).build();
            }
            else {
                if (employee.getDepartment().equals("Reception")) {


                    String responseMessage = visitManager.endVisit(visitId);
                    if (!responseMessage.equals("Successo")) {
                        List<VisitWithVisitorWithEmployee> visitWithVisitorWithEmployees = getVisitWithVisitorWithEmployees();
                        Map<String, Integer> badgeStats = badgeManager.getBadgeStats();
                        return Response.ok(reception.data(
                                "employee", employee,
                                "visitWithVisitorWithEmployees", visitWithVisitorWithEmployees,
                                "successMessage", null,
                                "errorMessage", responseMessage,
                                "badgeStats", badgeStats
                        )).build();
                    }
                    return Response.seeOther(URI.create("/reception")).build();
                }
            }
        }
        return Response.seeOther(URI.create("/")).build();
    }


    private List<VisitWithVisitorWithEmployee> getVisitWithVisitorWithEmployees() {
        LocalDate today = LocalDate.now();
        List<Visit> visits = visitManager.getVisitsByDate(today);
        visits.sort(Comparator.comparing(Visit::getDate));

        List<Visitor> visitors = visitManager.getVisitors(visits);
        List<Employee> employees = visitManager.getEmployees(visits);

        List<VisitWithVisitorWithEmployee> visitWithVisitorWithEmployees = utilService.getVisitWithGuestsWithEmployee(visitors, visits, employees);
        return visitWithVisitorWithEmployees;
    }

}
