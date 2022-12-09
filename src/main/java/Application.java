import bot.Bot;
import bot.BotUtils;
import org.glassfish.grizzly.utils.Pair;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Application {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final int delay = 60;

    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            Bot realtorBot = new Bot(BotUtils.BOT_USERNAME, BotUtils.BOT_TOKEN);
            botsApi.registerBot(realtorBot);

            scheduler.scheduleWithFixedDelay(() -> checkPage(1, realtorBot), 0, delay, TimeUnit.SECONDS);
            scheduler.scheduleWithFixedDelay(() -> checkPage(2, realtorBot), 30, delay, TimeUnit.SECONDS);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private static void checkPage(int page, Bot bot) {
        try {
            checkPageCaught(page, bot);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private static void checkPageCaught(int page, Bot bot) {
        ArrayList<Pair<String, ArrayList<String>>> list = HtmlUtils.parse(HtmlUtils.generatePage(page));
        if (!list.isEmpty()) {
            list.forEach((pair) -> {
                if (!pair.getSecond().isEmpty() && pair.getSecond().size() > 1) {
                    System.out.println("Has pics");
                    SendMediaGroup sendPics = new SendMediaGroup();
                    List<InputMedia> inputMedia = new ArrayList<>();
                    pair.getSecond().forEach((link) -> {
                        System.out.println(link);
                        InputMediaPhoto im = InputMediaPhoto.builder()
                                .media(link)
                                .mediaName(Math.random() + "pic" + ".jpg")
                                .build();
                        inputMedia.add(im);
                    });
                    inputMedia.get(0).setCaption(pair.getFirst());
                    sendPics.setMedias(inputMedia);
                    sendPics.setChatId(BotUtils.CHAT_ID);
                    sendMessage(bot, null, sendPics);
                } else { // if there are no photos
                    System.out.println("Doesn't have pics");
                    SendMessage message = new SendMessage();
                    message.setChatId(BotUtils.CHAT_ID);
                    message.setText(pair.getFirst());
                    sendMessage(bot, message, null);
                }
            });
        }
    }

    private static void sendMessage(Bot bot, SendMessage message, SendMediaGroup mediaGroup) {
        try {
            if (mediaGroup != null) {
                bot.execute(mediaGroup);
            } else {
                bot.execute(message);
            }
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
            if (e.getErrorCode() == 429 && e.getParameters().getRetryAfter() != null) {
                if (mediaGroup != null) {
                    scheduler.schedule(() -> bot.execute(mediaGroup), delay, TimeUnit.SECONDS);
                } else {
                    scheduler.schedule(() -> bot.execute(message), delay, TimeUnit.SECONDS);
                }
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
