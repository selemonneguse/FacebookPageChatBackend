package com.example.backend.controllers;

import com.example.backend.entity.PageData;
import com.example.backend.services.FacebookService;
import com.example.backend.services.CloudinaryService;
import com.example.backend.utils.GeminiAiService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.Cookie;


/**
 * MainController is a REST controller that manages HTTP endpoints related to chat interactions
 * and Facebook-related operations.
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

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private final FacebookService facebookService;
    private final GeminiAiService geminiAiService;
    private final CloudinaryService cloudinaryService;

    @Autowired
    private TaskScheduler taskScheduler;

    public MainController(FacebookService facebookService, GeminiAiService geminiAiService, CloudinaryService cloudinaryService) {
        this.facebookService = facebookService;
        this.geminiAiService = geminiAiService;
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * Handles chat requests by analyzing the provided messages in the payload,
     * responding accordingly, or performing specific actions like posting to Facebook.
     *
     * @param payload The request body containing a map with the message list.
     *                Expected key: "messages" (a list of message objects).
     * @return A ResponseEntity containing a map with the response, which may include:
     *         - "reply": the generated response or status update message.
     *         - "error": a description of any error, if an issue occurs.
     */
    @PostMapping("")
    public ResponseEntity<Map<String, Object>> handleChatRequest(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {

            HttpSession session = request.getSession(false);
            System.out.println("🔍 POST - Session object: " + session);

            Object messagesObj = payload.get("messages");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> messages = (List<Map<String, Object>>) messagesObj;

            String userText = extractLastUserMessage(messages);

            AtomicReference<String> dateRef = new AtomicReference<>();

            if (userText == null || !(messagesObj instanceof List<?>) || session == null) {
                logger.warn("No valid user message found in the request");
                return ResponseEntity.badRequest().body(Map.of("error", "No valid user message found"));
            }

            // Check if the user wants to post to Facebook
            if (geminiAiService.isPostIntent(userText)) {
                logger.debug("Post intent detected, attempting to post to Facebook");
                Map<String, Object> fbResponse = facebookService.postToFacebook(session);

                if (Boolean.TRUE.equals(fbResponse.get("success"))) {
                    logger.info("Successfully posted to Facebook");
                    return ResponseEntity.ok(Map.of("reply", "Post uploaded successfully! Message: " + fbResponse.get("message")));
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("reply", "Post upload failed!", "error", fbResponse));
                }
            } else if (geminiAiService.isScheduledPostIntent(userText, dateRef)) {
                String dateString = dateRef.get();
                try {
                    // נניח שהפורמט הוא: "2025-07-03 14:30"
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                    LocalDateTime scheduledDateTime = LocalDateTime.parse(dateString, formatter);

                    // המשימה שתתבצע בזמן המתוזמן
                    Runnable task = () -> {
                        System.out.println("🕒 Scheduled post triggered at: " + LocalDateTime.now());
                        facebookService.postToFacebook(session);
                    };

                    // תרגום הזמן ל־java.util.Date עבור המתזמן
                    Date executionTime = Date.from(scheduledDateTime.atZone(ZoneId.systemDefault()).toInstant());

                    // תזמון המשימה
                    taskScheduler.schedule(task, executionTime);

                    System.out.println("✅ Post scheduled for: " + scheduledDateTime);

                } catch (DateTimeParseException e) {
                    System.err.println("⚠️ תאריך לא תקין: " + dateString);
                }
            }


            // Generate text response using Gemini AI
            String reply = geminiAiService.generateText(messages);
            return ResponseEntity.ok(Map.of("reply", reply));

        } catch (Exception e) {
            logger.error("Error processing chat request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Something went wrong!"));
        }
    }

    /**
     * Handles the upload of an image file to an external image hosting service and posts it to Facebook.
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
    public ResponseEntity<Map<String, Object>> handleUploadImage(@RequestParam("image") MultipartFile file, HttpServletRequest request) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No image file uploaded"));
            }

            String imageUrl = cloudinaryService.uploadImage(file);

            Map<String, Object> reply = facebookService.uploadPhotoToFacebook(request.getSession(false), imageUrl);
            return ResponseEntity.ok(Map.of("reply", reply));

        } catch (Exception e) {
            logger.error("Error processing chat request", e);
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
                Object partsObj = msg.get("parts");
                if (partsObj instanceof List<?> parts && !parts.isEmpty() && parts.get(0) instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> firstPart = (Map<String, String>) parts.get(0);
                    return firstPart.get("text");
                }
            }
        }
        return null;
    }

    @PostMapping("/facebook/page-data")
    public ResponseEntity<?> receivePageData(@RequestBody PageData data, HttpServletRequest request,  HttpServletResponse response) {

        String pageId = (String) data.getPageId();
        String pageAccessToken = (String) data.getPageAccessToken();

        if (pageId == null || pageAccessToken == null) {
            return (ResponseEntity<?>) Map.of("error", "Session does not contain pageId or pageAccessToken");
        }

        System.out.println("✔️ Saved in session pageId: " + pageId);
        System.out.println("✔️ Saved in session pageAccessToken : " + pageAccessToken);

        // צור או שלוף סשן קיים
        HttpSession session = request.getSession(true);

        // שמור מידע בסשן
        session.setAttribute("pageId", data.getPageId());
        session.setAttribute("pageAccessToken", data.getPageAccessToken());

        // צור עוגייה עם מזהה הסשן
        Cookie cookie = new Cookie("JSESSIONID", session.getId());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(60 * 60 * 24 * 30); // חודש
        response.addCookie(cookie);

        System.out.println("✔️ Saved session with ID: " + session.getId());

        return ResponseEntity.ok("Session created and cookie sent");
    }

    @GetMapping("/facebook/check-session")
    public ResponseEntity<?> checkSession(HttpServletRequest request) {

        System.out.println("🔍 GET /check-session - Cookies: " + Arrays.toString(request.getCookies()));
        System.out.println("🔍 GET - Session ID from request: " + request.getRequestedSessionId());
        System.out.println("🔍 GET - User-Agent: " + request.getHeader("User-Agent"));
        System.out.println("🔍 GET - Origin: " + request.getHeader("Origin"));

        HttpSession session = request.getSession(false);
        System.out.println("🔍 GET - Session object: " + session);

        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No session");
        }

        String pageId = (String) session.getAttribute("pageId");
        String token = (String) session.getAttribute("pageAccessToken");

        if (pageId == null || token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Session found, but missing data");
        }

        return ResponseEntity.ok("Welcome back!");
    }
}