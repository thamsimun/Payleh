import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firestore.v1beta1.Write;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.*;
import java.util.stream.Collectors;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static java.lang.Math.*;

public class MyAmazingBot extends TelegramLongPollingBot {
    public void onUpdateReceived(Update update) {
        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message_text = update.getMessage().getText();
            String user_first_name = update.getMessage().getChat().getFirstName();
            String user_last_name = update.getMessage().getChat().getLastName();
            String user_username = update.getMessage().getChat().getUserName();
            String user_id = String.valueOf(update.getMessage().getChat().getId());
            long chat_id = update.getMessage().getChatId();

            if (message_text.equals("/start")) {
                SendMessage message = new SendMessage() // Create a message object object
                        .setChatId(chat_id)
                        .setText("Add me to a group chat and type /debt DEBT_AMOUNT DEBT_TITLE \n Type /iowe or /whoowe to view outstanding debts.");
                try {
                    execute(message); // Sending our message object to user
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if(message_text.equals("/iowe")){
                String s = "";
                List<UserPayer> payersList = Firebase.getAllPayees(update.getMessage().getFrom().getId().toString());
                HashMap<String, String> payersMap = new HashMap<String, String>();
                for(int i = 0; i < payersList.size(); i++) {
                    payersMap.put(payersList.get(i).uid, payersList.get(i).name);
                }

                for(String u: payersMap.values()){
                    s += u + "\n";
                }
                SendMessage message = new SendMessage()
                        .setChatId(chat_id)
                        .setText(s);
                try{
                    execute(message);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }


            if(message_text.equals("/whoowe")){
                String s = "";
                List<UserPayer> payersList = Firebase.getAllPayers(update.getMessage().getFrom().getId().toString());
                HashMap<String, String> payersMap = new HashMap<String, String>();
                for(int i = 0; i < payersList.size(); i++) {
                    payersMap.put(payersList.get(i).uid, payersList.get(i).name);
                }

                for(String u: payersMap.values()){
                    s += u + "\n";
                }
                SendMessage message = new SendMessage()
                        .setChatId(chat_id)
                        .setText(s);
                try{
                    execute(message);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }

            if (message_text.substring(0, 5).equals("/debt")) {
                List<String> list = Arrays.asList(message_text.split("\\s+"));

                if (list.size() < 3) {
                    SendMessage message = new SendMessage()
                            .setChatId(chat_id)
                            .setText("Wrong number of params.");
                    try {
                        execute(message);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                } else {

                    Firestore db = new Firebase().db;
                    Map<String, Object> docData = new HashMap<>();
                    docData.put("uid", update.getMessage().getFrom().getId());
                    docData.put("title", list.get(2));
                    docData.put("debt", list.get(1));
                    ApiFuture<DocumentReference> future = db.collection("debts").add(docData);
                    String value = "";
                    try {
                        value = future.get().getId();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    SendMessage message = new SendMessage()
                            .setChatId(chat_id)
                            .setText("\uD83D\uDCB8" + "\n" + list.get(2) + "\n" + list.get(1) + "\n" + value);

                    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                    List<InlineKeyboardButton> rowInline = new ArrayList<>();
                    rowInline.add(new InlineKeyboardButton().setText("Guilty").setCallbackData("Y"));
                    rowInline.add(new InlineKeyboardButton().setText("Lock Participants").setCallbackData("L"));
                    //rowInline.add(new InlineKeyboardButton().setText("GTFO").setCallbackData("N"));
                    //rowInline.add(new InlineKeyboardButton().setText("Who owe me").setCallbackData("O"));
                    rowsInline.add(rowInline);
                    markupInline.setKeyboard(rowsInline);
                    message.setReplyMarkup(markupInline);
                    try {
                        execute(message);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }

        } else if (update.hasCallbackQuery()) {
            int prev_id = update.getCallbackQuery().getMessage().getMessageId();
            String prev_msg = update.getCallbackQuery().getMessage().getText();
            long chat_id = update.getCallbackQuery().getMessage().getChatId();
            User user = update.getCallbackQuery().getFrom();
            List<String> list = Arrays.asList(prev_msg.split("\n"));

            if (update.getCallbackQuery().getData().equals("L")) {
                SendMessage message = new SendMessage()
                        .setChatId(chat_id)
                        .setText("You are not the admin.");
                Firestore db = new Firebase().db;
                ApiFuture<DocumentSnapshot> docRef = db.collection("debts").document(list.get(3)).get();
                try {
                    DocumentSnapshot ref = docRef.get();
                    String my_uid = ref.get("uid").toString();
                    if (my_uid.equals(user.getId().toString())) {

                        message = new SendMessage()
                                .setChatId(chat_id)
                                .setText("Bill split!");

                        double cost = Double.parseDouble(list.get(2)) / Firebase.getPayers(list.gpet(3)).size();

                        prev_msg =  "Please pay: " + cost + "\n" + prev_msg;

                        EditMessageText message2 = new EditMessageText()
                                .setChatId(chat_id)
                                .setMessageId(toIntExact(prev_id))
                                .setText(prev_msg);

                        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                        List<InlineKeyboardButton> rowInline = new ArrayList<>();
                        rowInline.add(new InlineKeyboardButton().setText("GTFO").setCallbackData("N"));
                        rowsInline.add(rowInline);
                        markupInline.setKeyboard(rowsInline);
                        message2.setReplyMarkup(markupInline);

                        try {
                            execute(message2);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    execute(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (update.getCallbackQuery().getData().equals("Y")) {

                Firestore db = new Firebase().db;
                DocumentReference docRef = db.collection("debts").document(list.get(3));
                Map<String, Object> docData = new HashMap<>();
                docData.put("name", user.getFirstName());
                ApiFuture<WriteResult> future = db.collection("debts").document(list.get(3))
                        .collection("payees").document(user.getId().toString()).set(docData);

                String new_message = addUser(prev_msg, user);

                EditMessageText message = new EditMessageText()
                        .setChatId(chat_id)
                        .setMessageId(toIntExact(prev_id))
                        .setText(new_message);

                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                rowInline.add(new InlineKeyboardButton().setText("Guilty").setCallbackData("Y"));
                rowInline.add(new InlineKeyboardButton().setText("Lock Participants").setCallbackData("L"));
                rowsInline.add(rowInline);
                markupInline.setKeyboard(rowsInline);
                message.setReplyMarkup(markupInline);

                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
            if (update.getCallbackQuery().getData().equals("N")) {

                Firestore db = new Firebase().db;
                DocumentReference docRef = db.collection("debts").document(list.get(3));
                ApiFuture<WriteResult> future = db.collection("debts").document(list.get(3))
                        .collection("payees").document(user.getId().toString()).delete();

                String new_message = removeUser(prev_msg, user);

                EditMessageText message = new EditMessageText()
                        .setChatId(chat_id)
                        .setMessageId(toIntExact(prev_id))
                        .setText(new_message);

                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                rowInline.add(new InlineKeyboardButton().setText("GTFO").setCallbackData("N"));

                rowsInline.add(rowInline);
                markupInline.setKeyboard(rowsInline);
                message.setReplyMarkup(markupInline);

                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if (update.getCallbackQuery().getData().equals("O")) {

                List<String> val = new ArrayList<>();
                Set<String> me = new HashSet<>();

                Firestore db = new Firebase().db;
                DocumentReference docRef = db.collection("debts").document(list.get(3));
                ApiFuture<WriteResult> future = db.collection("debts").document(list.get(3))
                        .collection("payees").document(user.getId().toString()).delete();


                ApiFuture<QuerySnapshot> ref = db.collection("debts").get();
                try {
                    QuerySnapshot query = ref.get();
                    List<QueryDocumentSnapshot> var = query.getDocuments();
                    for (DocumentSnapshot i : var) {
                        if (i.get("uid") == user.getId()) {
                            val.add(i.getId());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    for (String v : val) {
                        ApiFuture<QuerySnapshot> docRef2 = db.collection("debts").document(v).collection("payees").get();
                        QuerySnapshot future2 = docRef2.get();
                        for (DocumentSnapshot z : future2) {
                            me.add(z.get("name").toString());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String joined = String.join("\n", me);

                SendMessage message = new SendMessage() // Create a message object object
                        .setChatId(chat_id)
                        .setText(joined);
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

        }
    }


    public String addUser(String s, User u) {
        return s + "\n" + u.getFirstName();
    }

    public String removeUser(String s, User u) {
        String newString = "";

        List<String> list = Arrays.asList(s.split("\n"));
        for (String item : list) {
            System.out.println(item);
            if (!item.equals(u.getFirstName())) {
                newString += item + "\n";
            }
        }
        return newString;
    }


    public String getBotUsername() {
        // Return bot username
        // If bot username is @MyAmazingBot, it must return 'MyAmazingBot'
        return "payleh";
    }

    public String getBotToken() {
        // Return bot token from BotFather
        return "643779380:AAHT1HcKEkdW6_pv4SukLxvV49Ro7n2GIrE";
    }

}
