package kg.attractor.java;

import kg.attractor.java.server.BasicServer;

import java.io.IOException;

public class Main {
  public static void main(String[] args) {
    try {
      new BasicServer("localhost", 9889).start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
