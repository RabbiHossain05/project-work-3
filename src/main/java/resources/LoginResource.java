package resources;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
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
    public TemplateInstance showLogin(){
        return login.instance();
    }

    @POST
    public Response checkLogin(
            @FormParam("email") String email,
            @FormParam("password") String password
    )
    {
        String errorMessage = "";

        if (!credentialValidator.isValid(email)){
            errorMessage += "Email is empty\n";
        }

        if (!credentialValidator.isValid(password)){
            errorMessage += "Password is empty\n";
        }

        if (!errorMessage.isEmpty()){
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(login.data("message", errorMessage))
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
            errorMessage = "Username or password are incorrect";
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(login.data("message", errorMessage))
                    .build();
        }
    }
}
