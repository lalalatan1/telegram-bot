package com.example.telegrambot.config;

import com.example.telegrambot.bot.MyBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(MyBot bot) throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);
        System.out.println("createdddd");
        return botsApi;
    }

}