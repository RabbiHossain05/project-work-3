package resources.utility;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CredentialValidator {

    public boolean isValid(String input) {
        boolean valid;

        valid = input != null && !input.isEmpty();

        return valid;
    }
}
