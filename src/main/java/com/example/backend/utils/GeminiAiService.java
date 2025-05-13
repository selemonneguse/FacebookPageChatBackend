package com.example.backend.utils;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * This service is responsible for interacting with the Gemini AI API to generate
 * AI-based content and creating structured prompts/messages necessary for communication with the API.
 */
@Service
public class GeminiAiService {

    @Value("${geminiai.api.key}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Gson gson = new Gson();

    /**
     * Calls the Gemini API to generate text based on provided messages
     *
     * @param messages List of messages in Gemini API format
     * @return Generated text from Gemini API
     */
    public String generateText(List<Map<String, Object>> messages) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", geminiApiKey);

            Map<String, Object> content = Map.of("contents", messages);
            HttpEntity<String> request = new HttpEntity<>(gson.toJson(content), headers);

            String url = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent";
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            Map<String, Object> responseData = gson.fromJson(response.getBody(), Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseData.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                System.err.println("⚠️ Warning: No 'candidates' found in Gemini response.");
                return "";
            }

            Map<String, Object> contentMap = (Map<String, Object>) candidates.get(0).get("content");
            if (contentMap == null) {
                System.err.println("⚠️ Warning: No 'content' field found in Gemini response.");
                return "";
            }

            List<Map<String, String>> partsList = (List<Map<String, String>>) contentMap.get("parts");
            if (partsList == null || partsList.isEmpty()) {
                System.err.println("⚠️ Warning: No 'parts' found in Gemini response.");
                return "";
            }

            return partsList.get(0).getOrDefault("text", "");

        } catch (Exception e) {
            System.err.println("🚨 Unexpected error in generateText function: " + e.getMessage());
            return "Unexpected error.";
        }
    }


    /**
     * Creates a prompt for generating a unique Facebook post
     *
     * @param existingMessages List of existing Facebook post messages
     * @return A prompt to send to Gemini AI
     */
    public String createUniquePostPrompt(List<String> existingMessages) {
        return "Give me a clear short sentence to post on a Facebook page that is different from these existing sentences: \"" +
                String.join("\", \"", existingMessages) + "\"";
    }

    /**
     * Creates a single message structure for Gemini API
     *
     * @param text Text content of the message
     * @return A properly formatted message structure
     */
    public List<Map<String, Object>> createSingleUserMessage(String text) {
        return List.of(
                Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", text))
                )
        );
    }

    /**
     * Uses Gemini AI to determine whether the given message expresses an intent to post.
     *
     * @param message The input message
     * @return true if Gemini determines it intends to post, false otherwise
     */
    public boolean isPostIntent(String message) {
        try {
            String prompt = "Does the following message indicate that the user wants to create post on Facebook? " +
                    "Reply with only 'true' or 'false'.\n\nMessage: \"" + message + "\"";

            List<Map<String, Object>> aiMessage = createSingleUserMessage(prompt);
            String response = generateText(aiMessage);

            // Normalize and evaluate the response
            String result = response.trim().toLowerCase();
            return result.equals("true");
        } catch (Exception e) {
            System.err.println("⚠️ Error while checking post intent with AI: " + e.getMessage());
            return false;
        }
    }

}