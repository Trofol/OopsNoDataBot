package com.example.OopsNoDataBot;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final String botToken;
    private final String botUsername;
    private final AIService aiService;

    public TelegramBot(DefaultBotOptions options, String botToken, String botUsername, AIService aiService) {
        super(options, botToken);
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.aiService = aiService;
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()){
            var userMessage = update.getMessage().getText();
            var chatId = update.getMessage().getChatId();
            
            log.info("Сообщение от {}: {}", chatId, userMessage);
            
            String aiResponse = aiService.getChatResponse(userMessage);
            
            if (aiResponse.equals("Таймаут при обращении к AI модели") || aiResponse.equals("Ошибка соединения с AI моделью")) {
                aiResponse = "Извините, обработка запроса заняла слишком много времени. Попробуйте переформулировать вопрос или попробуйте позже.";
            }
            
            sendMessageInParts(chatId.toString(), aiResponse);
        }
    }
    
    @SneakyThrows
    private void sendMessageInParts(String chatId, String text) {
        final int MAX_MESSAGE_LENGTH = 4096;
        
        if (text.length() <= MAX_MESSAGE_LENGTH) {
            SendMessage message = new SendMessage(chatId, text);
            sendApiMethod(message);
        } else {
            int totalMessages = (int) Math.ceil((double) text.length() / MAX_MESSAGE_LENGTH);
            
            for (int i = 0; i < totalMessages; i++) {
                int start = i * MAX_MESSAGE_LENGTH;
                int end = Math.min(start + MAX_MESSAGE_LENGTH, text.length());
                String part = text.substring(start, end);
                
                if (totalMessages > 1) {
                    part = String.format("[Часть %d из %d]\n\n%s", i + 1, totalMessages, part);
                }
                
                SendMessage message = new SendMessage(chatId, part);
                sendApiMethod(message);
                Thread.sleep(500);
            }
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}
