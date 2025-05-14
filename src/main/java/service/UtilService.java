package service;

import jakarta.enterprise.context.ApplicationScoped;
import resources.utility.FormValidator;

import java.time.LocalDate;
import java.time.LocalTime;

@ApplicationScoped
public class UtilService {

    private final FormValidator formValidator;

    public UtilService(FormValidator formValidator) {
        this.formValidator = formValidator;
    }

    public String checkAddVisitorData(String firstName, String lastName, String email, String phone){

        String errorMessage = "";

        if (!formValidator.checkStringNotNullOrEmpty(firstName)) {
            errorMessage += "Nome non valido\n";
        }

        if (!formValidator.checkStringNotNullOrEmpty(lastName)) {
            errorMessage += "Cognome non valido\n";
        }

        if (!formValidator.checkStringNotNullOrEmpty(email)) {
            errorMessage += "Email non valida\n";
        }

        if (!formValidator.isEmailValid(email)) {
            errorMessage += "Email deve contenere una @\n";
        }

        phone = formValidator.checkPhoneNumber(phone);
        if (!formValidator.checkStringNotNullOrEmpty(phone)) {
            errorMessage += "Numero di telefono non valido";
        }

        return errorMessage;
    }

    public String checkAddVisitData(LocalDate date, LocalTime expectedStart, LocalTime expectedEnd, String visitorId, String employeeId){
        String errorMessage = "";

        if (!formValidator.checkDateNotNull(date)) {
            errorMessage += "La data non può essere vuota\n";
        }

        if (!formValidator.checkDateIsAfterToday(date)) {
            errorMessage += "La visita deve essere aggiunta almeno un giorno in anticipo\n";
        }

        if (!formValidator.checkTimeNotNull(expectedStart)) {
            errorMessage += "L'ora di inizio non può essere vuota\n";
        }

        if (!formValidator.checkTimeNotNull(expectedEnd)) {
            errorMessage += "L'ora di fine non può essere vuota\n";
        }

        if (formValidator.checkStartingTimeIsAfterEndingTime(expectedStart, expectedEnd)) {
            errorMessage += "L'ora di inizio non deve essere successiva a quella di fine\n";
        }

        if (!formValidator.checkStringNotNullOrEmpty(visitorId)) {
            errorMessage += "Ospite non valido\n";
        }

        if (!formValidator.checkStringNotNullOrEmpty(employeeId)) {
            errorMessage += "Dipendente non valido\n";
        }

        return errorMessage;
    }

}
