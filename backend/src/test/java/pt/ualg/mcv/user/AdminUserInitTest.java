package pt.ualg.mcv.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import pt.ualg.mcv.config.runners.AdminUserInitializer;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserInitializer adminUserInitializer;

    @Test
    void run_CreatesAdmin_WhenCredentialsConfiguredAndUserDoesNotExist() {
        ReflectionTestUtils.setField(adminUserInitializer, "adminEmail", "admin@ualg.pt");
        ReflectionTestUtils.setField(adminUserInitializer, "adminPassword", "secretAdmin123");

        when(userRepository.findByEmail("admin@ualg.pt")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secretAdmin123")).thenReturn("hashedSecretPassword");

        adminUserInitializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User createdAdmin = userCaptor.getValue();
        assertEquals("admin@ualg.pt", createdAdmin.getEmail());
        assertEquals("Admin", createdAdmin.getFirstName());
        assertEquals("System", createdAdmin.getLastName());
        assertEquals("hashedSecretPassword", createdAdmin.getPassword());
        assertEquals(UserRole.ADMIN, createdAdmin.getRole());
    }

    @Test
    void run_DoesNotCreateAdmin_WhenAdminAlreadyExists() {
        ReflectionTestUtils.setField(adminUserInitializer, "adminEmail", "admin@ualg.pt");
        ReflectionTestUtils.setField(adminUserInitializer, "adminPassword", "secretAdmin123");

        when(userRepository.findByEmail("admin@ualg.pt")).thenReturn(Optional.of(new User()));

        adminUserInitializer.run();

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void run_SkipsExecution_WhenEmailIsNull() {
        ReflectionTestUtils.setField(adminUserInitializer, "adminEmail", null);
        ReflectionTestUtils.setField(adminUserInitializer, "adminPassword", "secretAdmin123");

        adminUserInitializer.run();

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void run_SkipsExecution_WhenEmailIsBlank() {
        ReflectionTestUtils.setField(adminUserInitializer, "adminEmail", "   ");
        ReflectionTestUtils.setField(adminUserInitializer, "adminPassword", "secretAdmin123");

        adminUserInitializer.run();

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void run_SkipsExecution_WhenPasswordIsNull() {
        ReflectionTestUtils.setField(adminUserInitializer, "adminEmail", "admin@ualg.pt");
        ReflectionTestUtils.setField(adminUserInitializer, "adminPassword", null);

        adminUserInitializer.run();

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void run_SkipsExecution_WhenPasswordIsBlank() {
        ReflectionTestUtils.setField(adminUserInitializer, "adminEmail", "admin@ualg.pt");
        ReflectionTestUtils.setField(adminUserInitializer, "adminPassword", "   ");

        adminUserInitializer.run();

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }
}