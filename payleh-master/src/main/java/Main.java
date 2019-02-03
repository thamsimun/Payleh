import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.FirebaseApp;

import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileInputStream;

public class Main {
    public static void main(String[] args) {
        // Initialize Api Context
        ApiContextInitializer.init();

        // Instantiate Telegram Bots API
        TelegramBotsApi botsApi = new TelegramBotsApi();

        Firebase firebase = new Firebase();

        // Register our bot
        try {
            botsApi.registerBot(new MyAmazingBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }


    }
}
