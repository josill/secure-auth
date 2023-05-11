package com.josill.jwtlogin.confirmation_token;

import com.josill.jwtlogin.user.User;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ConfirmationTokenService {

    private final ConfirmationTokenRepository confirmationTokenRepository;

    public ConfirmationToken create(User user) {
        String tokenIdentifier = UUID.randomUUID().toString();
        ConfirmationToken token = new ConfirmationToken(
                tokenIdentifier,
                user
        );
        save(token);

        return token;
    }

    public Optional<ConfirmationToken> findByToken(String token) {
        return confirmationTokenRepository.findByToken(token);
    }

    public int updateConfirmedAt(String token, LocalDateTime confirmedAt) {
        return confirmationTokenRepository.updateConfirmedAt(token, confirmedAt);
    }

    public void save(ConfirmationToken token) {
        confirmationTokenRepository.save(token);
    }

}
