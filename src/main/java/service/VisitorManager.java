package service;

import jakarta.enterprise.context.ApplicationScoped;
import model.Visitor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class VisitorManager {

    final String FILE_PATH = "data/visitors.csv";
    final VisitManager visitManager;

    public VisitorManager(VisitManager visitManager) {
        this.visitManager = visitManager;
    }


    public List<Visitor> getVisitorsFromFile() {
        List<Visitor> visitors = new ArrayList<>();

        try (Reader reader = new FileReader(FILE_PATH); CSVParser csvParser = new CSVParser(reader, CSVFormat.EXCEL.withHeader());) {
            for (CSVRecord record : csvParser) {
                String id = record.get("id");
                String name = record.get("name");
                String surname = record.get("surname");
                String email = record.get("email");
                String phoneNumber = record.get("phone_number");
                String company = record.get("company");

                Visitor visitor = new Visitor(id, name, surname, email, phoneNumber, company);
                visitors.add(visitor);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return visitors;
    }


    public void saveVisitor(Visitor visitor) {

        try (Writer writer = new FileWriter(FILE_PATH, true); CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL)) {
            csvPrinter.printRecord(
                    visitor.getId(),
                    visitor.getFirstName(),
                    visitor.getLastName(),
                    visitor.getEmail(),
                    visitor.getPhoneNumber(),
                    visitor.getCompany()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Visitor getVisitorById(String id) {
        List<Visitor> visitors = getVisitorsFromFile();

        for (Visitor visitor : visitors) {
            if (visitor.getId().equals(id)) {
                return visitor;
            }
        }
        return null;
    }


    public int getNewId() {
        List<Visitor> visitors = getVisitorsFromFile();

        if (visitors.isEmpty()) {
            return 1;
        } else {
            return Integer.parseInt(visitors.getLast().getId()) + 1;
        }
    }


    public boolean isVisitorAlreadyExisting(Visitor inputVisitor) {
        List<Visitor> visitors = getVisitorsFromFile();

        for (Visitor visitor : visitors) {
            if (visitor.getEmail().equals(inputVisitor.getEmail())) {
                return false;
            }
        }
        return true;
    }
}
