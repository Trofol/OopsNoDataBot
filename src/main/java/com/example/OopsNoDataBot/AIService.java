package com.example.OopsNoDataBot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {
    
    @Value("${ai.url}")
    private String aiUrl;
    
    @Value("${ai.model}")
    private String aiModel;
    
    @Value("${ai.temperature:0.7}")
    private Double aiTemperature;
    
    @Value("${ai.max-tokens:1000}")
    private Integer aiMaxTokens;
    
    public String getChatResponse(String userMessage) {
        try {
            URL chatUrl = new URL(aiUrl);
            HttpURLConnection chatConnection = (HttpURLConnection) chatUrl.openConnection();
            chatConnection.setRequestMethod("POST");
            chatConnection.setRequestProperty("Content-Type", "application/json");
            chatConnection.setDoOutput(true);
            chatConnection.setConnectTimeout(30000);
            chatConnection.setReadTimeout(120000); // 2 минуты для обработки сложных запросов
            
            String jsonRequest = String.format(Locale.US,
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
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(chatConnection.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine);
                    }
                    
                    String responseStr = response.toString();
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(responseStr);
                    
                    JsonNode choices = jsonNode.get("choices");
                    if (choices != null && choices.isArray() && choices.size() > 0) {
                        JsonNode firstChoice = choices.get(0);
                        JsonNode message = firstChoice.get("message");
                        if (message != null) {
                            JsonNode content = message.get("content");
                            if (content != null) {
                                String aiResponse = content.asText();
                                
                                // Удаляем теги reasoning из ответа
                                aiResponse = aiResponse.replaceAll("(?s)<think>.*?</think>", "");
                                
                                return aiResponse;
                            }
                        }
                    }
                    return "Ошибка парсинга: не найден content";
                }
            } else {
                StringBuilder errorResponse = new StringBuilder();
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(chatConnection.getErrorStream(), "utf-8"))) {
                    String errorLine;
                    while ((errorLine = br.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                }
                log.error("AI модель вернула код ошибки: {}. Ответ: {}", chatResponseCode, errorResponse.toString());
                return "AI модель вернула ошибку: " + chatResponseCode;
            }
        } catch (java.net.SocketTimeoutException e) {
            log.error("Таймаут при запросе к AI: ", e);
            return "Таймаут при обращении к AI модели";
        } catch (Exception e) {
            log.error("Ошибка при запросе к AI: ", e);
            return "Ошибка соединения с AI моделью";
        }
    }
}

