package deb.easyaccess.looping.evaluator;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

public class SmokeTestResultEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmokeTestResultEvaluator.class);
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        (new SmokeTestResultEvaluator()).evaluate();
        EXECUTOR.shutdown();
    }

    private void evaluate() {
        LOGGER.info("Starting evaluation of Smoke Test results");
        
        Map<String, CompletableFuture<String>> futures = new HashMap<>();
        
        // Create async operations with individual timeouts
        futures.put("Trade picked up by Nasdaq", createAsyncOperation("Operation 1", 5, TimeUnit.SECONDS));
        futures.put("Trade received in MMDC Core", createAsyncOperation("Operation 2", 10, TimeUnit.SECONDS));
        futures.put("Trade Confirmed", createAsyncOperation("Operation 3", 15, TimeUnit.SECONDS));
        futures.put("NDX Matched", createAsyncOperation("Operation 4", 15, TimeUnit.SECONDS));
        futures.put("NSCC Matched", createAsyncOperation("Operation 5", 15, TimeUnit.SECONDS));
        
        LOGGER.info("Created {} async operations with individual timeouts", futures.size());
        
        // Iterate over the Set to verify completion
        List<String> successfulResults = new ArrayList<>();
        Map<String, String> failedResults = new HashMap<>();
        
        for (Map.Entry<String, CompletableFuture<String>> futureEntrySet : futures.entrySet()) {
            try {
                CompletableFuture<String> future = futureEntrySet.getValue();
                String result = future.join();
                successfulResults.add(result);
                LOGGER.info("Operation completed successfully: {}", result);
            } catch (CompletionException e) {
                String error = e.getCause() instanceof TimeoutException ? "Operation timed out" : "Operation failed: " + e.getCause().getMessage();
                failedResults.put(futureEntrySet.getKey(), error);
                LOGGER.error("Operation failed: {}", error);
            }
        }
        
        LOGGER.info("Evaluation complete. Successful: {}, Failed: {}", successfulResults.size(), failedResults.size());
        
        // Send email with results
        sendEmail(successfulResults, failedResults);
    }
    
    private CompletableFuture<String> createAsyncOperation(String operationName, long timeout, TimeUnit unit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Executing {}", operationName);
                Thread.sleep(3000); // Simulate work
                return operationName + " completed";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
        }, EXECUTOR).orTimeout(timeout, unit).exceptionally(ex -> {
            if (ex instanceof TimeoutException) {
                throw new CompletionException(ex);
            }
            throw new CompletionException(ex);
        }).thenApply(result -> result);
    }
    
    private void sendEmail(List<String> successfulResults, Map<String, String> failedResults) {
        LOGGER.info("Preparing email with evaluation results");
        
        String host = "smtp.gmail.com";
        String port = "587";
        String username = "your-email@gmail.com";
        String password = "your-app-password";
        String to = "recipient@example.com";
        
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("Smoke Test Results");
            
            String emailBody = buildEmailBody(successfulResults, failedResults);
            message.setText(emailBody);
            
            Transport.send(message);
            LOGGER.info("Email sent successfully to {}", to);
        } catch (MessagingException e) {
            LOGGER.error("Failed to send email: {}", e.getMessage(), e);
        }
    }
    
    private String buildEmailBody(List<String> successfulResults, Map<String, String> failedResults) {
        StringBuilder body = new StringBuilder();
        body.append("Smoke Test Results\n");
        body.append("========================\n\n");
        
        body.append("Successful Operations (").append(successfulResults.size()).append("):\n");
        successfulResults.forEach(result -> body.append("  - ").append(result).append("\n"));
        
        body.append("\nFailed Operations (").append(failedResults.size()).append("):\n");
        failedResults.forEach((scenario, error) -> body.append("  - ").append(error).append("\n"));
        
        body.append("\nTotal Operations: ").append(successfulResults.size() + failedResults.size());
        
        return body.toString();
    }
}
