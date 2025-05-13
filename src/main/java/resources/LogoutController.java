package resources;

import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import service.SessionManager;

import java.net.URI;

import static service.SessionManager.NAME_COOKIE_SESSION;

@Path("/logout")
public class LogoutController {

    private final SessionManager sessionManager;

    public LogoutController(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }


    @GET
    public Response processLogout(
            @CookieParam(NAME_COOKIE_SESSION) String sessionId
    ) {
        if (sessionId != null) {
            sessionManager.removeSession(sessionId);

            return Response.seeOther(URI.create("/"))
                    .cookie(new NewCookie.Builder(NAME_COOKIE_SESSION).value(null).build())
                    .build();
        }
        return Response.seeOther(URI.create("/")).build();

    }
}
