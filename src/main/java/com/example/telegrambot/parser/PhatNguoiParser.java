package com.example.telegrambot.parser;

import com.example.telegrambot.model.Violation;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.*;

@Component
public class PhatNguoiParser {

    public String parse(String text) {

        StringBuilder result = new StringBuilder();
        String[] blocks = text.split("Biển số: ");

        for (String block : blocks) {

            if (!block.contains("Chưa xử phạt")) continue;
            String violation = extract(block, "Lỗi vi phạm:\\s*(.*)");
            String time = extract(block, "Thời gian:\\s*(.*)");
            String location = extract(block, "Địa điểm:\\s*(.*)");
            String bienSo = block.split("\n")[0].trim();
            String line = """
                    🚗 Biển số: %s
                    ⚠  Vi phạm: %s
                    ⏰ Thời gian: %s
                    📍 Địa điểm: %s
                    📄 Trạng thái: %s
                    ---------------------
                    """.formatted(bienSo, violation, time, location, "Chưa xử phạt");

            result.append(line);
        }

        return "".equals(result.toString()) ? "0" : result.toString();
    }

    private static String extract(String text, String regex) {

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "";
    }
}