package kg.attractor.java.model;

import java.util.UUID;

public class Candidate {
  private String id;
  private String name;
  private String photo;
  private int votes;

  public Candidate() {
  }

  public Candidate(String name, String photo) {
    this.id = UUID.randomUUID().toString();
    this.name = name;
    this.photo = photo;
    this.votes = 0;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPhoto() {
    return photo;
  }

  public void setPhoto(String photo) {
    this.photo = photo;
  }

  public int getVotes() {
    return votes;
  }

  public void addVote() {
    this.votes++;
  }

  public int getPercent(int totalVotes) {
    if (totalVotes == 0) return 0;
    return (int) Math.round(((double) votes / totalVotes) * 100);
  }
}
