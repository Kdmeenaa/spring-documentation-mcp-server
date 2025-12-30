package com.spring.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

public class GenerateBCryptHashTest {

    @Test
    public void generateBCryptHash() {
        // Generate with BCryptPasswordEncoder
        BCryptPasswordEncoder bCryptEncoder = new BCryptPasswordEncoder();
        String rawPassword = "admin";
        String bcryptHash = bCryptEncoder.encode(rawPassword);

        System.out.println("===========================================");
        System.out.println("BCrypt hash for 'admin':");
        System.out.println(bcryptHash);
        System.out.println("With {bcrypt} prefix:");
        System.out.println("{bcrypt}" + bcryptHash);
        System.out.println("===========================================");

        // Verify it works
        boolean matchesBCrypt = bCryptEncoder.matches(rawPassword, bcryptHash);
        System.out.println("BCrypt verification: " + matchesBCrypt);

        // Test with DelegatingPasswordEncoder
        PasswordEncoder delegatingEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String delegatingHash = delegatingEncoder.encode(rawPassword);
        System.out.println("DelegatingPasswordEncoder hash:");
        System.out.println(delegatingHash);

        boolean matchesDelegating = delegatingEncoder.matches(rawPassword, delegatingHash);
        System.out.println("Delegating verification: " + matchesDelegating);
        System.out.println("===========================================");

        // Test with {bcrypt} prefix
        boolean matchesWithPrefix = delegatingEncoder.matches(rawPassword, "{bcrypt}" + bcryptHash);
        System.out.println("Delegating with {bcrypt} prefix verification: " + matchesWithPrefix);
    }
}
