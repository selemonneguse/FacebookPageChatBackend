package com.example.backend.services;

import com.example.backend.entity.PageData;
import com.example.backend.controllers.MainController;
//import com.example.backend.repository.FacebookPageRepository;
import com.example.backend.utils.GeminiAiService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * This service class provides functionality to interact with Facebook's Graph API
 * for posting text messages, uploading photos, and retrieving existing posts
 * from a Facebook page. It leverages the Gemini AI service for generating unique
 * content for posts.
 */
@Service
public class FacebookService {

    @Autowired
//    private FacebookPageRepository repository;

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final Gson gson = new Gson();
    private final GeminiAiService geminiAiService;


    public FacebookService(GeminiAiService geminiAiService) {
        this.geminiAiService = geminiAiService;
//        this.repository = repository;
    }

    /**
     * Posts a generated message to a Facebook page using the Graph API.
     * The method uses the `generateUniqueFacebookPost` function to generate
     * a new posts message and sends it to the specified Facebook page.
     * On success, it returns a response containing the posted message.
     * On failure, it returns an error message and additional details.
     *
     * @return A map containing the result of the operation.
     *         If the operation is successful, the map contains keys "success" (true) and "message" (the posted content).
     *         If the operation fails, the map contains keys "error" (error description) and optionally "details" (additional failure information).
     */
    public Map<String, Object> postToFacebook(HttpSession session) {
        try {
            String pageId = (String) session.getAttribute("pageId");
            String pageAccessToken = (String) session.getAttribute("pageAccessToken");

            if (pageId == null || pageAccessToken == null) {
                return Map.of("error", "Missing pageId or pageAccessToken from session.");
            }

            String generatedMessage = generateUniqueFacebookPost(session);
            if (generatedMessage == null) {
                return Map.of("error", "Failed to generate a unique post message.");
            }

            Map<String, Object> postData = new HashMap<>();
            postData.put("message", generatedMessage);
            postData.put("access_token", pageAccessToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(gson.toJson(postData), headers);

            ResponseEntity<String> fbResponse = restTemplate.postForEntity(
                    "https://graph.facebook.com/v22.0/" + pageId + "/feed",
                    request,
                    String.class
            );

            Map fbData = gson.fromJson(fbResponse.getBody(), Map.class);
            if (fbData.containsKey("id")) {
                return Map.of("success", true, "message", generatedMessage);
            } else {
                return Map.of("error", "Failed to post", "details", fbData);
            }

        } catch (Exception e) {
            logger.error("Error processing post request", e);
            return Map.of("error", "Server error", "message", e.getMessage());
        }
    }

    /**
     * Uploads a photo to Facebook page
     *
     * @param imageUrl URL of the image to upload
     * @return Response map with upload status
     */
    public Map<String, Object> uploadPhotoToFacebook(HttpSession session, String imageUrl) {
        try {
            String pageId = (String) session.getAttribute("pageId");
            String pageAccessToken = (String) session.getAttribute("pageAccessToken");

            if (pageId == null || pageAccessToken == null) {
                return Map.of("error", "Missing pageId or pageAccessToken from session.");
            }

            if (imageUrl == null) {
                return Map.of("error", "No image URL provided");
            }

            String generatedMessage = generateUniqueFacebookPost(session);
            if (generatedMessage == null) {
                return Map.of("error", "Failed to generate a unique post message.");
            }

            Map<String, String> postData = new HashMap<>();
            postData.put("url", imageUrl);
            postData.put("access_token", pageAccessToken);
            postData.put("message", generatedMessage);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(gson.toJson(postData), headers);

            ResponseEntity<String> fbResponse = restTemplate.postForEntity(
                    "https://graph.facebook.com/" + pageId + "/photos", request, String.class);

            Map fbData = gson.fromJson(fbResponse.getBody(), Map.class);
            if (fbData.containsKey("id")) {
                return Map.of("success", true, "message", generatedMessage);
            } else {
                return Map.of("error", "Image upload from URL failed!", "details", fbData);
            }

        } catch (Exception e) {
            logger.error("Error processing upload photo request", e);
            return Map.of("error", "Server error", "message", e.getMessage());
        }
    }

    /**
     * Generates a unique Facebook post using Gemini AI
     *
     * @return Unique text for a Facebook post
     */
    private String generateUniqueFacebookPost(HttpSession session) {
        try {

            String pageId = (String) session.getAttribute("pageId");
            String pageAccessToken = (String) session.getAttribute("pageAccessToken");

            if (pageId == null || pageAccessToken == null) {
                return "Missing pageId or pageAccessToken from session.";
            }

            // Step 1: Get existing posts from the page
            List<String> existingMessages = getExistingPagePosts(session);
            System.out.println("Existing messages on page " + pageId + ": " + existingMessages);

            // Step 2: Create a prompt for the AI
            String aiPrompt = geminiAiService.createUniquePostPrompt(existingMessages);
            System.out.println("Sending prompt for page " + pageId + ": " + aiPrompt);

            // Step 3: Get a response from the AI
            List<Map<String, Object>> promptAsList = geminiAiService.createSingleUserMessage(aiPrompt);
            String textSentence = geminiAiService.generateText(promptAsList);

            System.out.println("Generated sentence for page " + pageId + ": " + textSentence);
            return textSentence;

        } catch (Exception e) {
            System.err.println("Error generating unique Facebook post for pageId" + e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves existing posts from a Facebook page
     *
     * @return List of existing posts message
     */
    private List<String> getExistingPagePosts(HttpSession session) {

        String pageId = (String) session.getAttribute("pageId");
        String pageAccessToken = (String) session.getAttribute("pageAccessToken");

        if (pageId == null || pageAccessToken == null) {
            return null;
        }

        String url = "https://graph.facebook.com/v22.0/" + pageId + "/feed?access_token=" + pageAccessToken;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        JsonObject data = gson.fromJson(response.getBody(), JsonObject.class);

        List<String> existingMessages = new ArrayList<>();
        if (data != null && data.has("data") && data.get("data").isJsonArray()) {
            JsonArray postsArray = data.getAsJsonArray("data");
            for (JsonElement postElement : postsArray) {
                if (postElement.isJsonObject()) {
                    JsonObject post = postElement.getAsJsonObject();
                    if (post.has("message") && post.get("message").isJsonPrimitive()) {
                        existingMessages.add(post.getAsJsonPrimitive("message").getAsString());
                    }
                }
            }
        }

        return existingMessages;
    }

//    public void savePageToken(String pageId, String accessToken) {
//        PageData user = new PageData();
//        user.setPageId(pageId);
//        user.setPageAccessToken(accessToken);
//        repository.save(user);
//    }
//
//    public void saveOrUpdate(PageData data) {
//        repository.save(data); // אם pageId כבר קיים – תתבצע עדכון
//    }
//
//    public Optional<PageData> getPageDataById(String pageId) {
//        return repository.findByPageId(pageId);
//    }
}