package resources;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import model.Employee;
import resources.utility.CredentialValidator;
import service.EmployeeManager;
import service.SessionManager;

import java.net.URI;

@Path("/")
public class LoginResource {

    private final Template login;
    private final CredentialValidator credentialValidator;
    private final EmployeeManager employeeManager;
    private final SessionManager sessionManager;

    public LoginResource(Template login, CredentialValidator credentialValidator, EmployeeManager employeeManager, SessionManager sessionManager) {
        this.login = login;
        this.credentialValidator = credentialValidator;
        this.employeeManager = employeeManager;
        this.sessionManager = sessionManager;
    }

    @GET
    public Response showLogin(){

        return Response.ok(login.data("errorMessage", null)).build();
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    public Response checkLogin(
            @FormParam("email") String email,
            @FormParam("password") String password
    )
    {
        String errorMessage = "";

        if (!credentialValidator.isValid(email)){
            errorMessage += "Il campo email è vuoto\n";
        }

        if (!credentialValidator.isValid(password)){
            errorMessage += "Il campo password è vuoto\n";
        }

        if (!errorMessage.isEmpty()){
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(login.data("errorMessage", errorMessage))
                    .build();
        }

        Employee employee = employeeManager.getByCredentials(email, password);

        if (employee != null) {
            NewCookie sessionCookie = sessionManager.createSession(email);

            if(employee.getDepartment().equals("Reception")) {
                return Response
                        .seeOther(URI.create("/reception"))
                        .cookie(sessionCookie)
                        .build();
            }

            return Response
                    .seeOther(URI.create("/department"))
                    .cookie(sessionCookie)
                    .build();
        }
        else {
            errorMessage = "Email o password errate";
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(login.data("errorMessage", errorMessage))
                    .build();
        }
    }
}
