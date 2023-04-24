package cn.edu.sustech.cs209.chatting.common;

public class User {
  public String userName=null;
  private String passWord="000000";

  public User(String userName, String passWord) {
    this.userName = userName;
    this.passWord = passWord;
  }

  @Override
  public boolean equals(Object obj) {
    boolean temp=false;
    if (obj!=null&&obj.getClass()== User.class){
      if (((User) obj).userName.equals(userName)){
        temp=true;
      }
    }
    return temp;
  }

  @Override
  public int hashCode() {
    return userName.hashCode();
  }

  @Override
  public String toString() {
    return this.userName;
  }

  public String getUserName() {
    return userName;
  }

  public String getPassWord() {
    return passWord;
  }
}
