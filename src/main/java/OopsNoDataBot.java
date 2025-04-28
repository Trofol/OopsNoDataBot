public class OopsNoDataBot extends TelegramLongPollingBot {
    @Override
    public String getBotUsername() {
        return "OopsNoDataBot";
    }

    @Override
    public String getBotToken() {
        return "YOUR_BOT_TOKEN"; // Из config.properties
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if ("/start".equals(text)) {
                sendMessage(chatId, "Привет! Я OopsNoDataBot. Напиши /help для списка команд.");
            }
        }
    }
}