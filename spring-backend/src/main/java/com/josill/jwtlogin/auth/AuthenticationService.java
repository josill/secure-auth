package com.josill.jwtlogin.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josill.jwtlogin.config.JwtService;
import com.josill.jwtlogin.confirmation_token.ConfirmationToken;
import com.josill.jwtlogin.confirmation_token.ConfirmationTokenService;
import com.josill.jwtlogin.email.EmailService;
import com.josill.jwtlogin.token.Token;
import com.josill.jwtlogin.token.TokenService;
import com.josill.jwtlogin.token.TokenType;
import com.josill.jwtlogin.user.User;
import com.josill.jwtlogin.user.UserRepository;
import com.josill.jwtlogin.user.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ConfirmationTokenService confirmationTokenService;
    private final EmailService emailService;
    private final TokenService tokenService;
    @Value("${application.security.jwt.expiration}")
    private int accessTokenExpiration;
    @Value("${application.security.jwt.refresh-token.expiration}")
    private int refreshTokenExpiration;

    public ResponseEntity<String> register(RegistrationRequest request) {
        if (userRepository.existsUserByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format("User with email: %s already taken!", request.getEmail()));
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .userRole(UserRole.USER)
                .build();
        userRepository.save(user);

        // TODO move this into token repo
        // generate confirmation token
        ConfirmationToken confirmationToken = confirmationTokenService.create(user);

        // send confirmation token to email
        String link = "http://localhost:8081/register/confirm?token=" + confirmationToken.getToken();

        // Create a separate task so the whole response is given before the email is sent.
        Runnable sendEmailTask = () -> emailService.send(request.getEmail(), EmailService.buildEmail(String.format("%s %s", request.getFirstName(), request.getLastName()), link));
        new Thread(sendEmailTask).start();

        return ResponseEntity.status(HttpStatus.OK).body("User registered!");

    }

    public ResponseEntity<?> authenticate(AuthenticationRequest request, HttpServletResponse response) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format("User with email: %s not found!", request.getEmail()));
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // revoke all the other tokens before saving the new one
        revokeAllUserTokens(user);
        saveUserToken(user, accessToken);

        // Return the access and refresh tokens in cookies
        accessTokenCookie(accessToken, response);
        refreshTokenCookie(accessToken, response);

        return ResponseEntity.status(HttpStatus.OK).body(AuthenticationResponse.builder().accessToken(accessToken).refreshToken(refreshToken).build());
    }

    public ResponseEntity<String> confirm(String token) {
        ConfirmationToken confirmationToken = confirmationTokenService.findByToken(token).orElse(null);

        if (confirmationToken == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid token!");
        } else if (confirmationToken.getConfirmedAt() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email already confirmed!");
        } else if (confirmationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            User user = confirmationToken.getUser();
            ConfirmationToken newConfirmationToken = confirmationTokenService.create(user);
            String link = "http://localhost:8081/register/confirm?token=" + newConfirmationToken.getToken();

            // Create a separate task so the  response is given before the email is sent. This makes the response way faster and doesn't depend on the email being sent.
            Runnable sendEmailTask = () -> emailService.send(user.getEmail(), EmailService.buildEmail(String.format("%s %s", user.getFirstName(), user.getLastName()), link));
            new Thread(sendEmailTask).start();

            return ResponseEntity.status(HttpStatus.ACCEPTED).body("New confirmation token sent!");
        }

        confirmationTokenService.updateConfirmedAt(confirmationToken.getToken(), LocalDateTime.now());
        userRepository.enableUser(confirmationToken.getUser().getEmail());

        return ResponseEntity.status(HttpStatus.OK).body("User confirmed!");

    }

    public ResponseEntity<?> login(AuthenticationRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format("User with email: %s, does not exist.", request.getEmail()));
        } else if (!user.isEnabled()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not confirmed");

        } else if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Wrong password!");
        }

        return authenticate(request, response);
    }

    private void saveUserToken(User user, String jwtToken) {
        Token token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenService.save(token);
    }

    private void revokeAllUserTokens(User user) {
        List<Token> validUserTokens = tokenService.findAllValidTokensByUser(user.getId());

        if (validUserTokens.isEmpty()) {
            return;
        }

        validUserTokens.forEach(t -> {
            t.setExpired(true);
            t.setRevoked(true);
        });

        tokenService.saveAll(validUserTokens);
    }

    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        refreshToken = authHeader.substring(7);
        userEmail = jwtService.extractEmail(refreshToken);

        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new UsernameNotFoundException(String.format("User with email %s doesn't exist", userEmail)));
            boolean isTokenValid = tokenService.findByToken(refreshToken)
                    .map(t -> !t.isExpired() && !t.isRevoked())
                    .orElse(false);

            if (jwtService.isTokenValid(refreshToken, user) && isTokenValid) {
                String accessToken = jwtService.generateToken(user);

                revokeAllUserTokens(user);
                saveUserToken(user, accessToken);

                AuthenticationResponse authenticationResponse = AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
                new ObjectMapper().writeValue(response.getOutputStream(), authenticationResponse);
            }
        }
    }

    private void accessTokenCookie(String accessToken, HttpServletResponse response) {
        ResponseCookie accessTokenCookie = ResponseCookie
                .from("accessToken", accessToken)
//                .httpOnly(true) // when httpOnly the cookies can not be accessed in the client side using JS
                .secure(true) // when secure is true the cookies are only sent over https connections
                .maxAge(accessTokenExpiration)
//                .domain(".localhost")
                .path("/")
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
    }

    private void refreshTokenCookie(String refreshToken, HttpServletResponse response) {
        ResponseCookie refreshTokenCookie = ResponseCookie
                .from("refreshToken", refreshToken)
//                .httpOnly(true) // when httpOnly the cookies can not be accessed in the client side using JS
                .secure(true) // when secure is true the cookies are only sent over https connections
                .maxAge(refreshTokenExpiration)
//                .domain(".localhost")
                .path("/")
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }
}
