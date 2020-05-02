package com.github.hornta.messenger;

import java.util.HashMap;
import java.util.Map;

public class MessagesBuilder {
  private Map<Enum, String> identifiers;

  public MessagesBuilder() {
    this.identifiers = new HashMap<>();
  }

  public MessagesBuilder add(Enum id, String key) {
    identifiers.put(id, key);
    return this;
  }

  public MessageManager build() {
    return new MessageManager(identifiers);
  }
}
