package org.lecturestudio.web.portal.interceptor;

import java.security.Principal;
import java.util.List;
import java.util.Objects;

import org.lecturestudio.web.portal.exception.MessageInterceptedException;
import org.lecturestudio.web.portal.service.CourseFeatureService;
import org.lecturestudio.web.portal.service.MessengerFeatureUserRegistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.messaging.support.MessageBuilder;


public class StompInboundChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private MessengerFeatureUserRegistry messengerFeatureUserRegistry;

    @Autowired
    @Qualifier("clientOutboundChannel")
    private MessageChannel clientOutboundChannel;

    @Autowired
    private CourseFeatureService courseFeatureService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        try {
            intercept(message);
        } catch(MessageInterceptedException exc) {
            this.sendBackErrorFrame(exc.getMessage(), exc.getSessionId());
            return null;
        }

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        Principal user = accessor.getUser();
        if (! (user instanceof UsernamePasswordAuthenticationToken)) {
            messengerFeatureUserRegistry.onMessage(message);
        }

        return message;
    }

    private void intercept(Message<?> message) throws MessageInterceptedException {
        StompHeaderAccessor headerAccessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (Objects.isNull(headerAccessor)) {
            throw new MessageInterceptedException("The message headers do not conform the STOMP protocol specification!");
        }

        String sessionId = headerAccessor.getSessionId();

        StompCommand messageCommand = headerAccessor.getCommand();
        if (!Objects.isNull(messageCommand)) {
            if (messageCommand.equals(StompCommand.CONNECTED) || messageCommand.equals(StompCommand.SUBSCRIBE) || messageCommand.equals(StompCommand.UNSUBSCRIBE)) {
                List<String> courseIdList = headerAccessor.getNativeHeader("courseId");
                if (Objects.isNull(courseIdList) || courseIdList.isEmpty()) {
                    throw new MessageInterceptedException("The message has the wrong format: No courseId was given in the headers!", sessionId);
                }
                try {
                    final Long courseId = Long.parseLong(courseIdList.get(0));
                    Principal user = headerAccessor.getUser();
                    if (! (user instanceof UsernamePasswordAuthenticationToken)) {
                        courseFeatureService.findMessageByCourseId(courseId).orElseThrow(() -> new MessageInterceptedException("There is no messenger feature for the course with id " + courseId));
                    }

                } catch(NumberFormatException exc) {
                    throw new MessageInterceptedException("The message has the wrong format: The given courseId has the wrong format. " + exc.getMessage());
                }
            }
        }
    }

    private void sendBackErrorFrame(String errorMessage, String sessionId) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
        headerAccessor.setMessage(errorMessage);
        headerAccessor.setSessionId(sessionId);
        this.clientOutboundChannel.send(MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders()));
    }
}