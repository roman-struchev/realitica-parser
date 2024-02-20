package com.realitica.parser.service.subscriptions;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionSender {

    @Value("${app.telegram.bot.token:}")
    private String telegramBotToken;

    @Value("${app.twilio.bot.sid:}")
    private String twilioBotSid;
    @Value("${app.twilio.bot.token:}")
    private String twilioBotToken;

    private TelegramBot bot;

    public void sendToTelegram(List<String> telegramBotChatIds, String content) {
        telegramBotChatIds.forEach(telegramBotChatId -> sendToTelegram(telegramBotChatId, content));
    }

    public void sendToTelegram(String telegramBotChatId, String content) {
        try {
            if (bot != null && StringUtils.isNotEmpty(telegramBotChatId)) {
                var message = new SendMessage(telegramBotChatId, content);
                message.parseMode(ParseMode.Markdown);
                message.disableWebPagePreview(true);
                this.bot.execute(message);
            }
        } catch (Exception ex) {
            log.error("Can't send message to telegram {}: {}", telegramBotChatId, content);
        }
    }

    public void sendToWhatsapp(List<String> numbers, String content) {
        numbers.forEach(number -> sendToWhatsapp(number, content));
    }

    public void sendToWhatsapp(String number, String content) {
        if(StringUtils.isAllEmpty(twilioBotSid, twilioBotToken)) {
            return;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(twilioBotSid, twilioBotToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("To", "whatsapp:" + number);
            formData.add("From", "whatsapp:+14155238886");
            formData.add("Body", content.substring(0, 1599));

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

            String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", twilioBotSid);
            restTemplate.postForEntity(url, requestEntity, String.class);
        } catch (Exception ex) {
            log.error("Can't send message to whatsapp {}: {}", number, content, ex);
        }
    }

    @PostConstruct
    private void init() {
        if (StringUtils.isEmpty(telegramBotToken)) {
            log.info("Telegram bot token not filled");
            return;
        }

        this.bot = new TelegramBot(telegramBotToken);
        this.bot.setUpdatesListener(updates -> {
            updates.forEach(update -> {
                var chatId = update.message().chat().id();
                var message = update.message().text();
                if (message != null) {
                    log.info("Telegram group {}, message: {}", chatId, message);
                    var response = String.format("Your chat id: %s\nOriginal message: %s", chatId, message);
                    bot.execute(new SendMessage(update.message().chat().id(), response));
                }
            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

    }
}
