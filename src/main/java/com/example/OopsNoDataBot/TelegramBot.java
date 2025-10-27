package com.example.OopsNoDataBot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final OpenAiChatModel chatModel;
    private final String botToken;
    private final String botUsername;
    
    @Value("${ai.url}")
    private String aiUrl;
    
    @Value("${ai.model}")
    private String aiModel;
    
    @Value("${ai.temperature:0.7}")
    private Double aiTemperature;
    
    @Value("${ai.max-tokens:1000}")
    private Integer aiMaxTokens;

    public TelegramBot(DefaultBotOptions options, String botToken, String botUsername, OpenAiChatModel chatModel) {
        super(options, botToken);
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.chatModel = chatModel;
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        // Запросы от телеги
        if(update.hasMessage() && update.getMessage().hasText()){
            var userMessage = update.getMessage().getText();
            var chatId = update.getMessage().getChatId();
            
            log.info("Получено сообщение от пользователя {}: {}", chatId, userMessage);
            
            try {
                if (chatModel == null) {
                    log.error("ChatModel is null!");
                    SendMessage errorMessage = new SendMessage(chatId.toString(), 
                        "Ошибка: AI модель не инициализирована.");
                    sendApiMethod(errorMessage);
                    return;
                }
                
                String aiResponse;
                try {
                    java.net.URL chatUrl = new java.net.URL(aiUrl);
                    java.net.HttpURLConnection chatConnection = (java.net.HttpURLConnection) chatUrl.openConnection();
                    chatConnection.setRequestMethod("POST");
                    chatConnection.setRequestProperty("Content-Type", "application/json");
                    chatConnection.setDoOutput(true);
                    chatConnection.setConnectTimeout(30000);
                    chatConnection.setReadTimeout(60000);
                    
                    String jsonRequest = String.format(
                        "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"temperature\":%.1f,\"max_tokens\":%d}",
                        aiModel,
                        userMessage.replace("\"", "\\\""),
                        aiTemperature,
                        aiMaxTokens
                    );
                    
                    try (java.io.OutputStream os = chatConnection.getOutputStream()) {
                        byte[] input = jsonRequest.getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }
                    
                    int chatResponseCode = chatConnection.getResponseCode();
                    
                    if (chatResponseCode == 200) {
                        // Читаем ответ
                        try (java.io.BufferedReader br = new java.io.BufferedReader(
                                new java.io.InputStreamReader(chatConnection.getInputStream(), "utf-8"))) {
                            StringBuilder response = new StringBuilder();
                            String responseLine;
                            while ((responseLine = br.readLine()) != null) {
                                response.append(responseLine.trim());
                            }
                            
                            String responseStr = response.toString();
                            
                            try {
                                ObjectMapper objectMapper = new ObjectMapper();
                                JsonNode jsonNode = objectMapper.readTree(responseStr);
                                
                                JsonNode choices = jsonNode.get("choices");
                                if (choices != null && choices.isArray() && choices.size() > 0) {
                                    JsonNode firstChoice = choices.get(0);
                                    JsonNode message = firstChoice.get("message");
                                    if (message != null) {
                                        JsonNode content = message.get("content");
                                        if (content != null) {
                                            aiResponse = content.asText();
                                            
                                            // Удаляем теги reasoning из ответа
                                            if (aiResponse.contains("<think>")) {
                                                aiResponse = aiResponse.replaceAll("(?s)<think>.*?</think>", "").trim();
                                                aiResponse = aiResponse.replaceAll("\n{3,}", "\n\n").trim();
                                            }
                                        } else {
                                            aiResponse = "Ошибка парсинга: не найден content";
                                            log.error("Не найден content в ответе AI");
                                        }
                                    } else {
                                        aiResponse = "Ошибка парсинга: не найден message";
                                        log.error("Не найден message в ответе AI");
                                    }
                                } else {
                                    aiResponse = "Ошибка парсинга: не найден choices массив";
                                    log.error("Не найден choices массив в ответе AI");
                                }
                            } catch (Exception parseException) {
                                log.error("Ошибка парсинга JSON ответа: ", parseException);
                                aiResponse = "Ошибка парсинга JSON ответа";
                            }
                        }
                    } else {
                        aiResponse = "AI модель вернула ошибку: " + chatResponseCode;
                        log.error("AI модель вернула код ошибки: {}", chatResponseCode);
                    }
                } catch (Exception e) {
                    log.error("Ошибка при запросе к AI: ", e);
                    aiResponse = "Ошибка соединения с AI моделью";
                }
                
                SendMessage sendMessage = new SendMessage(chatId.toString(), aiResponse);
                sendApiMethod(sendMessage);
                
            } catch (Exception e) {
                log.error("Ошибка при обработке сообщения AI: ", e);
                
                String errorText;
                if (e.getMessage() != null && e.getMessage().contains("ECONNRESET")) {
                    errorText = "Соединение с AI моделью прервано. Попробуйте еще раз.";
                } else if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                    errorText = "AI модель слишком долго отвечает. Попробуйте еще раз.";
                } else {
                    errorText = "Извините, произошла ошибка при обработке вашего сообщения. Попробуйте позже.";
                }
                
                SendMessage errorMessage = new SendMessage(chatId.toString(), errorText);
                sendApiMethod(errorMessage);
            }
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}
