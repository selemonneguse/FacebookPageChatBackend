package com.example.backend.controllers;

import com.example.backend.services.FacebookService;
import com.example.backend.services.CloudinaryService;
import com.example.backend.utils.GeminiAiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;


/**
 * MainController is a REST controller that manages HTTP endpoints related to chat interactions
 * and Facebook-related operations.
 *
 * It provides the following functionalities:
 * - Handling chat requests and generating responses.
 * - Managing image uploads and posting them to Facebook.
 * - Posting text content to Facebook.
 * - Uploading photos to Facebook from a provided URL.
 */
@RestController
@RequestMapping("/chat")
@CrossOrigin
public class MainController {

    private final FacebookService facebookService;
    private final GeminiAiService geminiAiService;
    private final CloudinaryService cloudinaryService;

    public MainController(FacebookService facebookService, GeminiAiService geminiAiService, CloudinaryService cloudinaryService) {
        this.facebookService = facebookService;
        this.geminiAiService = geminiAiService;
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * Handles chat requests by analyzing the provided messages in the payload,
     * responding accordingly, or performing specific actions like posting to Facebook.
     *
     * @param payload The request body containing a map with the messages list.
     *                Expected key: "messages" (a list of message objects).
     * @return A ResponseEntity containing a map with the response, which may include:
     *         - "reply": the generated response or status update message.
     *         - "error": a description of any error, if an issue occurs.
     */
    @PostMapping("")
    public ResponseEntity<Map<String, Object>> handleChatRequest(@RequestBody Map<String, Object> payload) {
        try {
            List<Map<String, Object>> messages = (List<Map<String, Object>>) payload.get("messages");

            if (messages == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid request body"));
            }

            String userText = extractLastUserMessage(messages);
            if (userText == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "No valid user message found"));
            }

            // Check if the user wants to post to Facebook
            if (geminiAiService.isPostIntent(userText)) {
                Map<String, Object> fbResponse = facebookService.postToFacebook();
                if (Boolean.TRUE.equals(fbResponse.get("success"))) {
                    return ResponseEntity.ok(Map.of("reply", "Post uploaded successfully! Message: " + fbResponse.get("message")));
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("reply", "Post upload failed!", "error", fbResponse));
                }
            }

            // Generate text response using Gemini AI
            String reply = geminiAiService.generateText(messages);
            return ResponseEntity.ok(Map.of("reply", reply));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Something went wrong!"));
        }
    }

    /**
     * Handles the upload of an image file to an external image hosting service and posts it to Facebook.
     *
     * This method checks if the provided image file is valid and uploads it to Cloudinary.
     * Upon successful upload, the image is posted to Facebook using the FacebookService.
     * Handles errors by providing appropriate HTTP responses.
     *
     * @param file The uploaded image file. Must be a non-empty {@code MultipartFile}.
     * @return A ResponseEntity containing a map with the operation result:
     *         - On success: A map with the Facebook response details.
     *         - On failure: A map containing error details with appropriate HTTP status codes.
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> handleUploadImage(@RequestParam("image") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No image file uploaded"));
            }


            String imageUrl = cloudinaryService.uploadImage(file);

            Map<String, Object> response = facebookService.uploadPhotoToFacebook(imageUrl);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Server error"));
        }
    }

    /**
     * Helper method to extract the last user message from the messages list
     *
     * @param messages List of messages
     * @return The text of the last user message, or null if not found
     */
    private String extractLastUserMessage(List<Map<String, Object>> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<String, Object> msg = messages.get(i);
            if ("user".equals(msg.get("role"))) {
                List<Map<String, String>> parts = (List<Map<String, String>>) msg.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return parts.get(0).get("text");
                }
            }
        }
        return null;
    }
}