package com.example.telegrambot.controller;

import com.example.telegrambot.model.PhatNguoiResponse;
import com.example.telegrambot.service.PhatNguoiService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/phatnguoi")
public class PhatNguoiController {

    private final PhatNguoiService service;

    public PhatNguoiController(PhatNguoiService service) {
        this.service = service;
    }

    @GetMapping("/check")
    public String check(@RequestParam String bienso, @RequestParam String loaixe) throws Exception {

        return service.check(bienso, loaixe);
    }

    @GetMapping("/detail")
    public String detail() {
        return service.detailViolation();
    }
}