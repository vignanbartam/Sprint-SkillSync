package com.lpu.groupservice.websocket;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.lpu.groupservice.dto.GroupMessageRequestDTO;
import com.lpu.groupservice.dto.GroupMessageResponseDTO;
import com.lpu.groupservice.service.GroupService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class GroupChatSocketController {

    private final GroupService groupService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/groups/{groupId}/send")
    public void sendMessage(@DestinationVariable Long groupId,
                            GroupMessageRequestDTO request,
                            Principal principal) {
        Long senderId = Long.parseLong(principal.getName());
        GroupMessageResponseDTO message = groupService.saveMessage(groupId, senderId, request);
        messagingTemplate.convertAndSend("/topic/groups/" + groupId, message);
    }
}
