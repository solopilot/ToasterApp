package com.pejaver.toaster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.Gson;

public class Toaster extends Activity implements
        RecognitionListener {

    private TextView textViewSpeech;            // recognized speech from Google
    private TextView textViewResponse;          // understood by Toaster speech
    private TextView textViewAccepted;          // parameters accepted by Toaster
    private TextView textViewValues;            // current Toaster state
    private SpeechRecognizer speechRecognizer = null;
    private Intent recognizerIntent;
    private TextToSpeech textToSpeech;
    private String speechText = "";
    private String speechResponse = "";
    private String acceptedParams = "";
    private boolean listening = false;
    private final String LOG_TAG = "toaster";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textViewSpeech = findViewById(R.id.textViewSpeech);         // recognized speech from Google
        textViewResponse = findViewById(R.id.textViewResponse);     // understood by Toaster speech
        textViewAccepted = findViewById(R.id.textViewAccepted);     // parameters accepted by Toaster
        textViewValues = findViewById(R.id.textViewValues);         // current Toaster state

        // init speech recognizer
        recognizerIntent = startSpeechRecognizer();

        // init talker
        textToSpeech = new TextToSpeech(getApplicationContext(), err -> {
            if (err != TextToSpeech.ERROR)                      // To Choose language of speech
                textToSpeech.setLanguage(Locale.US);
        });

        TextView textViewTitle = findViewById(R.id.textViewTitle);
        textViewTitle.setOnLongClickListener(v -> {    // clear the fields
            speechText = "";
            speechResponse = "";
            acceptedParams = "";
            textViewSpeech.setText("");
            textViewResponse.setText("");
            textViewAccepted.setText("");
            textViewValues.setText("");
            return true;
        });
        textViewTitle.setOnClickListener(v -> {    // start / stop listening
            if (listening) {
                speechRecognizer.stopListening();
                listening = false;
            }
            else {
                Log.i(LOG_TAG, "Starting recognizer");
                speechRecognizer.startListening(recognizerIntent);
                listening = true;
            }
        });

        // start thread to do offline (network) tasks
        new Thread(thread).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (speechRecognizer == null)
            startSpeechRecognizer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
            Log.i(LOG_TAG, "destroy");
        }
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + Arrays.toString(buffer));
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
        listening = false;
    }

    @Override
    public void onError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        Log.d(LOG_TAG, "FAILED " + errorMessage);
        //textViewResult.setText(errorMessage);
        listening = false;
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.i(LOG_TAG, "onEvent");
    }

    @Override
    public void onPartialResults(Bundle arg0) {
        Log.i(LOG_TAG, "onPartialResults");     // will not be called by default
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.i(LOG_TAG, "onReadyForSpeech");
    }

    @Override
    public void onResults(Bundle results) {     // results of speech recognizer
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        speechText = "";
        assert matches != null;
        for (String result : matches)
            speechText += result + "\n";

        textViewSpeech.setText(speechText);     // result of SpeechRecognizer
        Log.i(LOG_TAG, "onResults: "+ speechText);
    }

    @Override
    public void onRmsChanged(float rmsdB) {}    // this gets called often.  Very loquacious

    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again";
                break;
        }
        return message;
    }

    public Intent startSpeechRecognizer() {
        Log.i(LOG_TAG, "startSpeechRecognizer");
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);
        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        return recognizerIntent;
    }

    // thread to do things that cannot be done from main UI thread
    Runnable thread = new Runnable() {
        @Override
        public void run() {
            String[] fanSpeeds = {"Off", "at Low", "at High"};
            try {
                for (;;) {
                    // send spoken words to Toaster for execution
                    if (!speechText.isEmpty()) {
                        if (!acceptedParams.isEmpty())
                            speechText += " " + acceptedParams;
                        sendData(speechText);          // send SpeechRecognizer results
                        speechText = "";
                    }

                    // speak Toaster response
                    if (!speechResponse.isEmpty()) {
                        Log.i("Speak", speechResponse);
                        textToSpeech.speak(speechResponse, TextToSpeech.QUEUE_FLUSH, null, null);
                        speechResponse = "";
                    }

                    // get current Toaster state and display it
                    if ((System.currentTimeMillis()/1000) % 5 == 0) {
                        final String valuesStr;
                        String response = getValues();
                        ToasterValues values = new Gson().fromJson(response, ToasterValues.class);
                        if (values != null) {   // if getValues() returned valid response
                            if (values.function == null) {
                                valuesStr = "Toaster is Off.\nCurrent Temperature is " + values.currentTemperature + "°F";
                            } else {
                                valuesStr = "Toaster function is: " + values.function +
                                        "\nFan is " + fanSpeeds[values.fan] +
                                        "\nTemperature setting is " + values.setPoint + "°F" +
                                        "\nCurrent Temperature is " + values.currentTemperature + "°F" +
                                        "\nDuration: " + Utils.convertSecs(values.duration) +
                                        "\nTime left: " + Utils.convertSecs(Math.round(values.timeLeft));
                            }
                        }
                        else
                            valuesStr = "Toaster is toast";
                        runOnUiThread(() -> textViewValues.setText(valuesStr));
                    }

                    // wait a sec
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    // called in thread
    public void sendData(String text) {
        HttpURLConnection connection = null;
        try {
            text = text.replaceAll("°", " degrees");    // replace degree symbol
            text = text.replaceAll("\n", "");           // remove newline
            text = "{\"speech\": \"" + text + "\"}\r\n";
            Log.i(LOG_TAG, "sendData: len="+text.length()+" <"+text+">");
            //URL url = new URL("http://192.168.1.61:8080");
            URL url = new URL("https://192.168.1.236:8080/toaster/1/speech");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "text/json");
            //connection.setRequestProperty("Content-Length", String.valueOf(text.length()));
            connection.setDoOutput(true);
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            out.write(text);
            out.flush();
            out.close();

            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                speechResponse = "";
                while ((line = in.readLine()) != null) {
                    Log.i("data", line);
                    speechResponse += line;     // text to speech it out
                }

                String[] parts = speechResponse.split("\\|");
                speechResponse = parts[0];          // this part is to be spoken out
                acceptedParams = parts.length > 1? parts[1] : "";  // to be appended to next speech sentence sent
                runOnUiThread(() -> {textViewResponse.setText(speechResponse);
                                     textViewAccepted.setText(acceptedParams);});
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            connection.disconnect();
        }
    }

    // data that comes from Toaster for getValues()
    class Step {
        String function;
        int temperature;        // in degrees F
        int duration;           // in seconds
        int fan;                // 0, 1, 2
        boolean beepAtEnd;
    }
    class ToasterValues {
        String function;
        int fan;                // 0, 1, 2
        String selectedBy;
        int duration;
        float timeLeft;
        int setPoint;
        float currentTemperature;
        boolean ovenReady;
        Step[] program;
        float time;
    }

    // get current values from toaster
    // called in thread
    public String getValues() {
        HttpURLConnection connection = null;
        String getResponse = "";
        try {
            URL url = new URL("https://192.168.1.236:8080/toaster/1/values");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    Log.i("data", line);
                    getResponse += line;     // text to speech it out
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            connection.disconnect();
        }
        return getResponse;
    }
}