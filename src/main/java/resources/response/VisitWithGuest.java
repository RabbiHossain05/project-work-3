package resources.response;

import model.Visitor;
import model.visit.Visit;

public class VisitWithGuest {
    private final Visit visit;
    private final Visitor visitor;

    public VisitWithGuest(Visit visit, Visitor visitor) {
        this.visit = visit;
        this.visitor = visitor;
    }

    public Visit getVisit() {
        return visit;
    }

    public Visitor getGuest() {
        return visitor;
    }
}

