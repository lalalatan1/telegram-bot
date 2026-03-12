package com.example.telegrambot.factory;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

public class ButtonFactory {

    public static InlineKeyboardButton button(String text, String callback) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(text);
        btn.setCallbackData(callback);
        return btn;
    }
}