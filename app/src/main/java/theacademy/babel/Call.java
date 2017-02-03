package theacademy.babel;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

//import com.google.android.gms.appindexing.Action;
//import com.google.android.gms.appindexing.AppIndex;
//import com.google.android.gms.appindexing.Thing;
//import com.google.android.gms.common.api.GoogleApiClient;
import com.firebase.client.Firebase;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.developer_cloud.dialog.v1.model.Message;
import com.ibm.watson.developer_cloud.language_translator.v2.LanguageTranslator;
import com.ibm.watson.developer_cloud.language_translator.v2.model.Language;
import com.ibm.watson.developer_cloud.language_translator.v2.model.TranslationResult;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.BaseRecognizeCallback;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


public class Call extends ActionBarActivity implements View.OnClickListener,
        MessageSource.MessagesCallbacks{

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    //private GoogleApiClient client;
    LanguageTranslator LT = new LanguageTranslator();
    String[] availableLanguages;
    TextToSpeech tts = new TextToSpeech();
    StreamPlayer streamPlayer;
    List<Voice> voiceList;
    Spinner voiceToTranslateTo;
    Spinner langToTranslateTo;
    EditText textField;
    TextView outputText;
    ToggleButton micToggle;
    InputStream inputStream;
    SpeechToText stt;
    Voice tempVoice;
    Language lang;

    public static final String USER_EXTRA = "USER";

    public static final String TAG = "ChatActivity";
    private static final int RESULT_SPEECH = 1;

    private ArrayList<Messages> mMessages;
    private MessagesAdapter mAdapter;
    private String mRecipient;
    private String mSender;
    private ListView mListView;
    private Date mLastMessageDate = new Date();
    private String mConvoId;
    private MessageSource.MessagesListener mListener;
    TextView txtText;
    Button btnSpeak;
    String temp = new String();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Firebase.setAndroidContext(this);


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        //client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        langToTranslateTo = (Spinner) findViewById(R.id.langSpinner);
        LT.setUsernameAndPassword("d53dacb3-0a8a-41d5-b7eb-ab5f2d95acc1", "SNW4VN2APaZR");

        availableLanguages = new String[Language.class.getEnumConstants().length];
        int count=0;
        for (Language l : Language.class.getEnumConstants())
        {
            availableLanguages[count] = l.name();
            count++;
        }
        ArrayAdapter<String> langListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, availableLanguages);
        langToTranslateTo.setAdapter(langListAdapter);


        tts.setUsernameAndPassword("0cf99e8d-2579-4fb8-aa7a-c4309f0137b1","LhnflMGeXr66");
        voiceToTranslateTo = (Spinner) findViewById(R.id.voiceSpinner);
        voiceList = tts.getVoices().execute();
        String[] availableVoices = new String[voiceList.size()];
        Iterator<Voice> it2 = voiceList.iterator();
        for (int i=0; i<voiceList.size(); i++)
        {
            if(it2.hasNext())
                availableVoices[i] = it2.next().getDescription().toString();
        }
        ArrayAdapter<String> voiceListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, availableVoices);
        voiceToTranslateTo.setAdapter(voiceListAdapter);

        micToggle = (ToggleButton) findViewById(R.id.toggleButton);


    }

    private String getLangLocale(String lang) {
        if (lang.equals("ar")) return"ar_EG";
        else if (lang.equals("en")) return"en_US";
        else if (lang.equals("es")) return"es_ES";
        else if (lang.equals("fr")) return"fr_FR";
        else if (lang.equals("it")) return"it_IT";
        else return "pt_PT";
    }

    public void changeLayout(View view) {

        final EditText recipient =  (EditText)findViewById(R.id.Recipient);
        mRecipient = recipient.getText().toString();
        final EditText sender =  (EditText)findViewById(R.id.Sender);
        mSender = sender.getText().toString();
        tempVoice = voiceList.get(voiceToTranslateTo.getSelectedItemPosition());
        lang = Language.valueOf(langToTranslateTo.getSelectedItem().toString());
        Log.e("Lang:", lang.toString() + "\t" + getLangLocale(lang.toString()));
        setContentView(R.layout.chat);

        mListView = (ListView)findViewById(R.id.message_list);
        mMessages = new ArrayList<>();
        mAdapter = new MessagesAdapter(mMessages);
        mListView.setAdapter(mAdapter);

        setTitle(mRecipient);
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Button sendMessage = (Button)findViewById(R.id.send_message);
        sendMessage.setOnClickListener(this);

        String[] ids = {mRecipient,"-", mSender};
        Arrays.sort(ids);
        mConvoId = ids[0]+ids[1]+ids[2];

        mListener = MessageSource.addMessagesListener(mConvoId, this, mSender);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);


                    Messages msg = new Messages();
                    msg.setmDate(new Date());
                    msg.setmText(text.get(0));
                    msg.setmSender(mSender);

                    MessageSource.saveMessage(msg, mConvoId);
                }
                break;
            }

        }
    }

    public void onClick(View v) {



        btnSpeak = (Button) findViewById(R.id.send_message);

        btnSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent intent = new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getLangLocale(lang.toString()));

                try {
                    startActivityForResult(intent, RESULT_SPEECH);

                } catch (ActivityNotFoundException a) {
                    Toast t = Toast.makeText(getApplicationContext(),
                            "Opps! Your device doesn't support Speech to Text",
                            Toast.LENGTH_SHORT);
                    t.show();
                }
            }
        });


    }

/*

    public void buttonOnClick(View view) {




    }

    private RecognizeOptions getRecognizeOptions() {
        return new RecognizeOptions.Builder()
                .continuous(true)
                .contentType(ContentType.OPUS.toString())
                .model("en-US_BroadbandModel")
                .interimResults(true)
                .inactivityTimeout(2000).build();
    }

    public void micToggle(View view){
        if (micToggle.getText() == "mic on")
        {
            inputStream = new MicrophoneInputStream(true);

            stt = new SpeechToText();
            stt.setUsernameAndPassword("f0a4c993-f533-4812-9f5d-5f66bc43648b","pufxJVTHxY4t");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        stt.recognizeUsingWebSocket(inputStream, getRecognizeOptions(),
                                new BaseRecognizeCallback() {
                                    @Override
                                    public void onTranscription(SpeechResults speechResults) {
                                        try {
                                            String text = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();
                                            outputText.setText(text);
                                        } catch (IndexOutOfBoundsException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        e.printStackTrace();
                                    }

                                    @Override
                                    public void onDisconnected() {
                                    }
                                });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        else
        {
            try {
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }*/

    @Override
    public void onMessageAdded(Messages message) {
        mMessages.add(message);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MessageSource.stop(mListener);
    }



    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */

    /*
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Call Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }*/

    private class MessagesAdapter extends ArrayAdapter<Messages> {
        MessagesAdapter(ArrayList<Messages> messages){
            super(Call.this, R.layout.item, R.id.msg, messages);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = super.getView(position, convertView, parent);
            Messages message = getItem(position);

            TextView nameView = (TextView)convertView.findViewById(R.id.msg);
            nameView.setText(message.getmText());
            if(!lang.equals(Language.ENGLISH)) {
                TranslationResult result = LT.translate(message.getmText(), Language.ENGLISH, lang).execute();
                nameView.setText(result.getFirstTranslation().toString());
            }

            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams)nameView.getLayoutParams();
            temp = "";

            int sdk = Build.VERSION.SDK_INT;
            if (message.getmSender().equals(mSender)){
                if (sdk >= Build.VERSION_CODES.JELLY_BEAN) {
                    nameView.setBackground(getDrawable(R.drawable.bubble_right_green));
                } else{
                    nameView.setBackgroundDrawable(getDrawable(R.drawable.bubble_right_green));
                }
                layoutParams.gravity = Gravity.RIGHT;
                nameView.setLayoutParams(layoutParams);
            }else{
                if (sdk >= Build.VERSION_CODES.JELLY_BEAN) {
                    nameView.setBackground(getDrawable(R.drawable.bubble_left_gray));
                } else{
                    nameView.setBackgroundDrawable(getDrawable(R.drawable.bubble_left_gray));
                }

                layoutParams.gravity = Gravity.LEFT;
                nameView.setLayoutParams(layoutParams);
                temp = nameView.getText().toString();

            }

            if (!temp.isEmpty() && (position == mMessages.size() - 1))
            {
                System.out.println(temp);
                streamPlayer = new StreamPlayer();
                streamPlayer.playStream(tts.synthesize(temp, tempVoice).execute());

            }


            return convertView;
        }


    }

}
