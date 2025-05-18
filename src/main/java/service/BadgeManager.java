package service;

import model.Badge;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class BadgeManager {
    private static final String FILE_PATH = "data/badges.csv";
    private static final String[] HEADER = {"code", "is_available"};

    private static final CSVFormat CSV_FORMAT_READ = CSVFormat.Builder.create()
            .setHeader(HEADER)
            .setSkipHeaderRecord(true)
            .get();

    private static final CSVFormat CSV_FORMAT_WRITE = CSVFormat.Builder.create()
            .setHeader(HEADER)
            .get();

    public List<Badge> getAllBadges() {
        List<Badge> badges = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(Paths.get(FILE_PATH), StandardCharsets.UTF_8);
             CSVParser parser = CSVParser.parse(reader, CSV_FORMAT_READ)) {
            for (CSVRecord record : parser) {
                String code = record.get("code");
                boolean available = "1".equals(record.get("is_available"));
                badges.add(new Badge(code, available));
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return badges;
    }

    public void updateBadges(List<Badge> badges) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(FILE_PATH), StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSV_FORMAT_WRITE)) {
            for (Badge badge : badges) {
                printer.printRecord(badge.getCode(), badge.isAvailable() ? "1" : "0");
            }
            printer.flush();
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public String assignFirstAvailableBadge() {
        List<Badge> badges = getAllBadges();
        for (Badge badge : badges) {
            if (badge.isAvailable()) {
                badge.setAvailability(false);
                updateBadges(badges);
                return badge.getCode();
            }
        }
        return null;
    }

    public void releaseBadge(String badgeCode) {
        List<Badge> badges = getAllBadges();
        for (Badge badge : badges) {
            if (badge.getCode().equals(badgeCode)) {
                badge.setAvailability(true);
                break;
            }
        }
        updateBadges(badges);
    }

    public int countAvailableBadges() {
        int availableBadges = 0;
        List<Badge> badges = getAllBadges();
        for (Badge badge : badges) {
            if (badge.isAvailable()) {
                availableBadges++;
            }
        }
        return availableBadges;
    }

    public Map<String, Integer> getBadgeStats() {
        int availableBadges = countAvailableBadges();
        return Map.of("available", availableBadges, "total", getAllBadges().size());
    }
}
