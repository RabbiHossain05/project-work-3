package resources.response;

import model.Visitor;
import model.visit.Visit;

public class VisitWithVisitor {
    private final Visit visit;
    private final Visitor visitor;

    public VisitWithVisitor(Visit visit, Visitor visitor) {
        this.visit = visit;
        this.visitor = visitor;
    }

    public Visit getVisit() {
        return visit;
    }

    public Visitor getVisitor() {
        return visitor;
    }
}

