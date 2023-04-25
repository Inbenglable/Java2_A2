package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ChatRoom implements Initializable {
  private final Socket s;
  private final Controller controller;
  public Stage stage;
  private final PrintWriter out;
  public String userName;
  public String type;
  public HashSet<String> otherUsers;
  public int groupChatRoomId = -1;

  public ChatRoom(Socket s, Controller controller, PrintWriter out, Stage stage, String userName, String type, HashSet<String> users) {
    this.controller = controller;
    this.stage = stage;
    this.out = out;
    this.s = s;
    this.userName = userName;
    this.type = type;
    otherUsers = users;
    otherUsers.remove(userName);
//    System.setProperty("file.encoding","UTF-8");
  }

  public void setGroupChatRoomId(int x) {
    groupChatRoomId = x;
  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    emoji1.setText("\uD83D\uDE00");
    emoji1.setOnAction(e -> doSendMessage("\uD83D\uDE00"));
    emoji2.setText("\uD83D\uDE04");
    emoji2.setOnAction(e -> doSendMessage("\uD83D\uDE04"));
    emoji3.setText("\uD83D\uDE0D");
    emoji3.setOnAction(e -> doSendMessage("\uD83D\uDE0D"));
    emoji4.setText("\uD83D\uDC97");
    emoji4.setOnAction(e -> doSendMessage("\uD83D\uDC97"));
    if (type.equals("Private")) {
      SendFile.setOnAction(e -> sendFile());
    }
    currentUsername.setText(String.format("Current User: %s", userName));
    currentOnlineCnt.setText(String.format("Online: %d", otherUsers.size() + 1));
    chatContentList.setCellFactory(new MessageCellFactory());
    for (String x : otherUsers) {
      chatList.getItems().add(x);
    }
    chatList.getItems().add(userName);
    chatList.getItems().sort(String::compareTo);
    if (type.equals("Private")) {
      ChatMenu.setText("Private Chat");
    } else {
      updateMenu();
    }
    send.setOnAction(e -> doSendMessage());
    Platform.runLater(() -> {
      File readMsg = new File(userName);
      if (type.equals("Private") && readMsg.exists() && readMsg.isDirectory()) {
        try {
          String talkTo = otherUsers.iterator().next();
          InputStream is = Files.newInputStream(Paths.get(String.format("%s/%s.txt", userName, talkTo)));
          BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
          String temp = br.readLine();
          if (temp != null && !temp.equals("")) {
            controller.onlineUsers.put(talkTo, Long.parseLong(temp));
            temp = br.readLine();
            while (temp != null && !temp.equals("")) {
              String dataTemp = br.readLine().replace('^', '\n');
              dataTemp = dataTemp.replaceAll("@1", "\uD83D\uDE00");
              dataTemp = dataTemp.replaceAll("@2", "\uD83D\uDE04");
              dataTemp = dataTemp.replaceAll("@3", "\uD83D\uDE0D");
              dataTemp = dataTemp.replaceAll("@4", "\uD83D\uDC97");
              chatContentList.getItems().add(new Message(0L, temp, dataTemp));
              temp = br.readLine();
            }
          }
        } catch (IOException ignored) {
        }
      }
    });
  }

  @FXML
  Menu ChatMenu;
  @FXML
  private Button SendFile;
  @FXML
  private Button emoji1;
  @FXML
  private Button emoji2;
  @FXML
  private Button emoji3;
  @FXML
  private Button emoji4;
  @FXML
  public ListView<Message> chatContentList;
  @FXML
  private ListView<String> chatList;
  @FXML
  private Label currentOnlineCnt;
  @FXML
  private Label currentUsername;
  @FXML
  public TextArea inputArea;
  @FXML
  private Button send;

  @FXML
  void doSendMessage() {//ActionEvent event
    if (inputArea.getText() != null && !inputArea.getText().equals("")) {
      String sendData = inputArea.getText().replace('\n', '^');
      chatContentList.getItems().add(new Message(System.currentTimeMillis(), userName, inputArea.getText()));
      if (type.equals("Group")) {
        StringBuilder sendMsg = new StringBuilder("SendGroupMsg_");
        sendMsg.append(groupChatRoomId);
        for (String x : otherUsers) {
          sendMsg.append("_").append(x);
        }
        sendMsg.append("_").append(sendData);
        out.println(sendMsg);
        out.flush();
      } else {
        String anoUser = otherUsers.iterator().next();
        out.printf("SendPrivateMsg_%s_%s\n", anoUser, sendData);
        out.flush();
      }
      inputArea.setText(null);
    }
    if (type.equals("Private")) {
      saveMsg();
    }
  }

  void doSendMessage(String text) {//ActionEvent event
    String emojiCode = "";
    switch (text) {
      case "\uD83D\uDE00":
        emojiCode = "1";
        break;
      case "\uD83D\uDE04":
        emojiCode = "2";
        break;
      case "\uD83D\uDE0D":
        emojiCode = "3";
        break;
      case "\uD83D\uDC97":
        emojiCode = "4";
        break;
    }

    chatContentList.getItems().add(new Message(System.currentTimeMillis(), userName, text));
    inputArea.setText(null);
    if (type.equals("Group")) {
      StringBuilder sendMsg = new StringBuilder("SendGroupMsg2_");
      sendMsg.append(groupChatRoomId);
      for (String x : otherUsers) {
        sendMsg.append("_").append(x);
      }
      sendMsg.append("_").append(emojiCode);
      out.println(sendMsg);
      out.flush();
    } else {
      String anoUser = otherUsers.iterator().next();
      out.printf("SendPrivateMsg2_%s_%s\n", anoUser, emojiCode);
      out.flush();
    }
    if (type.equals("Private")) {
      saveMsg();
    }
  }

  private void sendFile() {
    FileChooser fileChooser = new FileChooser();
    File file = fileChooser.showOpenDialog(new Stage());
    if (file != null && file.exists() && file.isFile()) {
      try (FileInputStream fi = new FileInputStream(file)) {
        int fileSize = (int) file.length();
        byte[] buffer = new byte[(int) fileSize];

        int offset = 0;
        int numRead;
        while (offset < buffer.length && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {
          offset += numRead;
        }
        out.println("File");
        out.flush();
        out.println(file.getName());
        out.flush();
        out.println(otherUsers.iterator().next());
        out.flush();
        out.println(buffer.length);
        out.flush();
        OutputStream outputStream = s.getOutputStream();
        outputStream.write(buffer);
        outputStream.flush();
        out.println("@FileEnd");
        out.flush();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void saveMsg() {

    try {
      File folder = new File(userName);
      if (!folder.exists() || !folder.isDirectory()) {
        folder.setWritable(true, false);
        folder.mkdirs();
      }
      String talkTo = otherUsers.iterator().next();
      controller.onlineUsers.put(talkTo, System.currentTimeMillis());
      controller.userListSort();
      System.out.println("has entered the directory");
      FileWriter fw = new FileWriter(String.format("%s/%s.txt", userName, talkTo));
      fw.write(String.format("%s\n", controller.onlineUsers.get(talkTo)));
      for (Message x : chatContentList.getItems()) {
        String msgData = x.getData().replace('\n', '^');
        msgData = msgData.replaceAll("\uD83D\uDE00", "@1");
        msgData = msgData.replaceAll("\uD83D\uDE04", "@2");
        msgData = msgData.replaceAll("\uD83D\uDE0D", "@3");
        msgData = msgData.replaceAll("\uD83D\uDC97", "@4");
        fw.write(String.format("%s\n", x.getSentBy()));
        fw.write(String.format("%s\n", msgData));
      }
      fw.flush();
      fw.close();
    } catch (IOException e) {
      System.out.println("Write MSG file wrong");
    } catch (ConcurrentModificationException c) {
      System.out.println("ConcurrentModificationException in saveMsg");
    }
  }

  public void receiveMsg(String sender, String data) {
    String newData = data.replace('^', '\n');
    Platform.runLater(() -> {
      stage.show();
      stage.setAlwaysOnTop(true);
      stage.setAlwaysOnTop(false);
      chatContentList.getItems().add(new Message(System.currentTimeMillis(), sender, newData));
      if (type.equals("Private")) {
        saveMsg();
      }
    });
  }

  public void receiveMsg2(String sender, String data) {
    String emoji = "";
    switch (data) {
      case "1":
        emoji = "\uD83D\uDE00";
        break;
      case "2":
        emoji = "\uD83D\uDE04";
        break;
      case "3":
        emoji = "\uD83D\uDE0D";
        break;
      case "4":
        emoji = "\uD83D\uDC97";
        break;
    }
    String finalEmoji = emoji;
    Platform.runLater(() -> {
      stage.show();
      stage.setAlwaysOnTop(true);
      stage.setAlwaysOnTop(false);
      chatContentList.getItems().add(new Message(System.currentTimeMillis(), sender, finalEmoji));
      if (type.equals("Private")) {
        saveMsg();
      }
    });
  }

  public void updateMenu() {
    StringBuilder menuTitle = new StringBuilder("GroupChat: ");
    Iterator<String> it = chatList.getItems().iterator();
    while (it.hasNext()) {
      menuTitle.append(it.next());
      if (it.hasNext()) {
        menuTitle.append(", ");
      }
    }
    String finalMenuTitle = menuTitle.toString();
    Platform.runLater(() -> {
      ChatMenu.setText(finalMenuTitle);
      currentOnlineCnt.setText(String.format("Online: %d", otherUsers.size() + 1));
    });
  }

  public void deleteGroupUser(String deleteUser) {
    Platform.runLater(() -> {
      chatList.getItems().remove(deleteUser);
      otherUsers.remove(deleteUser);
      updateMenu();
      if (chatList.getItems().size() == 1) {
        Dialog<ButtonType> temp = new Alert(Alert.AlertType.INFORMATION);
        temp.setHeaderText("No user");
        temp.showAndWait();
        stage.close();
      }
    });

  }

  /**
   * You may change the cell factory if you changed the design of {@code Message} model.
   * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
   */
  private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
    @Override
    public ListCell<Message> call(ListView<Message> param) {
      return new ListCell<Message>() {

        @Override
        public void updateItem(Message msg, boolean empty) {
          super.updateItem(msg, empty);
          if (empty || Objects.isNull(msg)) {
            setText(null);
            setGraphic(null);
            return;
          }

          HBox wrapper = new HBox();
          Label nameLabel = new Label(msg.getSentBy());
          Label msgLabel = new Label(msg.getData());

          nameLabel.setPrefSize(50, 20);
          nameLabel.setWrapText(true);
          nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

          if (userName.equals(msg.getSentBy())) {
            wrapper.setAlignment(Pos.TOP_RIGHT);
            wrapper.getChildren().addAll(msgLabel, nameLabel);
            msgLabel.setPadding(new Insets(0, 20, 0, 0));
          } else {
            wrapper.setAlignment(Pos.TOP_LEFT);
            wrapper.getChildren().addAll(nameLabel, msgLabel);
            msgLabel.setPadding(new Insets(0, 0, 0, 20));
          }

          setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
          setGraphic(wrapper);
        }
      };
    }
  }
}