package resources;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import model.Employee;
import model.Visitor;
import model.visit.Visit;
import resources.response.VisitWithVisitor;
import resources.response.VisitWithVisitorWithEmployee;
import service.BadgeManager;
import service.SessionManager;
import service.UtilService;
import service.VisitManager;

import java.net.URI;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static service.SessionManager.NAME_COOKIE_SESSION;

@Path("util")
public class UtilResource {

    @Inject
    Engine quteEngine;

    private final VisitManager visitManager;
    private final SessionManager sessionManager;
    private final UtilService utilService;
    private final BadgeManager badgeManager;

    public UtilResource(VisitManager visitManager, SessionManager sessionManager, UtilService utilService, BadgeManager badgeManager) {
        this.visitManager = visitManager;
        this.sessionManager = sessionManager;
        this.utilService = utilService;
        this.badgeManager = badgeManager;
    }

    /**
     * This function return a list of visits filtered by a date
     * @param sessionId Session id created after login
     * @param inputDate Date to filter on
     * @param templateName Page name that call this function
     * @return Response with a list of visits
     */
    @Path("/filter-visits/{template}")
    @POST
    @Produces(MediaType.TEXT_HTML)
    public Response filterVisits(
            @CookieParam(NAME_COOKIE_SESSION) String sessionId,
            @FormParam("date") LocalDate inputDate,
            @PathParam("template") String templateName
    ){
        Template template = quteEngine.getTemplate(templateName);

        if (sessionId != null) {
            List<Visit> visits;
            Employee employee = sessionManager.getEmployee(sessionId);

            if (employee != null) {
                if (employee.getDepartment().equals("Reception")) {
                    if (inputDate == null) {
                        visits = visitManager.getVisitsByDate(LocalDate.now());
                    } else {
                        visits = visitManager.getVisitsByDate(inputDate);
                    }

                    visits.sort(Comparator.comparing(Visit::getDate));

                    List<Visitor> visitors = visitManager.getVisitors(visits);
                    List<Employee> employees = visitManager.getEmployees(visits);

                    List<VisitWithVisitorWithEmployee> visitWithVisitorsWithEmployees = utilService.getVisitWithGuestsWithEmployee(visitors, visits, employees);

                    Map<String, Integer> badgeStats = badgeManager.getBadgeStats();

                    return Response.ok(template.data(
                            "employee", employee,
                            "visitWithVisitorWithEmployees", visitWithVisitorsWithEmployees,
                            "successMessage", "Visite trovate",
                            "errorMessage", null,
                            "badgeStats", badgeStats
                    )).build();
                } else {
                    if (inputDate == null) {
                        visits = visitManager.filterVisitsByEmployeeId(visitManager.getVisitsFromFile(), employee.getId());
                    } else {
                        visits = visitManager.filterVisitsByEmployeeId(visitManager.getVisitsByDate(inputDate), employee.getId());
                    }
                }
                visits.sort(Comparator.comparing(Visit::getDate));

                List<Visitor> visitors = visitManager.getVisitors(visits);

                List<VisitWithVisitor> visitWithVisitors = utilService.getVisitWithVisitor(visitors, visits);


                return Response.ok(template.data(
                        "employee", employee,
                        "visitWithVisitor", visitWithVisitors,
                        "successMessage", "Visite trovate",
                        "errorMessage", null
                )).build();

            }
            return Response.seeOther(URI.create("/")).build();
        }
        return Response.seeOther(URI.create("/")).build();
    }
}
