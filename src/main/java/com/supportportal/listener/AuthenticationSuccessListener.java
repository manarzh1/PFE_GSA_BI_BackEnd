package com.supportportal.listener;

import com.supportportal.domain.User;
import com.supportportal.domain.UserPrincipal;
import com.supportportal.service.LoginAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class AuthenticationSuccessListener {
    private LoginAttemptService loginAttemptservice;

    @Autowired
    public AuthenticationSuccessListener(LoginAttemptService loginAttemptservice) {
        this.loginAttemptservice = loginAttemptservice;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) throws ExecutionException {
        Object principal = event.getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            UserPrincipal user = (UserPrincipal) event.getAuthentication().getPrincipal();
            loginAttemptservice.addUserToLoginAttemptCache(user.getUsername());
        }
    }
}
