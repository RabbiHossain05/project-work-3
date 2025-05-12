package service;

import jakarta.enterprise.context.ApplicationScoped;
import model.Guest;
import model.visit.Visit;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class GuestManager {

    final String FILE_PATH = "data/guests.csv";
    final VisitManager visitManager;

    public GuestManager(VisitManager visitManager) {
        this.visitManager = visitManager;
    }


    public List<Guest> getGuestsFromFile() {
        List<Guest> guests = new ArrayList<>();

        try (Reader reader = new FileReader(FILE_PATH); CSVParser csvParser = new CSVParser(reader, CSVFormat.EXCEL.withHeader());) {
            for (CSVRecord record : csvParser) {
                String id = record.get("id");
                String name = record.get("name");
                String surname = record.get("surname");
                String email = record.get("email");
                String phoneNumber = record.get("phone_number");
                String role = record.get("role");
                String company = record.get("company");

                Guest guest = new Guest(id, name, surname, email, phoneNumber, role, company);
                guests.add(guest);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return guests;
    }


    public void saveGuest(Guest guest) {

        try (Writer writer = new FileWriter(FILE_PATH, true); CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL)) {
            csvPrinter.printRecord(
                    guest.getId(),
                    guest.getName(),
                    guest.getSurname(),
                    guest.getEmail(),
                    guest.getPhoneNumber(),
                    guest.getRole(),
                    guest.getCompany()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Guest getGuestById(String id) {
        List<Guest> guests = getGuestsFromFile();

        for (Guest guest : guests) {
            if (guest.getId().equals(id)) {
                return guest;
            }
        }
        return null;
    }


    public int getNewId() {
        List<Guest> guests = getGuestsFromFile();

        if (guests.isEmpty()) {
            return 1;
        } else {
            return Integer.parseInt(guests.getLast().getId()) + 1;
        }
    }


    public boolean isGuestAlreadyExisting(Guest inputGuest) {
        List<Guest> guests = getGuestsFromFile();

        for (Guest guest : guests) {
            if (guest.getEmail().equals(inputGuest.getEmail())) {
                return false;
            }
        }
        return true;
    }
}
