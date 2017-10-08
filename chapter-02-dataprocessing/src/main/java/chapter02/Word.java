package chapter02;

public class Word {
	
	private final String token;
	
	private final String pos;

	public Word(String token, String pos) {
		super();
		this.token = token;
		this.pos = pos;
	}

	public String getToken() {
		return token;
	}

	public String getPos() {
		return pos;
	}

	@Override
	public String toString() {
		return "Word [token=" + token + ", pos=" + pos + "]";
	}
	
}
