
public class Main {
    public static void main(String[] args) {
        TelegramBotsApi botsApi = new TelegramBotsApi();
        try {
            botsApi.registerBot(new OopsNoDataBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}