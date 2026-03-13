package com.example.telegrambot.service;

import com.example.telegrambot.model.*;
import com.example.telegrambot.parser.PhatNguoiParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PhatNguoiService {

    @Autowired
    PhatNguoiParser parser;

    private Document htmlDoc;

//    public String check(String bienSo, String type) throws Exception {
//
//        String url = "https://api.phatnguoi.vn/web/tra-cuu/" + bienSo + "/" + type;
//
//        HttpClient client = HttpClient.newHttpClient();
//
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(url))
//                .header("Origin", "https://phatnguoi.vn")
//                .header("Referer", "https://phatnguoi.vn/")
//                .GET()
//                .build();
//
//        HttpResponse<String> response = client.send(
//                request,
//                HttpResponse.BodyHandlers.ofString()
//        );
//
//        // 1. Chuyển String HTML thành Document
//        Document doc = Jsoup.parse(response.body());
//        htmlDoc = doc;
//
//        // 2. Tìm thẻ <p> chứa chữ "Vi phạm"
//        Element pTag = doc.select("li p:contains(Vi phạm)").first();
//        if (pTag != null) {
//            String cleanText = pTag.text(); // Kết quả: "Vi phạm: 2 | Đã xử lý: 2 | Chưa xử lý: 0"
//
//            // 2. Dùng Regex để quét tất cả các số (\d+)
//            List<String> numbers = new ArrayList<>();
//            Matcher m = Pattern.compile("\\d+").matcher(cleanText);
//
//            while (m.find()) {
//                numbers.add(m.group());
//            }
//
//            // 3. Gán giá trị vào biến hoặc format chuỗi
//            if (numbers.size() >= 3) {
//                String viPham = numbers.get(0);
//                String daXuLy = numbers.get(1);
//                String chuaXuLy = numbers.get(2);
//
//                String result = "Vi phạm: %s | Đã xử lý: %s | Chưa xử lý: %s"
//                        .formatted(viPham, daXuLy, chuaXuLy);
//
//                System.out.println(result);
//                return result;
//            }
//        }
//        return "0" ;
//    }

    public String check(String bienSo, String type) {
        String loaiXe = "";
        if ("1".equals(type)) {
            loaiXe = "car";
        } else if ("2".equals(type)) {
            loaiXe = "motorbike";
        } else if ("3".equals(type)) {
            loaiXe = "electricbike";
        }
        ChromeOptions options = new ChromeOptions();

        // chạy hidden
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--no-sandbox"); // Bắt buộc khi chạy trên Docker/Linux server
        options.addArguments("--disable-dev-shm-usage"); // Tránh lỗi crash WebDriver do thiếu RAM
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-setuid-sandbox");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-features=NetworkService");

        WebDriver driver = new ChromeDriver(options);

        try {

            driver.get("https://csgt.vn/tra-cuu-phat-nguoi");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            // WAIT plate_number
            WebElement plateInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.id("plate_number")
                    )
            );

            plateInput.sendKeys(bienSo);

            // WAIT vehicle_type
            WebElement vehicleSelect = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.id("vehicle_type")
                    )
            );

            Select select = new Select(vehicleSelect);
            select.selectByValue(loaiXe);

            // WAIT submit button
            WebElement submitBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.id("submitBtn")
                    )
            );

            submitBtn.click();
            System.out.println("Đã bấm tra cứu");
            Thread.sleep(2000);

            // WAIT kết quả load (ví dụ div kết quả)
            WebElement result = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.tagName("body")
                    )
            );

            return parser.parse(result.getText());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
        return null;
    }

    public String detailViolation() {
        StringBuilder finalResult = new StringBuilder();

        // Lấy tất cả các bảng vi phạm bên trong div showViolationData
        Elements tables = htmlDoc.select("#showViolationData table");
        int count = 1;

        for (Element table : tables) {
            // Lấy trạng thái của bảng này
            String status = table.select("tr:contains(Trạng thái:) td:last-child").text().trim();

            // Lọc: Nếu bạn muốn lấy "CHƯA XỬ PHẠT" (Ở đây tôi ví dụ lọc theo text)
            // Lưu ý: Trong HTML bạn gửi là "ĐÃ XỬ PHẠT", hãy đổi điều kiện theo thực tế
            if (status.equalsIgnoreCase("CHƯA XỬ PHẠT") || status.contains("CHƯA")) {

                String thoiGian = table.select("tr:contains(Thời gian vi phạm:) td:last-child").text();
                String diaDiem = table.select("tr:contains(Địa điểm vi phạm:) td:last-child").text();
                String hanhVi = table.select("tr:contains(Hành vi vi phạm:) td:last-child").text()
                        .replace("Xem mức phạt", "").trim(); // Xóa chữ trên button

                // Gom vào chuỗi
                finalResult.append(String.format("%d. Thời gian: %s\n", count, thoiGian));
                finalResult.append(String.format("   Địa điểm: %s\n", diaDiem));
                finalResult.append(String.format("   Hành vi: %s\n", hanhVi));
                finalResult.append("----------------------------\n");
                count++;
            }
        }

        return finalResult.length() > 0 ? finalResult.toString() : "Không có lỗi chưa xử phạt.";
    }
}