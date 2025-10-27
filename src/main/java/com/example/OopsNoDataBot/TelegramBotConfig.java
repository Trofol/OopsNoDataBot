package com.example.OopsNoDataBot;

import lombok.SneakyThrows;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotConfig {

    @Value("${bot.token}")
    private String botToken;
    
    @Value("${bot.username}")
    private String botUsername;
    
    @Value("${ai.url}")
    private String aiUrl;
    
    @Value("${ai.model}")
    private String aiModel;
    
    @Value("${ai.temperature:0.7}")
    private Double aiTemperature;
    
    @Value("${ai.max-tokens:1000}")
    private Integer aiMaxTokens;

    @Bean
    @SneakyThrows
    public TelegramBot telegramBot(TelegramBotsApi telegramBotsApi, OpenAiChatModel chatModel) {
        DefaultBotOptions botOptions = new DefaultBotOptions();
        var bot = new TelegramBot(botOptions, botToken, botUsername, chatModel, 
                                   aiUrl, aiModel, aiTemperature, aiMaxTokens);
        telegramBotsApi.registerBot(bot);
        return bot;
    }

    @Bean
    @SneakyThrows
    public TelegramBotsApi telegramBotsApi() {
        return new TelegramBotsApi(DefaultBotSession.class);
    }
}
