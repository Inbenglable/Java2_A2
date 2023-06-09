package cn.edu.sustech.cs209.chatting.client;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Controller implements Initializable {
  HashMap<String, ChatRoom> privateChats = new HashMap<>();
  HashMap<Integer, ChatRoom> groupChats = new HashMap<>();
  @FXML
  VBox vBox;
  @FXML
  Label UserName;
  public Socket s = null;
  private Scanner in;
  private PrintWriter out;
  String username;
  public HashMap<String, Long> onlineUsers;
  @FXML
  private ListView<String> userList;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    //System.setProperty("file.encoding","UTF-8");
    try {
      s = new Socket("localhost", 1234);
      while (true) {
        Dialog<String> login = new TextInputDialog();
        login.setTitle("Login");
        login.setHeaderText(null);
        login.setContentText("Username:");
        in = new Scanner(s.getInputStream());
        out = new PrintWriter(s.getOutputStream());
        Optional<String> input = login.showAndWait();
        if (input.isPresent() && input.get().isEmpty()) {
          Dialog<ButtonType> invalidUsername = new Alert(Alert.AlertType.WARNING);
          invalidUsername.setHeaderText("Invalid username");
          invalidUsername.showAndWait();
          continue;
        }
        input.ifPresent(value -> username = value);
        out.println(username);
        out.flush();
        String response = in.nextLine();

        switch (response) {
          case "User not found":
            Dialog<ButtonType> userNotFound = new Alert(Alert.AlertType.CONFIRMATION);
            userNotFound.setTitle("Notice");
            userNotFound.setHeaderText("User not found");
            userNotFound.setContentText("Create a new account with this user name?");
            userNotFound.showAndWait();
            ButtonType rtn = userNotFound.getResult();
            if (rtn == ButtonType.OK) {
              Dialog<String> signUp = new TextInputDialog();
              signUp.setTitle("signUp");
              signUp.setContentText("PassWord");
              Optional<String> signPassword = signUp.showAndWait();
              if (signPassword.isPresent()) {
                if (!signPassword.get().equals("")) {
                  out.println("Yes");
                  out.flush();
                  out.println(signPassword.get());
                  out.flush();
                  break;
                }
              }
            }
            out.println("No");
            out.flush();
            continue;
          case "User has already logged in":
            Dialog<ButtonType> userLogged = new Alert(Alert.AlertType.INFORMATION);
            userLogged.setTitle("Notice");
            userLogged.setHeaderText("User has already logged in");
            userLogged.showAndWait();
            continue;
          case "Yes":
            Dialog<String> enterPassword = new TextInputDialog();
            enterPassword.setHeaderText("Please enter your password");
            enterPassword.setContentText("Password");
            Optional<String> passWord = enterPassword.showAndWait();
            if (passWord.isPresent()) {
              out.println("Yes");
              out.flush();
              out.println(passWord.get());
              out.flush();
              if (in.nextLine().equals("Log success")) {
                break;
              } else {
                Dialog<ButtonType> wrongPassword = new Alert(Alert.AlertType.INFORMATION);
                wrongPassword.setTitle("Notice");
                wrongPassword.setHeaderText("Wrong password");
                wrongPassword.showAndWait();
                continue;
              }
            }
            out.println("No");
            out.flush();
            continue;
        }
        UserName.setText(String.format("User: %s", username));
        String temp;
        onlineUsers = new HashMap<>();
        int isDirExist = 0;
        File readTime = new File(username);
        if (readTime.exists() && readTime.isDirectory()) {
          isDirExist = 1;
        }
        while (!(temp = in.next()).equals("End")) {
          userList.getItems().add(temp);
          long latestTime = 0L;
          if (isDirExist == 1) {
            try {
              InputStream is = Files.newInputStream(Paths.get(String.format("%s/%s.txt", username, temp)));
              BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
              String latest = br.readLine();
              if (latest != null && !latest.equals("")) {
                latestTime = Long.parseLong(latest);
              }
            } catch (IOException e) {
              System.out.println("initial read latest time IO error");
            }
          }
          onlineUsers.put(temp, latestTime);
        }
        userListSort();
        Thread receiveCommand = new Thread(() -> {
          String[] command;
          String origin;
          while (true) {
            if (in.hasNext()) {
              origin = in.nextLine();
            } else {
              Platform.runLater(() -> {
                Dialog<ButtonType> temp1 = new Alert(Alert.AlertType.ERROR);
                temp1.setHeaderText("Server Network Error");
                temp1.showAndWait();
                Platform.exit();
                System.exit(0);
              });
              break;
            }
            if (origin.equals("File")) {
              String fileName = in.nextLine();
              int fileLength = Integer.parseInt(in.nextLine());
              byte[] buffer = new byte[fileLength];
              try {
                InputStream inputStream = s.getInputStream();
                inputStream.read(buffer);
                File file = new File(fileName);
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(buffer);
                fileOutputStream.flush();
                fileOutputStream.close();
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            } else {
              command = origin.split("_");
              System.out.println(command[0]);
              HashSet<String> anoUsers;
              switch (command[0]) {
                case "deleteUser":
                  String[] finalCommand = command;
                  Platform.runLater(() -> {
                    userList.getItems().remove(finalCommand[1]);
                    if (privateChats.containsKey(finalCommand[1]) && privateChats.get(finalCommand[1]).stage.isShowing()) {
                      Dialog<ButtonType> offLine = new Alert(Alert.AlertType.INFORMATION);
                      offLine.setHeaderText(String.format("User %s disconnected", finalCommand[1]));
                      offLine.showAndWait();
                      privateChats.get(finalCommand[1]).stage.close();
                      privateChats.remove(finalCommand[1]);
                    }
                    for (int i = 2; i < finalCommand.length; i++) {
                      if (groupChats.containsKey(Integer.parseInt(finalCommand[i]))) {
                        groupChats.get(Integer.parseInt(finalCommand[i])).deleteGroupUser(finalCommand[1]);
                      }
                    }
                  });
                  break;
                case "addUser":
                  String finalCommand2 = command[1];
                  File readTime1 = new File(username);
                  long latestTime = 0L;
                  if (readTime1.exists() && readTime1.isDirectory()) {
                    System.out.println("folder exist");
                    try {
                      InputStream is = Files.newInputStream(Paths.get(String.format("%s/%s.txt", username, finalCommand2)));
                      BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                      String temp1 = br.readLine();
                      if (temp1 != null && !temp1.equals("")) {
                        latestTime = Long.parseLong(temp1);
                      }
                    } catch (IOException e) {
                      System.out.println("addUser IO error");
                    }
                  }
                  System.out.printf("new User's latestTime: %s\n", latestTime);
                  onlineUsers.put(finalCommand2, latestTime);
                  Platform.runLater(() -> {
                    userList.getItems().add(finalCommand2);
                    userListSort();
                  });
                  break;
                case "askPrivate":
                  String anoUser = command[1];
                  Platform.runLater(() -> {
                    if (privateChats.containsKey(anoUser)) {
                      privateChats.get(anoUser).stage.show();
                      privateChats.get(anoUser).stage.setAlwaysOnTop(true);
                      privateChats.get(anoUser).stage.setAlwaysOnTop(false);
                      out.printf("askPrivateYes_%s\n", anoUser);
                      out.flush();
                    } else {
                      Dialog<ButtonType> askPrivate = new Alert(Alert.AlertType.CONFIRMATION);
                      askPrivate.setTitle("Private chat request");
                      askPrivate.setHeaderText(String.format("%s want to have a private chat with you", anoUser));
                      askPrivate.showAndWait();
                      ButtonType rtn = askPrivate.getResult();
                      if (rtn == ButtonType.OK) {
                        out.printf("askPrivateYes_%s\n", anoUser);
                        out.flush();
                        HashSet<String> temp1 = new HashSet<>();
                        temp1.add(anoUser);
                        createChatRoom(temp1, "Private", -1);
                      } else {
                        out.println("askPrivateNo");
                        out.flush();
                      }
                    }
                  });
                  break;
                case "askPrivateYes":
                  String user = command[1];
                  anoUsers = new HashSet<>();
                  anoUsers.add(user);
                  HashSet<String> finalAnoUsers = anoUsers;
                  Platform.runLater(() -> createChatRoom(finalAnoUsers, "Private", -1));
                  break;
                case "CreateGroupChat":
                  anoUsers = new HashSet<>(Arrays.asList(command).subList(1, command.length - 1));
                  HashSet<String> finalAnoUsers1 = anoUsers;
                  int tempGroupId = Integer.parseInt(command[command.length - 1]);
                  Platform.runLater(() -> createChatRoom(finalAnoUsers1, "Group", tempGroupId));
                  break;
                case "sendMsg":
                  privateChats.get(command[1]).receiveMsg(command[1], command[2]);
                  break;
                case "sendMsg2":
                  privateChats.get(command[1]).receiveMsg2(command[1], command[2]);
                  break;
                case "SendGroupMsg":
                  groupChats.get(Integer.parseInt(command[1])).receiveMsg(command[2], command[3]);
                  break;
                case "SendGroupMsg2":
                  groupChats.get(Integer.parseInt(command[1])).receiveMsg2(command[2], command[3]);
                  break;
                case "GroupClose":
                  System.out.println(username + " receive GroupClose");
                  groupChats.get(Integer.parseInt(command[1])).deleteGroupUser(command[2]);
                  break;
              }
            }
          }
        });
        receiveCommand.start();
        break;
      }
    } catch (IOException e) {
      Dialog<ButtonType> networkError = ExceptionWindow.netWorkError();
      networkError.showAndWait();
      Platform.exit();
      System.exit(0);
    }
  }

  public void userListSort() {
    Platform.runLater(() -> userList.getItems().sort((b, a) -> onlineUsers.get(a).compareTo(onlineUsers.get(b))));
  }

  @FXML
  public void createPrivateChat() {
    Stage stage = new Stage();
    ComboBox<String> userSel = new ComboBox<>();

    // FIXME: get the user list from server, the current user's name should be filtered out
    for (String x : userList.getItems()) {
      if (!x.equals(username)) {
        userSel.getItems().add(x);
      }
    }
    Button okBtn = new Button("OK");
    okBtn.setOnAction(e -> {
      if (userSel.getSelectionModel().getSelectedItem() != null) {
        if (!privateChats.containsKey(userSel.getSelectionModel().getSelectedItem())) {
          out.printf("CreatePrivate_%s\n", userSel.getSelectionModel().getSelectedItem());
          out.flush();
        } else {
          ChatRoom tempChatRoom = privateChats.get(userSel.getSelectionModel().getSelectedItem());
          Stage temp = tempChatRoom.stage;
          temp.show();
          temp.setAlwaysOnTop(true);
          temp.setAlwaysOnTop(false);
        }
        stage.close();
      }
    });

    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(userSel, okBtn);
    stage.setScene(new Scene(box));
    stage.showAndWait();
  }

  public void createChatRoom(HashSet<String> anoUsers, String type, int groupChatId) {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("chatRoom.fxml"));
    Stage chatStage = new Stage();
    anoUsers.remove(username);
    ChatRoom chatRoom = new ChatRoom(s, this, out, chatStage, username, type, anoUsers);
    loader.setController(chatRoom);

    try {
      chatStage.setScene(new Scene(loader.load()));
      chatStage.show();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (type.equals("Private")) {
      String talkTo = anoUsers.iterator().next();
      privateChats.put(talkTo, chatRoom);
      chatStage.setOnCloseRequest(windowEvent -> chatStage.hide());
    } else {
      chatStage.setOnCloseRequest(windowEvent -> {
        groupChats.remove(groupChatId);
        StringBuilder sendGroupClose = new StringBuilder("GroupClose_");
        sendGroupClose.append(groupChatId);
        for (String x : anoUsers) {
          sendGroupClose.append("_").append(x);
        }
        out.println(sendGroupClose);
        out.flush();
        chatStage.close();
      });
      groupChats.put(groupChatId, chatRoom);
      chatRoom.setGroupChatRoomId(groupChatId);
    }
  }

  @FXML
  public void createGroupChat() {
    HashSet<String> joinUsers = new HashSet<>();
    joinUsers.add(username);
    int checkBoxCnt = 0;
    Stage stage = new Stage();
    HBox box = new HBox(10);
    for (String x : userList.getItems()) {
      if (!x.equals(username)) {
        box.getChildren().add(new CheckBox(x));
        checkBoxCnt++;
      }
    }
    Button okBtn = new Button("OK");
    int finalCheckBoxCnt = checkBoxCnt;
    okBtn.setOnAction(e -> {
      if (finalCheckBoxCnt != 0) {
        for (Node x : box.getChildren()) {
          if (x.getClass().equals(CheckBox.class) && ((CheckBox) x).isSelected()) {
            joinUsers.add(((CheckBox) x).getText());
          }
        }
        StringBuilder groupSend = new StringBuilder("CreateGroupChat");
        for (String x : joinUsers) {
          groupSend.append("_").append(x);
        }
        out.println(groupSend);
        out.flush();
        stage.close();
      }
    });
    box.getChildren().add(okBtn);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(40, 40, 40, 40));
    stage.setScene(new Scene(box));
    stage.showAndWait();

  }
}
