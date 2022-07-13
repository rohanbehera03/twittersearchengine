package com.tweet.lucene;

public class Tweet {
	private long id;
	private long user_id;
	private String device;
	private String created_at;
	private String text;
	private long likes;
	private long dislikes;
	private long totalHits;
	private float score;
	public long getTotalHits() {
		return totalHits;
	}

	
	public void setTotalHits(long totalHits) {
		this.totalHits = totalHits;
	}
	public long getId() {
		return id;
	}
	public long getUser_id() {
		return user_id;
	}
	public String getDevice() {
		return device;
	}
	public String getCreated_at() {
		return created_at;
	}
	public String getText() {
		return text;
	}
	public long getLikes() {
		return likes;
	}
	public long getDislikes() {
		return dislikes;
	}
	public float getScore() {
		return score;
	}
	public void setId(long id) {
		this.id = id;
	}
	public void setUser_id(long user_id) {
		this.user_id = user_id;
	}
	public void setDevice(String device) {
		this.device = device;
	}
	public void setCreated_at(String created_at) {
		this.created_at = created_at;
	}
	public void setText(String text) {
		this.text = text;
	}
	public void setLikes(long likes) {
		this.likes = likes;
	}
	public void setDislikes(long dislikes) {
		this.dislikes = dislikes;
	}
	public void setScore(float score) {
		this.score = score;
	}
	
}
