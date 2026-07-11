package com.crewschedule.chat.config;

import com.crewschedule.chat.service.ChatBroadcast;
import com.crewschedule.chat.service.ChatSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/** Redis Pub/Sub 리스너 컨테이너. {@link ChatSubscriber}가 {@code chat:crew:*} 패턴을 구독한다. */
@Configuration
public class RedisChatPubSubConfig {

    @Bean
    public RedisMessageListenerContainer chatRedisContainer(
            RedisConnectionFactory factory, ChatSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(subscriber, new PatternTopic(ChatBroadcast.CHANNEL_PATTERN));
        return container;
    }
}
