package resources.response;

import model.Employee;
import model.Visitor;
import model.visit.Visit;

public class VisitWithVisitorWithEmployee {
    private Visit visit;
    private Visitor visitor;
    private Employee employee;

    public VisitWithVisitorWithEmployee(Visit visit, Visitor visitor, Employee employee) {
        this.visit = visit;
        this.visitor = visitor;
        this.employee = employee;
    }


    public Visit getVisit() {
        return visit;
    }

    public Visitor getVisitor() {
        return visitor;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setVisit(Visit visit) {
        this.visit = visit;
    }

    public void setVisitor(Visitor visitor) {
        this.visitor = visitor;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }
}

