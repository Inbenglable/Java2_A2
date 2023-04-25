package cn.edu.sustech.cs209.chatting.client;

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

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

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
        if (input.get().isEmpty()) {
          Dialog invalidUsername = new Alert(Alert.AlertType.WARNING);
          invalidUsername.setHeaderText("Invalid username");
          invalidUsername.showAndWait();
          continue;
        }
            /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
             */
        username = input.get();
        out.println(username);
        out.flush();
        String response = in.nextLine();

        switch (response) {
          case "User not found":
            Dialog userNotFound = new Alert(Alert.AlertType.CONFIRMATION);
            userNotFound.setTitle("Notice");
            userNotFound.setHeaderText("User not found");
            userNotFound.setContentText("Create a new account with this user name?");
            userNotFound.showAndWait();
            ButtonType rtn = (ButtonType) userNotFound.getResult();
            if (rtn == ButtonType.OK) {
              Dialog signUp = new TextInputDialog();
              signUp.setTitle("signUp");
              signUp.setContentText("PassWord");
              Optional signPassword = signUp.showAndWait();
              if (signPassword.isPresent() && signPassword.get() != null) {
                signPassword.get();
                out.println("Yes");
                out.flush();
                out.println(signPassword.get());
                out.flush();
                break;
              }
            }
            out.println("No");
            out.flush();
            continue;
          case "User has already logged in":
            Dialog userLogged = new Alert(Alert.AlertType.INFORMATION);
            userLogged.setTitle("Notice");
            userLogged.setHeaderText("User has already logged in");
            userLogged.showAndWait();
            continue;
          case "Yes":
            Dialog enterPassword = new TextInputDialog();
            enterPassword.setHeaderText("Please enter your password");
            enterPassword.setContentText("Password");
            Optional passWord = enterPassword.showAndWait();
            if (passWord.isPresent()) {
              out.println("Yes");
              out.flush();
              out.println(passWord.get());
              out.flush();
              if (in.nextLine().equals("Log success")) {
                break;
              } else {
                Dialog wrongPassword = new Alert(Alert.AlertType.INFORMATION);
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
        int isDirExist=0;
        File readTime=new File(username);
        if (readTime.exists()&&readTime.isDirectory()){
          isDirExist=1;
        }
        while (!(temp = in.next()).equals("End")) {
          userList.getItems().add(temp);
          long latestTime=0L;
          if (isDirExist==1){
            try {
              InputStream is = Files.newInputStream(Paths.get(String.format("%s/%s.txt",username,temp)));
              BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
              String latest=br.readLine();
              if (latest!=null&&!latest.equals("")){
                latestTime=Long.parseLong(latest);
              }
            } catch (IOException e) {
              System.out.println("initial read latest time IO error");
            }
          }
          onlineUsers.put(temp,latestTime);
        }
        userListSort();
        Thread receiveCommand = new Thread(new Runnable() {
          @Override
          public void run() {
            String[] command;
            String origin="";
            while (true) {
              if (in.hasNext()) {
                origin=in.nextLine();
              } else {
                Platform.runLater(new Runnable() {
                  @Override
                  public void run() {
                    Dialog temp=new Alert(Alert.AlertType.ERROR);
                    temp.setHeaderText("Server Network Error");
                    temp.showAndWait();
                    Platform.exit();
                    System.exit(0);
                  }
                });
                break;
              }
              if (origin.equals("File")){
                String fileName=in.nextLine();
//                StringBuilder fileSb=new StringBuilder();
                int fileLength=Integer.parseInt(in.nextLine());

//                while(!fileTemp.equals("@FileEnd")){
//                  fileSb.append(fileTemp);
//                  fileTemp=in.nextLine();
//                  if (!fileTemp.equals("@FileEnd")){
//                    fileSb.append('\n');
//                  }
//                }
                byte[] buffer=new byte[fileLength];
                try {
                  InputStream inputStream=s.getInputStream();
                  inputStream.read(buffer);
//                  String correct=new String(fileName.getBytes(), Charset.forName("GBK"));
                  File file=new File(fileName);
                  FileOutputStream fileOutputStream=new FileOutputStream(file);
                  fileOutputStream.write(buffer);
                  fileOutputStream.flush();
                  fileOutputStream.close();
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              } else{
              command = origin.split("_");
              System.out.println(command[0]);
              HashSet<String> anoUsers = new HashSet<>();
              switch (command[0]) {
                case "deleteUser":
                  String[] finalCommand = command;
                  Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                      userList.getItems().remove(finalCommand[1]);
                      if (privateChats.containsKey(finalCommand[1]) && privateChats.get(finalCommand[1]).stage.isShowing()) {
                        Dialog offLine = new Alert(Alert.AlertType.INFORMATION);
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
                    }
                  });
                  break;
                case "addUser":
                  String finalCommand2 = command[1];
                  File readTime=new File(username);
                  long latestTime=0L;
                  if (readTime.exists()&&readTime.isDirectory()){
                    System.out.println("folder exist");
                  try {
                    InputStream is = Files.newInputStream(Paths.get(String.format("%s/%s.txt",username,finalCommand2)));
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    String temp=br.readLine();
                    if (temp!=null&&!temp.equals("")){
                      latestTime=Long.parseLong(temp);
                    }
                  } catch (IOException e) {
                    System.out.println("addUser IO error");
                  }
                  }
                  System.out.printf("new User's latestTime: %s\n", latestTime);
                  onlineUsers.put(finalCommand2,latestTime);
                  Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                      userList.getItems().add(finalCommand2);
                      userListSort();
                    }
                  });
                  break;
                case "askPrivate":
                  String anoUser = command[1];
                  Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                      if (privateChats.containsKey(anoUser)){
                        privateChats.get(anoUser).stage.show();
                        privateChats.get(anoUser).stage.setAlwaysOnTop(true);
                        privateChats.get(anoUser).stage.setAlwaysOnTop(false);
                        out.printf("askPrivateYes_%s\n", anoUser);
                        out.flush();
                      }
                      else{
                      Dialog askPrivate = new Alert(Alert.AlertType.CONFIRMATION);
                      askPrivate.setTitle("Private chat request");
                      askPrivate.setHeaderText(String.format("%s want to have a private chat with you", anoUser));
                      askPrivate.showAndWait();
                      ButtonType rtn = (ButtonType) askPrivate.getResult();
                      if (rtn == ButtonType.OK) {
                        out.printf("askPrivateYes_%s\n", anoUser);
                        out.flush();
                        HashSet<String> temp = new HashSet<>();
                        temp.add(anoUser);
                        createChatRoom(temp, "Private", -1);
                      } else {
                        out.println("askPrivateNo");
                        out.flush();
                      }
                      }
                    }
                  });
                  break;
                case "askPrivateYes":
                  String user = command[1];
                  anoUsers = new HashSet<>();
                  anoUsers.add(user);
                  HashSet<String> finalAnoUsers = anoUsers;
                  Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                      createChatRoom(finalAnoUsers, "Private", -1);
                    }
                  });
                  break;
                case "CreateGroupChat":
                  anoUsers = new HashSet<>();
                  for (int i = 1; i < command.length - 1; i++) {
                    anoUsers.add(command[i]);
                  }
                  HashSet<String> finalAnoUsers1 = anoUsers;
                  int tempGroupId = Integer.parseInt(command[command.length - 1]);
                  Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                      createChatRoom(finalAnoUsers1, "Group", tempGroupId);
                    }
                  });
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
          }
        });
        receiveCommand.start();
        break;
      }
    } catch (IOException e) {
      Dialog networkError = exceptionWindow.netWorkError();
      networkError.showAndWait();
      Platform.exit();
      System.exit(0);
    }
  }
  public void userListSort(){
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        userList.getItems().sort((b, a) -> {
          if (onlineUsers.get(a) > onlineUsers.get(b)) {
            return 1;
          }
          else if (onlineUsers.get(a)== onlineUsers.get(b)){
            return 0;
          }
          else {
            return -1;
          }
        });
      }
    });
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

    // TODO: if the current user already chatted with the selected user, just open the chat with that user
    // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
  }

  public void createChatRoom(HashSet<String> anoUsers, String type, int groupChatId) {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("chatRoom.fxml"));
    Stage chatStage = new Stage();
    anoUsers.remove(username);
    ChatRoom chatRoom = new ChatRoom(s, this, in, out, chatStage, username, type, anoUsers);
    loader.setController(chatRoom);

    try {
      chatStage.setScene(new Scene(loader.load()));
      chatStage.show();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (type.equals("Private")) {
      String talkTo=anoUsers.iterator().next();
      privateChats.put(talkTo, chatRoom);
      chatStage.setOnCloseRequest(windowEvent -> {
        chatStage.hide();

      });
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

  /**
   * A new dialog should contain a multi-select list, showing all user's name.
   * You can select several users that will be joined in the group chat, including yourself.
   * <p>
   * The naming rule for group chats is similar to WeChat:
   * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
   * UserA, UserB, UserC... (10)
   * If there are <= 3 users: do not display the ellipsis, for example:
   * UserA, UserB (2)
   */
  @FXML
  public void createGroupChat() {
    HashSet<String> joinUsers = new HashSet<>();
    joinUsers.add(username);
    int checkBoxCnt = 0;
    Stage stage = new Stage();
    HBox box = new HBox(10);
    // FIXME: get the user list from server, the current user's name should be filtered out
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

  /**
   * Sends the message to the <b>currently selected</b> chat.
   * <p>
   * Blank messages are not allowed.
   * After sending the message, you should clear the text input field.
   */

}
