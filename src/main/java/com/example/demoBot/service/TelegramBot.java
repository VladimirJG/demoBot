package com.example.demoBot.service;

import com.example.demoBot.config.BotConfig;
import com.example.demoBot.model.User;
import com.example.demoBot.model.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private UserRepository userRepository;
    final BotConfig botConfig;
    static final String HELP_TEXT = "Этот бот создан в процессе изучения ЯП Java\n\n" +
            "Некоторые из представленных команд меню в процессе реализации\n\n" +
            "В данный момент действуют команды:\n\n" +
            "Команда /start запускает чат-бот и выводит приветствие\n\n" +
            "Команда /mydata показывает историю введенных/используемых команд\n\n" +
            "Команда /help выводит настоящее сообщение";

    public TelegramBot(BotConfig config) {
        this.botConfig = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Запуск бота"));
        listOfCommands.add(new BotCommand("/mydata", "Посмотреть историю вводимых комманд"));
        listOfCommands.add(new BotCommand("/deletemydata", "Удалить мою историю"));
        listOfCommands.add(new BotCommand("/help", "Используемые в боте команды"));
        listOfCommands.add(new BotCommand("/settings", "Установить/изменить настройки"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Ошибка вызова команды из списка: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (message) {
                case "/start":

                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/help":
                    sendMessage(chatId, HELP_TEXT);
                    break;
                case "/mydata":
                    sendMessage(chatId, update.getMessage().getChat().getInviteLink());
                default:
                    sendMessage(chatId, "Введенной команды не существует.");

            }
        }
    }

    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()){

            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAT(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("Пользователь сохранен: " + user);
        }
    }

    @Override
    public String getBotToken() {

        return botConfig.getToken();
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    private void startCommandReceived(long chatId, String name) {

        String answer = "Привет " + name + ", добро пожаловать!";
        log.info("Replied to user: " + name);
        sendMessage(chatId, answer);

    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }
}
