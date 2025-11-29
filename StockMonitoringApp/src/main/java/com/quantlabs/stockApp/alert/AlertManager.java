package com.quantlabs.stockApp.alert;

import java.awt.Toolkit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.mail.AuthenticationFailedException;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AlertManager {
    private final MessageManager messageManager;
    private final ConfigManager configManager;
    private final JLabel statusLabel;
    private ScheduledExecutorService executor;
    private boolean isAlerting = false;
    
    static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AlertManager(MessageManager messageManager, ConfigManager configManager, JLabel statusLabel) {
        this.messageManager = messageManager;
        this.configManager = configManager;
        this.statusLabel = statusLabel;
        this.executor = Executors.newScheduledThreadPool(1);
    }

    public void startAlerting(long intervalSeconds) {
        if (!isAlerting) {
            if (executor == null || executor.isShutdown() || executor.isTerminated()) {
                executor = Executors.newScheduledThreadPool(1);
                System.out.println("Created new ScheduledExecutorService");
            }
            try {
                isAlerting = true;
                executor.scheduleAtFixedRate(() -> checkAndSendAlerts(), 0, intervalSeconds, TimeUnit.SECONDS);
                updateStatusLabel("Alert system started with interval: " + intervalSeconds + " seconds");
            } catch (RejectedExecutionException e) {
                isAlerting = false;
                updateStatusLabel("Error starting alert system: " + e.getMessage());
                System.err.println("Failed to start alerting: " + e.getMessage());
            }
        }
    }

    public void stopAlerting() {
        if (isAlerting) {
            isAlerting = false;
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
                System.err.println("Interrupted while stopping alert system: " + e.getMessage());
            }
            updateStatusLabel("Alert system stopped");
        }
    }

    private void checkAndSendAlerts() {
        JSONObject alertConfig = configManager.getAlertConfig();
        boolean useBuzz = alertConfig.optBoolean("useBuzz", false);
        boolean useGmail = alertConfig.optBoolean("useGmail", false);
        boolean useOneSignal = alertConfig.optBoolean("useOneSignal", false);

        List<MessageManager.Message> newMessages = messageManager.getNewMessages();
        for (MessageManager.Message msg : newMessages) {
            String messageContent = "Header: " + msg.getHeader() + "\nBody: " + msg.getBody() + "\nTimestamp: " +
                    msg.getTimestamp().format(TIMESTAMP_FORMATTER);

            if (useBuzz) {
                sendBuzzAlert(messageContent);
            }
            if (useGmail) {
                sendGmailAlert(messageContent, alertConfig);
            }
            if (useOneSignal) {
                sendOneSignalAlert(messageContent, alertConfig);
            }
            msg.setStatus(MessageManager.MessageStatus.SENT);
        }
    }

    public void sendBuzzAlert(String message) {
        boolean soundPlayed = false;
        try {
            for (int i = 0; i < 3; i++) {
                playTone(800, 400);
                Thread.sleep(200);
            }
            soundPlayed = true;
        } catch (LineUnavailableException e) {
            System.err.println("Audio line unavailable: " + e.getMessage());
            updateStatusLabel("Error: Audio line unavailable for buzz alert: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error playing buzz sound: " + e.getMessage());
            updateStatusLabel("Error playing buzz sound: " + e.getMessage());
        }

        if (!soundPlayed) {
            System.err.println("Falling back to Toolkit beep");
            for (int i = 0; i < 3; i++) {
                Toolkit.getDefaultToolkit().beep();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    System.err.println("Interrupted during beep: " + ie.getMessage());
                }
            }
            updateStatusLabel("Buzz alert: Using fallback beep due to audio error");
        }

        System.out.println("Buzz Alert: " + message);
        if (soundPlayed) {
            updateStatusLabel("Sent buzz alert: " + message.substring(0, Math.min(message.length(), 50)) + "...");
        }
    }

    private void playTone(int frequency, int durationMs) throws LineUnavailableException {
        int sampleRate = 44100;
        byte[] buffer = new byte[sampleRate * durationMs / 1000];
        for (int i = 0; i < buffer.length; i++) {
            double angle = i / (sampleRate / (double) frequency) * 2.0 * Math.PI;
            buffer[i] = (byte) (Math.sin(angle) * 127.0);
        }
        AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format);
        line.start();
        line.write(buffer, 0, buffer.length);
        line.drain();
        line.close();
    }

    private void sendGmailAlert(String message, JSONObject config) {
        String fromEmail = config.optString("gmailFromEmail", "");
        String password = config.optString("gmailPassword", "");
        String toEmail = config.optString("gmailToEmail", "");
        String smtpPort = config.optString("smtpPort", "587");

        if (fromEmail.isEmpty() || password.isEmpty() || toEmail.isEmpty() || smtpPort.isEmpty()) {
            updateStatusLabel("Error: Gmail configuration incomplete");
            System.err.println("Gmail alert failed: Incomplete configuration - " +
                    "fromEmail: " + (fromEmail.isEmpty() ? "empty" : "set") +
                    ", password: " + (password.isEmpty() ? "empty" : "set") +
                    ", toEmail: " + (toEmail.isEmpty() ? "empty" : "set") +
                    ", smtpPort: " + (smtpPort.isEmpty() ? "empty" : smtpPort));
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.debug", "true"); // Enable debug output for troubleshooting

        try {
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(fromEmail, password);
                }
            });

            Message email = new MimeMessage(session);
            email.setFrom(new InternetAddress(fromEmail));
            email.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            email.setSubject("Stock Alert: " + message.split("\n")[0]);
            email.setText(message);
            Transport.send(email);
            updateStatusLabel("Sent Gmail alert to " + toEmail);
            System.out.println("Successfully sent Gmail alert to " + toEmail);
        } catch (AuthenticationFailedException e) {
            updateStatusLabel("Gmail authentication failed: Application-specific password required. See https://support.google.com/mail/?p=InvalidSecondFactor");
            System.err.println("Gmail authentication failed: " + e.getMessage() +
                    ". Ensure 2-Step Verification is enabled and use an application-specific password.");
        } catch (MessagingException e) {
            updateStatusLabel("Error sending Gmail alert: " + e.getMessage());
            System.err.println("MessagingException in sendGmailAlert: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            updateStatusLabel("Unexpected error sending Gmail alert: " + e.getMessage());
            System.err.println("Unexpected error in sendGmailAlert: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendOneSignalAlert(String message, JSONObject config) {
        String appId = config.optString("oneSignalAppId", "");
        String apiKey = config.optString("oneSignalApiKey", "");
        String userId = config.optString("oneSignalUserId", "");

        if (appId.isEmpty() || apiKey.isEmpty() || userId.isEmpty()) {
            updateStatusLabel("Error: OneSignal configuration incomplete");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        String json = null;
		try {
			json = new JSONObject()
			        .put("app_id", appId)
			        .put("include_external_user_ids", new String[]{userId})
			        .put("target_channel", "push")
			        .put("contents", new JSONObject().put("en", message))
			        .put("headings", new JSONObject().put("en", "Stock Alert"))
			        .toString();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        Request request = new Request.Builder()
                .url("https://api.onesignal.com/notifications")
                .addHeader("Authorization", "Basic " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                updateStatusLabel("Sent OneSignal alert");
            } else {
                updateStatusLabel("Error sending OneSignal alert: " + response.message());
            }
        } catch (Exception e) {
            updateStatusLabel("Error sending OneSignal alert: " + e.getMessage());
        }
    }

    public void sendTestBuzzAlert() {
        sendBuzzAlert("Test Buzz Alert\nThis is a test message sent at " +
                      LocalDateTime.now().format(TIMESTAMP_FORMATTER));
    }

    public void sendTestGmailAlert() {
        JSONObject alertConfig = configManager.getAlertConfig();
        sendGmailAlert("Test Gmail Alert\nThis is a test message sent at " +
                       LocalDateTime.now().format(TIMESTAMP_FORMATTER), alertConfig);
    }

    public void sendTestOneSignalAlert() {
        JSONObject alertConfig = configManager.getAlertConfig();
        sendOneSignalAlert("Test OneSignal Alert\nThis is a test message sent at " +
                           LocalDateTime.now().format(TIMESTAMP_FORMATTER), alertConfig);
    }

    private void updateStatusLabel(String text) {
        SwingUtilities.invokeLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(text);
            }
        });
    }
}