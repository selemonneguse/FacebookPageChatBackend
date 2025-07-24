# Backend Service

## Overview

This is a backend service built with Spring Boot that provides a RESTful API interface. The service is configured to
work with a frontend application running on `localhost:3000`.

## Technologies

- Java 24
- Spring Boot
- Spring MVC
- Spring Data JPA
- Jakarta EE

## Features

- RESTful API endpoints
- CORS configuration for secure cross-origin requests
- Integration with frontend application
- Scheduled task execution using Spring Scheduler

## Prerequisites

- Java Development Kit (JDK) 24
- Maven (for dependency management)
- Your preferred IDE (IntelliJ IDEA recommended)

## Configuration

The application includes CORS (Cross-Origin Resource Sharing) configuration to allow secure communication with the
frontend application. The following settings are configured:

- Allowed Origin: `http://localhost:3000`
- Allowed Methods: GET, POST, PUT, DELETE, OPTIONS
- Allowed Headers: All headers allowed
- Max Age: 3600 seconds (1 hour) for preflight response caching
- Credentials: Allowed

## Getting Started

1. Clone the repository:
   git clone [repository-url]

2. Navigate to the project directory:
   cd [project-directory]

3. Build the project:
   ./mvnw clean install

4. Run the application:
   ./mvnw spring-boot:run

The server will start on the default port (usually 8080).

## API Documentation

[Add your API endpoints and documentation here]

## Development

To set up the development environment:

1. Ensure you have JDK 24 installed
2. Import the project into your IDE
3. Install required dependencies
4. Configure your application.properties/application.yml file

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

[Add your license information here]

## Contact

[Add contact information or maintainer details here]


# MainController Documentation

## Overview

The `MainController` is a REST controller that manages chat interactions and Facebook-related operations in the
application. It serves as the main interface for handling user messages, processing AI-generated responses, and managing
Facebook post operations.

## Features

- Chat request handling with AI integration
- Facebook post management
- Image upload processing
- Session management
- Scheduled posts functionality

## API Endpoints

### Chat Endpoints

#### 1. Handle Chat Request

POST /chat
 
- Processes incoming chat messages
- Generates AI responses using Gemini AI
- Handles Facebook posting intents
- Supports scheduled post functionality

**Request Body:**

json { "messages": [ {  }
latex_unknown_tag


#### 2. Image Upload

POST /chat/upload

- Handles image file uploads
- Processes images through Cloudinary
- Posts images to Facebook
- Returns upload status and Facebook response

**Parameters:**
- `image`: MultipartFile (form-data)

### Facebook Integration Endpoints

#### 1. Save Page Data

POST /chat/facebook/page-data
 
- Saves Facebook page credentials
- Creates secure session
- Sets HTTP-only cookies

**Request Body:**

json { "pageId": "facebook_page_id", "pageAccessToken": "page_access_token" }


#### 2. Check Session

GET /chat/facebook/check-session
 
- Validates session existence
- Verifies Facebook credentials
- Logs session details for debugging

## Dependencies
- FacebookService
- GeminiAiService
- CloudinaryService
- TaskScheduler

## Session Management
- Uses HTTP sessions for maintaining user state
- Secure cookie implementation
- Session timeout handling

## Security Features
- CORS support
- Secure session cookies
- HTTPS requirement
- Token validation

## Error Handling
- Comprehensive error logging
- Structured error responses
- Session validation checks
- File upload validation

## Logging
- Detailed debug logging
- Session tracking
- Error tracking
- Operation status logging

## Usage Examples

### Sending a Chat Message

java POST /chat Content-Type: application/json
{ "messages": [ {  }
latex_unknown_tag
 

### Uploading an Image

java POST /chat/upload Content-Type: multipart/form-data
image: [binary-data]


### Setting Up Facebook Integration

java POST /chat/facebook/page-data Content-Type: application/json
{ "pageId": "your-page-id", "pageAccessToken": "your-page-access-token" }
 

## Best Practices
1. Always validate session state before operations
2. Handle file uploads with proper error checking
3. Implement proper error handling for API responses
4. Use logging for debugging and monitoring
5. Maintain secure session management

## Error Responses
- 400 Bad Request: Invalid input data
- 401 Unauthorized: Invalid or missing session
- 500 Internal Server Error: Server-side processing errors

## Development Notes
- Implements WebMvcConfigurer for CORS configuration
- Uses Spring's ResponseEntity for standardized responses
- Integrates with multiple services for comprehensive functionality
- Supports both synchronous and scheduled operations

## Future Enhancements
1. Enhanced error handling
2. Additional Facebook integration features
3. Expanded AI capabilities
4. Improved session management
5. Additional security features


