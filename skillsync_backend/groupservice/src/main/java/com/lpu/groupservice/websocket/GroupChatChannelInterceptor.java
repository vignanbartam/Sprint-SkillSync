package com.lpu.groupservice.websocket;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import com.lpu.groupservice.service.GroupService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GroupChatChannelInterceptor implements ChannelInterceptor {

    private static final Pattern SEND_DESTINATION = Pattern.compile("^/app/groups/(\\d+)/send$");
    private static final Pattern SUBSCRIBE_DESTINATION = Pattern.compile("^/topic/groups/(\\d+)$");

    private final GroupService groupService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command != StompCommand.SEND && command != StompCommand.SUBSCRIBE) {
            return message;
        }

        Principal user = accessor.getUser();
        if (user == null) {
            throw new IllegalArgumentException("Authenticated WebSocket user is required");
        }

        Long groupId = resolveGroupId(command, accessor.getDestination());
        if (groupId != null) {
            groupService.ensureApprovedMember(groupId, Long.parseLong(user.getName()));
        }

        return message;
    }

    private Long resolveGroupId(StompCommand command, String destination) {
        if (destination == null) {
            return null;
        }

        Pattern pattern = command == StompCommand.SEND ? SEND_DESTINATION : SUBSCRIBE_DESTINATION;
        Matcher matcher = pattern.matcher(destination);
        return matcher.matches() ? Long.parseLong(matcher.group(1)) : null;
    }
}
