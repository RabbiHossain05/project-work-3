package service;

import jakarta.enterprise.context.ApplicationScoped;
import model.Visitor;
import model.visit.Visit;
import resources.response.VisitWithGuest;
import resources.utility.FormValidator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        else if (!formValidator.checkDateIsAfterToday(date)) {
            errorMessage += "La visita deve essere aggiunta almeno un giorno in anticipo\n";
        }

        if (!formValidator.checkTimeNotNull(expectedStart) || !formValidator.checkTimeNotNull(expectedEnd)) {
            errorMessage += "L'ora di inizio e/o fine non può essere vuota\n";
        }
        else if (formValidator.checkStartingTimeIsAfterEndingTime(expectedStart, expectedEnd)) {
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

    /**
     * Function that create a list of VisitWithGuest, where every object contain one Visit and one Visitor
     * @param visitors List of Visitor to insert in the return list
     * @param visits List of Visit to insert in the return list
     * @return A list of VisitWithGuest
     */
    public List<VisitWithGuest> getVisitWithGuests(List<Visitor> visitors, List<Visit> visits) {
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
        return visitWithGuests;
    }

}
