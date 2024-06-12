package com.supportportal.enumeration;

import static com.supportportal.constant.Authority.*;

public enum Role {
    ROLE_USER(USER_AUTHORITIES),

    ROLE_ADMIN(ADMIN_AUTHORITIES);


    private String[] authorities;

    Role(String... authorities) {

        this.authorities = authorities;
    }

    public String[] getAuthorities() {
        return authorities;
    }
}
