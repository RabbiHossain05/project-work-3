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
import resources.response.VisitWithGuest;
import service.SessionManager;
import service.VisitManager;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static service.SessionManager.NAME_COOKIE_SESSION;

@Path("util")
public class UtilResource {

    @Inject
    Engine quteEngine;

    private final VisitManager visitManager;
    private final SessionManager sessionManager;

    public UtilResource(VisitManager visitManager, SessionManager sessionManager) {
        this.visitManager = visitManager;
        this.sessionManager = sessionManager;
    }

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
                        visits = visitManager.getVisitsFromFile();
                    } else {
                        visits = visitManager.getVisitsByDate(inputDate);
                    }
                } else {
                    if (inputDate == null) {
                        visits = visitManager.filterVisitsByEmployeeId(visitManager.getVisitsFromFile(), employee.getId());
                    } else {
                        visits = visitManager.filterVisitsByEmployeeId(visitManager.getVisitsByDate(inputDate), employee.getId());
                    }
                }

                visits.sort(Comparator.comparing(Visit::getDate));

                List<Visitor> visitors = visitManager.getVisitors(visits);

                Map<String, List<Visitor>> guestMultiMap = visitors.stream()
                        .collect(Collectors.groupingBy(Visitor::getId));

                List<VisitWithGuest> visitWithGuests = new ArrayList<>();
                for (Visit visit : visits) {
                    List<Visitor> matchingVisitors = guestMultiMap.get(visit.getGuestId());
                    if (matchingVisitors != null) {
                        for (Visitor visitor : matchingVisitors) {
                            visitWithGuests.add(new VisitWithGuest(visit, visitor));
                        }
                    }
                }
                return Response.ok(template.data(
                        "employee", employee,
                        "visitWithGuest", visitWithGuests,
                        "date", inputDate)).build();
            }
            return Response.seeOther(URI.create("/")).build();
        }
        return Response.seeOther(URI.create("/")).build();
    }
}
