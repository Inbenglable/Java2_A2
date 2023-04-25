package cn.edu.sustech.cs209.chatting.common;

public class Message {

  private final String sentBy;

  private final String data;

  public Message(Long timestamp, String sentBy, String data) {
    this.sentBy = sentBy;
    this.data = data;
  }

  public String getSentBy() {
    return sentBy;
  }

  public String getData() {
    return data;
  }

  @Override
  public String toString() {
    return data;
  }
}
