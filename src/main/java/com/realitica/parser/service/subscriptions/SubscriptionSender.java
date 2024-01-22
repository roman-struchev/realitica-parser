package com.realitica.parser.service.subscriptions;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionSender {

    @Value("${app.telegram.bot.token:}")
    private String telegramBotToken;

    private TelegramBot bot;

    public void send(List<String> telegramBotChatIds, String content) {
        telegramBotChatIds.forEach(telegramBotChatId -> send(telegramBotChatId, content));
    }

    public void send(String telegramBotChatId, String content) {
        if (bot != null && StringUtils.isNotEmpty(telegramBotChatId)) {
            var message = new SendMessage(telegramBotChatId, content);
            message.parseMode(ParseMode.Markdown);
            message.disableWebPagePreview(true);
            this.bot.execute(message);
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
            updates.stream().forEach(update -> {
                var chatId = update.message().chat().id();
                var message = update.message().text();
                if (message != null) {
                    log.info("Telegram group {}, message: {}", chatId, message);
                    var response = String.format("Your chat id: %s", chatId);
                    bot.execute(new SendMessage(update.message().chat().id(), response));
                }
            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

    }
}
