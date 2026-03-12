package com.example.telegrambot.bot;

import com.example.telegrambot.factory.ButtonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MyBot extends TelegramLongPollingBot {

    private final String username;

    private final Map<Long, String> userVehicleType = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastActivity = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> isStarted = new ConcurrentHashMap<>();

    public MyBot(
            @Value("${bot.token}") String token,
            @Value("${bot.username}") String username) {
        super(token);
        this.username = username;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            Long chatId = null;
            if (update.hasCallbackQuery()) {
                chatId = update.getCallbackQuery().getMessage().getChatId();
            } else if (update.hasMessage()) {
                chatId = update.getMessage().getChatId();
            }

            if (chatId == null) return;

            long currentTime = System.currentTimeMillis();
            Long lastTime = lastActivity.get(chatId);

            // Kiểm tra timeout 5 phút (5 * 60 * 1000 ms)
            if (lastTime != null && (currentTime - lastTime) > 300000) {
                isStarted.put(chatId, false);
            }
            lastActivity.put(chatId, currentTime);

            String text = (update.hasMessage() && update.getMessage().hasText()) ? update.getMessage().getText() : null;
            String data = update.hasCallbackQuery() ? update.getCallbackQuery().getData() : null;

            // Xử lý Welcome Screen nếu chưa bắt đầu (chưa bấm nút Bắt đầu hoặc vừa quá 5 phút)
            if ((text != null && text.equals("/start")) || !isStarted.getOrDefault(chatId, false)) {
                if (data != null && data.equals("begin_session")) {
                    isStarted.put(chatId, true);
                    showMainMenu(chatId);
                    return;
                }

                SendMessage message = new SendMessage();
                message.setChatId(chatId.toString());
                message.setText("👋 *Chào mừng bạn đến với Hệ thống Bot Tiện Ích!*\n\n" +
                        "Tại đây, bạn có thể dễ dàng:\n" +
                        "1️⃣ Cập nhật tin tức mới nhất.\n" +
                        "2️⃣ Tra cứu lỗi vi phạm phạt nguội.\n" +
                        "3️⃣ Xem giá vàng, giá xăng dầu.\n\n" +
                        "⏱️ _Hệ thống sẽ tự trở về màn hình này sau 5 phút không tương tác._\n\n" +
                        "Hãy nhấn nút **🚀 Bắt đầu** bên dưới để sử dụng dịch vụ!");
                message.setParseMode("Markdown");
                InlineKeyboardButton beginBtn = ButtonFactory.button("🚀 Bắt đầu", "begin_session");
                drawUIComponent(message, List.of(beginBtn));
                execute(message);
                return;
            }

            if (update.hasCallbackQuery()) {

                if ("check_phat_nguoi".equals(data) || "recheck".equals(data)) {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId.toString());
                    message.setText("🚓 *Tra Cứu Phạt Nguội*\n\nVui lòng chọn loại phương tiện bạn muốn tra cứu:");
                    message.setParseMode("Markdown");
                    InlineKeyboardButton button1 = ButtonFactory.button("🚗 Ô tô (Xe hơi)", "xe_hoi");
                    InlineKeyboardButton button2 = ButtonFactory.button("🏍️ Xe máy", "xe_may");
                    drawUIComponent(message, List.of(button1, button2));
                    execute(message);
                    return;
                }

                if ("xe_hoi".equals(data) || "xe_may".equals(data)) {
                    userVehicleType.put(chatId, "xe_hoi".equals(data) ? "1" : "2");
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId.toString());
                    String typeName = "xe_hoi".equals(data) ? "Ô tô 🚗" : "Xe máy 🏍️";
                    message.setText("✍️ *Bạn đã chọn " + typeName + "*\n\nVui lòng nhập biển số phương tiện cần tra cứu\n_(VD: 30A12345, 59X11223)_:");
                    message.setParseMode("Markdown");
                    execute(message);
                    return;
                }

                if ("yes".equals(data)) {
                    // TODO gọi API check phạt nguội
                    RestTemplate rest = new RestTemplate();
                    String url = "http://localhost:8080/api/phatnguoi/detail";
                    String resultStr = rest.getForObject(url, String.class);
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId.toString());
                    message.setText(resultStr);
                    execute(message);
                    return;
                }

                if ("update_news".equals(data)) {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId.toString());
                    message.setText("📝 *Bản tin hôm nay:*\n\n" +
                            "1️⃣ Thời tiết có mưa rào vào buổi chiều 🌧️\n" +
                            "2️⃣ Giá vàng có dấu hiệu tăng nhẹ 📈\n\n" +
                            "_(Bạn có thể gọi API tin tức thực tế ở đây)_");
                    message.setParseMode("Markdown"); // Cho chữ in đậm/nghiêng nếu cần thiết

                    // Nút để quay lại Menu chính
                    InlineKeyboardButton buttonEnd = ButtonFactory.button("↩️ Quay lại", "end");
                    drawUIComponent(message, List.of(buttonEnd));
                    execute(message);
                    return;
                }

                if ("gold_price".equals(data)) {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId.toString());
                    try {
                        RestTemplate restTemplate = new RestTemplate();
                        String url = "https://www.vang.today/api/prices?type=SJL1L10";
                        String jsonString = restTemplate.getForObject(url, String.class);

                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode root = mapper.readTree(jsonString);

                        String name = root.get("name").asText();
                        long buy = root.get("buy").asLong();
                        long sell = root.get("sell").asLong();
                        long changeBuy = root.get("change_buy").asLong();
                        long changeSell = root.get("change_sell").asLong();
                        String date = root.get("date").asText();
                        String time = root.get("time").asText();

                        String goldText = String.format("💰 *Giá %s*\n" +
                                "🕒 _Cập nhật: %s ngày %s_\n\n" +
                                "📉 *Giá Mua vào:* %,d VND\n" +
                                " *(Biến động: %+,d VND)*\n\n" +
                                "📈 *Giá Bán ra:* %,d VND\n" +
                                " *(Biến động: %+,d VND)*",
                                name, time, date, buy, changeBuy, sell, changeSell);

                        message.setText(goldText);
                        message.setParseMode("Markdown");
                    } catch (Exception e) {
                        message.setText("❌ Rất tiếc, đã có lỗi khi lấy dữ liệu giá vàng. Vui lòng thử lại sau.");
                    }

                    // Nút để quay lại Menu chính
                    InlineKeyboardButton buttonEnd = ButtonFactory.button("↩️ Menu chính", "end");
                    drawUIComponent(message, List.of(buttonEnd));
                    execute(message);
                    return;
                }

                if ("petrol_price".equals(data)) {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId.toString());
                    try {
                        LocalDate today = LocalDate.now();
                        LocalDate yesterday = today.minusDays(1);
                        LocalDate lastWeek = today.minusWeeks(1);
                        LocalDate lastMonth = today.minusMonths(1);
                        
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        
                        java.util.Map<String, Double> pricesToday = getPetrolPrices(today.format(formatter));
                        java.util.Map<String, Double> pricesYesterday = getPetrolPrices(yesterday.format(formatter));
                        java.util.Map<String, Double> pricesLastWeek = getPetrolPrices(lastWeek.format(formatter));
                        java.util.Map<String, Double> pricesLastMonth = getPetrolPrices(lastMonth.format(formatter));
                        
                        StringBuilder sb = new StringBuilder();
                        sb.append("⛽ *Giá Xăng Dầu*\n\n");
                        
                        if (pricesToday.isEmpty()) {
                            sb.append("❌ Không có dữ liệu cho hôm nay.\n");
                        } else {
                            for (String type : pricesToday.keySet()) {
                                sb.append(String.format("🔹 *%s*\n", type));
                                Double pToday = pricesToday.get(type);
                                sb.append(String.format("   ▪️ *Hôm nay:* %,.0f VNĐ/lít\n", pToday));
                                sb.append(formatPriceCompare("Hôm qua", pricesYesterday.get(type), pToday));
                                sb.append(formatPriceCompare("Tuần qua", pricesLastWeek.get(type), pToday));
                                sb.append(formatPriceCompare("Tháng qua", pricesLastMonth.get(type), pToday));
                                sb.append("\n");
                            }
                        }
                        
                        message.setText(sb.toString());
                        message.setParseMode("Markdown");
                    } catch (Exception e) {
                        message.setText("❌ Lỗi khi lấy dữ liệu xăng dầu.");
                    }
                    
                    InlineKeyboardButton buttonEnd = ButtonFactory.button("↩️ Menu chính", "end");
                    drawUIComponent(message, List.of(buttonEnd));
                    execute(message);
                    return;
                }

                if ("end".equals(data)) {
                    showMainMenu(chatId);
                }

            }

            if (update.hasMessage() && update.getMessage().hasText()) {
                if (!"/start".equals(text)) {
                    String bienSo = text;

                    SendMessage message = new SendMessage();
                    message.setChatId(chatId.toString());
                    message.setText("🔎 *Đang tra cứu dữ liệu cho biển số:* `" + bienSo + "`...");
                    message.setParseMode("Markdown");
                    execute(message);

                    try {
                        RestTemplate rest = new RestTemplate();
                        String url = "http://localhost:8080/api/phatnguoi/check?bienso=" + bienSo + "&loaixe="
                                + userVehicleType.getOrDefault(chatId, "1");

                        String resultStr = rest.getForObject(url, String.class);
                        message.setChatId(chatId.toString());
                        
                        if ("0".equals(resultStr)) {
                            message.setText("✅ *Tuyệt vời!*\nPhương tiện mang biển số `" + bienSo + "` hiện tại **KHÔNG CÓ** lỗi vi phạm phạt nguội nào trên hệ thống.");
                        } else {
                            message.setText("⚠️ *PHÁT HIỆN VI PHẠM!* ⚠️\n\nChi tiết lỗi phạt nguội của biển số `" + bienSo + "`:\n\n" + resultStr);
                        }
                        message.setParseMode("Markdown");
                        execute(message);
                    } catch (Exception e) {
                        message.setChatId(chatId.toString());
                        message.setText("❌ Rất tiếc, máy chủ tra cứu đang gặp sự cố hoặc quá tải. Vui lòng thử lại sau.");
                        execute(message);
                    }

                    message.setChatId(chatId.toString());
                    message.setText("Bạn muốn làm gì tiếp theo?");
                    InlineKeyboardButton button2 = ButtonFactory.button("🔄 Tra cứu số khác", "recheck");
                    InlineKeyboardButton button3 = ButtonFactory.button("↩️ Menu chính", "end");
                    drawUIComponent(message, List.of(button2, button3));
                    execute(message);
                }
            }
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void showMainMenu(Long chatId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Chào bạn! Chọn chức năng:");

        InlineKeyboardButton button = ButtonFactory.button("🚗 Kiểm tra phạt nguội", "check_phat_nguoi");
        InlineKeyboardButton newsButton = ButtonFactory.button("📰 Cập nhật tin tức", "update_news");
        InlineKeyboardButton goldButton = ButtonFactory.button("💰 Xem giá vàng", "gold_price");
        InlineKeyboardButton petrolButton = ButtonFactory.button("⛽ Xem giá xăng", "petrol_price");

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(List.of(button));
        keyboard.add(List.of(newsButton));
        keyboard.add(List.of(goldButton));
        keyboard.add(List.of(petrolButton));
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        execute(message);
    }

    private SendMessage drawUIComponent(SendMessage message, List<InlineKeyboardButton> buttons) {
        if (buttons.isEmpty())
            return message;
        List<InlineKeyboardButton> row = new ArrayList<>();
        if (buttons.size() == 1) {
            row.add(buttons.get(0));
        } else {
            for (InlineKeyboardButton button : buttons) {
                row.add(button);
            }
        }

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        message.setReplyMarkup(markup);
        return message;
    }

    private java.util.Map<String, Double> getPetrolPrices(String dateStr) {
        java.util.Map<String, Double> prices = new java.util.LinkedHashMap<>();
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://giaxanghomnay.com/api/pvdate/" + dateStr;
            String jsonString = restTemplate.getForObject(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonString);
            if (root.isArray() && root.size() > 0) {
                JsonNode firstArray = root.get(0);
                for (JsonNode node : firstArray) {
                    if (node.has("title")) {
                        String title = node.get("title").asText();
                        Double price = null;
                        if (node.has("zone1_price")) {
                            price = node.get("zone1_price").asDouble();
                        } else if (node.has("price")) {
                            price = node.get("price").asDouble();
                        }
                        if (price != null) {
                            prices.put(title, price);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return prices;
    }

    private String formatPriceCompare(String label, Double oldPrice, Double currentPrice) {
        if (oldPrice == null || currentPrice == null) {
            return String.format("   ▫️ *%s:* Không có dữ liệu\n", label);
        }
        if (oldPrice.equals(currentPrice)) {
            return String.format("   ▫️ *%s:* %,.0f VNĐ _(Không đổi)_\n", label, oldPrice);
        }
        double diff = currentPrice - oldPrice;
        double pct = (diff / oldPrice) * 100;
        String icon = diff > 0 ? "↗️" : "↘️";
        String sign = diff > 0 ? "+" : "";
        return String.format("   ▫️ *%s:* %,.0f VNĐ _(%s %s%,.0f VNĐ | %s%,.2f%%)_\n", 
                label, oldPrice, icon, sign, diff, sign, pct);
    }
}