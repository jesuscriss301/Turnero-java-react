package com.turnero.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseHub {
  private final Map<String, CopyOnWriteArrayList<SseEmitter>> channels = new ConcurrentHashMap<>();
  private final ObjectMapper mapper = new ObjectMapper();

  public SseEmitter subscribe(String channel) {
    SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
    channels.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(emitter);
    Runnable remove = () -> {
      List<SseEmitter> list = channels.get(channel);
      if (list != null) list.remove(emitter);
    };
    emitter.onCompletion(remove);
    emitter.onTimeout(remove);
    emitter.onError(e -> remove.run());
    return emitter;
  }

  public void send(String channel, String event, Object payload) {
    List<SseEmitter> list = channels.get(channel);
    if (list == null || list.isEmpty()) return;
    String json;
    try { json = mapper.writeValueAsString(payload); }
    catch (Exception e) { return; }
    for (SseEmitter em : list) {
      try { em.send(SseEmitter.event().name(event).data(json)); }
      catch (Exception e) { em.complete(); }
    }
  }
}
