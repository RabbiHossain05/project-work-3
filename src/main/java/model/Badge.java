package model;

public class Badge {
    private final String code;
    private boolean available;

    public Badge(String code, boolean available) {
        this.code = code;
        this.available = available;
    }

    public String getCode() {
        return code;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailability(boolean available) {
        this.available = available;
    }
}
