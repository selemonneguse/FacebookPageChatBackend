package com.example.backend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Service for handling image uploads to Cloudinary.
 *
 * This class integrates with the Cloudinary API to facilitate the uploading
 * of image files and returning their respective URLs. It uses the Spring
 * Framework to manage dependencies and simplify the service configuration.
 */
@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    /**
     * Uploads an image to Cloudinary and returns its URL.
     *
     * @param file the image file to upload; must not be null and should conform to the MultipartFile format
     * @return the URL of the uploaded image as a String
     * @throws IOException if an I/O error occurs during file upload
     */
    public String uploadImage(MultipartFile file) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        return (String) uploadResult.get("url");
    }
}
