package org.example.documentmanagementservice;

import org.example.documentmanagementservice.Controllers.NotificationController;
import org.example.documentmanagementservice.Model.Notification;
import org.example.documentmanagementservice.Repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationRepository notificationRepository;

    @Test
    @WithMockUser(roles = "HR")
    void testGetPendingNotifications_Success() throws Exception {
        Notification notification = new Notification();
        notification.setRecipientRole("HR");
        notification.setRead(false);
        when(notificationRepository.findByRecipientRoleAndReadFalse("HR")).thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void testGetPendingNotifications_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/notifications/pending"))
                .andExpect(status().isUnauthorized());
    }
}
