package com.example.telegrambot.model;

public class Violation {

    private String time;
    private String location;
    private String violation;
    private String status;
    private String plateNumber;

    public Violation(String time, String location, String violation, String status, String plateNumber) {
        this.time = time;
        this.location = location;
        this.violation = violation;
        this.status = status;
        this.plateNumber = plateNumber;
    }

    public String getTime() { return time; }
    public String getLocation() { return location; }
    public String getViolation() { return violation; }
    public String getStatus() { return status; }

    public String getPlateNumber() {
        return plateNumber;
    }
}
