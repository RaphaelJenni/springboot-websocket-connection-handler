# springboot-websocket-connection-handler

This code repository is a side product of a bachelor' thesis.
It is meant to be included into an existing springboot application as sub-project - it is not designed to run standalone.

Everything is written in kotlin and builds upon the springboot event system (with the provided configuration designed for Java 16 - also tested on Java 11).

## Usage

### Setting up the websocket

**Kotlin:** (With Authentication)
```kotlin
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : AbstractSecurityWebSocketMessageBrokerConfigurer() {

    override fun configureInbound(messages: MessageSecurityMetadataSourceRegistry) {
        messages
            .nullDestMatcher().permitAll()
            .simpSubscribeDestMatchers("/user/queue/errors").permitAll()
            .simpDestMatchers("/app/**").authenticated()
            .simpSubscribeDestMatchers("/topic/**", "/queue/**", "/user/**").authenticated()
            .anyMessage().denyAll()
    }

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic", "/queue")
        config.setApplicationDestinationPrefixes("/app")
        config.setUserDestinationPrefix("/user")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*")
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS()
    }

    override fun sameOriginDisabled(): Boolean {
        return true
    }


}
```

**Java:** (Without Authentication)
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages.anyMessage().permitAll();
    }

    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }
}
```

### Events

The library contains 6 different events.

**Non-User-Specific Paths:**
* `WebSocketNewPathConnectionEvent`
* `WebSocketExistingPathConnectionEvent`
* `WebSocketPathConnectionClosedEvent`

**User-Specific Paths:**
* `WebSocketNewUserPathConnectionEvent`
* `WebSocketExistingUserPathConnectionEvent`
* `WebSocketUserPathConnectionClosedEvent`

The user specific paths, also contain the user principal.


### Filtering

In order to filter on only certain requests, the simp path can be used together with a regex or a simple string comaprison.

**Kotlin:**
```kotlin
    @EventListener
    fun handleNewConnection(event: WebSocketNewPathConnectionEvent) {
        if (event.simpPath != "/topic/messages") return
    
        ...
    }
```

**Java:**
```java
    private static final Pattern DESTIONATION_PATTERN = Pattern.compile("^/topic/messages/([0-9]+)/?$");
    @EventListener()
    public void handleNewConnection(WebSocketNewPathConnectionEvent event) {
    
        final var destination = event.getSimpPath();
        Matcher destinationMatcher = DESTIONATION_PATTERN.matcher(destination);
        if (!destinationMatcher.matches()) return;
        
        ...
    }
```

## Test Coverage

This project contains 49 tests covering almost 100% of the provided code.
