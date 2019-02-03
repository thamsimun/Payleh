import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.api.core.ApiFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.FileInputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Firebase {
    public static Firestore db;

    private static List<QueryDocumentSnapshot> databaseSnapshot;

    public Firebase() {
        // Instantiate Firebase
        try {
            FileInputStream serviceAccount =
                    new FileInputStream("src/main/Firebase/payleh-firebase-adminsdk.json");

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://payleh.firebaseio.com")
                    .build();

            FirebaseApp.initializeApp(options);
            db = FirestoreClient.getFirestore();
        } catch (Exception FileNotFoundException) {
            // nothing
        }

        this.databaseSnapshot = new ArrayList<QueryDocumentSnapshot>();
    }

    public static void addBill(String title, String details, String uid) {
        Map<String, String> docData = new HashMap<String, String>();
        docData.put("Title", title);
        docData.put("Details", details);
        docData.put("OwnerUID", uid);
        ApiFuture<DocumentReference> addedDocRef = db.collection("bills").add(docData);
        try {
            System.out.println("Added document with ID: " + addedDocRef.get().getId());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }

    private static List<String> noPayees(String bid) throws Exception{
        List<String> values = new ArrayList<>();
        ApiFuture<QuerySnapshot> ref = db.collection("bills").document(bid).collection("payees").get();
        QuerySnapshot query = ref.get();
        List<QueryDocumentSnapshot> list = query.getDocuments();
        for(DocumentSnapshot i : list){
            values.add(i.get("name").toString());
        }
        return values;
    }


    public static void addPayer(String bid, String uid) {
        Map<String, String> docData = new HashMap<String, String>();
        docData.put(uid, "");
        db.collection("bills").document(bid).collection("payers").document(uid).set(docData);
    }

    public static void deletePayer(String bid, String uid) {

        db.collection("bills").document(bid).collection("payers").document(uid).delete();
        deleteBillOrNot(bid);
    }

    public static List<UserPayer> getAllPayees(String myUid) {
        List<String> bids = new ArrayList<>();

        //asynchronously retrieve all documents
        ApiFuture<QuerySnapshot> future = db.collection("debts").get();
        // future.get() blocks on response
        List<QueryDocumentSnapshot> documents = null;
        try {
            documents = future.get().getDocuments();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        for (QueryDocumentSnapshot document : documents) {
            System.out.println(document.getId() + "\n");
            bids.add(document.getId());
        }

        List<UserPayer> users = new ArrayList<>();
        for (String s: bids) {
            List<UserPayer> temp = getPayers(s);
            for (UserPayer u: temp) {
                if (u.uid.equals(myUid)) {
                    users.add(u);
                }
            }
        }

        return users;
    }

    public static List<UserPayer> getPayers(String bid) {
        List<UserPayer> users = new ArrayList<>();

        //asynchronously retrieve all documents
        ApiFuture<QuerySnapshot> future = db.collection("debts").document(bid).collection("payees").get();
        // future.get() blocks on response
        List<QueryDocumentSnapshot> documents = null;
        try {
            documents = future.get().getDocuments();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        for (QueryDocumentSnapshot document : documents) {
            UserPayer u = new UserPayer(document.getId(), document.get("name").toString());
            users.add(u);
        }

        return users;
    }

    public static int getNumPayers(String bid) {
        return getPayers(bid).size();
    }

    public static List<String> getAllBids(String myUid) {
        List<String> bids = new ArrayList<>();

        //asynchronously retrieve all documents
        ApiFuture<QuerySnapshot> future = db.collection("debts").get();
        // future.get() blocks on response
        List<QueryDocumentSnapshot> documents = null;
        try {
            documents = future.get().getDocuments();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        for (QueryDocumentSnapshot document : documents) {
            if (document.get("uid").toString().equals(myUid)) {
                bids.add(document.getId());
            }
        }

        return bids;
    }

    public static List<UserPayer> getAllPayers(String myUid) {
        List<String> bids = getAllBids(myUid);
        List<UserPayer> users = new ArrayList<>();
        for (String s: bids) {
            users.addAll(getPayers(s));
        }

        return users;
    }

    private static void deleteBillOrNot(String bid) {

        boolean delete = true;

        Iterable<CollectionReference> collections =
                db.collection("bills").document(bid).getCollections();

        for (CollectionReference collRef : collections) {
            if (collRef.getId().equals("payers")) {
                delete = false;
            }
        }

        if (delete) {
            ApiFuture<WriteResult> writeResult = db.collection("bills").document(bid).delete();

            //debug
            try {
                System.out.println("Update time : " + writeResult.get().getUpdateTime());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }


    }

    private static void copyToList() {
        ApiFuture<QuerySnapshot> future = db.collection("bills").get();

        try {
            // future.get() blocks on response
            databaseSnapshot = future.get().getDocuments();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }


}