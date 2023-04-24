package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.User;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
  public static int groupChatId=0;
  static ConcurrentHashMap<String,Service> services;
  static HashMap<String,String> users;
  static HashSet<String> onlineUsers;
  static FileWriter fw;
  public static void main(String[] args) {
    System.out.println("Starting server");
    initialize();
    ServerSocket serverSocket = null;
    try {
      serverSocket = new ServerSocket(1234);
    } catch (IOException e) {
      System.out.println("serverSocket fails to start");
    }
    while (true) {
      try {
        Socket s = serverSocket.accept();
        Service service = new Service(s);
        Thread t = new Thread(service);
        t.start();
      } catch (IOException e) {
        System.out.println("User disconnected!");
      }
    }
  }

  public static void initialize() {
    services=new ConcurrentHashMap<>();
    users = new HashMap<>();
    onlineUsers=new HashSet<>();
    try {
      fw=new FileWriter("users.txt",true);
      InputStream is = new FileInputStream("users.txt");
      BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
      String temp = null;
      while ((temp = br.readLine()) != null) {
        String[] info = temp.split(";");
        users.put(info[0],info[1]);
      }
    } catch (FileNotFoundException e) {
      File save = new File("users.txt");
      try {
        save.createNewFile();
      } catch (IOException ex) {
        System.out.println("create New File failed");
      }
    } catch (IOException e) {
      System.out.println("read users.txt file failed");
    }
  }
}

class Service implements Runnable {
  private Socket s = null;
  private String userName = null;
  private Scanner in;
  public PrintWriter out;
  private int userCnt=0;
  HashSet<Integer> groupChatRooms=new HashSet<>();
  HashSet<String> localUserlist=new HashSet<>();
  public Service(Socket s) {
    this.s = s;
    userName = String.valueOf(s.getRemoteSocketAddress());
  }

  @Override
  public void run() {
    try {
      try {
        in = new Scanner(s.getInputStream());
        out = new PrintWriter(s.getOutputStream());
        doService();
      } finally {
        System.out.printf("User %s close\n",userName);
        Server.services.remove(userName);
        Server.onlineUsers.remove(userName);
        for (Service x:Server.services.values()){
          x.sendDeleteUser(userName,groupChatRooms);
        }
        s.close();
      }
    } catch (IOException e) {
      System.out.printf("User %s disconnected!\n", userName);
    }
  }

  public void doService() throws IOException{
try{
  while(true) {
    System.out.println("wait for in");
    userName = in.next();
    if (!(Server.users.containsKey(userName))) {
      System.out.println("User not found");
      out.println("User not found");
      out.flush();
      if (in.next().equals("Yes")){
        System.out.println("Enter password");
      User signIn = new User(userName,in.next());
      Server.users.put(userName, signIn.getPassWord());
      Server.fw.write(String.format("%s;%s\n",userName,signIn.getPassWord()));
      Server.fw.flush();
      Server.onlineUsers.add(userName);
      System.out.println(Server.onlineUsers.contains(userName));
      break;
      }
    } else if (Server.onlineUsers.contains(userName)) {
      System.out.println("User has already logged in");
      out.println("User has already logged in");
      out.flush();
    } else  {
      out.println("Yes");
      out.flush();
      if (in.next().equals("Yes")){
      String clientPassword=in.next();
      if (!Server.users.get(userName).equals(clientPassword)) {
        System.out.println("Wrong password");
        out.println("Wrong password");
        out.flush();
      }
    else if(Server.users.get(userName).equals(clientPassword)){
      out.println("Log success");
      out.flush();
      break;
    }
    }
    }
  }
  for (Service x:Server.services.values()){
    x.sendAddUser(userName);
  }
  Server.services.put(userName,this);
    Server.onlineUsers.add(userName);
  Iterator onlineUserIt=Server.onlineUsers.iterator();
  while(onlineUserIt.hasNext()){
    out.println(onlineUserIt.next());
    out.flush();
  }
  out.println("End");
  out.flush();
  userCnt=Server.onlineUsers.size();
  localUserlist.addAll(Server.onlineUsers);


    while (true) {
      if (!in.hasNext()) {
        return;
      }
      String origin=in.nextLine();
      String[] command = origin.split("_");
      if ("QUIT".equals(command[0])) {
        return;
      }
      String anoUser="";
      switch (command[0]) {
        case "CreatePrivate":
          anoUser=command[1];
          Server.services.get(anoUser).askPrivate(userName);
          break;
        case "askPrivateYes":
           anoUser=command[1];
          Server.services.get(anoUser).sendAgreePrivate(userName);
          break;
        case "SendPrivateMsg":
          anoUser=command[1];
          Server.services.get(anoUser).sendMsg(userName,command[2]);
          break;
        case "SendPrivateMsg2":
          anoUser=command[1];
          Server.services.get(anoUser).sendMsg2(userName,command[2]);
          break;
        case "CreateGroupChat":
          Server.groupChatId++;
          origin+="_"+ Server.groupChatId;
          for (int i=1;i<command.length;i++){
            Server.services.get(command[i]).sendGroupChat(origin);
            Server.services.get(command[i]).addGroupRoom(Server.groupChatId);
          }
          break;
        case "SendGroupMsg":
          for (int i=2;i<command.length-1;i++){
            Server.services.get(command[i]).sendGroupMsg(command[1],userName,command[command.length-1]);
          }
          break;
        case "SendGroupMsg2":
          for (int i=2;i<command.length-1;i++){
            Server.services.get(command[i]).sendGroupMsg2(command[1],userName,command[command.length-1]);
          }
          break;
        case "GroupClose":
          groupChatRooms.remove(Integer.parseInt(command[1]));
          for (int i=2;i<command.length;i++){
            Server.services.get(command[i]).sendGroupClose(command[1],userName);
          }
      }
    }
  } catch (NoSuchElementException e) {
  System.out.println("User disconnected");
}
  }
  public void addGroupRoom(Integer id){
    groupChatRooms.add(id);
  }
  public void sendGroupClose(String id,String delete){
    out.println(String.format("GroupClose_%s_%s",id,delete));
    out.flush();
  }
  public void sendGroupMsg(String id,String sender,String data){
    out.println(String.format("SendGroupMsg_%s_%s_%s",id,sender,data));
    out.flush();
  }
  public void sendGroupMsg2(String id,String sender,String data){
    out.println(String.format("SendGroupMsg2_%s_%s_%s",id,sender,data));
    out.flush();
  }
  public void sendAddUser(String userName){
    out.printf("addUser_%s\n",userName);
    out.flush();
  }
  public void sendDeleteUser(String userName,HashSet<Integer> groupChatRooms){
    StringBuilder temp=new StringBuilder("deleteUser_");
        temp.append(userName);
        for (Integer x:groupChatRooms){
          temp.append("_").append(x);
        }
    out.println(temp);
    out.flush();
  }
  public void askPrivate(String userName){
    out.printf("askPrivate_%s\n",userName);
    out.flush();
  }
  public void sendAgreePrivate(String userName){
    out.printf("askPrivateYes_%s\n",userName);
    out.flush();
  }
  public void sendGroupChat(String info){
    out.println(info);
    out.flush();
  }
  public void sendMsg(String userName,String data){
    out.printf("sendMsg_%s_%s\n",userName,data);
    out.flush();
  }
  public void sendMsg2(String userName,String data){
    out.printf("sendMsg2_%s_%s\n",userName,data);
    out.flush();
  }
}



