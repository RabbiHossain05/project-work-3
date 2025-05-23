package service;

import jakarta.enterprise.context.ApplicationScoped;
import model.Employee;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class EmployeeManager {

    private final String FILE_PATH = "data/employees.csv";


    public List<Employee> getEmployeesFromFile() {
        List<Employee> employees = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try (Reader reader = new FileReader(FILE_PATH); CSVParser csvParser = new CSVParser(reader, CSVFormat.EXCEL.withHeader());) {
            for (CSVRecord record : csvParser) {
                String id = record.get("id");
                String name = record.get("name");
                String surname = record.get("surname");
                LocalDate dateOfBirth = LocalDate.parse(record.get("date_of_birth"), formatter);
                String phoneNumber = record.get("phone_number");
                String department = record.get("department");
                String email = record.get("email");
                String password = record.get("password");

                Employee employee = new Employee(id, name, surname, dateOfBirth, phoneNumber, department, email, password);
                employees.add(employee);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return employees;
    }


    public void saveEmployee(Employee employee) {
        try (Writer writer = new FileWriter(FILE_PATH, true); CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL.withHeader("id", "name", "surname", "date_of_birth", "department", "email", "password"))) {
            csvPrinter.printRecord(
                    employee.getId(),
                    employee.getFirstName(),
                    employee.getLastName(),
                    employee.getDateOfBirth(),
                    employee.getPhoneNumber(),
                    employee.getDepartment(),
                    employee.getEmail(),
                    employee.getPassword()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Employee getEmployeeById(String id) {
        List<Employee> employees = getEmployeesFromFile();

        for (Employee employee : employees) {
            if (employee.getId().equals(id)) {
                return employee;
            }
        }
        return null;
    }


    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }


    public Employee getByCredentials(String email, String password) {
        List<Employee> employees = getEmployeesFromFile();

        for (Employee employee : employees) {
            if (employee.getEmail().equals(email) && checkPassword(password, employee.getPassword())) {
                return employee;
            }
        }
        return null;
    }


    public List<Employee> getEmployeesExcludingReception() {
        List<Employee> filteredEmployees = new ArrayList<>();

        List<Employee> employees = getEmployeesFromFile();

        for (Employee employee : employees) {
            if (!(employee.getDepartment()).equalsIgnoreCase("reception")) {
                filteredEmployees.add(employee);
            }
        }
        return filteredEmployees;
    }
}
