package com.example.telegrambot.model;

import java.util.List;

public class PhatNguoiResponse {

    private String bienSo;
    private int count;
    private List<Violation> data;

    public PhatNguoiResponse(String bienSo, List<Violation> data) {
        this.bienSo = bienSo;
        this.data = data;
        this.count = data.size();
    }

    public String getBienSo() { return bienSo; }
    public int getCount() { return count; }
    public List<Violation> getData() { return data; }
}