package swyp12.team9.server.domain.chatbot.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swyp12.team9.server.domain.chatbot.service.ChatbotIndexingService;
import swyp12.team9.server.domain.userlink.event.UserLinkChatbotReindexEvent;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatbotIndexingEventListenerTest {

    @Mock
    private ChatbotIndexingService chatbotIndexingService;

    @Mock
    private UserLinkRepository userLinkRepository;

    @InjectMocks
    private ChatbotIndexingEventListener chatbotIndexingEventListener;

    @Test
    @DisplayName("성공: UserLink 챗봇 재인덱싱 이벤트를 받으면 챗봇 인덱싱을 수행한다")
    void success_handleUserLinkChatbotReindex() {
        Long userLinkId = 1L;

        chatbotIndexingEventListener.handleUserLinkChatbotReindex(
                UserLinkChatbotReindexEvent.of(userLinkId)
        );

        verify(chatbotIndexingService).indexUserLink(userLinkId);
    }
}
