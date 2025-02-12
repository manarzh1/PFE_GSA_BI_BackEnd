package com.supportportal.resource;

import com.supportportal.domain.HttpResponse;
import com.supportportal.domain.User;
import com.supportportal.domain.UserPrincipal;
import com.supportportal.exception.ExceptionHandling;
import com.supportportal.exception.domain.*;
import com.supportportal.service.UserService;
import com.supportportal.utility.JWTTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.supportportal.constant.FileConstant.*;
import static com.supportportal.constant.SecurityConstant.JWT_TOKEN_HEADER;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;

@RestController
@RequestMapping(path = { "/", "/user"})
public class UserResource extends ExceptionHandling {
    public static final String EMAIL_SENT = "An Email with a new password was sent to :";
    public static final String USER_DELETED_SUCCESSFULLY = "User deleted successfully";

    private AuthenticationManager authenticationManager;
    private UserService userService;
    private JWTTokenProvider jwtTokenProvider;

    @Autowired
    public UserResource(AuthenticationManager authenticationManager, UserService userService, JWTTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody User user) {
        User loginUser = userService.findUserByUsername(user.getUsername());
        if (!loginUser.isActive()) {
            LOGGER.error("Account is inactive. Please contact the administrator.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"message\": \"Account is inactive. Please contact the administrator.\"}");
        }
        authenticate(user.getUsername(), user.getPassword());
        UserPrincipal userPrincipal = new UserPrincipal(loginUser);
        HttpHeaders jwtHeader = getJwtHeader(userPrincipal);
        return ResponseEntity.ok().headers(jwtHeader).body(loginUser);
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) throws UserNotFoundException, UsernameExistException, EmailExistException, MessagingException {
        User newUser = userService.register(user.getFirstName(), user.getLastName(), user.getUsername(), user.getEmail());
        return new ResponseEntity<>(newUser, OK);
    }

    @PostMapping("/add")
    public ResponseEntity<User> addNewUser(@RequestParam("firstName") String firstName,
                                           @RequestParam("lastName") String lastName,
                                           @RequestParam("username") String userName,
                                           @RequestParam("email") String email,
                                           @RequestParam("role") String role,
                                           @RequestParam("isActive") String isActive,
                                           @RequestParam("isNonLocked") String isNonLocked,
                                           @RequestParam(value = "profileImage", required = false ) MultipartFile profileImage) throws UserNotFoundException, EmailExistException, IOException, UsernameExistException, NotAnImageFileException, MessagingException {
        User newUser = userService.addNewUser(firstName,lastName,userName,email,role,Boolean.parseBoolean(isNonLocked),
                Boolean.parseBoolean(isActive),profileImage);
        return new ResponseEntity<>(newUser, OK);
    }
    @PostMapping("/update")
    public  ResponseEntity<User> update(@RequestParam("currentUsername") String currentUsername,
                                        @RequestParam("firstName") String firstName,
                                        @RequestParam("lastName") String lastName,
                                        @RequestParam("username") String userName,
                                        @RequestParam("email") String email,
                                        @RequestParam("role") String role,
                                        @RequestParam("isActive") String isActive,
                                        @RequestParam("isNonLocked") String isNonLocked,
                                        @RequestParam(value = "profileImage", required = false )MultipartFile profileImage) throws UserNotFoundException, EmailExistException, IOException, UsernameExistException, NotAnImageFileException {
        User updateUser = userService.UpdateUser(currentUsername,firstName,lastName,userName,email,role,Boolean.parseBoolean(isNonLocked), Boolean.parseBoolean(isActive),profileImage);
        return  new ResponseEntity<>( updateUser, OK);
    }
    @GetMapping("/find/{username}")
    public ResponseEntity<User> getUser(@PathVariable("username") String username) {
        User user = userService.findUserByUsername(username);
        return new ResponseEntity<>(user, OK);
    }
    @GetMapping("/list")
    public ResponseEntity<List<User>> getAllUsers()  {
        List <User> users = userService.getUsers();
        return new ResponseEntity<>(users, OK);
    }
    @GetMapping("/resetpassword/{email}")
    public ResponseEntity<HttpResponse> resetPassword(@PathVariable("email") String email) throws EmailNotFoundException, MessagingException {
        userService.resetPassword(email);
        return response(OK, EMAIL_SENT + email);
    }




    @PostMapping("/requestPasswordReset")
    public ResponseEntity<CustomHttpResponse> requestPasswordReset(@RequestBody EmailRequest emailRequest) {
        try {
            userService.sendPasswordResetLink(emailRequest.getEmail());
            return new ResponseEntity<>(new CustomHttpResponse(HttpStatus.OK.value(), "Password reset link sent successfully."), HttpStatus.OK);
        } catch (EmailNotFoundException | MessagingException e) {
            return new ResponseEntity<>(new CustomHttpResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/updatePassword")
    public ResponseEntity<CustomHttpResponse> updatePassword(@RequestParam("token") String token,
                                                             @RequestParam("newPassword") String newPassword,
                                                             @RequestParam("confirmPassword") String confirmPassword) {
        try {
            userService.updatePassword(token, newPassword, confirmPassword);
            return new ResponseEntity<>(new CustomHttpResponse(HttpStatus.OK.value(), "Password updated successfully."), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new CustomHttpResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/delete/{username}")
    @PreAuthorize("hasAnyAuthority('user:delete')")
    public ResponseEntity<HttpResponse> deleteUser(@PathVariable("username") String username) throws IOException {
        userService.deleteUser(username);
        return response(OK, USER_DELETED_SUCCESSFULLY);
    }

    @PostMapping("/updateProfileImage")
    public  ResponseEntity<User> updateProfileImage(@RequestParam("username") String username, @RequestParam(value = "profileImage")MultipartFile profileImage) throws UserNotFoundException, EmailExistException, IOException, UsernameExistException, NotAnImageFileException {
        User user = userService.updateProfileImage(username,profileImage);
        return  new ResponseEntity<>(user, OK);
    }
    @GetMapping(path = "/image/{username}/{fileName}", produces = IMAGE_JPEG_VALUE)
    public byte[] getProfileImage(@PathVariable("username") String username, @PathVariable("fileName") String fileName) throws IOException {
        return Files.readAllBytes(Paths.get(USER_FOLDER + username + FORWARD_SLASH + fileName));
    }

    @GetMapping(path = "/image/profile/{username}", produces = IMAGE_JPEG_VALUE)
    public byte[] getTempProfileImage(@PathVariable("username") String username) throws IOException {
        URL url = new URL(TEMP_PROFILE_IMAGE_BASE_URL + username);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (InputStream inputStream = url.openStream()) {
            int bytesRead;
            byte[] chunk = new byte[1024];
            while ((bytesRead = inputStream.read(chunk)) > 0) {
                byteArrayOutputStream.write(chunk, 0, bytesRead);
            }
        }

        return byteArrayOutputStream.toByteArray();
    }



    private ResponseEntity<HttpResponse> response(HttpStatus httpStatus, String message) {

        return  new ResponseEntity<>(new HttpResponse(httpStatus.value(), httpStatus,httpStatus.getReasonPhrase().toUpperCase(),
                message.toUpperCase()),httpStatus);

    }

    private HttpHeaders getJwtHeader(UserPrincipal user) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(JWT_TOKEN_HEADER, jwtTokenProvider.generateJwtToken(user));
        return headers;
    }

    private void authenticate(String username, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }
}