package cn.edu.sustech.cs209.chatting.client;

import javafx.scene.control.Alert;

public class exceptionWindow {
  public static Alert netWorkError(){
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setContentText("Network Error");
    return alert;
  }
}
